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
 * Bright or dark marks laid on the ground at fixed places: a ring (like the bright ring on the floor of Umbriel's Wunda
 * crater) or a filled disc (a dark floor). The edge wobbles a little; the ground keeps its shape. Placed once per chunk.
 *
 * @param marks each mark: centre (block x, latitude), radius, ring width (0 = filled disc) and block
 */
public class RingFeature extends Feature<RingFeature.Config> {
    public record Mark(double x, double latitude, double radius, double width, BlockState block) {
        public static final Codec<Mark> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Mark::x),
                Codec.DOUBLE.fieldOf("latitude").forGetter(Mark::latitude),
                Codec.DOUBLE.fieldOf("radius").forGetter(Mark::radius),
                Codec.DOUBLE.optionalFieldOf("width", 0.0).forGetter(Mark::width),
                BlockState.CODEC.fieldOf("block").forGetter(Mark::block)
        ).apply(i, Mark::new));
    }

    public record Config(List<Mark> marks) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = Mark.CODEC.listOf().fieldOf("marks").xmap(Config::new, Config::marks).codec();
    }

    public RingFeature() {
        super(Config.CODEC);
    }

    /** The mark covering a column, or null. */
    public static Mark markAt(PlanetTerrain t, Config cfg, double x, double z) {
        SimplexNoise n = Columns.noise(t.seed(), 73);
        for (Mark m : cfg.marks()) {
            double d = Math.hypot(x - m.x(), z + m.latitude() / 90.0 * t.profile().poleDistance());
            double r = m.radius() * (1 + 0.05 * n.getValue(x / 25.0, z / 25.0));
            boolean in = m.width() <= 0 ? d < r : Math.abs(d - r) < m.width() / 2;
            if (in) {
                return m;
            }
        }
        return null;
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
                Mark m = markAt(t, ctx.config(), x0 + dx + 0.5, z0 + dz + 0.5);
                if (m != null) {
                    Columns.paint(level, x0 + dx, z0 + dz, 2, m.block());
                    any = true;
                }
            }
        }
        return any;
    }
}
