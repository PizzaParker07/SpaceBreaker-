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
    private static boolean warned;

    private ShaderSkyPass() {
    }

    /** Whether the pack's sky must be replaced in the current world right now. */
    public static boolean active() {
        CompatConfig.SkyMode mode = CompatConfig.skyMode();
        if (mode == CompatConfig.SkyMode.OFF) {
            return false;
        }
        if (mode == CompatConfig.SkyMode.AUTO && Compat.lodModInstalled()) {
            return false; // LOD terrain is not in Minecraft's depth buffer: it would be painted over
        }
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && SpaceSky.kind(level).ownSky() && ShaderState.shaderPackInUse();
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
            draw(mc, level, camera, modelView, projection, partialTick);
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
            target = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
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
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.addVertex(-1F, -1F, 0F).setUv(0F, 0F);
        b.addVertex(1F, -1F, 0F).setUv(1F, 0F);
        b.addVertex(1F, 1F, 0F).setUv(1F, 1F);
        b.addVertex(-1F, 1F, 0F).setUv(0F, 1F);
        BufferUploader.drawWithShader(b.buildOrThrow());
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
    }

    /**
     * Sky colour handed to shader packs in airless worlds. Packs light the ground with their own sun colour (warm:
     * sunlight seen through air) plus an ambient fill derived from this colour; with the black biome sky the fill
     * was zero and grey regolith looked like sand. A dim blue fill balances the warm sun to an almost neutral white
     * on lit faces while shadows stay dark. Tuned with Complementary Reimagined and checked with BSL.
     */
    private static final Vec3 AIRLESS_PACK_SKY = new Vec3(0.18, 0.30, 0.90);

    /** The sky colour shader packs should see in this world, or null to keep vanilla's. */
    public static Vec3 packSkyColor(ClientLevel level) {
        if (level != Minecraft.getInstance().level || !SpaceSky.kind(level).airless() || !ShaderState.shaderPackInUse()) {
            return null;
        }
        return AIRLESS_PACK_SKY;
    }

    /** True when the shader pack's own clouds should be moved out of sight in this world. */
    public static boolean hidePackClouds(DimensionSpecialEffects effects) {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.effects() == effects && CompatConfig.hidePackClouds()
                && SpaceSky.kind(level) != SpaceWorlds.Kind.VANILLA && ShaderState.shaderPackInUse();
    }
}
