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
 * Coronae: great racetrack-shaped patches where the crust was pulled apart over rising warm ice - concentric ridges and
 * grooves running round a rounded-rectangle outline, in alternating bright and dark bands, sharply cut off from the
 * cratered plains around them. Placed once per chunk.
 *
 * @param coronae  each corona: centre (block x, latitude), half-length, half-width and direction
 * @param spacing  distance between neighbouring ridges
 * @param height   ridge height
 * @param bright   block of the bright bands
 * @param dark     block of the dark bands
 */
public class CoronaFeature extends Feature<CoronaFeature.Config> {
    public record Corona(double x, double latitude, double halfLength, double halfWidth, double angleDeg) {
        public static final Codec<Corona> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Corona::x),
                Codec.DOUBLE.fieldOf("latitude").forGetter(Corona::latitude),
                Codec.DOUBLE.fieldOf("half_length").forGetter(Corona::halfLength),
                Codec.DOUBLE.fieldOf("half_width").forGetter(Corona::halfWidth),
                Codec.DOUBLE.optionalFieldOf("angle", 0.0).forGetter(Corona::angleDeg)
        ).apply(i, Corona::new));
    }

    public record Config(List<Corona> coronae, double spacing, double height, BlockState bright, BlockState dark) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Corona.CODEC.listOf().fieldOf("coronae").forGetter(Config::coronae),
                Codec.DOUBLE.optionalFieldOf("spacing", 18.0).forGetter(Config::spacing),
                Codec.DOUBLE.optionalFieldOf("height", 5.0).forGetter(Config::height),
                BlockState.CODEC.fieldOf("bright").forGetter(Config::bright),
                BlockState.CODEC.fieldOf("dark").forGetter(Config::dark)
        ).apply(i, Config::new));
    }

    public CoronaFeature() {
        super(Config.CODEC);
    }

    /** Normalised racetrack distance from a corona's centre (1 = its outline), with a wobbly edge. */
    public static double distance(PlanetTerrain t, Corona c, double x, double z) {
        double a = Math.toRadians(c.angleDeg());
        double dx = x - c.x();
        double dz = z + c.latitude() / 90.0 * t.profile().poleDistance();
        double u = (dx * Math.cos(a) + dz * Math.sin(a)) / c.halfLength();
        double v = (-dx * Math.sin(a) + dz * Math.cos(a)) / c.halfWidth();
        double d = Math.pow(Math.pow(Math.abs(u), 4) + Math.pow(Math.abs(v), 4), 0.25);
        SimplexNoise n = Columns.noise(t.seed(), 71);
        return d * (1 + 0.04 * n.getValue(x / 60.0, z / 60.0));
    }

    /** Ridge phase at a column inside a corona: in [-1, 1], positive on bright ridges; NaN outside. */
    public static double band(PlanetTerrain t, Config cfg, double x, double z) {
        for (Corona c : cfg.coronae()) {
            double d = distance(t, c, x, z);
            if (d < 1) {
                double r = d * Math.min(c.halfLength(), c.halfWidth());
                return Math.sin(r / cfg.spacing() * Math.PI * 2);
            }
        }
        return Double.NaN;
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
        boolean near = false;
        for (Corona c : cfg.coronae()) {
            near |= distance(t, c, x0 + 8, z0 + 8) < 1.1 + 24.0 / Math.min(c.halfLength(), c.halfWidth());
        }
        if (!near) {
            return false;
        }
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                double b = band(t, cfg, x + 0.5, z + 0.5);
                if (Double.isNaN(b)) {
                    continue;
                }
                int dy = (int) Math.round(cfg.height() * 0.5 * (b + 1));
                if (dy != 0) {
                    Columns.shift(level, x, z, dy, cfg.dark());
                }
                Columns.paint(level, x, z, 2, b > 0 ? cfg.bright() : cfg.dark());
                any = true;
            }
        }
        return any;
    }
}
