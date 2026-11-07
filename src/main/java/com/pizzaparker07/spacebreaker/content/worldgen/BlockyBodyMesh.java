package com.pizzaparker07.spacebreaker.content.worldgen;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Planets and moons seen from outside, Minecraft style: either a ball built from blocks (stepped, pixel-coloured
 * voxels) or a slowly turning cube with pixel faces. This is the shape and light maths only (no rendering), so it can
 * be checked without a client; client/sky/BlockyBodyRenderer draws it.
 * <p>
 * Body space: a unit sphere, north pole along +Y, longitude 0 along +Z (facing the viewer before any spin), longitude
 * 90 E along +X. Every face is a square tile: centre, two half-edge vectors, outward normal and the surface direction
 * its colour comes from.
 */
public final class BlockyBodyMesh {
    /** How bodies are drawn. */
    public enum Style {
        /** A sphere of voxels: stepped edges, one colour per block. */
        VOXEL,
        /** A cube whose six faces are pixel grids. */
        CUBE
    }

    /** One square tile; {@code surface} is the unit direction whose map colour it shows. */
    public record Face(Vector3f center, Vector3f u, Vector3f v, Vector3f normal, Vector3f surface) {
    }

    /** Half the cube's edge in body units: its silhouette is about as big as the unit sphere's. */
    public static final float CUBE_HALF = 0.8F;

    private static final Map<String, List<Face>> CACHE = new ConcurrentHashMap<>();

    private BlockyBodyMesh() {
    }

    /** The faces of a body in {@code style} with {@code n} blocks across (cached; do not modify the list). */
    public static List<Face> body(Style style, int n) {
        int size = Math.max(2, n);
        return CACHE.computeIfAbsent(style + "/" + size, k -> style == Style.VOXEL ? voxels(size) : cube(size));
    }

    /**
     * A flat ring in the equator plane made of square tiles, between {@code inner} and {@code outer} body radii,
     * {@code n} tiles across the outer diameter. {@code surface.x} carries the tile's radius as a fraction 0..1 from the
     * inner to the outer edge (for colour bands); its normal is +Y.
     */
    public static List<Face> ring(float inner, float outer, int n) {
        return CACHE.computeIfAbsent("ring/" + inner + "/" + outer + "/" + n, k -> {
            List<Face> out = new ArrayList<>();
            float cell = 2 * outer / n;
            float h = cell / 2;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    float x = -outer + (i + 0.5F) * cell;
                    float z = -outer + (j + 0.5F) * cell;
                    float r = (float) Math.sqrt(x * x + z * z);
                    if (r < inner || r > outer) {
                        continue;
                    }
                    out.add(new Face(new Vector3f(x, 0, z), new Vector3f(h, 0, 0), new Vector3f(0, 0, h), new Vector3f(0, 1, 0),
                            new Vector3f((r - inner) / (outer - inner), 0, 0)));
                }
            }
            return List.copyOf(out);
        });
    }

    /** Voxels whose centres lie inside the unit sphere; only faces open to the outside. */
    static List<Face> voxels(int n) {
        float cell = 2F / n;
        float h = cell / 2;
        boolean[][][] in = new boolean[n][n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    // a little past the sphere so the stepped outline is not eaten away at the equator and poles
                    in[i][j][k] = sq(centre(i, n).x) + sq(centre(j, n).x) + sq(centre(k, n).x) <= 1.0F + 0.5F / n;
                }
            }
        }
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        List<Face> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    if (!in[i][j][k]) {
                        continue;
                    }
                    Vector3f c = new Vector3f(centre(i, n).x, centre(j, n).x, centre(k, n).x);
                    Vector3f surface = new Vector3f(c).normalize();
                    for (int[] d : dirs) {
                        int a = i + d[0], b = j + d[1], e = k + d[2];
                        if (a >= 0 && a < n && b >= 0 && b < n && e >= 0 && e < n && in[a][b][e]) {
                            continue;
                        }
                        Vector3f normal = new Vector3f(d[0], d[1], d[2]);
                        out.add(new Face(new Vector3f(normal).mul(h).add(c), side(normal, h, true), side(normal, h, false), normal, surface));
                    }
                }
            }
        }
        return List.copyOf(out);
    }

    /** Six faces of {@code n}×{@code n} pixels each; each pixel shows the map colour in its own direction. */
    static List<Face> cube(int n) {
        float h = CUBE_HALF / n;
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        List<Face> out = new ArrayList<>();
        for (int[] d : dirs) {
            Vector3f normal = new Vector3f(d[0], d[1], d[2]);
            Vector3f u = side(normal, 1, true);
            Vector3f v = side(normal, 1, false);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    float a = (-1 + (i + 0.5F) * 2F / n) * CUBE_HALF;
                    float b = (-1 + (j + 0.5F) * 2F / n) * CUBE_HALF;
                    Vector3f c = new Vector3f(normal).mul(CUBE_HALF).add(new Vector3f(u).mul(a)).add(new Vector3f(v).mul(b));
                    out.add(new Face(c, new Vector3f(u).mul(h), new Vector3f(v).mul(h), normal, new Vector3f(c).normalize()));
                }
            }
        }
        return List.copyOf(out);
    }

    /**
     * Brightness (night..1) of a face: the day/night split follows the sphere ({@code surface}) in hard steps, like
     * a block lit by the sun, and each face is shaded by how squarely it meets the sun (Minecraft's side shading).
     *
     * @param surface face surface direction, already rotated into the same frame as {@code sun}
     * @param normal  face normal in that frame
     * @param sun     unit direction toward the sun
     * @param night   brightness of the dark side (0..1)
     */
    public static float light(Vector3f surface, Vector3f normal, Vector3f sun, float night) {
        float day = surface.dot(sun);
        float lit;
        if (day > 0.25F) {
            lit = 1F;
        } else if (day > 0.05F) {
            lit = 0.8F;
        } else if (day > -0.08F) {
            lit = 0.45F; // one step of twilight on the terminator
        } else {
            return night;
        }
        float facing = normal.dot(sun);
        float side = facing > 0.5F ? 1F : facing > 0F ? 0.85F : 0.7F;
        return Math.max(night, lit * side);
    }

    /** Longitude (radians, -pi..pi, 0 toward +Z, east toward +X) and latitude of a body-space direction. */
    public static double longitude(Vector3f d) {
        return Math.atan2(d.x, d.z);
    }

    public static double latitude(Vector3f d) {
        return Math.asin(Math.max(-1, Math.min(1, d.y / d.length())));
    }

    private static Vector3f centre(int i, int n) {
        return new Vector3f(-1 + (i + 0.5F) * 2F / n, 0, 0);
    }

    private static float sq(float x) {
        return x * x;
    }

    /** The two in-plane half-edge directions of a face with this normal, ordered so u × v = normal (counter-clockwise). */
    private static Vector3f side(Vector3f normal, float h, boolean first) {
        Vector3f u;
        Vector3f v;
        if (normal.x != 0) {
            u = new Vector3f(0, 0, -normal.x);
            v = new Vector3f(0, 1, 0);
        } else if (normal.y != 0) {
            u = new Vector3f(1, 0, 0);
            v = new Vector3f(0, 0, -normal.y);
        } else {
            u = new Vector3f(normal.z, 0, 0);
            v = new Vector3f(0, 1, 0);
        }
        return (first ? u : v).mul(h);
    }
}
