package com.pizzaparker07.spacebreaker.content.audio;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * Every sound the mod adds. Files live in {@code assets/spacebreaker/sounds/}, the event list in
 * {@code assets/spacebreaker/sounds.json} (owned by this module: ask the integrator to add new sounds).
 * Recorded sounds and voice lines were produced with ElevenLabs, the radio beeps are synthesised.
 */
public final class SbSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Spacebreaker.MOD_ID);

    // --- rocket ---------------------------------------------------------------------------------
    /** Steady engine roar; loop it from a tickable sound while an engine burns. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_ENGINE_LOOP = event("rocket.engine_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_IGNITION = event("rocket.ignition");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_STAGE_SEPARATION = event("rocket.stage_separation");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_PARACHUTE_DEPLOY = event("rocket.parachute_deploy");
    /** Roar of atmospheric entry heard inside the capsule; loop it while the plasma shows. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_REENTRY_LOOP = event("rocket.reentry_loop");

    // --- suit -----------------------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> SUIT_OXYGEN_ALARM = event("suit.oxygen_alarm");

    // --- mission control ------------------------------------------------------------------------
    /** Short radio tones played before and after a spoken line. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RADIO_IN = event("mission_control.radio_in");
    public static final DeferredHolder<SoundEvent, SoundEvent> RADIO_OUT = event("mission_control.radio_out");

    /** One spoken line, recorded in English and Turkish. Play with {@code client.audio.MissionControlVoice#say}. */
    public enum Line {
        COUNTDOWN_10("countdown_10", true), COUNTDOWN_9("countdown_9", true), COUNTDOWN_8("countdown_8", true),
        COUNTDOWN_7("countdown_7", true), COUNTDOWN_6("countdown_6", true), COUNTDOWN_5("countdown_5", true),
        COUNTDOWN_4("countdown_4", true), COUNTDOWN_3("countdown_3", true), COUNTDOWN_2("countdown_2", true),
        COUNTDOWN_1("countdown_1", true),
        IGNITION("ignition", false),
        LIFTOFF("liftoff", false),
        STAGE_SEPARATION("stage_separation", false),
        ORBIT_REACHED("orbit_reached", false),
        ENTRY_INTERFACE("entry_interface", false),
        PARACHUTES("parachutes", false),
        TOUCHDOWN("touchdown", false),
        FUEL_LOW("fuel_low", false),
        OXYGEN_LOW("oxygen_low", false),
        COUNTDOWN_HOLD("countdown_hold", false);

        public final String id;
        /** Countdown numbers are spoken without the radio tones so they keep the one-second rhythm. */
        public final boolean countdown;

        Line(String id, boolean countdown) {
            this.id = id;
            this.countdown = countdown;
        }

        /** The countdown number line for {@code seconds} (1–10), or {@code null}. */
        public static Line countdown(int seconds) {
            return seconds >= 1 && seconds <= 10 ? valueOf("COUNTDOWN_" + seconds) : null;
        }

        public SoundEvent event(boolean turkish) {
            return (turkish ? TR : EN).get(this).get();
        }

        public String subtitleKey() {
            return "subtitles.spacebreaker.mission_control." + id;
        }
    }

    public static final Map<Line, DeferredHolder<SoundEvent, SoundEvent>> EN = new EnumMap<>(Line.class);
    public static final Map<Line, DeferredHolder<SoundEvent, SoundEvent>> TR = new EnumMap<>(Line.class);

    static {
        for (Line line : Line.values()) {
            EN.put(line, event("mission_control.en." + line.id));
            TR.put(line, event("mission_control.tr." + line.id));
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> event(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Spacebreaker.id(name)));
    }

    private SbSounds() {
    }
}
