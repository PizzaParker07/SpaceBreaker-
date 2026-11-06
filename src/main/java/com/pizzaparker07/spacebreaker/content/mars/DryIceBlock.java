package com.pizzaparker07.spacebreaker.content.mars;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Frozen carbon dioxide. Stable in the polar cold of Mars, it sublimates straight into gas anywhere warmer than
 * {@link #SUBLIMATION_C}: it smokes and slowly vanishes, leaving nothing behind (no water).
 */
public class DryIceBlock extends Block {
    public static final MapCodec<DryIceBlock> CODEC = simpleCodec(DryIceBlock::new);
    /** Above this ambient temperature dry ice sublimates (a gameplay value between Mars' and Earth's sublimation points). */
    public static final double SUBLIMATION_C = -60;

    public DryIceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public static boolean warm(Level level, BlockPos pos) {
        return Environment.temperatureC(level, pos) > SUBLIMATION_C;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (warm(level, pos) && random.nextInt(3) == 0) {
            level.removeBlock(pos, false);
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.01);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0 && warm(level, pos)) {
            // cold CO2 gas pours over the edge and sinks
            double x = pos.getX() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.CLOUD, x, pos.getY() + 1.02, z, 0, -0.01, 0);
        }
    }
}
