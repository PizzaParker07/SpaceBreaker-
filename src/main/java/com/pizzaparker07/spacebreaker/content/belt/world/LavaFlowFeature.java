package com.pizzaparker07.spacebreaker.content.belt.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.Optional;

/**
 * Lava flows (Io): from vents scattered over a coarse grid, a few long flows wander out across the plains as tongues of
 * dark rock a few blocks wide, tapering toward their tips, each fringed by a band of a second soil (a sulfur margin). They
 * only repaint the top of the ground - no relief - so they read from up high as dark winding streaks.
 *
 * <p>Whole-world and deterministic: every chunk rebuilds the flows of the vents within reach from the terrain seed and
 * paints its own columns, so flows run seamlessly over chunk borders.
 *
 * @param cell      vent grid spacing (blocks)
 * @param chance    share of grid cells that have a vent
 * @param flows     most flows per vent (2 + up to this many more)
 * @param minLength shortest flow (blocks)
 * @param maxLength longest flow (blocks)
 * @param width     half-width of a flow at its vent (blocks)
 * @param core      the flow's rock
 * @param margin    optional soil along the flow's edges
 */
public class LavaFlowFeature extends Feature<LavaFlowFeature.Config> {
    public record Config(int cell, double chance, int flows, double minLength, double maxLength, double width, BlockState core,
                         Optional<BlockState> margin) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("cell", 320).forGetter(Config::cell),
                Codec.DOUBLE.optionalFieldOf("chance", 0.5).forGetter(Config::chance),
                Codec.INT.optionalFieldOf("flows", 3).forGetter(Config::flows),
                Codec.DOUBLE.optionalFieldOf("min_length", 90.0).forGetter(Config::minLength),
                Codec.DOUBLE.optionalFieldOf("max_length", 220.0).forGetter(Config::maxLength),
                Codec.DOUBLE.optionalFieldOf("width", 3.0).forGetter(Config::width),
                BlockState.CODEC.fieldOf("core").forGetter(Config::core),
                BlockState.CODEC.optionalFieldOf("margin").forGetter(Config::margin)
        ).apply(i, Config::new));
    }

    private static final double STEP = 3.0;
    private static final double MARGIN = 1.6;

    public LavaFlowFeature() {
        super(Config.CODEC);
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
        double reach = cfg.maxLength() + cfg.width() + MARGIN + 8;
        // for each column of the chunk: 0 untouched, 1 margin, 2 core
        byte[] mark = new byte[256];
        boolean any = false;
        SimplexNoise n = Columns.noise(t.seed(), 83);
        int gx0 = Mth.floor((x0 - reach) / cfg.cell());
        int gx1 = Mth.floor((x0 + 16 + reach) / cfg.cell());
        int gz0 = Mth.floor((z0 - reach) / cfg.cell());
        int gz1 = Mth.floor((z0 + 16 + reach) / cfg.cell());
        for (int gx = gx0; gx <= gx1; gx++) {
            for (int gz = gz0; gz <= gz1; gz++) {
                long h = Columns.cell(t.seed(), 0x1A7A, gx, gz);
                if (Columns.unit(h, 0) > cfg.chance()) {
                    continue;
                }
                double vx = (gx + 0.15 + 0.7 * Columns.unit(h, 1)) * cfg.cell();
                double vz = (gz + 0.15 + 0.7 * Columns.unit(h, 2)) * cfg.cell();
                int count = 2 + (int) (Columns.unit(h, 3) * (cfg.flows() + 0.999));
                for (int f = 0; f < count; f++) {
                    any |= flow(cfg, n, h, f, vx, vz, x0, z0, mark);
                }
            }
        }
        if (!any) {
            return false;
        }
        for (int i = 0; i < 256; i++) {
            if (mark[i] == 2) {
                Columns.paint(level, x0 + (i & 15), z0 + (i >> 4), 1, cfg.core());
            } else if (mark[i] == 1 && cfg.margin().isPresent()) {
                Columns.paint(level, x0 + (i & 15), z0 + (i >> 4), 1, cfg.margin().get());
            }
        }
        return true;
    }

    /** Walks one flow from its vent and marks the chunk's columns it covers; true if it touched the chunk. */
    private static boolean flow(Config cfg, SimplexNoise n, long h, int f, double vx, double vz, int x0, int z0, byte[] mark) {
        double heading = Columns.unit(h, 4 + f) * Math.PI * 2;
        double length = cfg.minLength() + (cfg.maxLength() - cfg.minLength()) * Columns.unit(h, 12 + f);
        int steps = (int) (length / STEP);
        double x = vx;
        double z = vz;
        boolean touched = false;
        for (int s = 0; s < steps; s++) {
            double u = s / (double) steps;
            // the heading drifts like a flow finding its way down gentle slopes
            heading += n.getValue(s * 0.06 + f * 17.3 + vx * 0.01, vz * 0.01 - f * 5.1) * 0.32;
            double nx = x + Math.cos(heading) * STEP;
            double nz = z + Math.sin(heading) * STEP;
            double w = Math.max(0.8, cfg.width() * (1 - 0.75 * u) * (0.85 + 0.3 * n.getValue(s * 0.2 + f * 3, vx * 0.02)));
            double r = w + MARGIN;
            if (Math.max(x, nx) + r >= x0 && Math.min(x, nx) - r < x0 + 16 && Math.max(z, nz) + r >= z0 && Math.min(z, nz) - r < z0 + 16) {
                touched |= segment(x, z, nx, nz, w, x0, z0, mark);
            }
            x = nx;
            z = nz;
        }
        return touched;
    }

    private static boolean segment(double ax, double az, double bx, double bz, double w, int x0, int z0, byte[] mark) {
        double dx = bx - ax;
        double dz = bz - az;
        double len2 = dx * dx + dz * dz;
        boolean touched = false;
        for (int i = 0; i < 256; i++) {
            double px = x0 + (i & 15) + 0.5;
            double pz = z0 + (i >> 4) + 0.5;
            double k = len2 == 0 ? 0 : Mth.clamp(((px - ax) * dx + (pz - az) * dz) / len2, 0, 1);
            double ex = ax + dx * k - px;
            double ez = az + dz * k - pz;
            double d = Math.sqrt(ex * ex + ez * ez);
            if (d < w) {
                mark[i] = 2;
                touched = true;
            } else if (d < w + MARGIN && mark[i] == 0) {
                mark[i] = 1;
                touched = true;
            }
        }
        return touched;
    }
}
