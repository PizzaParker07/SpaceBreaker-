package com.pizzaparker07.spacebreaker.content.belt.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * A buried layer spanning a whole chunk between two heights, over the bedrock floor: Europa's ocean under its ice shell.
 * Bedrock is kept; everything else in the band becomes the layer block, with a ceiling block on top.
 *
 * @param block   the layer (e.g. water)
 * @param ceiling the block just above it (e.g. ice)
 * @param bottom  lowest y of the layer
 * @param top     highest y of the layer
 */
public class LayerFeature extends Feature<LayerFeature.Config> {
    public record Config(BlockState block, BlockState ceiling, int bottom, int top) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("block").forGetter(Config::block),
                BlockState.CODEC.fieldOf("ceiling").forGetter(Config::ceiling),
                Codec.INT.fieldOf("bottom").forGetter(Config::bottom),
                Codec.INT.fieldOf("top").forGetter(Config::top)
        ).apply(i, Config::new));
    }

    public LayerFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        Config cfg = ctx.config();
        WorldGenLevel level = ctx.level();
        int x0 = ctx.origin().getX() & ~15;
        int z0 = ctx.origin().getZ() & ~15;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                for (int y = cfg.bottom(); y <= cfg.top() + 1; y++) {
                    p.set(x0 + dx, y, z0 + dz);
                    if (!level.getBlockState(p).is(Blocks.BEDROCK)) {
                        level.setBlock(p, y > cfg.top() ? cfg.ceiling() : cfg.block(), 2);
                    }
                }
            }
        }
        return true;
    }
}
