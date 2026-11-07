package com.pizzaparker07.spacebreaker.content.suit;

/**
 * How the air around the listener carries sound ("realistic sound"). Sound needs a medium: in thin air (Mars,
 * 0.6 kPa) the world sounds faint and dull, high tones fade first; in vacuum nothing outside carries at all. What the
 * suit touches still reaches the ear through the suit: one's own footsteps, the tool in hand, the block being mined,
 * the machine underfoot, all dull. Sounds inside the helmet (radio, mission control, alarms, breathing) and anything
 * heard in breathable air or a pressurised room stay as they are.
 *
 * <p>A {@link Muffle} is a gain for the whole sound and a gain for its high frequencies (a low-pass filter); the
 * client applies it once when a sound starts.
 */
public final class SoundMuffling {
    /** Full air: at and above this pressure sound carries normally. */
    public static final double FULL_AIR_KPA = 50.0;
    /** Below this it is vacuum for sound. */
    public static final double VACUUM_KPA = 0.1;
    /** Outside sounds in vacuum: all but gone. */
    public static final Muffle VACUUM = new Muffle(0.03F, 0.05F);
    /** Sound carried through the suit from what it touches: quieter and dull. */
    public static final Muffle THROUGH_SUIT = new Muffle(0.45F, 0.15F);

    /** {@code gain}: volume factor; {@code highGain}: extra factor for high frequencies (1 = no filter). */
    public record Muffle(float gain, float highGain) {
        public static final Muffle NONE = new Muffle(1F, 1F);

        public boolean none() {
            return gain >= 1F && highGain >= 1F;
        }
    }

    private SoundMuffling() {
    }

    /**
     * A sound from outside, heard through {@code pressureKpa} of air. {@code airAround}: the listener stands in
     * breathable air or a pressurised room, so everything sounds normal.
     */
    public static Muffle outside(double pressureKpa, boolean airAround) {
        if (airAround || pressureKpa >= FULL_AIR_KPA) {
            return Muffle.NONE;
        }
        if (pressureKpa < VACUUM_KPA) {
            return VACUUM;
        }
        // thin air: loudness falls with the square root of the pressure, the highs fall faster
        double t = pressureKpa / FULL_AIR_KPA;
        return new Muffle((float) (0.3 + 0.7 * Math.sqrt(t)), (float) (0.1 + 0.9 * t));
    }

    /** A sound from something the suit touches: carried through the suit, or through the air if that is better. */
    public static Muffle contact(double pressureKpa, boolean airAround) {
        Muffle air = outside(pressureKpa, airAround);
        return air.gain() >= THROUGH_SUIT.gain() ? air : THROUGH_SUIT;
    }
}
