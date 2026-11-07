package com.pizzaparker07.spacebreaker.content.belt.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.List;

/**
 * Grabens (chasmata): long, straight valleys where a strip of crust dropped between two parallel faults - steep walls, a
 * flat floor, often later flooded smooth by icy lava with a low ridge down the middle. Several per world, in a fixed
 * list. Placed once per chunk.
 *
 * @param grabens each valley: centre (block x, latitude), length, floor width, depth and direction
 * @param wall    horizontal width of each wall (smaller = steeper)
 * @param ridge   height of the medial ridge on the floor (0 = none)
 * @param floor   block of the smooth floor
 * @param rock    rock of the walls
 */
public class GrabenFeature extends Feature<GrabenFeature.Config> {
    public record Graben(double x, double latitude, double length, double width, double depth, double angleDeg) {
        public static final Codec<Graben> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Graben::x),
                Codec.DOUBLE.fieldOf("latitude").forGetter(Graben::latitude),
                Codec.DOUBLE.fieldOf("length").forGetter(Graben::length),
                Codec.DOUBLE.fieldOf("width").forGetter(Graben::width),
                Codec.DOUBLE.fieldOf("depth").forGetter(Graben::depth),
                Codec.DOUBLE.optionalFieldOf("angle", 0.0).forGetter(Graben::angleDeg)
        ).apply(i, Graben::new));
    }

    public record Config(List<Graben> grabens, double wall, double ridge, BlockState floor, BlockState rock) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Graben.CODEC.listOf().fieldOf("grabens").forGetter(Config::grabens),
                Codec.DOUBLE.optionalFieldOf("wall", 10.0).forGetter(Config::wall),
                Codec.DOUBLE.optionalFieldOf("ridge", 3.0).forGetter(Config::ridge),
                BlockState.CODEC.fieldOf("floor").forGetter(Config::floor),
                BlockState.CODEC.fieldOf("rock").forGetter(Config::rock)
        ).apply(i, Config::new));
    }

    public GrabenFeature() {
        super(Config.CODEC);
    }

    /** Position across (0 = centre line, signed) and along a valley, with a gentle waver of the line. */
    public static double[] local(PlanetTerrain t, Graben g, double x, double z) {
        double a = Math.toRadians(g.angleDeg());
        double dx = x - g.x();
        double dz = z + g.latitude() / 90.0 * t.profile().poleDistance();
        double u = dx * Math.cos(a) + dz * Math.sin(a);
        double v = -dx * Math.sin(a) + dz * Math.cos(a);
        SimplexNoise n = Columns.noise(t.seed(), 67);
        v += 10 * n.getValue(u / 220.0, g.x() / 100.0);
        return new double[]{u, v};
    }

    /** Height change and whether the column is on a valley floor ({@code floorOut[0] = 1}). */
    public static double offset(PlanetTerrain t, Config cfg, double x, double z, double[] floorOut) {
        double best = 0;
        floorOut[0] = 0;
        for (Graben g : cfg.grabens()) {
            double[] uv = local(t, g, x, z);
            double half = g.length() / 2;
            if (Math.abs(uv[0]) > half) {
                continue;
            }
            double taper = Columns.smooth((half - Math.abs(uv[0])) / (g.length() * 0.12));
            double across = Math.abs(uv[1]) - g.width() / 2;
            double sink = 1 - Columns.smooth(across / cfg.wall());
            double dy = -g.depth() * taper * sink;
            if (across < 0) {
                floorOut[0] = 1;
                // the medial ridge of the flooded floor
                dy += cfg.ridge() * taper * Math.max(0, 1 - Math.abs(uv[1]) / (g.width() * 0.12));
            }
            if (dy < best) {
                best = dy;
            }
        }
        return best;
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        PlanetTerrain t = Columns.terrain(ctx.chunkGenerator());
        if (t == null) {
            return false;
        }
        Config cfg = ctx.config();
        WorldGenLevel level = ctx.level();
        int x0 = ctx.origin().getX() & ~15;
        int z0 = ctx.origin().getZ() & ~15;
        boolean any = false;
        double[] floor = new double[1];
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                int dy = (int) Math.round(offset(t, cfg, x + 0.5, z + 0.5, floor));
                if (dy == 0) {
                    continue;
                }
                int top = Columns.shift(level, x, z, dy, cfg.rock());
                Columns.paint(level, x, top, z, floor[0] > 0 ? 2 : 3, floor[0] > 0 ? cfg.floor() : cfg.rock());
                any = true;
            }
        }
        return any;
    }
}
