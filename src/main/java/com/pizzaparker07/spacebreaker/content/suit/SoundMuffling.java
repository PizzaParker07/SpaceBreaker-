package com.pizzaparker07.spacebreaker.content.suit;

/**
 * How the air around the listener colours sound ("realistic sound", an option, off by default). Kept deliberately
 * light so the game never sounds broken: in thin air (Mars) the world is a touch quieter and softer, in vacuum a bit
 * more so; what the suit touches (one's own steps, the tool in hand) sounds normal. Sounds inside the helmet (radio,
 * mission control, alarms) and anything heard in breathable air or a pressurised room stay as they are.
 *
 * <p>A {@link Muffle} is a gain for the whole sound and a gain for its high frequencies (a low-pass filter); the
 * client applies it once when a sound starts.
 */
public final class SoundMuffling {
    /** Full air: at and above this pressure sound carries normally. */
    public static final double FULL_AIR_KPA = 50.0;
    /** Below this it is vacuum for sound. */
    public static final double VACUUM_KPA = 0.1;
    /** Outside sounds in vacuum: quieter and softer, never silent. */
    public static final Muffle VACUUM = new Muffle(0.5F, 0.4F);
    /** Thin air just above vacuum: the gentlest muffle, blending to none at {@link #FULL_AIR_KPA}. */
    public static final Muffle THIN_AIR = new Muffle(0.8F, 0.6F);

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
        // thin air: from a light muffle up to none at full air
        double t = Math.sqrt(pressureKpa / FULL_AIR_KPA);
        return new Muffle((float) (THIN_AIR.gain() + (1 - THIN_AIR.gain()) * t), (float) (THIN_AIR.highGain() + (1 - THIN_AIR.highGain()) * t));
    }

    /** A sound from something the suit touches (own steps, the tool in hand): heard normally. */
    public static Muffle contact(double pressureKpa, boolean airAround) {
        return Muffle.NONE;
    }
}
