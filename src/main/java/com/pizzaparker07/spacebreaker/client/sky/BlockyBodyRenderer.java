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
        Vector3f u = new Vector3f(c).div(d);
        // the outline of a sphere seen from the camera: a circle in the plane square to the line of sight
        float inner = r * (float) Math.sqrt(1 - (r * r) / (d * d));
        float outer = ra * (float) Math.sqrt(1 - (ra * ra) / (d * d));
        Vector3f ci = new Vector3f(u).mul(d - r * r / d);
        Vector3f co = new Vector3f(u).mul(d - ra * ra / d);
        Vector3f a = Math.abs(u.y) < 0.9F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f e1 = new Vector3f(a).sub(new Vector3f(u).mul(u.dot(a))).normalize();
        Vector3f e2 = new Vector3f(u).cross(e1).normalize();
        Vector3f s = new Vector3f(sun).normalize();
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
            double t0 = Math.PI * 2 * i / segs;
            double t1 = Math.PI * 2 * (i + 1) / segs;
            Vector3f dir = new Vector3f(e1).mul((float) Math.cos((t0 + t1) / 2)).add(new Vector3f(e2).mul((float) Math.sin((t0 + t1) / 2)));
            float day = Mth.clamp(dir.dot(s) * 2F + 0.5F, 0.12F, 1F);
            for (int k = 0; k < steps; k++) {
                float f0 = (float) k / steps;
                float f1 = (float) (k + 1) / steps;
                // hard steps: each ring of the band one shade, thinning outward
                int al = (int) (Mth.clamp(alpha * (1 - f0) * day, 0, 1) * 255);
                for (float[] v : new float[][]{{f0, (float) t0}, {f1, (float) t0}, {f1, (float) t1}, {f0, (float) t1}}) {
                    float rad = Mth.lerp(v[0], inner, outer);
                    Vector3f cen = new Vector3f(ci).lerp(co, v[0]);
                    Vector3f p = new Vector3f(e1).mul(rad * (float) Math.cos(v[1])).add(new Vector3f(e2).mul(rad * (float) Math.sin(v[1]))).add(cen);
                    bb.addVertex(pose, p.x, p.y, p.z).setColor(cr, cg, cb, al);
                }
            }
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.enableCull();
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

    private static final Map<Object, Cached> BUFFERS = new java.util.HashMap<>();
    private static final Map<CacheKey, Lod> LODS = new java.util.HashMap<>();
    private static long rebuildNanos;
    private static int rebuilds;
    private static long drawNanos;
    private static int draws;
    private static long lastReport;

    private static void render(Matrix4f pose, BlockyBodyMesh.Style style, int n, ColorMap map, Pose body, Vector3f sun, float night,
                               float tint, float alpha, Ring ring, Vector3f viewDir) {
        if (viewDir != null) {
            immediate(pose, style, n, map, body, sun, night, tint, alpha, ring, viewDir, true);
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
            immediate(pose, style, Math.max(level, LEVELS[0]), map, body, sun, night, tint, alpha, ring, null, false);
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
        volatile List<BlockyBodyMesh.Face> faces;
        volatile int[] colours;

        Patch(float halfAngle) {
            this.halfAngle = halfAngle;
        }
    }

    private static final Map<PatchKey, Patch> PATCHES = new java.util.LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<PatchKey, Patch> e) {
            return size() > 6;
        }
    };
    private static final Map<ColorMap, PatchKey> LAST_PATCH = new java.util.HashMap<>();

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
        Matrix3f rot = body.rotation(style);
        // the camera's direction from the body's centre, in the body's own frame
        Vector3f toward = new Matrix3f(rot).transpose().transform(new Vector3f(body.center()).negate().normalize());
        float horizon = (float) Math.acos(Math.min(1F, body.radius() / dist));
        float half = horizon + 4F / n + 0.03F;
        float q = 8F / half;
        PatchKey key = new PatchKey(map, n, Math.round(toward.x * q), Math.round(toward.y * q), Math.round(toward.z * q));
        Patch patch = PATCHES.get(key);
        if (patch == null) {
            Patch p = new Patch(half * 1.2F);
            PATCHES.put(key, p);
            patch = p;
            Vector3f centre = new Vector3f(key.qx(), key.qy(), key.qz()).div(q).normalize();
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
        drawFaces(pose, key, patch.faces, patch.colours, key.n(), body, rot, sun, night, tint, alpha);
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
        Lod lod = LODS.computeIfAbsent(new CacheKey(map, style, 0), k -> {
            Lod l = new Lod();
            l.current = LEVELS[0];
            return l;
        });
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
        drawFaces(pose, new CacheKey(map, style, n), faces, colours(map, faces, false), n, body, body.rotation(style), sun, night, tint, alpha);
    }

    /** Draws faces from a vertex buffer kept under {@code key}, rebuilt only when what it shows changes. */
    private static void drawFaces(Matrix4f pose, Object key, List<BlockyBodyMesh.Face> faces, int[] colours, int n, Pose body, Matrix3f rot,
                                  Vector3f sun, float night, float tint, float alpha) {
        Cached c = BUFFERS.computeIfAbsent(key, k -> new Cached());
        float[] sig = {rot.m00, rot.m01, rot.m02, rot.m10, rot.m11, rot.m12, rot.m20, rot.m21, rot.m22, sun.x, sun.y, sun.z,
                body.center().x, body.center().y, body.center().z, body.radius(), night, tint, alpha};
        if (c.buffer == null || c.faces != faces || !same(sig, c.signature, n)) {
            long t0 = System.nanoTime();
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            int count = emitBody(bb, faces, colours, body, rot, sun, night, tint, alpha);
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
            c.signature = sig;
            c.faces = faces;
            timing(System.nanoTime() - t0, true);
        }
        if (c.empty) {
            return;
        }
        c.buffer.bind();
        c.buffer.drawWithShader(pose, RenderSystem.getProjectionMatrix(), GameRenderer.getPositionColorShader());
        com.mojang.blaze3d.vertex.VertexBuffer.unbind();
    }

    private static boolean same(float[] a, float[] b, int n) {
        if (b == null || a.length != b.length) {
            return false;
        }
        float turn = 0.25F * 2F / n;
        for (int i = 0; i < a.length; i++) {
            float eps = i < 12 ? turn : i < 16 ? 1e-3F * Math.max(1F, Math.abs(a[i])) : 0.01F;
            if (Math.abs(a[i] - b[i]) > eps) {
                return false;
            }
        }
        return true;
    }

    /** The body's faces toward the viewer into {@code bb}, in the camera-centred frame (the pose matrix is applied when drawing). */
    private static int emitBody(BufferBuilder bb, List<BlockyBodyMesh.Face> faces, int[] colours, Pose body, Matrix3f rot,
                                Vector3f sun, float night, float tint, float alpha) {
        Vector3f s = new Vector3f(sun).normalize();
        Vector3f look = new Vector3f(body.center()).normalize();
        Vector3f screenUp = new Vector3f(0, 1, 0);
        screenUp.sub(new Vector3f(look).mul(look.dot(screenUp)));
        if (screenUp.lengthSquared() < 1e-6F) {
            screenUp.set(0, 0, -1);
        }
        screenUp.normalize();
        Vector3f screenRight = new Vector3f(look).cross(screenUp).normalize();
        boolean flat = depthKept(body) < 0.5F;
        int count = 0;
        for (int i = 0; i < faces.size(); i++) {
            BlockyBodyMesh.Face f = faces.get(i);
            Vector3f normal = rot.transform(new Vector3f(f.normal()));
            Vector3f centre = rot.transform(new Vector3f(f.center())).mul(body.radius()).add(body.center());
            if (facesAway(normal, centre, look, flat)) {
                continue;
            }
            float light = BlockyBodyMesh.light(rot.transform(new Vector3f(f.surface())), normal, s, night)
                    * BlockyBodyMesh.blockShade(normal.dot(screenUp), normal.dot(screenRight)) * tint;
            put(bb, null, quad(f, rot, body, colours[i], light, alpha, null, look));
            count++;
        }
        return count;
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
    private static void timing(long nanos, boolean rebuild) {
        if (!Boolean.getBoolean("spacebreaker.blockyTiming")) {
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
            Spacebreaker.LOGGER.info("Blocky bodies: {} draws, {} ms each on average ({} rebuilds, {} ms each); levels {}",
                    draws, String.format("%.3f", drawNanos / 1e6 / draws), rebuilds,
                    String.format("%.3f", rebuilds == 0 ? 0 : rebuildNanos / 1e6 / rebuilds),
                    LODS.values().stream().map(l -> l.current).toList());
            lastReport = now;
            drawNanos = rebuildNanos = 0;
            draws = rebuilds = 0;
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
        BUFFERS.values().forEach(c -> {
            if (c.buffer != null) {
                c.buffer.close();
            }
        });
        BUFFERS.clear();
        LODS.clear();
        COLOURING.clear();
        PATCHES.clear();
        LAST_PATCH.clear();
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
