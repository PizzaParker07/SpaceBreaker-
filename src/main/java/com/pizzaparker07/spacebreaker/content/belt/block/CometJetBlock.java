package com.pizzaparker07.spacebreaker.content.belt.block;

import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A comet jet: a patch of ice where sunlight warms the surface enough for the ice to turn straight to gas, lifting a
 * thin column of gas and dust. Purely visual and client-side (it neither hurts nor pushes, and does nothing on the
 * server): only in sunlight, only under open sky, a few particles per tick from the blocks the client is already
 * animating nearby, so it costs next to nothing.
 */
public class CometJetBlock extends Block {
    public CometJetBlock(Properties props) {
        super(props);
    }

    /** True if the sun is high enough over this jet to drive it. */
    public static boolean active(Level level, BlockPos pos) {
        // open sky: nothing above it (the heightmap, which follows block changes at once, unlike sky light)
        return active(Environment.sunElevation(level),
                level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()), pos.getY());
    }

    /**
     * @param sunElevation sun height above the horizon (radians)
     * @param columnTop    first free y above the highest solid block of the column
     * @param y            the jet's y
     */
    public static boolean active(float sunElevation, int columnTop, int y) {
        return sunElevation > 0.12F && columnTop <= y + 1;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!active(level, pos)) {
            return;
        }
        // the client animates a given block only now and then, so each call draws a whole thin column at once
        for (int i = 0; i < 8; i++) {
            double h = 1.05 + i * 0.9 + random.nextDouble() * 0.5;
            double spread = 0.08 + i * 0.05;
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * spread * 2;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * spread * 2;
            level.addParticle(i % 3 == 0 ? ParticleTypes.CLOUD : ParticleTypes.WHITE_ASH, x, pos.getY() + h, z,
                    (random.nextDouble() - 0.5) * 0.03, 0.12 + random.nextDouble() * 0.1, (random.nextDouble() - 0.5) * 0.03);
        }
        level.addParticle(ParticleTypes.ASH, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0, 0.2, 0);
    }
}
