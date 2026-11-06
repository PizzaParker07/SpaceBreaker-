package com.pizzaparker07.spacebreaker.compat.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.SpaceWorlds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * Thick and dusty atmospheres over a shader pack's finished frame.
 *
 * <p>Our worlds express their air through vanilla fog ({@code ViewportEvent.RenderFog / ComputeFogColor}) and
 * through the light map ({@code DimensionSpecialEffects.adjustLightmapColors}). Shader packs ignore both: Venus
 * became a bright beige desert with no haze. This pass is pack-independent and generic: it captures the terrain fog
 * the world set up this frame (start, end, shape, colour — after every mod's fog events), asks the world's own
 * effects for its full-daylight light-map colour, and after the pack's final pass applies both to the scene pixels
 * using the depth buffer: {@code frame = frame * tint * (1 - f) + fogColour * f}. Sky pixels are left alone (the
 * sky is already ours). New worlds (Titan...) need no code: their fog and light map are used as they are.
 */
public final class HazePass {
    /** Vanilla fog beyond this share of the render distance is only the chunk-edge fade: no haze then. */
    private static final float CLOSE_FOG = 0.8F;

    private static ShaderInstance shader;
    private static boolean captured;
    private static float fogStart;
    private static float fogEnd;
    private static int fogShape;
    private static final float[] FOG_COLOR = new float[4];
    private static int depthCopy = -1;
    private static int copyWidth;
    private static int copyHeight;
    private static boolean warned;

    private HazePass() {
    }

    static void setShader(ShaderInstance instance) {
        shader = instance;
    }

    /** Called at AFTER_SOLID_BLOCKS: the terrain fog of this frame is set up then. */
    public static void captureFog() {
        captured = false;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || shader == null || !wanted(level)) {
            return;
        }
        fogStart = RenderSystem.getShaderFogStart();
        fogEnd = RenderSystem.getShaderFogEnd();
        fogShape = RenderSystem.getShaderFogShape().getIndex();
        float[] c = RenderSystem.getShaderFogColor();
        System.arraycopy(c, 0, FOG_COLOR, 0, 4);
        captured = true;
    }

    private static boolean wanted(ClientLevel level) {
        return SpaceSky.kind(level).ownSky() && ShaderSkyPass.active();
    }

    /** Called at AFTER_LEVEL, after the sky pass. */
    public static void afterLevel(Matrix4f modelView, Matrix4f projection, float partialTick) {
        if (!captured) {
            return;
        }
        captured = false;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !wanted(level)) {
            return;
        }
        SpaceWorlds.Kind kind = SpaceSky.kind(level);
        float renderDistance = mc.gameRenderer.getRenderDistance();
        boolean haze = !kind.airless() && fogEnd < renderDistance * CLOSE_FOG;
        Vector3f tint = switch (kind) {
            case THICK_ATMOSPHERE -> daylightTint(level, partialTick);
            case AIRLESS, ORBIT -> dawnBalance(level, partialTick);
            default -> new Vector3f(1F);
        };
        if (!haze && tint.x > 0.99F && tint.y > 0.99F && tint.z > 0.99F) {
            return;
        }
        try {
            draw(mc, modelView, projection, haze, tint);
        } catch (RuntimeException e) {
            if (!warned) {
                warned = true;
                Spacebreaker.LOGGER.error("Atmosphere haze over the shader pack failed", e);
            }
        }
    }

    /**
     * The world's own surface daylight colour: its light map at full sky light and no block light. Worlds that do
     * not change the light map give white (no tint).
     */
    static Vector3f daylightTint(ClientLevel level, float partialTick) {
        Vector3f colors = new Vector3f(1F, 1F, 1F);
        level.effects().adjustLightmapColors(level, partialTick, level.getSkyDarken(partialTick), 1F, 1F, 0, 15, colors);
        colors.set(clamp(colors.x), clamp(colors.y), clamp(colors.z));
        return colors;
    }

    /**
     * White balance for airless worlds at low sun. Packs colour sunlight by the sun's height as if it shone through
     * Earth's air (orange at dawn and dusk, also on the Moon and Mercury). Packs read the sun height from vanilla's
     * sun angle, so the same height tells us how much orange they added; red and green are pulled back toward the
     * blue channel on scene pixels (a multiply can only darken: a grazing sun is dim anyway). Night is untouched.
     */
    static Vector3f dawnBalance(ClientLevel level, float partialTick) {
        float h = net.minecraft.util.Mth.cos(level.getTimeOfDay(partialTick) * net.minecraft.util.Mth.TWO_PI);
        float low = 1F - smooth((h - 0.05F) / 0.60F);
        float up = smooth((h + 0.10F) / 0.12F);
        float w = low * up;
        return new Vector3f(1F - DAWN_RED * w, 1F - DAWN_GREEN * w, 1F);
    }

    private static final float DAWN_RED = 0.42F;
    private static final float DAWN_GREEN = 0.18F;

    private static float smooth(float t) {
        t = Math.max(0F, Math.min(1F, t));
        return t * t * (3F - 2F * t);
    }

    private static float clamp(float v) {
        return Float.isNaN(v) ? 1F : Math.max(0.06F, Math.min(1F, v));
    }

    private static void draw(Minecraft mc, Matrix4f viewMatrix, Matrix4f projection, boolean haze, Vector3f tint) {
        RenderTarget main = mc.getMainRenderTarget();
        copyDepth(main);

        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSorting = RenderSystem.getVertexSorting();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);

        main.bindWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, depthCopy);
        shader.safeGetUniform("InvProjMat").set(new Matrix4f(projection).invert());
        shader.safeGetUniform("InvViewMat").set(new Matrix4f(viewMatrix).invert());
        shader.safeGetUniform("HazeStart").set(haze ? fogStart : 1.0e9F);
        shader.safeGetUniform("HazeEnd").set(haze ? fogEnd : 2.0e9F);
        shader.safeGetUniform("HazeShape").set(fogShape);
        shader.safeGetUniform("HazeColor").set(FOG_COLOR[0], FOG_COLOR[1], FOG_COLOR[2], FOG_COLOR[3]);
        shader.safeGetUniform("LightTint").set(tint.x, tint.y, tint.z);

        // 1) dim, coloured daylight; 2) the haze itself
        shader.safeGetUniform("Mode").set(0);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.SRC_COLOR);
        quad();
        if (haze) {
            shader.safeGetUniform("Mode").set(1);
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            quad();
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static void quad() {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        b.addVertex(-1F, -1F, 0F);
        b.addVertex(1F, -1F, 0F);
        b.addVertex(1F, 1F, 0F);
        b.addVertex(-1F, 1F, 0F);
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    /**
     * Copies the frame's depth into our own texture, so it is not sampled while it is attached to the framebuffer
     * being drawn to (a feedback loop). glCopyTexSubImage2D converts between depth formats, unlike a blit.
     */
    private static void copyDepth(RenderTarget main) {
        if (depthCopy < 0 || copyWidth != main.width || copyHeight != main.height) {
            if (depthCopy < 0) {
                depthCopy = GlStateManager._genTexture();
            }
            copyWidth = main.width;
            copyHeight = main.height;
            GlStateManager._bindTexture(depthCopy);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL14.GL_TEXTURE_COMPARE_MODE, 0);
            GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL30.GL_DEPTH_COMPONENT32F, copyWidth, copyHeight, 0,
                    GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, null);
        }
        main.bindWrite(false); // binds the frame's framebuffer for reading too
        GlStateManager._bindTexture(depthCopy);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, copyWidth, copyHeight);
        GlStateManager._bindTexture(0);
    }

    /** Frees the depth copy (world unload). */
    public static void release() {
        if (depthCopy >= 0) {
            RenderSystem.deleteTexture(depthCopy);
            depthCopy = -1;
        }
        captured = false;
    }
}
