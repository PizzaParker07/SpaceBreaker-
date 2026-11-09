package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer;
import com.pizzaparker07.spacebreaker.client.sky.SkyConfig;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations;
import com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How the rocketry views draw a world in the chosen planet style (voxel ball or pixel cube): the colour map for each
 * body (kept, so the renderer's caches hold), its ring if it has one, and the two places it is drawn: the big body
 * below an orbit (and the ascent) and the small discs of the star map. With the classic style these return false and
 * the caller draws its textured disc as before.
 */
final class BodyLook {
    private static final Map<ResourceLocation, BlockyBodyRenderer.ColorMap> MAPS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, java.util.Optional<BlockyBodyRenderer.Ring>> RINGS = new ConcurrentHashMap<>();

    private BodyLook() {
    }

    @Nullable
    static BlockyBodyMesh.Style style() {
        return SkyConfig.blockyStyle();
    }

    static BlockyBodyRenderer.ColorMap map(ResourceLocation body) {
        return MAPS.computeIfAbsent(body, b -> {
            if (Location.EARTH.equals(b)) {
                return BlockyBodyRenderer.withClouds(BlockyBodyRenderer.texture(OrbitSkyEffects.EARTH), BlockyBodyRenderer.texture(OrbitSkyEffects.EARTH_CLOUDS));
            }
            if (Location.MOON.equals(b)) {
                return BlockyBodyRenderer.texture(OrbitSkyEffects.MOON);
            }
            return BlockyBodyRenderer.texture(OrbitSkyEffects.surface(b));
        });
    }

    @Nullable
    static BlockyBodyRenderer.Ring ring(ResourceLocation body) {
        return RINGS.computeIfAbsent(body, b -> Locations.body(b).flatMap(c -> c.rings().map(r -> {
            double km = Math.max(1, c.physical().radiusKm());
            int tone = 0xD8C8A0;
            return new BlockyBodyRenderer.Ring((float) (r.innerRadiusKm() / km), (float) (r.outerRadiusKm() / km),
                    (f, unused) -> (int) (150 - 60 * Math.abs(f - 0.5) * 2) << 24 | tone);
        }))).orElse(null);
    }

    /**
     * The big body below, seen from orbit or on the way up (camera at the origin, the body {@code below} it).
     *
     * @return false when the classic style is chosen (draw the textured sphere instead)
     */
    static boolean drawBelow(Matrix4f modelView, ResourceLocation body, Vector3f center, float radius, float spin, Vector3f sun,
                             float alpha, boolean air) {
        return drawBelow(modelView, body, center, radius, spin, sun, alpha, air, BlockyBodyRenderer.APPROACH);
    }

    /**
     * As above, at a fixed resolution {@code n} (blocks across the body), or {@link BlockyBodyRenderer#APPROACH} for the
     * close-up surface patch. A ball that grows or shrinks on screen uses a fixed resolution: the close-up patch is
     * built for one size and breaks up while the size runs.
     */
    static boolean drawBelow(Matrix4f modelView, ResourceLocation body, Vector3f center, float radius, float spin, Vector3f sun,
                             float alpha, boolean air, int n) {
        BlockyBodyMesh.Style st = style();
        if (st == null) {
            return false;
        }
        // pole along the horizon, longitude 0 facing up at the viewer: we look down on the tropics
        var pose = new BlockyBodyRenderer.Pose(center, radius, new Vector3f(0, 0, 1), new Vector3f(0, 1, 0), spin * (float) (Math.PI * 2));
        // close below an orbit: up to the finest level, or the steps show as big bands
        BlockyBodyRenderer.draw(modelView, st, n, map(body), pose, sun, 0.05F, 1F, alpha, null);
        if (air) {
            // a thin stepped band of air along the planet's outline (never over its face): the horizon from low orbit
            BlockyBodyRenderer.drawLimb(modelView, pose, sun, 0x7FB4FF, 0.85F * alpha, 0.025F);
        }
        return true;
    }

    /** A world as a small disc on a GUI (the star map), lit from the upper left. */
    static boolean drawFlat(net.minecraft.client.gui.GuiGraphics g, ResourceLocation body, float cx, float cy, float r, float spin) {
        BlockyBodyMesh.Style st = style();
        if (st == null) {
            return false;
        }
        var pose = new BlockyBodyRenderer.Pose(new Vector3f(cx, cy, 0), r, new Vector3f(0, -1, 0), new Vector3f(0, 0, 1), spin);
        // a fixed, modest resolution: about a GUI pixel per block at these sizes
        int n = BlockyBodyRenderer.resolution(st, r < 8);
        BlockyBodyRenderer.drawFlat(g.pose().last().pose(), st, n, map(body), pose, new Vector3f(-0.55F, -0.35F, 0.76F).normalize(),
                0.3F, 1F, 1F, ring(body));
        return true;
    }
}
