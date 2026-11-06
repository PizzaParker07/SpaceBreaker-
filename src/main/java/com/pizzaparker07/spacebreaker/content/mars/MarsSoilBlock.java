package com.pizzaparker07.spacebreaker.content.mars;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.moon.Footprint;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Loose soil of a world with wind (Mars): boot prints are pressed in like on the Moon, but the thin wind slowly
 * fills them again. A print lasts a few in-game days; dust storms erase prints much faster.
 */
public class MarsSoilBlock extends LunarRegolithBlock {
    public static final MapCodec<MarsSoilBlock> CODEC = simpleCodec(MarsSoilBlock::new);

    public MarsSoilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(PRINT) != Footprint.NONE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (Environment.isVacuum(level)) {
            return; // no wind: prints stay
        }
        float chance = MarsWeather.stormStrength(level) > 0.3F ? 0.5F : 0.04F;
        if (random.nextFloat() < chance && level.canSeeSky(pos.above())) {
            level.setBlock(pos, state.setValue(PRINT, Footprint.NONE), Block.UPDATE_CLIENTS);
        }
    }
}
