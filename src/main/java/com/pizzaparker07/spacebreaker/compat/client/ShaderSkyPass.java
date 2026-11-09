package com.pizzaparker07.spacebreaker.compat.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.Compat;
import com.pizzaparker07.spacebreaker.compat.CompatConfig;
import com.pizzaparker07.spacebreaker.compat.SpaceWorlds;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

/**
 * Shader-pack sky replacement for space worlds.
 *
 * <p>Iris decides which program set of a shader pack to use from the dimension id; unknown modded dimensions get
 * the Overworld programs, which paint their own atmosphere (blue/brown sky, sun glow, volumetric clouds) over every
 * pixel that has no geometry, and the sky our {@link DimensionSpecialEffects} draws is lost. Iris renders the
 * world into Minecraft's own depth buffer, which still holds the scene depth after the pack's final pass. So, once
 * the pack has finished the frame, this pass draws our normal sky (unchanged vanilla sky code) into an off-screen
 * target and copies it onto the frame where the depth is still the cleared far value: only the sky pixels. Terrain
 * keeps the pack's lighting and shadows (the Overworld program follows the world's own sun angle), the sky is ours.
 */
public final class ShaderSkyPass {
    private static TextureTarget target;
    private static boolean afterSkyStage;

    /** True while AFTER_SKY is replayed into our off-screen sky (listeners may want to know). */
    public static boolean replayingAfterSky() {
        return afterSkyStage;
    }
    private static net.minecraft.client.renderer.ShaderInstance composite;

    static void setCompositeShader(net.minecraft.client.renderer.ShaderInstance shader) {
        composite = shader;
    }

    private static net.minecraft.client.renderer.ShaderInstance glass;
    static void setGlassShader(net.minecraft.client.renderer.ShaderInstance shader) {
        glass = shader;
    }

    private static final DepthCopy BLOCKS_DEPTH = new DepthCopy();
    private static boolean blocksDepthCaptured;

    /**
     * Called at AFTER_TRANSLUCENT_BLOCKS: the depth with glass and water but without translucent entities and
     * effects (rocket plumes, glows), which Iris draws later. Only translucent blocks get the sky blended behind them.
     */
    public static void captureTranslucentBlocksDepth() {
        blocksDepthCaptured = false;
        if (glass == null || !active()) {
            return;
        }
        try {
            BLOCKS_DEPTH.copyFrom(Minecraft.getInstance().getMainRenderTarget());
            blocksDepthCaptured = true;
        } catch (RuntimeException e) {
            if (!warned) {
                warned = true;
                Spacebreaker.LOGGER.error("Space sky over the shader pack: depth copy failed", e);
            }
        }
    }

    private static boolean warned;
    private static final PassTimer TIMER = new PassTimer("sky pass");

    private ShaderSkyPass() {
    }

    /**
     * Set by features that turn the sky of a world that normally keeps the pack's sky (the Overworld) into space for a
     * while, such as rocketry's seamless ascent and descent: while it answers true, the pack's sky and clouds are
     * replaced there too, so the view climbs into the same space sky the orbit shows. Called once a frame.
     */
    public static volatile java.util.function.BooleanSupplier spaceView = () -> false;

    private static boolean spaceViewNow() {
        try {
            return spaceView.getAsBoolean();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Whether the pack's sky must be replaced in the current world right now. */
    public static boolean active() {
        CompatConfig.SkyMode mode = CompatConfig.skyMode();
        if (mode == CompatConfig.SkyMode.OFF) {
            return false;
        }
        if (mode == CompatConfig.SkyMode.AUTO && Compat.lodModInstalled()
                && (Compat.isInstalled("voxy") || composite == null || !LodDepth.available())) {
            return false; // LOD terrain we cannot see in a depth buffer would be painted over
        }
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && (SpaceSky.kind(level).ownSky() || spaceViewNow()) && ShaderState.shaderPackInUse();
    }

    /**
     * Called at NeoForge's AFTER_LEVEL stage, which GameRenderer fires after LevelRenderer.renderLevel has returned,
     * i.e. after Iris has run the pack's composite and final passes, and before the depth buffer is cleared for the
     * hand.
     */
    public static void afterLevel(Matrix4f modelView, Matrix4f projection, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !active()) {
            return;
        }
        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera.getFluidInCamera() != FogType.NONE) {
            return; // under water / in lava the pack's fog is right
        }
        try {
            TIMER.begin();
            try {
                draw(mc, level, camera, modelView, projection, partialTick);
            } finally {
                TIMER.end();
            }
        } catch (RuntimeException e) {
            if (!warned) {
                warned = true;
                Spacebreaker.LOGGER.error("Space sky over the shader pack failed; leaving the pack's sky", e);
            }
            GL11.glDepthRange(0.0, 1.0);
        }
    }

    private static void draw(Minecraft mc, ClientLevel level, Camera camera, Matrix4f modelViewMatrix, Matrix4f projection,
                             float partialTick) {
        RenderTarget main = mc.getMainRenderTarget();
        if (target == null) {
            // with a depth buffer: sky bodies drawn as blocks (voxel planets) can depth-test among themselves
            target = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        } else if (target.width != main.width || target.height != main.height) {
            target.resize(main.width, main.height, Minecraft.ON_OSX);
        }

        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSorting = RenderSystem.getVertexSorting();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();

        // 1) our sky, exactly as without shaders, into the off-screen target
        target.setClearColor(0F, 0F, 0F, 1F);
        // glClear leaves depth alone while depth writes are off (as they are once the pack has finished), which left
        // this depth at its initial 0 and every depth-tested sky body behind it
        RenderSystem.depthMask(true);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.setProjectionMatrix(new Matrix4f(projection), VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        DimensionSpecialEffects effects = level.effects();
        effects.renderSky(level, (int) level.getGameTime(), partialTick, new Matrix4f(modelViewMatrix), camera,
                new Matrix4f(projection), false, () -> {
        });
        // everything other code draws "right after the sky" (planet dots, ascent darkening...) belongs to the sky too:
        // without this it would be painted over on our sky pixels
        afterSkyStage = true;
        try {
            net.neoforged.neoforge.client.ClientHooks.dispatchRenderStage(
                    net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_SKY, mc.levelRenderer, null,
                    new Matrix4f(modelViewMatrix), new Matrix4f(projection), mc.levelRenderer.getTicks(), camera, mc.levelRenderer.getFrustum());
        } finally {
            afterSkyStage = false;
        }
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        // 2) copy it onto the finished frame where nothing was drawn (depth still 1.0)
        main.bindWrite(true);
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        GL11.glDepthRange(1.0, 1.0);
        int lod = LodDepth.texture();
        if (lod > 0 && composite != null) {
            // Distant Horizons: LOD terrain is only in DH's depth buffer, so the shader keeps it
            RenderSystem.setShader(() -> composite);
            RenderSystem.setShaderTexture(0, target.getColorTextureId());
            RenderSystem.setShaderTexture(1, lod);
            composite.safeGetUniform("LodFar").set(LodDepth.farDepth());
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            b.addVertex(-1F, -1F, 0F);
            b.addVertex(1F, -1F, 0F);
            b.addVertex(1F, 1F, 0F);
            b.addVertex(-1F, 1F, 0F);
            BufferUploader.drawWithShader(b.buildOrThrow());
        } else {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, target.getColorTextureId());
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            b.addVertex(-1F, -1F, 0F).setUv(0F, 0F);
            b.addVertex(1F, -1F, 0F).setUv(1F, 0F);
            b.addVertex(1F, 1F, 0F).setUv(1F, 1F);
            b.addVertex(-1F, 1F, 0F).setUv(0F, 1F);
            BufferUploader.drawWithShader(b.buildOrThrow());
        }
        int opaqueDepth = glass == null || !blocksDepthCaptured ? 0 : ShaderState.opaqueDepthTexture();
        blocksDepthCaptured = false;
        if (opaqueDepth > 0) {
            // 3) behind translucent surfaces: where only glass/water covers the sky (Glass and water write depth;
            // the pack's depth from just before its translucent pass tells them apart from terrain and entities.)
            RenderSystem.depthFunc(GL11.GL_GREATER);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(() -> glass);
            RenderSystem.setShaderTexture(0, target.getColorTextureId());
            RenderSystem.setShaderTexture(1, opaqueDepth);
            RenderSystem.setShaderTexture(2, BLOCKS_DEPTH.texture());
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            b.addVertex(-1F, -1F, 0F);
            b.addVertex(1F, -1F, 0F);
            b.addVertex(1F, 1F, 0F);
            b.addVertex(-1F, 1F, 0F);
            BufferUploader.drawWithShader(b.buildOrThrow());
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }
        GL11.glDepthRange(0.0, 1.0);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    /** Frees the off-screen target (world unload). */
    public static void release() {
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
        BLOCKS_DEPTH.release();
        blocksDepthCaptured = false;
    }

    /**
     * Sky colour handed to shader packs in airless worlds. Packs light the ground with their own sun colour plus an
     * ambient fill derived from Iris's skyColor uniform; with the black biome sky the fill was zero. How strongly a
     * pack uses it differs: Complementary adds pow(skyColor, 0.75) to a warm, atmosphere-filtered sun and needs a
     * clearly blue fill to bring grey regolith back to neutral; MakeUp mixes 60 % of skyColor into its (already
     * neutral) ambient and turns lavender with that. BSL does not read it. Tuned per pack family from the
     * screenshots in docs/screenshots/compat; unknown packs get the moderate value.
     */
    private static final Vec3 PACK_SKY_COMPLEMENTARY = new Vec3(0.20, 0.30, 0.72);
    private static final Vec3 PACK_SKY_DEFAULT = new Vec3(0.20, 0.27, 0.55);
    private static String lastPack;
    private static Vec3 lastPackSky = PACK_SKY_DEFAULT;

    /** The sky colour shader packs should see in this world, or null to keep vanilla's. */
    public static Vec3 packSkyColor(ClientLevel level) {
        if (level != Minecraft.getInstance().level || !SpaceSky.kind(level).airless() || !ShaderState.shaderPackInUse()) {
            return null;
        }
        String pack = ShaderState.packName();
        if (!pack.equals(lastPack)) {
            lastPack = pack;
            lastPackSky = pack.contains("complementary") ? PACK_SKY_COMPLEMENTARY : PACK_SKY_DEFAULT;
        }
        return lastPackSky;
    }

    /** True when the shader pack's own clouds should be moved out of sight in this world. */
    public static boolean hidePackClouds(DimensionSpecialEffects effects) {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.effects() == effects && CompatConfig.hidePackClouds()
                && (SpaceSky.kind(level) != SpaceWorlds.Kind.VANILLA || spaceViewNow()) && ShaderState.shaderPackInUse();
    }
}
