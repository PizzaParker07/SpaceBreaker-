package com.pizzaparker07.spacebreaker.client.sky;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws a planet or moon the Minecraft way ({@link BlockyBodyMesh}): a ball of blocks or a pixel cube, coloured from
 * the body's surface map, turning on its axis, lit by the sun with a stepped day/night line, and optionally a ring of
 * square tiles. Plain coloured quads (no render types), sorted back to front and depth-tested against a depth buffer
 * cleared before and after, so it works in the same places and with the same shader packs as the old textured discs.
 * <p>
 * Use: build a {@link Pose} for the body (where it is, which way its pole points, how far it has turned), then
 * {@link #draw} in the sky (camera at the origin) or {@link #drawFlat} on a GUI (looking down -Z).
 */
public final class BlockyBodyRenderer {
    /** Colour (0xRRGGBB, alpha in the top byte for rings) at a longitude and latitude in radians. */
    @FunctionalInterface
    public interface ColorMap {
        int argb(double lon, double lat);
    }

    /**
     * Where a body is and how it is turned.
     *
     * @param center    body centre, in the frame the pose matrix maps from (camera at the origin for {@link #draw})
     * @param radius    body radius in the same units
     * @param pole      direction of the north pole
     * @param facing    direction of longitude 0 before spinning (usually toward the viewer)
     * @param spin      rotation about the pole, radians (east toward the viewer as it grows)
     */
    public record Pose(Vector3f center, float radius, Vector3f pole, Vector3f facing, float spin) {
        /**
         * Rotation for a style: a cube is shown like a block in the inventory, its pole tipped toward the viewer and
         * turned by an eighth, so its top and two sides show instead of one flat face.
         */
        Matrix3f rotation(BlockyBodyMesh.Style style) {
            if (style != BlockyBodyMesh.Style.CUBE) {
                return rotation();
            }
            Matrix3f r = rotation();
            r.mul(new Matrix3f().rotationY(spin)).mul(new Matrix3f().rotationX(CUBE_TILT)).mul(new Matrix3f().rotationY(-spin + (float) Math.PI / 4));
            return r;
        }

        /** {@link #rotation(BlockyBodyMesh.Style)} written into {@code dest}, allocating nothing (render thread only). */
        Matrix3f rotationInto(BlockyBodyMesh.Style style, Matrix3f dest) {
            Vector3f y = S_Y.set(pole).normalize();
            Vector3f z = S_Z.set(facing).sub(S_T.set(y).mul(y.dot(facing)));
            if (z.lengthSquared() < 1e-8F) {
                z.set(Math.abs(y.x) < 0.9F ? 1 : 0, 0, Math.abs(y.x) < 0.9F ? 0 : 1);
                z.sub(S_T.set(y).mul(y.dot(z)));
            }
            z.normalize();
            Vector3f x = S_X.set(y).cross(z).normalize();
            dest.set(x, y, z).mul(S_M.rotationY(-spin));
            if (style == BlockyBodyMesh.Style.CUBE) {
                dest.mul(S_M.rotationY(spin)).mul(S_M.rotationX(CUBE_TILT)).mul(S_M.rotationY(-spin + (float) Math.PI / 4));
            }
            return dest;
        }

        /** Rotation from body space (pole +Y, longitude 0 +Z, 90 E +X) into the drawing frame. */
        Matrix3f rotation() {
            Vector3f y = new Vector3f(pole).normalize();
            Vector3f z = new Vector3f(facing).sub(new Vector3f(y).mul(y.dot(facing)));
            if (z.lengthSquared() < 1e-8F) {
                z = Math.abs(y.x) < 0.9F ? new Vector3f(1, 0, 0) : new Vector3f(0, 0, 1);
                z.sub(new Vector3f(y).mul(y.dot(z)));
            }
            z.normalize();
            Vector3f x = new Vector3f(y).cross(z).normalize();
            Matrix3f basis = new Matrix3f(x, y, z);
            return basis.mul(new Matrix3f().rotationY(-spin));
        }
    }

    /**
     * A ring around the equator.
     *
     * @param inner inner edge in body radii
     * @param outer outer edge in body radii
     * @param bands colour by radius: called with (fraction 0..1 from inner to outer edge, 0); alpha in the top byte
     */
    public record Ring(float inner, float outer, ColorMap bands) {
    }

    private record Quad(float depth, float[] xyz, int argb) {
    }

    // scratch for the every-frame path (render thread only), so drawing a cached body allocates nothing
    private static final Vector3f S_X = new Vector3f(), S_Y = new Vector3f(), S_Z = new Vector3f(), S_T = new Vector3f();
    private static final Matrix3f S_M = new Matrix3f(), S_ROT = new Matrix3f(), S_ROT_T = new Matrix3f();
    private static final Vector3f S_LOOK = new Vector3f(), S_UP = new Vector3f(), S_RIGHT = new Vector3f();
    private static final Vector3f S_SUN_B = new Vector3f(), S_LOOK_B = new Vector3f(), S_UP_B = new Vector3f(), S_RIGHT_B = new Vector3f(),
            S_CAM_B = new Vector3f();
    private static final Matrix4f S_MODEL = new Matrix4f(), S_M4 = new Matrix4f();
    private static final float[] S_SIG = new float[16];

    /** How far a cube's pole is tipped toward the viewer (radians). */
    static final float CUBE_TILT = 0.45F;

    private static final Map<ResourceLocation, ColorMap> MAPS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, int[]> PIXELS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, int[]> SIZES = new ConcurrentHashMap<>();
    private static final Map<ColorMap, Map<List<BlockyBodyMesh.Face>, int[]>> COLOURS = new WeakHashMap<>();

    private BlockyBodyRenderer() {
    }

    /**
     * A thin shell of air around a body: one layer of see-through blocks that shows only toward the rim (where the line of
     * sight grazes it), fading to nothing over the face of the planet, so the surface below stays sharp and opaque.
     */
    public static void drawAirShell(Matrix4f pose, BlockyBodyMesh.Style style, int n, int rgb, Pose body, Vector3f sun, float alpha) {
        Matrix3f rot = body.rotation(style);
        Vector3f s = new Vector3f(sun).normalize();
        Vector3f look = new Vector3f(body.center()).normalize();
        List<BlockyBodyMesh.Face> faces = BlockyBodyMesh.body(style, n);
        List<Quad> quads = new ArrayList<>();
        for (BlockyBodyMesh.Face f : faces) {
            Vector3f normal = rot.transform(new Vector3f(f.normal()));
            Vector3f centre = rot.transform(new Vector3f(f.center())).mul(body.radius()).add(body.center());
            if (facesAway(normal, centre, look, depthKept(body) < 0.5F)) {
                continue;
            }
            Vector3f surface = rot.transform(new Vector3f(f.surface()));
            Vector3f toEye = new Vector3f(centre).negate().normalize();
            float facing = Math.abs(surface.dot(toEye));
            float rim = Mth.clamp((0.45F - facing) / 0.3F, 0F, 1F);
            if (rim <= 0) {
                continue;
            }
            float day = Mth.clamp(surface.dot(s) * 3F + 0.3F, 0.08F, 1F);
            quads.add(quad(f, rot, body, rgb, day, alpha * rim, null, look));
        }
        if (quads.isEmpty()) {
            return;
        }
        quads.sort((a, b) -> Float.compare(b.depth(), a.depth()));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (Quad q : quads) {
            put(bb, pose, q);
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.enableCull();
    }

    /**
     * The air of a planet as a thin band hugging its outline (the limb), from the edge of the planet out to the edge of a
     * slightly bigger sphere, in a few hard steps; never over the planet's face, so the surface stays sharp. Lit on the
     * day side, faint on the night side. Works from far away and from low orbit (where the limb is the horizon).
     *
     * @param thickness air height as a fraction of the radius
     */
    public static void drawLimb(Matrix4f pose, Pose body, Vector3f sun, int rgb, float alpha, float thickness) {
        Vector3f c = body.center();
        float d = c.length();
        float r = body.radius();
        float ra = r * (1 + thickness);
        if (d <= ra * 1.0005F) {
            return; // inside the air
        }
        float ux = c.x / d, uy = c.y / d, uz = c.z / d;
        // the outline of a sphere seen from the camera: a circle in the plane square to the line of sight
        float inner = r * (float) Math.sqrt(1 - (r * r) / (d * d));
        float outer = ra * (float) Math.sqrt(1 - (ra * ra) / (d * d));
        float di = d - r * r / d, dout = d - ra * ra / d;
        // two axes in that plane
        float ax = Math.abs(uy) < 0.9F ? 0 : 1, ay = Math.abs(uy) < 0.9F ? 1 : 0;
        float dot = ux * ax + uy * ay;
        float e1x = ax - ux * dot, e1y = ay - uy * dot, e1z = -uz * dot;
        float l1 = (float) Math.sqrt(e1x * e1x + e1y * e1y + e1z * e1z);
        e1x /= l1;
        e1y /= l1;
        e1z /= l1;
        float e2x = uy * e1z - uz * e1y, e2y = uz * e1x - ux * e1z, e2z = ux * e1y - uy * e1x;
        float sl = sun.length();
        float sx = sun.x / sl, sy = sun.y / sl, sz = sun.z / sl;
        int segs = 160;
        int steps = 3;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int cr = rgb >> 16 & 255, cg = rgb >> 8 & 255, cb = rgb & 255;
        for (int i = 0; i < segs; i++) {
            float t0 = Mth.TWO_PI * i / segs;
            float t1 = Mth.TWO_PI * (i + 1) / segs;
            float tm = (t0 + t1) / 2;
            float mx = e1x * Mth.cos(tm) + e2x * Mth.sin(tm), my = e1y * Mth.cos(tm) + e2y * Mth.sin(tm), mz = e1z * Mth.cos(tm) + e2z * Mth.sin(tm);
            float day = Mth.clamp((mx * sx + my * sy + mz * sz) * 2F + 0.5F, 0.12F, 1F);
            for (int k = 0; k < steps; k++) {
                float f0 = (float) k / steps;
                float f1 = (float) (k + 1) / steps;
                // hard steps: each ring of the band one shade, thinning outward
                int al = (int) (Mth.clamp(alpha * (1 - f0) * day, 0, 1) * 255);
                limbVertex(bb, pose, f0, t0, inner, outer, di, dout, ux, uy, uz, e1x, e1y, e1z, e2x, e2y, e2z, cr, cg, cb, al);
                limbVertex(bb, pose, f1, t0, inner, outer, di, dout, ux, uy, uz, e1x, e1y, e1z, e2x, e2y, e2z, cr, cg, cb, al);
                limbVertex(bb, pose, f1, t1, inner, outer, di, dout, ux, uy, uz, e1x, e1y, e1z, e2x, e2y, e2z, cr, cg, cb, al);
                limbVertex(bb, pose, f0, t1, inner, outer, di, dout, ux, uy, uz, e1x, e1y, e1z, e2x, e2y, e2z, cr, cg, cb, al);
            }
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.enableCull();
    }

    private static void limbVertex(BufferBuilder bb, Matrix4f pose, float f, float t, float inner, float outer, float di, float dout,
                                   float ux, float uy, float uz, float e1x, float e1y, float e1z, float e2x, float e2y, float e2z,
                                   int r, int g, int b, int a) {
        float rad = Mth.lerp(f, inner, outer);
        float along = Mth.lerp(f, di, dout);
        float c = Mth.cos(t) * rad, sn = Mth.sin(t) * rad;
        bb.addVertex(pose, ux * along + e1x * c + e2x * sn, uy * along + e1y * c + e2y * sn, uz * along + e1z * c + e2z * sn).setColor(r, g, b, a);
    }

    /** Blocks across a body for a style: a voxel ball needs a few more than a cube face. */
    public static int resolution(BlockyBodyMesh.Style style, boolean small) {
        return style == BlockyBodyMesh.Style.VOXEL ? (small ? 10 : 16) : (small ? 6 : 10);
    }

    /**
     * A colour map from an equirectangular surface texture (longitude -180..180 left to right, north at the top). The
     * texture's alpha is passed on in the top byte, but bodies ignore it (their faces are always opaque; only ring bands
     * use alpha), so a transparent pixel shows its stored colour, often black: fill or pull in soft edges first.
     */
    public static ColorMap texture(ResourceLocation id) {
        return MAPS.computeIfAbsent(id, k -> {
            ColorMap m = textureMap(k);
            PIXEL_MAPS.put(m, new PixelMap(k, null, 0.5F));
            return m;
        });
    }

    private static ColorMap textureMap(ResourceLocation id) {
        return (lon, lat) -> {
            int[] px = pixels(id);
            int[] wh = SIZES.get(id);
            if (px == null || wh == null) {
                return 0xFF808080;
            }
            double u = Mth.positiveModulo(lon / Mth.TWO_PI + 0.5, 1.0);
            double v = 0.5 - lat / Math.PI;
            int ix = Mth.clamp((int) (u * wh[0]), 0, wh[0] - 1);
            int iy = Mth.clamp((int) (v * wh[1]), 0, wh[1] - 1);
            return px[iy * wh[0] + ix];
        };
    }

    /** One colour everywhere. */
    public static ColorMap flat(int rgb) {
        return (lon, lat) -> 0xFF000000 | rgb;
    }

    /** A surface map with a cloud layer laid over it (the clouds' alpha says how thick they are). */
    public static ColorMap withClouds(ColorMap surface, ColorMap clouds) {
        return (lon, lat) -> {
            int s = surface.argb(lon, lat);
            int c = clouds.argb(lon, lat);
            float k = (c >>> 24) / 255F;
            if (k <= 0) {
                return s;
            }
            int r = (int) Mth.lerp(k, s >> 16 & 255, c >> 16 & 255);
            int g = (int) Mth.lerp(k, s >> 8 & 255, c >> 8 & 255);
            int b = (int) Mth.lerp(k, s & 255, c & 255);
            return 0xFF000000 | r << 16 | g << 8 | b;
        };
    }

    /**
     * Draws a body in the sky. {@code pose} maps the frame of {@code body} (camera at the origin) to the screen.
     *
     * @param sun    unit direction toward the sun, same frame
     * @param night  brightness of the night side (0..1)
     * @param tint   overall brightness (1 normally; less while an eclipse darkens a planet)
     * @param n      blocks across; 0 to choose by size on screen (steps through {@link #LEVELS} up to 64, fading between
     *               them); {@link #APPROACH} the same but up to 128 (flying close to a planet)
     * @param ring   optional ring
     */
    public static void draw(Matrix4f pose, BlockyBodyMesh.Style style, int n, ColorMap map, Pose body, Vector3f sun, float night,
                            float tint, float alpha, Ring ring) {
        render(pose, style, n, map, body, sun, night, tint, alpha, ring, null);
    }

    /** No air: {@link #draw(Matrix4f, BlockyBodyMesh.Style, int, ColorMap, Pose, Vector3f, float, float, float, Ring, int)} without a limb. */
    public static final int NO_AIR = -1;
    /** Night side of a body with thick air (gas giants): lit faintly by its moons and its own air, never a black hole in the sky. */
    static final float AIR_NIGHT = 0.1F;
    /** The air band along the outline of such a body: opacity on the day side (the night side keeps about an eighth). */
    static final float AIR_LIMB_ALPHA = 0.55F;
    static final float AIR_LIMB_THICKNESS = 0.02F;

    /**
     * As {@link #draw(Matrix4f, BlockyBodyMesh.Style, int, ColorMap, Pose, Vector3f, float, float, float, Ring)}, for a body
     * with air of colour {@code air} (rgb; {@link #NO_AIR} for none): its night side gets a faint fill in its own colours
     * (at least {@link #AIR_NIGHT}) and a thin band of air hugs the outline, bright on the day side and faint but still
     * there on the night side, so a giant seen from its moons' day side reads as a planet, not as a black disc.
     */
    /**
     * Air colour of a giant planet by its name or disc texture path ({@link #NO_AIR} for anything else): Jupiter cream,
     * Saturn pale gold, Uranus cyan, Neptune blue.
     */
    public static int giantAir(String path) {
        if (path.contains("jupiter")) {
            return 0xE8D9B8;
        }
        if (path.contains("saturn")) {
            return 0xE6D3A3;
        }
        if (path.contains("uranus")) {
            return 0x9FE3E8;
        }
        if (path.contains("neptune")) {
            return 0x6F9BFF;
        }
        return NO_AIR;
    }

    public static void draw(Matrix4f pose, BlockyBodyMesh.Style style, int n, ColorMap map, Pose body, Vector3f sun, float night,
                            float tint, float alpha, Ring ring, int air) {
        if (air == NO_AIR) {
            render(pose, style, n, map, body, sun, night, tint, alpha, ring, null);
            return;
        }
        render(pose, style, n, map, body, sun, Math.max(night, AIR_NIGHT), tint, alpha, ring, null);
        drawLimb(pose, body, sun, air, AIR_LIMB_ALPHA * alpha * tint, AIR_LIMB_THICKNESS);
    }

    /**
     * Draws a body on a flat view looking down -Z (GUI screens: x right, y down). With {@code n} 0 the size is chosen from
     * the body's size in real screen pixels (GUI units times the GUI scale), a block every few pixels.
     */
    public static void drawFlat(Matrix4f pose, BlockyBodyMesh.Style style, int n, ColorMap map, Pose body, Vector3f sun, float night,
                                float tint, float alpha, Ring ring) {
        int size = n > 0 ? n : flatResolution(style, body.radius(), Minecraft.getInstance().getWindow().getGuiScale());
        render(pose, style, size, map, body, sun, night, tint, alpha, ring, new Vector3f(0, 0, -1));
    }

    /** Blocks across for a body {@code radius} GUI units wide drawn at {@code guiScale}: about one block per 3 screen pixels. */
    public static int flatResolution(BlockyBodyMesh.Style style, float radius, double guiScale) {
        double pixels = 2 * radius * guiScale;
        int n = (int) Math.round(pixels / 3);
        if (style == BlockyBodyMesh.Style.CUBE) {
            n = n * 2 / 3;
        }
        return Mth.clamp(n, 8, 64);
    }

    /** Voxel resolutions a body steps through as it grows on screen. */
    public static final int[] LEVELS = {16, 32, 64, 128};
    /**
     * Screen pixels per block a level aims for: finer levels come in only when blocks would get this big, so a body stays
     * clearly blocky at every size.
     */
    static final float BLOCK_PX = 30F;
    /** Finest level for bodies in the sky; the finest of all ({@link #LEVELS}' last) only on a close approach in flight. */
    static final int SKY_MAX = 64;
    /** Pass as {@code n} for a close approach in flight: chosen by size like 0, but may go up to the finest level. */
    public static final int APPROACH = -1;
    /** Length of the cross-fade between two levels, ms. */
    static final long FADE_MS = 350;

    private record CacheKey(ColorMap map, BlockyBodyMesh.Style style, int n) {
    }

    private static final class Cached {
        long builtAt;
        com.mojang.blaze3d.vertex.VertexBuffer buffer;
        List<BlockyBodyMesh.Face> faces;
        float[] signature;
        boolean empty;
    }

    private static final class Lod {
        int current;
        int previous;
        long since;
    }

    /** Level-of-detail state per colour map and style (found without building a key). */
    private static final Map<ColorMap, Lod[]> LODS = new java.util.IdentityHashMap<>();
    private static long rebuildNanos;
    private static int rebuilds;
    private static long drawNanos;
    private static int draws;
    private static long lastReport;

    private static void render(Matrix4f pose, BlockyBodyMesh.Style style, int n, ColorMap map, Pose body, Vector3f sun, float night,
                               float tint, float alpha, Ring ring, Vector3f viewDir) {
        if (!timingOn()) {
            renderBody(pose, style, n, map, body, sun, night, tint, alpha, ring, viewDir);
            return;
        }
        // development measurement: time and memory of draws that rebuilt nothing (the steady, every-frame cost)
        int rebuildsBefore = rebuilds;
        long a0 = allocated();
        long t0 = System.nanoTime();
        renderBody(pose, style, n, map, body, sun, night, tint, alpha, ring, viewDir);
        long t1 = System.nanoTime();
        long a1 = allocated();
        if (viewDir != null) {
            timing(t1 - t0, false);
        }
        if (rebuilds == rebuildsBefore) {
            steadyNanos += t1 - t0;
            steadyBytes += Math.max(0, a1 - a0);
            steadyDraws++;
        }
    }

    private static long steadyNanos;
    private static long steadyBytes;
    private static int steadyDraws;

    private static long allocated() {
        java.lang.management.ThreadMXBean bean = java.lang.management.ManagementFactory.getThreadMXBean();
        return bean instanceof com.sun.management.ThreadMXBean sun ? sun.getCurrentThreadAllocatedBytes() : 0;
    }

    private static void renderBody(Matrix4f pose, BlockyBodyMesh.Style style, int n, ColorMap map, Pose body, Vector3f sun, float night,
                                   float tint, float alpha, Ring ring, Vector3f viewDir) {
        if (viewDir != null) {
            if (ring != null) {
                // a ringed body on a flat view: body and ring sorted together, drawn each time (rare: a map's ringed planets)
                immediate(pose, style, n, map, body, sun, night, tint, alpha, ring, viewDir, true);
                return;
            }
            List<BlockyBodyMesh.Face> faces = BlockyBodyMesh.body(style, n);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            drawFaces(pose, slot(FLAT_BUFFERS, map, style.ordinal() * (BlockyBodyMesh.MAX_SIZE + 1) + Math.min(n, BlockyBodyMesh.MAX_SIZE)), faces,
                    colours(map, faces, false), body, body.rotationInto(style, S_ROT), sun, night, tint, alpha, viewDir, false);
            RenderSystem.enableCull();
            return;
        }
        long t0 = System.nanoTime();
        // the stepped body has hidden corners: draw it with its own depth, then clear that depth again so the terrain
        // drawn after the sky is not hidden behind a planet 100 blocks away (the sky comes before terrain, so nothing
        // else has written depth yet). glClear leaves depth alone while the depth mask is off: open the mask first.
        RenderSystem.depthMask(true);
        RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        int level = n > 0 ? n : 0;
        PixelMap pixels = style == BlockyBodyMesh.Style.VOXEL ? pixelMap(map) : null;
        float blend = pixels == null ? 0F : pixelBlend(body);
        if (blend >= 1F) {
            // close up: only the pixel sphere
        } else if (n == APPROACH && drawPatch(pose, style, map, body, sun, night, tint, alpha)) {
            level = LEVELS[LEVELS.length - 1];
        } else if (n <= 0) {
            Lod lod = lod(map, style, body, n == APPROACH ? LEVELS[LEVELS.length - 1] : SKY_MAX);
            float t = lod.previous == 0 ? 1F : Mth.clamp((net.minecraft.Util.getMillis() - lod.since) / (float) FADE_MS, 0F, 1F);
            if (t < 1F) {
                // cross-fade: the old level underneath, the new one fading in over it
                cached(pose, style, lod.previous, map, body, sun, night, tint, alpha);
                cached(pose, style, lod.current, map, body, sun, night, tint, alpha * t);
            } else {
                lod.previous = 0;
                cached(pose, style, lod.current, map, body, sun, night, tint, alpha);
            }
            level = lod.current;
        } else {
            cached(pose, style, level, map, body, sun, night, tint, alpha);
        }
        if (ring != null) {
            int tn = Math.max(24, Math.min(Math.max(level, LEVELS[0]), 64) * 3);
            List<BlockyBodyMesh.Face> tiles = BlockyBodyMesh.ring(ring.inner(), ring.outer(), tn);
            drawFaces(pose, slot(RING_BUFFERS, ring.bands(), Math.min(tn, 2 * BlockyBodyMesh.MAX_SIZE + 1)), tiles, colours(ring.bands(), tiles, true),
                    body, body.rotationInto(style, S_ROT), sun, night, tint, alpha, null, true);
        }
        RenderSystem.enableCull();
        RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX); // while the mask is still open
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        if (blend > 0F) {
            // fading from the ball of blocks into the pixel sphere as the body grows
            drawPixelSphere(pose, pixels, body, sun, night, tint, alpha * blend);
            RenderSystem.disableCull();
        }
        timing(System.nanoTime() - t0, false);
    }

    /** Finest surface patch, blocks across the whole body (only the visible cap is built). */
    static final int PATCH_MAX = 1024;

    private record PatchKey(ColorMap map, int n, int qx, int qy, int qz) {
    }

    private static final class Patch {
        final float halfAngle;
        final Cached buffer = new Cached();
        volatile List<BlockyBodyMesh.Face> faces;
        volatile int[] colours;

        Patch(float halfAngle) {
            this.halfAngle = halfAngle;
        }
    }

    private static final Map<PatchKey, Patch> PATCHES = new java.util.LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<PatchKey, Patch> e) {
            if (size() > 6) {
                if (e.getValue().buffer.buffer != null) {
                    e.getValue().buffer.buffer.close();
                }
                return true;
            }
            return false;
        }
    };
    private static final Map<ColorMap, PatchKey> LAST_PATCH = new java.util.IdentityHashMap<>();
    private static final Map<ColorMap, PatchKey> LAST_WANTED = new java.util.IdentityHashMap<>();

    /**
     * Very close to a body (low orbit): the whole ball even at its finest level has blocks far bigger than intended, so only
     * the cap of surface in view is built, finer, in the background, and drawn instead. Returns false while there is no
     * patch to draw yet (or none is needed), and the caller draws the usual ball.
     */
    private static boolean drawPatch(Matrix4f pose, BlockyBodyMesh.Style style, ColorMap map, Pose body, Vector3f sun, float night,
                                     float tint, float alpha) {
        if (style != BlockyBodyMesh.Style.VOXEL) {
            return false;
        }
        float dist = body.center().length();
        float near = dist - body.radius();
        if (near > body.radius() * 0.6F || near <= 0) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        double fov = Math.toRadians(mc.options.fov().get());
        double want = 2 * body.radius() / Math.max(near, body.radius() * 0.02) / (2 * Math.tan(fov / 2)) * mc.getWindow().getHeight() / BLOCK_PX;
        if (want <= LEVELS[LEVELS.length - 1] * 1.25) {
            return false;
        }
        int n = 256;
        while (n * 2 <= Math.min(PATCH_MAX, want)) {
            n *= 2;
        }
        Matrix3f rot = body.rotationInto(style, S_ROT);
        // the camera's direction from the body's centre, in the body's own frame
        Vector3f toward = S_ROT_T.set(rot).transpose().transform(S_T.set(body.center()).negate().normalize());
        float horizon = (float) Math.acos(Math.min(1F, body.radius() / dist));
        float half = horizon + 4F / n + 0.03F;
        float q = 8F / half;
        int qx = Math.round(toward.x * q), qy = Math.round(toward.y * q), qz = Math.round(toward.z * q);
        PatchKey key = LAST_WANTED.get(map);
        if (key == null || key.n() != n || key.qx() != qx || key.qy() != qy || key.qz() != qz) {
            key = new PatchKey(map, n, qx, qy, qz); // only when the patch in view changes
            LAST_WANTED.put(map, key);
        }
        Patch patch = PATCHES.get(key);
        if (patch == null) {
            Patch p = new Patch(half * 1.2F);
            PATCHES.put(key, p);
            patch = p;
            Vector3f centre = new Vector3f(qx, qy, qz).div(q).normalize();
            int size = n;
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                List<BlockyBodyMesh.Face> faces = BlockyBodyMesh.patch(size, centre, p.halfAngle);
                p.colours = computeColours(map, faces, false);
                p.faces = faces;
            });
        }
        if (patch.faces == null) {
            // still building: keep showing the last finished patch if there is one
            PatchKey last = LAST_PATCH.get(map);
            patch = last == null ? null : PATCHES.get(last);
            if (patch == null || patch.faces == null) {
                return false;
            }
            key = last;
        }
        LAST_PATCH.put(map, key);
        drawFaces(pose, patch.buffer, patch.faces, patch.colours, body, rot, sun, night, tint, alpha, null, false);
        return true;
    }

    /** The resolution to draw a body at, stepping up as it grows on screen and fading between steps. */
    private static Lod lod(ColorMap map, BlockyBodyMesh.Style style, Pose body, int max) {
        Minecraft mc = Minecraft.getInstance();
        // blocks are judged where they are biggest on screen: at the nearest point of the surface (for a far body that is
        // its distance; from low orbit only the height above it)
        double near = Math.max(body.radius() * 0.02, body.center().length() - body.radius());
        double fov = Math.toRadians(mc.options.fov().get());
        double pixels = 2 * body.radius() / near / (2 * Math.tan(fov / 2)) * mc.getWindow().getHeight();
        double want = pixels / BLOCK_PX;
        Lod[] row = LODS.get(map);
        if (row == null) {
            row = new Lod[BlockyBodyMesh.Style.values().length];
            LODS.put(map, row);
        }
        Lod lod = row[style.ordinal()];
        if (lod == null) {
            lod = new Lod();
            lod.current = LEVELS[0];
            row[style.ordinal()] = lod;
        }
        for (int l : LEVELS) {
            if (l <= max) {
                BlockyBodyMesh.prepare(style, l); // build every size in use in the background once
            }
        }
        if (lod.previous != 0) {
            return lod; // finish the running fade first
        }
        // the level that fits now (with some slack either way so it does not flicker), reached in one fade even if it is
        // several steps away (a body that suddenly fills the view should not linger coarse)
        int next = lod.current;
        if (lod.current > max || want > lod.current * 1.25 || want < lod.current / 2.0 * 0.8) {
            next = LEVELS[0];
            for (int l : LEVELS) {
                if (l <= max && l <= want * 1.25) {
                    next = l;
                }
            }
        }
        if (next != lod.current && coloursReady(map, style, next)) {
            lod.previous = lod.current;
            lod.current = next;
            lod.since = net.minecraft.Util.getMillis();
        }
        return lod;
    }

    /**
     * Draws the opaque body from a vertex buffer that is rebuilt only when what it shows changes (the body turns, the
     * sun moves, the viewer moves). Only the faces toward the viewer are in it.
     */
    private static void cached(Matrix4f pose, BlockyBodyMesh.Style style, int n, ColorMap map, Pose body, Vector3f sun, float night,
                               float tint, float alpha) {
        if (n > LEVELS[0] && !coloursReady(map, style, n)) {
            BlockyBodyMesh.prepare(style, n);
            n = LEVELS[0];
        }
        List<BlockyBodyMesh.Face> faces = BlockyBodyMesh.body(style, n);
        drawFaces(pose, slot(BODY_BUFFERS, map, style.ordinal() * (BlockyBodyMesh.MAX_SIZE + 1) + n), faces, colours(map, faces, false), body,
                body.rotationInto(style, S_ROT), sun, night, tint, alpha, null, false);
    }

    /** Cache slots per colour map, found by index so the every-frame lookup builds no key. */
    private static final Map<ColorMap, Cached[]> BODY_BUFFERS = new java.util.IdentityHashMap<>();
    private static final Map<ColorMap, Cached[]> FLAT_BUFFERS = new java.util.IdentityHashMap<>();
    private static final Map<ColorMap, Cached[]> RING_BUFFERS = new java.util.IdentityHashMap<>();

    private static Cached slot(Map<ColorMap, Cached[]> table, ColorMap map, int index) {
        Cached[] row = table.get(map);
        if (row == null) {
            row = new Cached[2 * (BlockyBodyMesh.MAX_SIZE + 1)];
            table.put(map, row);
        }
        Cached c = row[index];
        if (c == null) {
            c = new Cached();
            row[index] = c;
        }
        return c;
    }

    /**
     * Draws faces from a vertex buffer kept in {@code c}. The buffer holds the body in its own frame (a unit ball), so
     * turning, moving and growing it is only a matrix; it is rebuilt only when what the faces show changes: where the sun
     * is and where the viewer is, seen from the body (light, which faces are in view), or brightness.
     *
     * @param viewDir null in the sky (camera at the origin); the view direction on a flat GUI view (then sorted, no depth)
     * @param ring    the faces are ring tiles (lit as a flat sheet, shadowed by the body, sorted far to near)
     */
    private static void drawFaces(Matrix4f pose, Cached c, List<BlockyBodyMesh.Face> faces, int[] colours, Pose body, Matrix3f rot,
                                  Vector3f sun, float night, float tint, float alpha, Vector3f viewDir, boolean ring) {
        Vector3f look = viewDir != null ? S_LOOK.set(viewDir) : S_LOOK.set(body.center()).normalize();
        Vector3f up = viewDir != null ? S_UP.set(0, -1, 0) : S_UP.set(0, 1, 0);
        up.sub(S_T.set(look).mul(look.dot(up)));
        if (up.lengthSquared() < 1e-6F) {
            up.set(0, 0, -1);
        }
        up.normalize();
        Vector3f right = S_RIGHT.set(look).cross(up).normalize();
        Matrix3f inv = S_ROT_T.set(rot).transpose();
        Vector3f sunB = inv.transform(S_SUN_B.set(sun).normalize());
        Vector3f lookB = inv.transform(S_LOOK_B.set(look));
        Vector3f upB = inv.transform(S_UP_B.set(up));
        Vector3f rightB = inv.transform(S_RIGHT_B.set(right));
        float kept = viewDir != null ? 1F : depthKept(body);
        boolean flat = viewDir != null || kept < 0.5F;
        // the camera in body units (only matters for a big near body, whose faces are judged one by one)
        Vector3f camB = flat ? S_CAM_B.set(0, 0, 0) : inv.transform(S_CAM_B.set(body.center()).negate()).div(body.radius());
        float[] sig = S_SIG;
        sig[0] = sunB.x; sig[1] = sunB.y; sig[2] = sunB.z;
        sig[3] = lookB.x; sig[4] = lookB.y; sig[5] = lookB.z;
        sig[6] = upB.x; sig[7] = upB.y; sig[8] = upB.z;
        sig[9] = camB.x; sig[10] = camB.y; sig[11] = camB.z;
        sig[12] = night; sig[13] = tint; sig[14] = alpha; sig[15] = flat ? 1 : 0;
        boolean stale = c.buffer == null || c.faces != faces || !same(sig, c.signature);
        // light and view drift are rebuilt at most three times a second (the steps are too small to see); a change of
        // brightness (fades) or of the faces themselves is applied at once
        if (stale && c.buffer != null && c.faces == faces && sameBrightness(sig, c.signature)
                && net.minecraft.Util.getMillis() - c.builtAt < MIN_REBUILD_MS) {
            stale = false;
        }
        if (stale) {
            c.builtAt = net.minecraft.Util.getMillis();
            long t0 = System.nanoTime();
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            int count = ring ? emitRing(bb, faces, colours, sunB, lookB, night, tint, alpha)
                    : emitBody(bb, faces, colours, sunB, lookB, upB, rightB, camB, flat, viewDir != null, night, tint, alpha);
            var mesh = bb.build();
            if (c.buffer == null) {
                c.buffer = new com.mojang.blaze3d.vertex.VertexBuffer(com.mojang.blaze3d.vertex.VertexBuffer.Usage.DYNAMIC);
            }
            c.empty = mesh == null || count == 0;
            if (!c.empty) {
                c.buffer.bind();
                c.buffer.upload(mesh);
                com.mojang.blaze3d.vertex.VertexBuffer.unbind();
            } else if (mesh != null) {
                mesh.close();
            }
            if (c.signature == null) {
                c.signature = new float[sig.length];
            }
            System.arraycopy(sig, 0, c.signature, 0, sig.length);
            c.faces = faces;
            timing(System.nanoTime() - t0, true);
        }
        if (c.empty) {
            return;
        }
        // model: the screen's model-view, the caller's pose, then place, flatten along the line of sight, turn and size
        Matrix4f model = S_MODEL.set(RenderSystem.getModelViewMatrix()).mul(pose).translate(body.center());
        if (kept < 1F) {
            float k = 1 - kept;
            model.mul(S_M4.set(1 - k * look.x * look.x, -k * look.x * look.y, -k * look.x * look.z, 0,
                    -k * look.y * look.x, 1 - k * look.y * look.y, -k * look.y * look.z, 0,
                    -k * look.z * look.x, -k * look.z * look.y, 1 - k * look.z * look.z, 0,
                    0, 0, 0, 1));
        }
        model.mul(S_M4.set(rot)).scale(body.radius());
        c.buffer.bind();
        c.buffer.drawWithShader(model, RenderSystem.getProjectionMatrix(), GameRenderer.getPositionColorShader());
        com.mojang.blaze3d.vertex.VertexBuffer.unbind();
    }

    /**
     * Whether a cached build still shows the body closely enough: sun and viewer may move about a degree (as seen from
     * the body), the near camera a little, brightness by a hundredth.
     */
    /** Shortest time between two rebuilds of one body for light or view drift, ms. */
    static final long MIN_REBUILD_MS = 330;

    private static boolean sameBrightness(float[] a, float[] b) {
        for (int i = 12; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > 0.01F) {
                return false;
            }
        }
        return true;
    }

    private static boolean same(float[] a, float[] b) {
        if (b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            float eps = i < 9 ? 0.015F : i < 12 ? 0.004F * Math.max(1F, Math.abs(a[i])) : 0.01F;
            if (Math.abs(a[i] - b[i]) > eps) {
                return false;
            }
        }
        return true;
    }

    /**
     * The body's faces in its own frame (unit ball), those toward the viewer (with a small margin, the rest is hidden by
     * depth), coloured and lit. On a flat view they are sorted far to near instead, as there is no depth.
     */
    private static int emitBody(BufferBuilder bb, List<BlockyBodyMesh.Face> faces, int[] colours, Vector3f sunB, Vector3f lookB,
                                Vector3f upB, Vector3f rightB, Vector3f camB, boolean flat, boolean sorted, float night, float tint, float alpha) {
        int a = (int) (Mth.clamp(alpha, 0, 1) * 255);
        List<float[]> order = sorted ? new ArrayList<>() : null;
        int count = 0;
        for (int i = 0; i < faces.size(); i++) {
            BlockyBodyMesh.Face f = faces.get(i);
            Vector3f nrm = f.normal();
            Vector3f cen = f.center();
            if (flat) {
                if (nrm.dot(lookB) >= (sorted ? 0F : 0.08F)) {
                    continue;
                }
            } else {
                float dx = cen.x - camB.x, dy = cen.y - camB.y, dz = cen.z - camB.z;
                float d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (nrm.x * dx + nrm.y * dy + nrm.z * dz >= 0.02F * d) {
                    continue;
                }
            }
            float light = BlockyBodyMesh.light(f.surface(), nrm, sunB, night) * BlockyBodyMesh.blockShade(nrm.dot(upB), nrm.dot(rightB)) * tint;
            int argb = shade(colours[i], light, a);
            if (sorted) {
                order.add(new float[]{cen.dot(lookB), i, Float.intBitsToFloat(argb)});
            } else {
                face(bb, f, argb);
            }
            count++;
        }
        if (sorted) {
            order.sort((x, y) -> Float.compare(y[0], x[0]));
            for (float[] o : order) {
                face(bb, faces.get((int) o[1]), Float.floatToRawIntBits(o[2]));
            }
        }
        return count;
    }

    /** Ring tiles in the body's frame, sorted far to near, lit as a flat sheet and dark in the body's shadow. */
    private static int emitRing(BufferBuilder bb, List<BlockyBodyMesh.Face> tiles, int[] bands, Vector3f sunB, Vector3f lookB, float night,
                                float tint, float alpha) {
        List<float[]> order = new ArrayList<>();
        float sheet = Math.max(night, 0.55F + 0.45F * Math.abs(sunB.y));
        for (int i = 0; i < tiles.size(); i++) {
            Vector3f p = tiles.get(i).center();
            float along = p.dot(sunB);
            float ox = p.x - sunB.x * along, oy = p.y - sunB.y * along, oz = p.z - sunB.z * along;
            boolean shadow = along < 0 && ox * ox + oy * oy + oz * oz < 1;
            int c = bands[i];
            int argb = shade(c, (shadow ? night : sheet) * tint, (int) (Mth.clamp(alpha * ((c >>> 24) / 255F), 0, 1) * 255));
            order.add(new float[]{p.dot(lookB), i, Float.intBitsToFloat(argb)});
        }
        order.sort((x, y) -> Float.compare(y[0], x[0]));
        for (float[] o : order) {
            face(bb, tiles.get((int) o[1]), Float.floatToRawIntBits(o[2]));
        }
        return order.size();
    }

    private static int shade(int rgb, float light, int alpha) {
        float l = Mth.clamp(light, 0, 1);
        return alpha << 24 | (int) ((rgb >> 16 & 255) * l) << 16 | (int) ((rgb >> 8 & 255) * l) << 8 | (int) ((rgb & 255) * l);
    }

    /** One face's four corners, counter-clockwise from outside, in the body's own units. */
    private static void face(BufferBuilder bb, BlockyBodyMesh.Face f, int argb) {
        Vector3f c = f.center(), u = f.u(), v = f.v();
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        bb.addVertex(c.x - u.x - v.x, c.y - u.y - v.y, c.z - u.z - v.z).setColor(r, g, b, a);
        bb.addVertex(c.x + u.x - v.x, c.y + u.y - v.y, c.z + u.z - v.z).setColor(r, g, b, a);
        bb.addVertex(c.x + u.x + v.x, c.y + u.y + v.y, c.z + u.z + v.z).setColor(r, g, b, a);
        bb.addVertex(c.x - u.x + v.x, c.y - u.y + v.y, c.z - u.z + v.z).setColor(r, g, b, a);
    }

    private static void put(BufferBuilder bb, Matrix4f pose, Quad q) {
        float[] v = q.xyz();
        int c = q.argb();
        int a = c >>> 24, r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
        for (int k = 0; k < 4; k++) {
            if (pose == null) {
                bb.addVertex(v[k * 3], v[k * 3 + 1], v[k * 3 + 2]).setColor(r, g, b, a);
            } else {
                bb.addVertex(pose, v[k * 3], v[k * 3 + 1], v[k * 3 + 2]).setColor(r, g, b, a);
            }
        }
    }

    /** Frame cost of blocky bodies, logged every ten seconds when -Dspacebreaker.blockyTiming=true. */
    /** Measuring on: -Dspacebreaker.blockyTiming=true, or a showcase run with -PshotPrefix=blockyperf. */
    private static boolean timingOn() {
        return Boolean.getBoolean("spacebreaker.blockyTiming") || "blockyperf".equals(System.getProperty("spacebreaker.shotPrefix"));
    }

    private static void timing(long nanos, boolean rebuild) {
        if (!timingOn()) {
            return;
        }
        if (rebuild) {
            rebuildNanos += nanos;
            rebuilds++;
        } else {
            drawNanos += nanos;
            draws++;
        }
        long now = net.minecraft.Util.getMillis();
        if (now - lastReport > 10000 && draws > 0) {
            Spacebreaker.LOGGER.info("Blocky bodies: {} draws, {} ms each on average ({} rebuilds, {} ms each); steady {} draws, {} ms and {} bytes each; {} fps; levels {}",
                    draws, String.format("%.3f", drawNanos / 1e6 / draws), rebuilds,
                    String.format("%.3f", rebuilds == 0 ? 0 : rebuildNanos / 1e6 / rebuilds), steadyDraws,
                    String.format("%.3f", steadyDraws == 0 ? 0 : steadyNanos / 1e6 / steadyDraws), steadyDraws == 0 ? 0 : steadyBytes / steadyDraws,
                    Minecraft.getInstance().getFps(), LODS.values().stream().flatMap(java.util.Arrays::stream)
                            .filter(java.util.Objects::nonNull).map(l -> l.current).toList());
            lastReport = now;
            drawNanos = rebuildNanos = steadyNanos = steadyBytes = 0;
            draws = rebuilds = steadyDraws = 0;
        }
    }

    /** Sorted, immediate drawing: GUI views, and rings (translucent tiles) in the sky. */
    private static void immediate(Matrix4f pose, BlockyBodyMesh.Style style, int n, ColorMap map, Pose body, Vector3f sun, float night,
                                  float tint, float alpha, Ring ring, Vector3f viewDir, boolean withBody) {
        Matrix3f rot = body.rotation(style);
        Vector3f s = new Vector3f(sun).normalize();
        // line of sight to the body, and "up" on screen around it (for Minecraft's block shading: tops light, sides darker)
        Vector3f look = viewDir != null ? new Vector3f(viewDir) : new Vector3f(body.center()).normalize();
        Vector3f screenUp = viewDir != null ? new Vector3f(0, -1, 0) : new Vector3f(0, 1, 0);
        screenUp.sub(new Vector3f(look).mul(look.dot(screenUp)));
        if (screenUp.lengthSquared() < 1e-6F) {
            screenUp.set(0, 0, -1);
        }
        screenUp.normalize();
        Vector3f screenRight = new Vector3f(look).cross(screenUp).normalize();
        List<Quad> quads = new ArrayList<>();
        if (withBody) {
            List<BlockyBodyMesh.Face> faces = BlockyBodyMesh.body(style, n);
            int[] colours = colours(map, faces, false);
            boolean flat = viewDir == null && depthKept(body) < 0.5F;
            for (int i = 0; i < faces.size(); i++) {
                BlockyBodyMesh.Face f = faces.get(i);
                Vector3f normal = rot.transform(new Vector3f(f.normal()));
                Vector3f centre = rot.transform(new Vector3f(f.center())).mul(body.radius()).add(body.center());
                if (viewDir != null ? normal.dot(viewDir) >= 0 : facesAway(normal, centre, look, flat)) {
                    continue; // faces away from the viewer
                }
                float light = BlockyBodyMesh.light(rot.transform(new Vector3f(f.surface())), normal, s, night)
                        * BlockyBodyMesh.blockShade(normal.dot(screenUp), normal.dot(screenRight)) * tint;
                quads.add(quad(f, rot, body, colours[i], light, alpha, viewDir, look));
            }
        }
        if (ring != null) {
            List<BlockyBodyMesh.Face> tiles = BlockyBodyMesh.ring(ring.inner(), ring.outer(), Math.max(24, Math.min(n, 64) * 3));
            int[] bands = colours(ring.bands(), tiles, true);
            Vector3f sunBody = new Matrix3f(rot).transpose().transform(new Vector3f(s));
            for (int i = 0; i < tiles.size(); i++) {
                BlockyBodyMesh.Face f = tiles.get(i);
                // in the planet's shadow: behind it from the sun and within one radius of the shadow axis
                Vector3f p = f.center();
                float along = p.dot(sunBody);
                boolean shadow = along < 0 && new Vector3f(p).sub(new Vector3f(sunBody).mul(along)).lengthSquared() < 1;
                Vector3f normal = rot.transform(new Vector3f(0, 1, 0));
                float lit = shadow ? night : Math.max(night, 0.55F + 0.45F * Math.abs(normal.dot(s)));
                int c = bands[i];
                quads.add(quad(f, rot, body, c, lit * tint, alpha * ((c >>> 24) / 255F), viewDir, look));
            }
        }
        if (quads.isEmpty()) {
            return;
        }
        quads.sort((a, b) -> Float.compare(b.depth(), a.depth()));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (Quad q : quads) {
            put(bb, pose, q);
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.enableCull();
    }

    /**
     * How much of a body's depth is kept when it is drawn: a big body close to the camera would otherwise bulge like a
     * pillow in perspective. Flattened along the line of sight it looks like an item in a picture, with straight edges,
     * while the small depth left keeps its faces correctly in front of each other.
     */
    static final float DEPTH_KEPT = 0.04F;

    /**
     * Whether a face is turned away from the viewer. A flattened body is seen almost straight on, so its faces are judged
     * against the line of sight to its centre; a full-depth (big, near) body face by face from the camera.
     */
    private static boolean facesAway(Vector3f normal, Vector3f centre, Vector3f look, boolean flat) {
        return flat ? normal.dot(look) >= 0 : normal.dot(centre) >= 0;
    }

    /**
     * Depth kept for a body: flattened while it looks small (up to about 17° in radius), fully round once it fills much
     * of the view (a planet seen from orbit, or while landing), with a smooth change in between.
     */
    static float depthKept(Pose body) {
        float ang = (float) Math.asin(Math.min(1F, body.radius() / Math.max(1e-3F, body.center().length())));
        float t = Mth.clamp((ang - 0.3F) / 0.25F, 0F, 1F);
        return Mth.lerp(t * t * (3 - 2 * t), DEPTH_KEPT, 1F);
    }

    private static Quad quad(BlockyBodyMesh.Face f, Matrix3f rot, Pose body, int rgb, float light, float alpha, Vector3f viewDir,
                             Vector3f look) {
        Vector3f c = rot.transform(new Vector3f(f.center())).mul(body.radius()).add(body.center());
        Vector3f u = rot.transform(new Vector3f(f.u())).mul(body.radius());
        Vector3f v = rot.transform(new Vector3f(f.v())).mul(body.radius());
        float[] xyz = new float[12];
        int k = 0;
        for (float[] s : new float[][]{{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
            Vector3f p = new Vector3f(c).add(new Vector3f(u).mul(s[0])).add(new Vector3f(v).mul(s[1]));
            float kept = viewDir == null ? depthKept(body) : 1F;
            if (kept < 1F) {
                // offset from the body centre: keep its sideways part, squeeze its depth
                Vector3f d = p.sub(body.center());
                float along = d.dot(look);
                d.sub(new Vector3f(look).mul(along * (1 - kept)));
                p = d.add(body.center());
            }
            xyz[k++] = p.x;
            xyz[k++] = p.y;
            xyz[k++] = p.z;
        }
        float l = Mth.clamp(light, 0, 1);
        int r = (int) ((rgb >> 16 & 255) * l);
        int g = (int) ((rgb >> 8 & 255) * l);
        int b = (int) ((rgb & 255) * l);
        int a = (int) (Mth.clamp(alpha, 0, 1) * 255);
        float depth = viewDir == null ? c.lengthSquared() : -c.dot(viewDir);
        return new Quad(depth, xyz, a << 24 | r << 16 | g << 8 | b);
    }

    /**
     * Map colours of every face, cached per map and mesh. A body face takes the average colour over the patch of surface
     * its block covers (a 3 x 3 sample), so a small ball of blocks shows the land and sea it really stands for instead
     * of whichever single map pixel its centre happens to hit.
     */
    private static int[] colours(ColorMap map, List<BlockyBodyMesh.Face> faces, boolean ring) {
        int[] done = readyColours(map, faces);
        if (done != null) {
            return done;
        }
        int[] out = computeColours(map, faces, ring);
        synchronized (COLOURS) {
            COLOURS.computeIfAbsent(map, m -> new java.util.IdentityHashMap<>()).put(faces, out);
        }
        return out;
    }

    private static int[] readyColours(ColorMap map, List<BlockyBodyMesh.Face> faces) {
        synchronized (COLOURS) {
            Map<List<BlockyBodyMesh.Face>, int[]> m = COLOURS.get(map);
            return m == null ? null : m.get(faces);
        }
    }

    private static final java.util.Set<Object> COLOURING = ConcurrentHashMap.newKeySet();

    /** Whether a body size's colours are ready; if not, they are worked out in the background (big balls take a moment). */
    private static boolean coloursReady(ColorMap map, BlockyBodyMesh.Style style, int n) {
        if (!BlockyBodyMesh.ready(style, n)) {
            return false;
        }
        List<BlockyBodyMesh.Face> faces = BlockyBodyMesh.body(style, n);
        if (readyColours(map, faces) != null) {
            return true;
        }
        if (COLOURING.add(List.of(map, faces))) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                int[] out = computeColours(map, faces, false);
                synchronized (COLOURS) {
                    COLOURS.computeIfAbsent(map, m -> new java.util.IdentityHashMap<>()).put(faces, out);
                }
            });
        }
        return false;
    }

    private static int[] computeColours(ColorMap map, List<BlockyBodyMesh.Face> fs, boolean ring) {
        int[] out = new int[fs.size()];
        // block size as an angle on the unit sphere, from the face's half-edge
        float footprint = fs.isEmpty() ? 0F : 2F * fs.get(0).u().length();
        for (int i = 0; i < fs.size(); i++) {
            Vector3f d = fs.get(i).surface();
            out[i] = ring ? map.argb(d.x, 0) : average(map, BlockyBodyMesh.longitude(d), BlockyBodyMesh.latitude(d), footprint);
        }
        return out;
    }

    private static int average(ColorMap map, double lon, double lat, double size) {
        double dLat = size / 3;
        double dLon = dLat / Math.max(0.15, Math.cos(lat));
        int r = 0, g = 0, b = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int c = map.argb(lon + i * dLon, Mth.clamp(lat + j * dLat, -Math.PI / 2, Math.PI / 2));
                r += c >> 16 & 255;
                g += c >> 8 & 255;
                b += c & 255;
            }
        }
        return 0xFF000000 | (r / 9) << 16 | (g / 9) << 8 | b / 9;
    }

    private static final Map<String, ColorMap> SURFACES = new ConcurrentHashMap<>();

    /**
     * The detailed surface map of a body for close views (orbit, approach): Earth with its clouds, the Moon, Mars, Venus
     * and Mercury, drawn by code at 512-2048 pixels around. Null for bodies without one (use their sky disc colours).
     *
     * @param body body id path, e.g. "earth"
     */
    public static ColorMap surface(String body) {
        return switch (body) {
            case "earth" -> SURFACES.computeIfAbsent(body, k -> {
                ColorMap m = withClouds(texture(Spacebreaker.id("textures/environment/orbit_earth.png")),
                        texture(Spacebreaker.id("textures/environment/orbit_earth_clouds.png")));
                PIXEL_MAPS.put(m, new PixelMap(Spacebreaker.id("textures/environment/orbit_earth.png"),
                        Spacebreaker.id("textures/environment/orbit_earth_clouds.png"), 0.5F));
                return m;
            });
            case "moon" -> SURFACES.computeIfAbsent(body, k -> {
                ColorMap m = texture(Spacebreaker.id("textures/environment/orbit_moon.png"));
                return m;
            });
            // these maps start at longitude 0 on the left edge
            case "mars", "venus", "mercury" -> SURFACES.computeIfAbsent(body, k -> {
                ColorMap m = texture(Spacebreaker.id("textures/environment/orbit_" + k + ".png"));
                ColorMap shifted = (lon, lat) -> m.argb(lon + Math.PI, lat);
                PIXEL_MAPS.put(shifted, new PixelMap(Spacebreaker.id("textures/environment/orbit_" + k + ".png"), null, 1F));
                return shifted;
            });
            default -> null;
        };
    }

    // ------------------------------------------------------------------ pixel sphere (close views)

    /**
     * A surface map as a texture for the close-up pixel sphere: equirectangular, longitude 0 at {@code uOffset} across it.
     *
     * @param clouds optional cloud layer (alpha = cover), drawn a little above the ground
     */
    public record PixelMap(ResourceLocation surface, ResourceLocation clouds, float uOffset) {
    }

    private static final Map<ColorMap, PixelMap> PIXEL_MAPS = new ConcurrentHashMap<>();

    /** Angular radius (radians) where a body starts turning from a ball of blocks into a pixel sphere, and where it is one. */
    static final float PIXEL_FROM = 0.55F;
    static final float PIXEL_FULL = 0.68F;

    /** The texture behind a colour map made by {@link #texture} or {@link #surface}, for the pixel sphere; null if none. */
    static PixelMap pixelMap(ColorMap map) {
        // only when chosen in the client settings (planetStyle = PIXEL), or for side-by-side checks (-Dspacebreaker.pixelSphere=true);
        // the default keeps balls of blocks at every size, with the fine surface patch close up
        boolean on = SkyConfig.planetStyle() == SkyConfig.PlanetStyle.PIXEL || Boolean.getBoolean("spacebreaker.pixelSphere");
        return on ? PIXEL_MAPS.get(map) : null;
    }

    /**
     * How far a body is into the pixel-sphere look (0 = ball of blocks, 1 = pixel sphere): when it fills much of the view
     * (seen from orbit), stepped blocks the size of houses would show, so it becomes a smooth sphere wrapped in its pixel-art
     * map (sharp texels, nearest filtering): the Minecraft feel comes from the pixels then, not from the steps.
     */
    static float pixelBlend(Pose body) {
        float ang = (float) Math.asin(Math.min(1F, body.radius() / Math.max(1e-3F, body.center().length())));
        float t = Mth.clamp((ang - PIXEL_FROM) / (PIXEL_FULL - PIXEL_FROM), 0F, 1F);
        return t * t * (3 - 2 * t);
    }

    /**
     * A smooth sphere wrapped in a pixel texture (nearest filtering), lit in hard steps like the blocky bodies, with an
     * optional cloud layer just above it. Back faces are culled (a sphere is convex), so it needs no depth.
     */
    public static void drawPixelSphere(Matrix4f pose, PixelMap tex, Pose body, Vector3f sun, float night, float tint, float alpha) {
        Matrix3f rot = body.rotation();
        Vector3f s = new Vector3f(sun).normalize();
        sphere(pose, tex.surface(), tex.uOffset(), rot, body.center(), body.radius(), s, night, tint, alpha, false);
        if (tex.clouds() != null) {
            sphere(pose, tex.clouds(), tex.uOffset(), rot, body.center(), body.radius() * 1.006F, s, night, tint, alpha, true);
        }
    }

    private static void sphere(Matrix4f pose, ResourceLocation texture, float uOffset, Matrix3f rot, Vector3f centre, float radius, Vector3f sun,
                               float night, float tint, float alpha, boolean clouds) {
        int lon = 192;
        int lat = 96;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Vector3f p = new Vector3f();
        for (int j = 0; j < lat; j++) {
            for (int i = 0; i < lon; i++) {
                // one light per quad, in hard steps (the day/night line steps like the blocky bodies)
                double mlon = Math.PI * 2 * (i + 0.5) / lon - Math.PI;
                double mlat = Math.PI / 2 - Math.PI * (j + 0.5) / lat;
                Vector3f mid = rot.transform(dir(mlon, mlat, new Vector3f()));
                float light = BlockyBodyMesh.light(mid, mid, sun, clouds ? night * 0.6F : night) * tint;
                int c = (int) (Mth.clamp(light, 0, 1) * 255);
                int a = (int) (Mth.clamp(alpha, 0, 1) * 255);
                // counter-clockwise seen from outside: down, right, up
                int[][] corners = {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}};
                for (int[] k : corners) {
                    double vl = Math.PI * 2 * k[0] / lon - Math.PI;
                    double vb = Math.PI / 2 - Math.PI * k[1] / lat;
                    rot.transform(dir(vl, vb, p)).mul(radius).add(centre);
                    float u = (float) k[0] / lon - 0.5F + uOffset;
                    bb.addVertex(pose, p.x, p.y, p.z).setUv(u, (float) k[1] / lat).setColor(c, c, c, a);
                }
            }
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.disableCull();
    }

    /** Body-space direction of a longitude and latitude (pole +Y, longitude 0 +Z, east +X). */
    private static Vector3f dir(double lon, double lat, Vector3f out) {
        return out.set((float) (Math.cos(lat) * Math.sin(lon)), (float) Math.sin(lat), (float) (Math.cos(lat) * Math.cos(lon)));
    }

    /** Forgets loaded textures, colours and buffers (resource reload, leaving a world). */
    public static void reset() {
        PIXELS.clear();
        SIZES.clear();
        for (Map<ColorMap, Cached[]> table : List.of(BODY_BUFFERS, FLAT_BUFFERS, RING_BUFFERS)) {
            for (Cached[] row : table.values()) {
                for (Cached c : row) {
                    if (c != null && c.buffer != null) {
                        c.buffer.close();
                    }
                }
            }
            table.clear();
        }
        for (Patch p : PATCHES.values()) {
            if (p.buffer.buffer != null) {
                p.buffer.buffer.close();
            }
        }
        LODS.clear();
        COLOURING.clear();
        PATCHES.clear();
        LAST_PATCH.clear();
        LAST_WANTED.clear();
        synchronized (COLOURS) {
            COLOURS.clear();
        }
    }

    private static int[] pixels(ResourceLocation id) {
        int[] px = PIXELS.get(id);
        if (px != null) {
            return px;
        }
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(id); NativeImage img = NativeImage.read(in)) {
            int w = img.getWidth();
            int h = img.getHeight();
            int[] out = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int abgr = img.getPixelRGBA(x, y);
                    out[y * w + x] = (abgr & 0xFF00FF00) | (abgr & 255) << 16 | (abgr >> 16 & 255);
                }
            }
            SIZES.put(id, new int[]{w, h});
            PIXELS.put(id, out);
            return out;
        } catch (Exception e) {
            Spacebreaker.LOGGER.warn("Could not read the surface map {}", id, e);
            PIXELS.put(id, new int[]{0xFF808080});
            SIZES.put(id, new int[]{1, 1});
            return PIXELS.get(id);
        }
    }
}
