package com.pizzaparker07.spacebreaker.core.env;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;


/**
 * Dust settling on exposed surfaces (solar panels) on worlds with windblown dust. Like the Mars rovers' panels, a
 * surface darkens over a few sols, much faster during a dust storm, and is wiped clean by a passing dust whirl or a
 * brush. The machine stores its own dust value ({@link Dusty}) and adds {@link Environment#dustRate} while it sees the
 * sky; this class holds the Martian rate and the cleaning.
 */
public final class DustLoad {
    /** Ticks for a clean surface to become fully dusty in clear weather (about three Mars days). */
    public static final int FULL_TICKS = 3 * 24000;
    /** How much faster dust settles in a full-strength storm. */
    public static final float STORM_BOOST = 6F;

    /** A block entity that collects dust (0 = clean, 1 = fully covered). */
    public interface Dusty {
        float dust();

        void setDust(float dust);
    }

    private DustLoad() {
    }

    /**
     * Martian settling rate as a fraction of full cover per tick in a storm of this strength (0..1): clean to covered
     * in {@link #FULL_TICKS} in calm weather, {@link #STORM_BOOST} times faster in a full storm. Registered with
     * {@link Environment#addDustSource} by the Mars module; panels add it while they see the sky.
     */
    public static double rate(double storm) {
        return (1.0 + (STORM_BOOST - 1.0) * Mth.clamp(storm, 0, 1)) / FULL_TICKS;
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
