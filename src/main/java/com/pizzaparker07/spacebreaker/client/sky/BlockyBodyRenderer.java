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
        return MAPS.computeIfAbsent(id, BlockyBodyRenderer::textureMap);
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
     * @param n      blocks across, or 0 to choose by size on screen (steps through {@link #LEVELS}, fading between them)
     * @param ring   optional ring
     */
    public static void draw(Matrix4f pose, BlockyBodyMesh.Style style, int n, ColorMap map, Pose body, Vector3f sun, float night,
                            float tint, float alpha, Ring ring) {
        render(pose, style, n, map, body, sun, night, tint, alpha, ring, null);
    }

    /** Draws a body on a flat view looking down -Z (GUI screens: x right, y down). */
    public static void drawFlat(Matrix4f pose, BlockyBodyMesh.Style style, int n, ColorMap map, Pose body, Vector3f sun, float night,
                                float tint, float alpha, Ring ring) {
        render(pose, style, n, map, body, sun, night, tint, alpha, ring, new Vector3f(0, 0, -1));
    }

    /** Voxel resolutions a body steps through as it grows on screen. */
    public static final int[] LEVELS = {16, 32, 64, 128};
    /** Screen pixels per block a level aims for: finer levels come in only when blocks would get this big. */
    static final float BLOCK_PX = 14F;
    /** Length of the cross-fade between two levels, ms. */
    static final long FADE_MS = 350;

    private record CacheKey(ColorMap map, BlockyBodyMesh.Style style, int n) {
    }

    private static final class Cached {
        com.mojang.blaze3d.vertex.VertexBuffer buffer;
        float[] signature;
        boolean empty;
    }

    private static final class Lod {
        int current;
        int previous;
        long since;
    }

    private static final Map<CacheKey, Cached> BUFFERS = new java.util.HashMap<>();
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
        if (n <= 0) {
            Lod lod = lod(map, style, body);
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
            immediate(pose, style, level, map, body, sun, night, tint, alpha, ring, null, false);
        }
        RenderSystem.enableCull();
        RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX); // while the mask is still open
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        timing(System.nanoTime() - t0, false);
    }

    /** The resolution to draw a body at, stepping up as it grows on screen and fading between steps. */
    private static Lod lod(ColorMap map, BlockyBodyMesh.Style style, Pose body) {
        Minecraft mc = Minecraft.getInstance();
        double distance = Math.max(1e-3, body.center().length());
        double fov = Math.toRadians(mc.options.fov().get());
        double pixels = 2 * body.radius() / distance / (2 * Math.tan(fov / 2)) * mc.getWindow().getHeight();
        double want = pixels / BLOCK_PX;
        Lod lod = LODS.computeIfAbsent(new CacheKey(map, style, 0), k -> {
            Lod l = new Lod();
            l.current = LEVELS[0];
            return l;
        });
        for (int l : LEVELS) {
            BlockyBodyMesh.prepare(style, l); // build every size in the background once
        }
        if (lod.previous != 0) {
            return lod; // finish the running fade first
        }
        int index = java.util.Arrays.binarySearch(LEVELS, lod.current);
        int next = lod.current;
        if (want > lod.current * 1.25 && index + 1 < LEVELS.length) {
            next = LEVELS[index + 1];
        } else if (want < lod.current / 2.0 * 0.8 && index > 0) {
            next = LEVELS[index - 1];
        }
        if (next != lod.current && BlockyBodyMesh.ready(style, next)) {
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
        if (!BlockyBodyMesh.ready(style, n) && n > LEVELS[0]) {
            BlockyBodyMesh.prepare(style, n);
            n = LEVELS[0];
        }
        CacheKey key = new CacheKey(map, style, n);
        Cached c = BUFFERS.computeIfAbsent(key, k -> new Cached());
        Matrix3f rot = body.rotation(style);
        float[] sig = {rot.m00, rot.m01, rot.m02, rot.m10, rot.m11, rot.m12, rot.m20, rot.m21, rot.m22, sun.x, sun.y, sun.z,
                body.center().x, body.center().y, body.center().z, body.radius(), night, tint, alpha};
        if (c.buffer == null || !same(sig, c.signature, n)) {
            long t0 = System.nanoTime();
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            int count = emitBody(bb, style, n, map, body, rot, sun, night, tint, alpha);
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
            timing(System.nanoTime() - t0, true);
        }
        if (c.empty) {
            return;
        }
        c.buffer.bind();
        c.buffer.drawWithShader(pose, RenderSystem.getProjectionMatrix(), GameRenderer.getPositionColorShader());
        com.mojang.blaze3d.vertex.VertexBuffer.unbind();
    }

    /**
     * Whether a cached build still shows the body closely enough: the turn and the sunlight may drift by about a quarter
     * of a block before the buffer is rebuilt (so the body turns in small block steps, as Minecraft things do), position
     * and size by a thousandth, brightness by a hundredth.
     */
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
    private static int emitBody(BufferBuilder bb, BlockyBodyMesh.Style style, int n, ColorMap map, Pose body, Matrix3f rot,
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
        List<BlockyBodyMesh.Face> faces = BlockyBodyMesh.body(style, n);
        int[] colours = colours(map, faces, false);
        int count = 0;
        for (int i = 0; i < faces.size(); i++) {
            BlockyBodyMesh.Face f = faces.get(i);
            Vector3f normal = rot.transform(new Vector3f(f.normal()));
            Vector3f centre = rot.transform(new Vector3f(f.center())).mul(body.radius()).add(body.center());
            if (normal.dot(centre) >= 0) {
                continue; // faces away from the viewer
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
            for (int i = 0; i < faces.size(); i++) {
                BlockyBodyMesh.Face f = faces.get(i);
                Vector3f normal = rot.transform(new Vector3f(f.normal()));
                Vector3f centre = rot.transform(new Vector3f(f.center())).mul(body.radius()).add(body.center());
                float toward = viewDir == null ? normal.dot(centre) : normal.dot(viewDir);
                if (toward >= 0) {
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

    /** Map colours of every face, cached per map and mesh. */
    private static int[] colours(ColorMap map, List<BlockyBodyMesh.Face> faces, boolean ring) {
        synchronized (COLOURS) {
            return COLOURS.computeIfAbsent(map, m -> new java.util.IdentityHashMap<>()).computeIfAbsent(faces, fs -> {
                int[] out = new int[fs.size()];
                for (int i = 0; i < fs.size(); i++) {
                    Vector3f d = fs.get(i).surface();
                    out[i] = ring ? map.argb(d.x, 0) : map.argb(BlockyBodyMesh.longitude(d), BlockyBodyMesh.latitude(d));
                }
                return out;
            });
        }
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
