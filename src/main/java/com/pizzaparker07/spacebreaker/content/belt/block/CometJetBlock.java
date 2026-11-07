package com.pizzaparker07.spacebreaker.content.belt.block;

import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
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
        return sunElevation > 0.03F && columnTop <= y + 1;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!active(level, pos)) {
            return;
        }
        // the client animates a given block only now and then, so each call draws a whole thin column at once: small
        // grey-white specks (1-2 pixels) rising from the vent and fanning out slightly with height, fading as they go
        for (int i = 0; i < 16; i++) {
            double h = 1.05 + i * 0.45 + random.nextDouble() * 0.3;
            double spread = 0.05 + i * 0.03;
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * spread * 2;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * spread * 2;
            float grey = 0.78F + random.nextFloat() * 0.22F;
            float size = 0.8F - i * 0.025F;
            level.addParticle(new DustParticleOptions(new Vector3f(grey, grey, grey * 1.02F), size), x, pos.getY() + h, z,
                    (x - pos.getX() - 0.5) * 2, 2.0, (z - pos.getZ() - 0.5) * 2);
        }
    }
}
