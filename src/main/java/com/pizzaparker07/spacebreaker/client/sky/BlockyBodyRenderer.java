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

    /** A colour map from an equirectangular surface texture (longitude -180..180 left to right, north at the top). */
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

    private static void render(Matrix4f pose, BlockyBodyMesh.Style style, int n, ColorMap map, Pose body, Vector3f sun, float night,
                               float tint, float alpha, Ring ring, Vector3f viewDir) {
        Matrix3f rot = body.rotation(style);
        Vector3f s = new Vector3f(sun).normalize();
        List<Quad> quads = new ArrayList<>();
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
            float light = BlockyBodyMesh.light(rot.transform(new Vector3f(f.surface())), normal, s, night) * tint;
            quads.add(quad(f, rot, body, colours[i], light, alpha, viewDir));
        }
        if (ring != null) {
            List<BlockyBodyMesh.Face> tiles = BlockyBodyMesh.ring(ring.inner(), ring.outer(), Math.max(24, n * 3));
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
                quads.add(quad(f, rot, body, c, lit * tint, alpha * ((c >>> 24) / 255F), viewDir));
            }
        }
        if (quads.isEmpty()) {
            return;
        }
        quads.sort((a, b) -> Float.compare(b.depth(), a.depth()));
        boolean sky = viewDir == null;
        if (sky) {
            // the stepped body has hidden corners: draw it with its own depth, then clear that depth again so the
            // terrain drawn after the sky is not hidden behind a planet 100 blocks away (the sky comes before terrain,
            // so nothing else has written depth yet)
            RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (Quad q : quads) {
            float[] v = q.xyz();
            int c = q.argb();
            int a = c >>> 24, r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
            for (int k = 0; k < 4; k++) {
                bb.addVertex(pose, v[k * 3], v[k * 3 + 1], v[k * 3 + 2]).setColor(r, g, b, a);
            }
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.enableCull();
        if (sky) {
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        }
    }

    private static Quad quad(BlockyBodyMesh.Face f, Matrix3f rot, Pose body, int rgb, float light, float alpha, Vector3f viewDir) {
        Vector3f c = rot.transform(new Vector3f(f.center())).mul(body.radius()).add(body.center());
        Vector3f u = rot.transform(new Vector3f(f.u())).mul(body.radius());
        Vector3f v = rot.transform(new Vector3f(f.v())).mul(body.radius());
        float[] xyz = new float[12];
        int k = 0;
        for (float[] s : new float[][]{{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
            xyz[k++] = c.x + u.x * s[0] + v.x * s[1];
            xyz[k++] = c.y + u.y * s[0] + v.y * s[1];
            xyz[k++] = c.z + u.z * s[0] + v.z * s[1];
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

    /** Forgets loaded textures and colours (resource reload). */
    public static void reset() {
        PIXELS.clear();
        SIZES.clear();
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
