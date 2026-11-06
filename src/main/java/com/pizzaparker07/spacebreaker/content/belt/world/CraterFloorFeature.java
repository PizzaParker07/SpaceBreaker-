package com.pizzaparker07.spacebreaker.content.belt.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pizzaparker07.spacebreaker.content.worldgen.Crater;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bright deposits on the floors of some large craters, like the sodium-carbonate faculae of Ceres' Occator crater: a
 * central bright spot on a low dome (where brine welled up through the floor and dried), scattered smaller spots across
 * the floor, and the salt soaked a few blocks deep. Placed once per chunk; crater positions come from the planet
 * terrain, so a deposit continues seamlessly across chunk borders.
 *
 * @param block     the deposit
 * @param minRadius smallest crater that can hold a deposit
 * @param chance    share of those craters that do
 * @param extent    radius of the central spot in crater radii
 * @param spots     density of the scattered spots (0 = none)
 * @param dome      height of the central dome in blocks for the largest craters (0 = flat)
 * @param depth     how many blocks deep the deposit soaks
 * @param slump     height of the slumped, broken lobes on the inner wall (0 = leave the wall as generated)
 * @param debris    optional rock for blocks fallen from the wall onto the floor
 */
public class CraterFloorFeature extends Feature<CraterFloorFeature.Config> {
    public record Config(BlockState block, double minRadius, double chance, double extent, double spots, double dome, int depth,
                         double slump, Optional<BlockState> debris) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("block").forGetter(Config::block),
                Codec.DOUBLE.optionalFieldOf("min_radius", 14.0).forGetter(Config::minRadius),
                Codec.DOUBLE.optionalFieldOf("chance", 0.3).forGetter(Config::chance),
                Codec.DOUBLE.optionalFieldOf("extent", 0.16).forGetter(Config::extent),
                Codec.DOUBLE.optionalFieldOf("spots", 0.5).forGetter(Config::spots),
                Codec.DOUBLE.optionalFieldOf("dome", 3.0).forGetter(Config::dome),
                Codec.INT.optionalFieldOf("depth", 2).forGetter(Config::depth),
                Codec.DOUBLE.optionalFieldOf("slump", 0.0).forGetter(Config::slump),
                BlockState.CODEC.optionalFieldOf("debris").forGetter(Config::debris)
        ).apply(i, Config::new));
    }

    public CraterFloorFeature() {
        super(Config.CODEC);
    }

    /** True when this crater holds a deposit. */
    public static boolean holds(Crater c, Config cfg) {
        return c.radius() >= cfg.minRadius() && Columns.unit(c.seed(), 41) < cfg.chance();
    }

    /** Deposit strength at a column: > 1 central spot, 0..1 none; also the dome height through {@code domeOut}. */
    public static double strength(Crater c, Config cfg, SimplexNoise n, double x, double z, double[] domeOut) {
        double s = c.normalizedDistance(x, z);
        if (s > 0.85) {
            return 0;
        }
        double wobble = 0.75 + 0.5 * (Columns.fbm(n, x / 9.0, z / 9.0, 2) * 0.5 + 0.5);
        double core = cfg.extent() * wobble;
        if (s < core) {
            double t = 1 - s / core;
            domeOut[0] = Math.max(domeOut[0], cfg.dome() * Math.min(1, c.radius() / 60.0) * Columns.smooth(t * 1.6));
            return 2;
        }
        if (cfg.spots() > 0 && s < 0.8) {
            // lobate salt patches: warped, clustered toward the central spot and thinning out toward the walls
            double warp = n.getValue(x / 30.0 + 50, z / 30.0) * 7;
            double sp = Columns.fbm(n, (x + warp) / 13.0 + 300, (z - warp) / 13.0 - 300, 3);
            double bias = 0.5 * cfg.spots() * Math.pow(1 - s / 0.8, 1.5);
            if (sp + bias > 0.58) {
                return 1.5;
            }
        }
        return 0;
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
        List<Crater> hosts = new ArrayList<>();
        for (Crater c : t.cratersNear(x0, z0, x0 + 15, z0 + 15)) {
            if (holds(c, cfg) && Math.abs(c.x() - (x0 + 8)) < c.radius() + 12 && Math.abs(c.z() - (z0 + 8)) < c.radius() + 12) {
                hosts.add(c);
            }
        }
        if (hosts.isEmpty()) {
            return false;
        }
        SimplexNoise n = Columns.noise(t.seed(), 41);
        double[] dome = new double[1];
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                double best = 0;
                dome[0] = 0;
                for (Crater c : hosts) {
                    best = Math.max(best, strength(c, cfg, n, x + 0.5, z + 0.5, dome));
                }
                for (Crater c : hosts) {
                    any |= wall(level, c, cfg, n, x, z);
                }
                if (best <= 0) {
                    continue;
                }
                int up = (int) Math.round(dome[0]);
                if (up > 0) {
                    Columns.shift(level, x, z, up, cfg.block());
                }
                Columns.paint(level, x, z, best > 1.8 ? cfg.depth() + up : 1, cfg.block());
                any = true;
            }
        }
        return any;
    }

    /**
     * Breaks up the smooth inner wall of a host crater: slumped lobes (a few blocks up or down along the wall, like
     * collapsed terraces) and a scatter of blocks fallen onto the floor at its foot.
     */
    private static boolean wall(WorldGenLevel level, Crater c, Config cfg, SimplexNoise n, int x, int z) {
        if (cfg.slump() <= 0) {
            return false;
        }
        double s = c.normalizedDistance(x + 0.5, z + 0.5);
        if (s < 0.6 || s > 1.08) {
            return false;
        }
        boolean any = false;
        double bump = Math.exp(-Math.pow((s - 0.9) / 0.1, 2));
        double lobe = Columns.fbm(n, x / 14.0 - 77, z / 14.0 + 77, 2);
        int dy = (int) Math.round(cfg.slump() * bump * lobe * Math.min(1, c.radius() / 30.0));
        if (dy != 0) {
            Columns.shift(level, x, z, dy, cfg.debris().orElse(level.getBlockState(new BlockPos(x, Columns.top(level, x, z) - Columns.CARRY, z))));
            any = true;
        }
        if (cfg.debris().isPresent() && s < 0.8 && s > 0.64) {
            long h = Columns.cell(c.seed(), 43, x, z);
            if (Columns.unit(h, 0) < 0.025) {
                int top = Columns.top(level, x, z);
                int tall = 1 + (Columns.unit(h, 1) < 0.35 ? 1 : 0);
                for (int k = 1; k <= tall; k++) {
                    level.setBlock(new BlockPos(x, top + k, z), cfg.debris().get(), 2);
                }
                any = true;
            }
        }
        return any;
    }
}
