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
 * Cracked ice: a network of long curving fractures (lineae) like Europa's, each a narrow central trough between two
 * low ridges with salt- and sulfur-stained flanks; optional chaos terrain where the shell broke into tilted rafts in a
 * stained, refrozen matrix; optional straight parallel fractures inside a latitude band (Enceladus' tiger stripes).
 *
 * @param stain       stained ice along the cracks
 * @param ridge       optional cleaner ice heaped into the ridges
 * @param scale       size of the crack network
 * @param width       crack half-width (noise units, ~0.02-0.06)
 * @param ridgeHeight height of the ridges on both sides
 * @param sets        number of crossing crack sets (1-4)
 * @param chaos       optional chaos matrix block
 * @param chaosScale  size of chaos regions
 * @param chaosThreshold noise level above which the ground is chaos terrain
 * @param stripes     optional parallel stripes: spacing in blocks (0 = none)
 * @param stripeLatitude stripes only poleward of this latitude (negative = south)
 * @param where       "any", "lowlands" or "highlands": where the cracks run (chaos and stripes ignore it)
 */
public class LineaeFeature extends Feature<LineaeFeature.Config> {
    public record Config(BlockState stain, Optional<BlockState> ridge, double scale, double width, double ridgeHeight, int sets,
                         Optional<BlockState> chaos, double chaosScale, double chaosThreshold, double stripes, double stripeLatitude,
                         String where) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("stain").forGetter(Config::stain),
                BlockState.CODEC.optionalFieldOf("ridge").forGetter(Config::ridge),
                Codec.DOUBLE.optionalFieldOf("scale", 420.0).forGetter(Config::scale),
                Codec.DOUBLE.optionalFieldOf("width", 0.03).forGetter(Config::width),
                Codec.DOUBLE.optionalFieldOf("ridge_height", 2.0).forGetter(Config::ridgeHeight),
                Codec.intRange(1, 4).optionalFieldOf("sets", 3).forGetter(Config::sets),
                BlockState.CODEC.optionalFieldOf("chaos").forGetter(Config::chaos),
                Codec.DOUBLE.optionalFieldOf("chaos_scale", 900.0).forGetter(Config::chaosScale),
                Codec.DOUBLE.optionalFieldOf("chaos_threshold", 0.55).forGetter(Config::chaosThreshold),
                Codec.DOUBLE.optionalFieldOf("stripes", 0.0).forGetter(Config::stripes),
                Codec.DOUBLE.optionalFieldOf("stripe_latitude", -70.0).forGetter(Config::stripeLatitude),
                Codec.STRING.optionalFieldOf("where", "any").forGetter(Config::where)
        ).apply(i, Config::new));
    }

    /** What the feature does to one column. */
    public record Mark(int dy, int kind) {
        public static final int NONE = 0;
        public static final int STAIN = 1;
        public static final int RIDGE = 2;
        public static final int CHAOS = 3;
        public static final Mark EMPTY = new Mark(0, NONE);
    }

    public LineaeFeature() {
        super(Config.CODEC);
    }

    /** Chaos share 0..1 (the regions where the shell broke up). */
    public static double chaos(PlanetTerrain t, Config cfg, double x, double z) {
        if (cfg.chaos().isEmpty()) {
            return 0;
        }
        SimplexNoise n = Columns.noise(t.seed(), 71);
        double v = Columns.fbm(n, x / cfg.chaosScale(), z / cfg.chaosScale(), 3) * 0.5 + 0.5;
        return Columns.smooth((v - cfg.chaosThreshold()) / 0.06);
    }

    /** Distance to the nearest crack centre in units of the crack half-width (0 = centre). */
    public static double crack(PlanetTerrain t, Config cfg, double x, double z) {
        double best = 99;
        boolean fits = switch (cfg.where()) {
            case "lowlands" -> t.lowland(x, z, t.latitude(z)) > 0.5;
            case "highlands" -> t.lowland(x, z, t.latitude(z)) <= 0.5;
            default -> true;
        };
        for (int k = 0; k < (fits ? cfg.sets() : 0); k++) {
            SimplexNoise n = Columns.noise(t.seed(), 72 + k);
            // each set is stretched along its own direction, so cracks run long and gently curved
            double ang = k * 1.1 + 0.4;
            double ca = Math.cos(ang);
            double sa = Math.sin(ang);
            double u = (x * ca + z * sa) / (cfg.scale() * (2.6 + k * 0.4));
            double v = (-x * sa + z * ca) / (cfg.scale() * (0.55 + 0.2 * k));
            double warp = 0.35 * n.getValue(u * 3 + 50, v * 3 - 50);
            double val = Math.abs(n.getValue(u + warp, v));
            best = Math.min(best, val / cfg.width());
        }
        if (cfg.stripes() > 0 && sameSide(t.latitude(z), cfg.stripeLatitude())) {
            SimplexNoise n = Columns.noise(t.seed(), 79);
            double phase = x / cfg.stripes() + 0.6 * n.getValue(z / 500.0, x / 1500.0);
            double d = Math.abs(phase - Math.round(phase)) * cfg.stripes();
            best = Math.min(best, d / Math.max(1.5, cfg.width() * 60));
        }
        return best;
    }

    private static boolean sameSide(double lat, double limit) {
        return limit < 0 ? lat <= limit : lat >= limit;
    }

    public static Mark mark(PlanetTerrain t, Config cfg, double x, double z) {
        double ch = chaos(t, cfg, x, z);
        if (ch > 0.5) {
            // tilted rafts of old crust in a lowered, stained matrix
            int cell = 13;
            int gx = Mth.floor(x / cell);
            int gz = Mth.floor(z / cell);
            long h = Columns.cell(t.seed(), 73, gx, gz);
            double cx = (gx + 0.5) * cell + (Columns.unit(h, 1) - 0.5) * 4;
            double cz = (gz + 0.5) * cell + (Columns.unit(h, 2) - 0.5) * 4;
            double half = 3 + Columns.unit(h, 3) * 3.5;
            double rot = Columns.unit(h, 4) * Math.PI;
            double lx = (x - cx) * Math.cos(rot) + (z - cz) * Math.sin(rot);
            double lz = -(x - cx) * Math.sin(rot) + (z - cz) * Math.cos(rot);
            if (Math.abs(lx) < half && Math.abs(lz) < half * (0.7 + 0.5 * Columns.unit(h, 5))) {
                // a raft: lifted and tilted
                double tilt = (Columns.unit(h, 6) - 0.5) * 0.5;
                return new Mark((int) Math.round(1 + Columns.unit(h, 7) * 3 + lx * tilt), Mark.NONE);
            }
            return new Mark(-2, Mark.CHAOS);
        }
        double d = crack(t, cfg, x, z);
        if (d > 3.2) {
            return Mark.EMPTY;
        }
        if (d < 0.45) {
            return new Mark(-1, Mark.STAIN);
        }
        if (d < 1.6) {
            double rise = cfg.ridgeHeight() * Math.sin((d - 0.45) / 1.15 * Math.PI);
            return new Mark((int) Math.round(rise), cfg.ridge().isPresent() && d > 0.8 && d < 1.3 ? Mark.RIDGE : Mark.STAIN);
        }
        // stained flanks fade out irregularly
        SimplexNoise n = Columns.noise(t.seed(), 78);
        return n.getValue(x / 7.0, z / 7.0) > (d - 1.6) / 1.6 * 1.6 - 0.6 ? new Mark(0, Mark.STAIN) : Mark.EMPTY;
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
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                Mark m = mark(t, cfg, x + 0.5, z + 0.5);
                if (m == Mark.EMPTY) {
                    continue;
                }
                BlockState top = level.getBlockState(new net.minecraft.core.BlockPos(x, Columns.top(level, x, z), z));
                Columns.shift(level, x, z, m.dy(), top);
                switch (m.kind()) {
                    case Mark.STAIN -> Columns.paint(level, x, z, 2, cfg.stain());
                    case Mark.RIDGE -> Columns.paint(level, x, z, 1, cfg.ridge().orElse(cfg.stain()));
                    case Mark.CHAOS -> Columns.paint(level, x, z, 2, cfg.chaos().orElse(cfg.stain()));
                    default -> {
                    }
                }
                any = true;
            }
        }
        return any;
    }
}
