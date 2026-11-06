package com.pizzaparker07.spacebreaker.content.belt.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.Optional;

/**
 * A giant fault cliff (rupes): along a straight, slightly wavering line the ground on one side drops by a great height in
 * a near-vertical, jagged wall that fades out toward the ends - Miranda's cliffs are among the tallest in the Solar
 * System, and in its weak gravity a fall from the top takes minutes. The wall shows bare layered rock. Placed once per
 * chunk.
 *
 * @param x        centre of the cliff (block x)
 * @param latitude centre of the cliff (latitude)
 * @param length   length of the cliff
 * @param angleDeg direction of the cliff line (0 = along x)
 * @param height   drop at the middle of the cliff
 * @param wall     horizontal width over which the ground falls (smaller = steeper)
 * @param rock     rock of the cliff face
 * @param band     optional second rock in layers across the face
 */
public class RupesFeature extends Feature<RupesFeature.Config> {
    public record Config(double x, double latitude, double length, double angleDeg, double height, double wall, BlockState rock,
                         Optional<BlockState> band) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("x", 0.0).forGetter(Config::x),
                Codec.DOUBLE.optionalFieldOf("latitude", 0.0).forGetter(Config::latitude),
                Codec.DOUBLE.optionalFieldOf("length", 800.0).forGetter(Config::length),
                Codec.DOUBLE.optionalFieldOf("angle", 0.0).forGetter(Config::angleDeg),
                Codec.DOUBLE.optionalFieldOf("height", 80.0).forGetter(Config::height),
                Codec.DOUBLE.optionalFieldOf("wall", 8.0).forGetter(Config::wall),
                BlockState.CODEC.fieldOf("rock").forGetter(Config::rock),
                BlockState.CODEC.optionalFieldOf("band").forGetter(Config::band)
        ).apply(i, Config::new));
    }

    public RupesFeature() {
        super(Config.CODEC);
    }

    public static double centreZ(PlanetTerrain t, Config cfg) {
        return -cfg.latitude() / 90.0 * t.profile().poleDistance();
    }

    /** Coordinates across (v, + on the low side) and along (u) the cliff line, with the line's waver applied. */
    public static double[] local(PlanetTerrain t, Config cfg, double x, double z) {
        double a = Math.toRadians(cfg.angleDeg());
        double dx = x - cfg.x();
        double dz = z - centreZ(t, cfg);
        double u = dx * Math.cos(a) + dz * Math.sin(a);
        double v = -dx * Math.sin(a) + dz * Math.cos(a);
        SimplexNoise n = Columns.noise(t.seed(), 63);
        v += 14 * n.getValue(u / 160.0, 2.1) + 3 * n.getValue(u / 23.0, 7.7);
        return new double[]{u, v};
    }

    /** Height change at a column: 0 on the high side, -height (tapering at the ends) on the low side. */
    public static double offset(PlanetTerrain t, Config cfg, double x, double z) {
        double[] uv = local(t, cfg, x, z);
        double half = cfg.length() / 2;
        if (Math.abs(uv[0]) > half + cfg.wall()) {
            return 0;
        }
        double taper = Columns.smooth((half - Math.abs(uv[0])) / (cfg.length() * 0.2));
        double fall = Columns.smooth(uv[1] / cfg.wall());
        // the foot of the cliff: a talus slope of fallen blocks
        double talus = uv[1] > cfg.wall() ? Math.max(0, 1 - (uv[1] - cfg.wall()) / (cfg.wall() * 2.5)) * 0.18 : 0;
        return -cfg.height() * taper * (fall - talus * fall);
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
        double reach = cfg.length() / 2 + 40;
        if (Math.abs(local(t, cfg, x0 + 8, z0 + 8)[0]) > reach + 24) {
            return false;
        }
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                int dy = (int) Math.round(offset(t, cfg, x + 0.5, z + 0.5));
                double[] uv = local(t, cfg, x + 0.5, z + 0.5);
                boolean face = uv[1] > -2 && uv[1] < cfg.wall() * 1.2 && Math.abs(uv[0]) < cfg.length() / 2;
                if (dy == 0 && !face) {
                    continue;
                }
                int top = dy == 0 ? Columns.top(level, x, z) : Columns.shift(level, x, z, dy, cfg.rock());
                // bare layered rock on the face, down the whole height the cliff can show
                if (face) {
                    // the face: bare rock in layers, down the whole height that this column shows in the wall
                    int depth = (int) cfg.height() + 4;
                    for (int y = top; y > top - depth; y--) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (!level.getBlockState(p).isAir()) {
                            boolean layer = cfg.band().isPresent() && Math.floorMod(y + (int) (uv[0] / 40), 9) < 3;
                            level.setBlock(p, layer ? cfg.band().get() : cfg.rock(), 2);
                        }
                    }
                }
                any = true;
            }
        }
        return any;
    }
}
