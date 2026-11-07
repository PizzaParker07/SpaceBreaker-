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
 * server): only in sunlight, only under open sky, only while the comet is awake (far from the Sun it sleeps; near it
 * the jets grow - the astro library's activity model), a few particles per tick from the blocks the client is already
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

    /**
     * How strongly the comet this level belongs to is outgassing (astro library's activity model at the current sky
     * time): 0 dormant (far from the Sun), 0.35 weak, 1 active, 1.6 a great comet; 1 on a world that is not a known
     * comet. Recomputed at most every 100 ticks per level.
     */
    public static float strength(Level level) {
        long tick = level.getGameTime() / 100;
        var key = level.dimension().location();
        Cached c = CACHE.get(key);
        if (c == null || c.tick != tick) {
            float v = com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.byId(key.getPath())
                    .map(comet -> strength(com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.at(comet,
                            com.pizzaparker07.spacebreaker.core.astro.BodyAdapter.simTime(level)).level()))
                    .orElse(1F);
            c = new Cached(tick, v);
            CACHE.put(key, c);
        }
        return c.strength;
    }

    public static float strength(com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.Level activity) {
        return switch (activity) {
            case DORMANT -> 0F;
            case WEAK -> 0.35F;
            case ACTIVE -> 1F;
            case GREAT -> 1.6F;
        };
    }

    private record Cached(long tick, float strength) {
    }

    private static final java.util.Map<net.minecraft.resources.ResourceLocation, Cached> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!active(level, pos)) {
            return;
        }
        float strength = strength(level);
        if (strength <= 0 || random.nextFloat() > Math.min(1F, strength)) {
            return;
        }
        int specks = Math.round(40 * Math.max(1F, strength));
        // the client animates a given block only now and then, so each call draws a whole thin column at once: small
        // grey-white specks (1-2 pixels) rising from the vent and fanning out slightly with height, fading as they go
        for (int i = 0; i < specks; i++) {
            double h = 1.05 + i * 0.18 * 40 / specks * Math.max(1F, strength) + random.nextDouble() * 0.3;
            double spread = 0.05 + i * 0.012;
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * spread * 2;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * spread * 2;
            float grey = 0.86F + random.nextFloat() * 0.14F;
            float size = Math.max(0.4F, 0.95F - i * 0.011F * 40 / specks);
            level.addParticle(new DustParticleOptions(new Vector3f(grey, grey, grey * 1.02F), size), x, pos.getY() + h, z,
                    (x - pos.getX() - 0.5) * 2, 2.0, (z - pos.getZ() - 0.5) * 2);
        }
    }
}
