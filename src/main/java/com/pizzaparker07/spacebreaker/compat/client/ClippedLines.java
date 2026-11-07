package com.pizzaparker07.spacebreaker.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Outline boxes for {@code RenderType.lines()} that stay right under shader packs. Vanilla's line shader copes with a
 * segment that runs behind the camera; Iris's line programs widen each segment in screen space and turn such a
 * segment into a large flat quad across the view. So every edge is cut where it crosses a plane just in front of the
 * camera, and edges wholly behind it are dropped. Without a pack this draws exactly what
 * {@code LevelRenderer.renderLineBox} would on screen.
 */
public final class ClippedLines {
    /** Distance of the cutting plane in front of the camera, a little past the near plane of every pack we know. */
    private static final double NEAR = 0.2;

    private ClippedLines() {
    }

    /**
     * Draws the 12 edges of {@code box} (world coordinates). {@code pose} must already be translated by minus the
     * camera position, as for {@code LevelRenderer.renderLineBox}.
     */
    public static void box(PoseStack pose, VertexConsumer lines, AABB box, Camera camera, float r, float g, float b, float a) {
        edges(pose, lines, box, camera.getPosition(), camera.getLookVector(), r, g, b, a);
    }

    /**
     * Draws the 12 edges of {@code box} given relative to the camera (already moved by minus the camera position,
     * with {@code pose} not translated), the other way {@code LevelRenderer.renderLineBox} is used.
     */
    public static void boxRelative(PoseStack pose, VertexConsumer lines, AABB box, Camera camera, float r, float g, float b, float a) {
        edges(pose, lines, box, Vec3.ZERO, camera.getLookVector(), r, g, b, a);
    }

    private static void edges(PoseStack pose, VertexConsumer lines, AABB box, Vec3 cam, Vector3f look,
                              float r, float g, float b, float a) {
        double x0 = box.minX, y0 = box.minY, z0 = box.minZ, x1 = box.maxX, y1 = box.maxY, z1 = box.maxZ;
        double[][] corners = {
                {x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0},
                {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            segment(pose, lines, cam, look, corners[e[0]], corners[e[1]], r, g, b, a);
        }
    }

    static void segment(PoseStack pose, VertexConsumer lines, Vec3 cam, Vector3f look, double[] p, double[] q,
                        float r, float g, float b, float a) {
        double dp = ahead(cam, look, p), dq = ahead(cam, look, q);
        if (dp < 0 && dq < 0) {
            return;
        }
        double[] from = p, to = q;
        if (dp < 0) {
            from = cut(p, q, dp, dq);
        } else if (dq < 0) {
            to = cut(p, q, dp, dq);
        }
        float nx = (float) (to[0] - from[0]), ny = (float) (to[1] - from[1]), nz = (float) (to[2] - from[2]);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-6F) {
            return;
        }
        nx /= len;
        ny /= len;
        nz /= len;
        PoseStack.Pose last = pose.last();
        lines.addVertex(last, (float) from[0], (float) from[1], (float) from[2]).setColor(r, g, b, a).setNormal(last, nx, ny, nz);
        lines.addVertex(last, (float) to[0], (float) to[1], (float) to[2]).setColor(r, g, b, a).setNormal(last, nx, ny, nz);
    }

    /** How far {@code p} is in front of the cutting plane (negative: behind it). */
    static double ahead(Vec3 cam, Vector3f look, double[] p) {
        return (p[0] - cam.x) * look.x() + (p[1] - cam.y) * look.y() + (p[2] - cam.z) * look.z() - NEAR;
    }

    static double[] cut(double[] p, double[] q, double dp, double dq) {
        double t = dp / (dp - dq);
        return new double[]{p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t, p[2] + (q[2] - p[2]) * t};
    }
}
