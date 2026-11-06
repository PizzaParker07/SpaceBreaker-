package com.pizzaparker07.spacebreaker.content.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * A lumpy, half-buried rock on the surface: crater ejecta blocks and boulders rolled down crater walls.
 * Works on any solid ground (unlike vanilla forest rocks, which need dirt or stone below).
 */
public class BoulderFeature extends Feature<BoulderFeature.Config> {
    public record Config(BlockStateProvider block, FloatProvider radius, float buried) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockStateProvider.CODEC.fieldOf("block").forGetter(Config::block),
                FloatProvider.CODEC.fieldOf("radius").forGetter(Config::radius),
                Codec.FLOAT.optionalFieldOf("buried", 0.4F).forGetter(Config::buried)
        ).apply(i, Config::new));
    }

    public BoulderFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        Config cfg = ctx.config();
        BlockPos origin = ctx.origin();
        int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        BlockPos base = new BlockPos(origin.getX(), surface - 1, origin.getZ());
        if (!level.getBlockState(base).isSolid()) {
            return false;
        }
        float r = cfg.radius().sample(random);
        float rx = r * (0.8F + random.nextFloat() * 0.4F);
        float ry = r * (0.6F + random.nextFloat() * 0.3F);
        float rz = r * (0.8F + random.nextFloat() * 0.4F);
        double cy = base.getY() + 1 - ry * cfg.buried() * 2 + ry;
        int ir = (int) Math.ceil(Math.max(rx, Math.max(ry, rz))) + 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                for (int dy = -ir; dy <= ir; dy++) {
                    double nx = dx / rx;
                    double ny = (base.getY() + dy + 0.5 - cy) / ry;
                    double nz = dz / rz;
                    double d = nx * nx + ny * ny + nz * nz;
                    // rough surface: knock off some edge blocks
                    if (d > 1 || (d > 0.7 && random.nextFloat() < 0.35F)) {
                        continue;
                    }
                    p.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    if (!level.ensureCanWrite(p)) {
                        continue;
                    }
                    level.setBlock(p, cfg.block().getState(random, p), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }
}
