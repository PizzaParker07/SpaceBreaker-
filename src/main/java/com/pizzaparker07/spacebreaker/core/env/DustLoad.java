package com.pizzaparker07.spacebreaker.core.env;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * Dust settling on exposed surfaces (solar panels) on worlds with windblown dust. Like the Mars rovers' panels, a
 * surface darkens over a few sols, much faster during a dust storm, and is wiped clean by a passing dust whirl or a
 * brush. The machine stores its own dust value ({@link Dusty}); this class owns the rules.
 */
public final class DustLoad {
    /** Ticks for a clean surface to become fully dusty in clear weather (about three Mars days). */
    public static final int FULL_TICKS = 3 * 24000;
    /** How much faster dust settles in a full-strength storm. */
    public static final float STORM_BOOST = 6F;
    /** Output left at full dust: the panel keeps 60 %. */
    public static final float MIN_OUTPUT = 0.6F;

    /** A block entity that collects dust (0 = clean, 1 = fully covered). */
    public interface Dusty {
        float dust();

        void setDust(float dust);
    }

    private static final List<Predicate<Level>> DUSTY_WORLDS = new ArrayList<>();
    private static final List<ToDoubleFunction<Level>> STORMS = new ArrayList<>();

    private DustLoad() {
    }

    /** Registers a check for worlds where dust settles (Mars registers itself). */
    public static void addDustyWorld(Predicate<Level> dusty) {
        DUSTY_WORLDS.add(dusty);
    }

    /** Registers a storm strength source (0..1) that speeds up settling. */
    public static void addStorm(ToDoubleFunction<Level> storm) {
        STORMS.add(storm);
    }

    public static boolean settles(Level level) {
        for (Predicate<Level> p : DUSTY_WORLDS) {
            if (p.test(level)) {
                return true;
            }
        }
        return false;
    }

    /** Storm strength 0..1 here (the strongest registered source). */
    public static double storm(Level level) {
        double s = 0;
        for (ToDoubleFunction<Level> f : STORMS) {
            s = Math.max(s, f.applyAsDouble(level));
        }
        return Mth.clamp(s, 0, 1);
    }

    /**
     * New dust value after {@code ticks} of exposure at a position. Only grows on dusty worlds and under open sky;
     * anywhere else the value is returned unchanged.
     */
    public static float accumulate(Level level, BlockPos pos, float dust, int ticks) {
        if (!settles(level) || !level.canSeeSky(pos.above())) {
            return dust;
        }
        return settle(dust, ticks, storm(level));
    }

    /** The settling rule itself: dust after {@code ticks} under open sky in a storm of this strength (0..1). */
    public static float settle(float dust, int ticks, double storm) {
        float rate = (float) (1.0 + (STORM_BOOST - 1.0) * Mth.clamp(storm, 0, 1)) / FULL_TICKS;
        return Mth.clamp(dust + rate * ticks, 0F, 1F);
    }

    /** Share of the clean output that a panel with this much dust delivers: 1 clean, {@link #MIN_OUTPUT} fully covered. */
    public static float outputFactor(float dust) {
        return 1F - (1F - MIN_OUTPUT) * Mth.clamp(dust, 0F, 1F);
    }

    /** Dust as a whole percentage for messages. */
    public static int percent(float dust) {
        return Math.round(Mth.clamp(dust, 0F, 1F) * 100F);
    }

    /**
     * Wipes every dusty block entity within {@code radius} (horizontal) of {@code center}, a few blocks up and down.
     *
     * @return how many surfaces were cleaned
     */
    public static int clean(Level level, BlockPos center, double radius) {
        int r = Mth.ceil(radius);
        int cleaned = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                for (int dy = -2; dy <= 3; dy++) {
                    p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.isLoaded(p)) {
                        continue;
                    }
                    BlockEntity be = level.getBlockEntity(p);
                    if (be instanceof Dusty d && d.dust() > 0) {
                        d.setDust(0F);
                        be.setChanged();
                        cleaned++;
                    }
                }
            }
        }
        return cleaned;
    }
}
