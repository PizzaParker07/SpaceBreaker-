package com.pizzaparker07.spacebreaker.content.vehicles;

/**
 * What sets one rover model apart from another: top speed, battery, energy per block driven, cargo space, how well
 * the tyres grip and how loud the motors are. Each type is its own entity type and item, so saved rovers keep theirs.
 *
 * @param maxSpeed   top speed, blocks per tick (0.45 = 9 m/s)
 * @param accel      speed gained per tick at full throttle on good ground
 * @param capacity   battery, FE
 * @param fePerBlock energy for one block driven
 * @param cargoSlots cargo bay size (multiples of 9 for the chest screen)
 * @param grip       tyre grip relative to the standard rover (a light rover skids sooner)
 * @param loudness   motor sound volume relative to the standard rover
 */
public record RoverType(String id, double maxSpeed, double accel, int capacity, int fePerBlock, int cargoSlots, double grip,
                        float loudness, boolean cabin) {
    public RoverType(String id, double maxSpeed, double accel, int capacity, int fePerBlock, int cargoSlots, double grip, float loudness) {
        this(id, maxSpeed, accel, capacity, fePerBlock, cargoSlots, grip, loudness, false);
    }

    /** The open two-seat planetary rover. */
    public static final RoverType STANDARD = new RoverType("rover", RoverPhysics.MAX_SPEED, RoverPhysics.ACCEL, 400_000,
            RoverPhysics.FE_PER_BLOCK, 27, 1.0, 1.0F);
    /** Light, fast and quiet: an aluminium-lithium frame, bigger wheels, a smaller battery and cargo bay. */
    public static final RoverType SPRINT = new RoverType("sprint_rover", 0.65, 0.016, 300_000, 35, 9, 0.85, 0.45F);

    /**
     * Closed pressurised cabin: no helmet needed inside; slower and heavier, a big battery for long traverses, an
     * oxygen tank for the crew and walls that stop half of the radiation.
     */
    public static final RoverType PRESSURIZED = new RoverType("pressurized_rover", 0.35, 0.010, 1_200_000, 80, 27, 1.0, 0.7F, true);

    /** Blocks the full battery drives on flat ground. */
    public int range() {
        return capacity / fePerBlock;
    }
}
