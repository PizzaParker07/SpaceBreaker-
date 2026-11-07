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

import java.util.Optional;

/**
 * Ground broken into irregular polygonal cells (a jittered grid, measured as distance to the nearest cell centre and to
 * the border with the next one). Two looks:
 * <ul>
 *   <li>{@code pillows}: each cell a low swell with a trough along its border - the slowly overturning convection cells of
 *   a nitrogen-ice plain (Pluto's great heart-shaped basin), dark debris collected in the troughs;</li>
 *   <li>{@code dimples}: each cell a round pit with a sharp ridge between neighbours - the "cantaloupe" terrain of Triton.</li>
 * </ul>
 * Optionally limited to a disc (around a basin centre) or to a band of latitude. Placed once per chunk.
 *
 * @param mode        "pillows" or "dimples"
 * @param cell        typical cell width in blocks
 * @param depth       relief: swell height and trough depth (pillows) or pit depth (dimples)
 * @param edge        half-width of the border trough or ridge
 * @param edgeBlock   optional block painted along the borders
 * @param topBlock    optional block painted over the cell interiors
 * @param x           centre of the limiting disc (block x)
 * @param latitude    centre of the limiting disc (latitude)
 * @param radius      radius of the limiting disc (0 = no disc)
 * @param minLatitude cells only at or above this latitude (signed)
 * @param maxLatitude cells only at or below this latitude (signed)
 * @param level       optional ground level the cells are levelled to first (a flat ice plain), or absent to follow the ground
 */
public class CellsFeature extends Feature<CellsFeature.Config> {
    public record Config(String mode, double cell, double depth, double edge, Optional<BlockState> edgeBlock, Optional<BlockState> topBlock,
                         double x, double latitude, double radius, double minLatitude, double maxLatitude, Optional<Integer> level)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("mode", "pillows").forGetter(Config::mode),
                Codec.DOUBLE.optionalFieldOf("cell", 48.0).forGetter(Config::cell),
                Codec.DOUBLE.optionalFieldOf("depth", 3.0).forGetter(Config::depth),
                Codec.DOUBLE.optionalFieldOf("edge", 2.5).forGetter(Config::edge),
                BlockState.CODEC.optionalFieldOf("edge_block").forGetter(Config::edgeBlock),
                BlockState.CODEC.optionalFieldOf("top_block").forGetter(Config::topBlock),
                Codec.DOUBLE.optionalFieldOf("x", 0.0).forGetter(Config::x),
                Codec.DOUBLE.optionalFieldOf("latitude", 0.0).forGetter(Config::latitude),
                Codec.DOUBLE.optionalFieldOf("radius", 0.0).forGetter(Config::radius),
                Codec.DOUBLE.optionalFieldOf("min_latitude", -90.0).forGetter(Config::minLatitude),
                Codec.DOUBLE.optionalFieldOf("max_latitude", 90.0).forGetter(Config::maxLatitude),
                Codec.INT.optionalFieldOf("level").forGetter(Config::level)
        ).apply(i, Config::new));

        public boolean dimples() {
            return mode.equals("dimples");
        }
    }

    /** Distance to the nearest cell centre and half the gap to the second nearest (0 on a border). */
    public record Sample(double centre, double border) {
    }

    public CellsFeature() {
        super(Config.CODEC);
    }

    /** z of the limiting disc's centre. */
    public static double centreZ(PlanetTerrain t, Config cfg) {
        return -cfg.latitude() / 90.0 * t.profile().poleDistance();
    }

    /** How strongly the cells show at a column: 1 inside the disc and band, fading out at their edges. */
    public static double weight(PlanetTerrain t, Config cfg, double x, double z) {
        double lat = t.latitude(z);
        if (lat < cfg.minLatitude() || lat > cfg.maxLatitude()) {
            return 0;
        }
        double w = Columns.smooth(Math.min(lat - cfg.minLatitude(), cfg.maxLatitude() - lat) / 4.0);
        if (cfg.radius() > 0) {
            double d = Math.hypot(x - cfg.x(), z - centreZ(t, cfg)) / cfg.radius();
            w *= Columns.smooth((1 - d) / 0.08);
        }
        return w;
    }

    public static Sample sample(PlanetTerrain t, Config cfg, double x, double z) {
        double c = cfg.cell();
        int gx = Mth.floor(x / c);
        int gz = Mth.floor(z / c);
        double f1 = Double.MAX_VALUE;
        double f2 = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long h = Columns.cell(t.seed(), 77, gx + dx, gz + dz);
                double px = (gx + dx + 0.15 + 0.7 * Columns.unit(h, 0)) * c;
                double pz = (gz + dz + 0.15 + 0.7 * Columns.unit(h, 1)) * c;
                double d = Math.hypot(x - px, z - pz);
                if (d < f1) {
                    f2 = f1;
                    f1 = d;
                } else if (d < f2) {
                    f2 = d;
                }
            }
        }
        return new Sample(f1, (f2 - f1) / 2);
    }

    /** Height change at a column (before weighting). */
    public static double offset(Config cfg, Sample s) {
        double border = 1 - Columns.smooth(s.border() / cfg.edge());
        if (cfg.dimples()) {
            double pit = 1 - Columns.smooth(s.centre() / (cfg.cell() * 0.55));
            return -cfg.depth() * pit + cfg.depth() * 0.45 * border;
        }
        double swell = Columns.smooth(s.border() / (cfg.cell() * 0.3));
        return cfg.depth() * 0.6 * swell - cfg.depth() * border;
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
        if (weight(t, cfg, x0 + 8, z0 + 8) <= 0 && weight(t, cfg, x0, z0) <= 0 && weight(t, cfg, x0 + 15, z0 + 15) <= 0) {
            return false;
        }
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                double w = weight(t, cfg, x + 0.5, z + 0.5);
                if (w <= 0) {
                    continue;
                }
                Sample s = sample(t, cfg, x + 0.5, z + 0.5);
                int dy = (int) Math.round(offset(cfg, s) * w);
                // the column's top, found once and carried through the steps below
                int top = Columns.top(level, x, z);
                if (cfg.level().isPresent()) {
                    // level the ground first: the plain is a flat sheet of ice, the cells ride on it
                    dy += (int) Math.round((cfg.level().get() - top) * w);
                }
                if (dy != 0) {
                    top = Columns.shift(level, x, top, z, dy, level.getBlockState(new net.minecraft.core.BlockPos(x, top - Columns.CARRY, z)));
                }
                boolean onBorder = s.border() < cfg.edge() * 0.6 && w > 0.5;
                if (onBorder && cfg.edgeBlock().isPresent()) {
                    Columns.paint(level, x, top, z, 1, cfg.edgeBlock().get());
                } else if (!onBorder && cfg.topBlock().isPresent() && w > 0.3) {
                    Columns.paint(level, x, top, z, 2, cfg.topBlock().get());
                }
                any = true;
            }
        }
        return any;
    }
}
