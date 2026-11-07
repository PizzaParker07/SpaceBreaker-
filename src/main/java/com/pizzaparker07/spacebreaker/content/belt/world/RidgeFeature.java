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

/**
 * A raised ridge running all the way round a body along a line of latitude, like the equatorial bulge of a spinning-top
 * asteroid (Ryugu, Bennu): the ground rises smoothly toward the line and the crest wanders a little. Placed once per chunk.
 *
 * @param latitude line of the crest
 * @param height   height of the crest above the surroundings
 * @param width    half-width of the ridge in degrees of latitude
 * @param rock     rock built up under the carried soil
 */
public class RidgeFeature extends Feature<RidgeFeature.Config> {
    public record Config(double latitude, double height, double width, BlockState rock) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("latitude", 0.0).forGetter(Config::latitude),
                Codec.DOUBLE.fieldOf("height").forGetter(Config::height),
                Codec.DOUBLE.optionalFieldOf("width", 20.0).forGetter(Config::width),
                BlockState.CODEC.fieldOf("rock").forGetter(Config::rock)
        ).apply(i, Config::new));
    }

    public RidgeFeature() {
        super(Config.CODEC);
    }

    /** Height change at a column. */
    public static double offset(PlanetTerrain t, Config cfg, double x, double z) {
        SimplexNoise n = Columns.noise(t.seed(), 81);
        double lat = t.latitude(z) + 2.5 * n.getValue(x / 90.0, 0.7);
        double d = Math.abs(lat - cfg.latitude()) / cfg.width();
        if (d >= 1) {
            return 0;
        }
        double s = 1 - Columns.smooth(d);
        return cfg.height() * s * (0.85 + 0.15 * n.getValue(x / 25.0, 3.3));
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        PlanetTerrain t = Columns.terrain(ctx.chunkGenerator());
        if (t == null) {
            return false;
        }
        WorldGenLevel level = ctx.level();
        int x0 = ctx.origin().getX() & ~15;
        int z0 = ctx.origin().getZ() & ~15;
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int dy = (int) Math.round(offset(t, ctx.config(), x0 + dx + 0.5, z0 + dz + 0.5));
                if (dy != 0) {
                    Columns.shift(level, x0 + dx, z0 + dz, dy, ctx.config().rock());
                    any = true;
                }
            }
        }
        return any;
    }
}
