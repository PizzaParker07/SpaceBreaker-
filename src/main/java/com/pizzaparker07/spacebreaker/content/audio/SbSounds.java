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

    /** Clamp release, spring push and solar panels unfolding as a satellite leaves the rocket. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_SATELLITE_RELEASE = event("rocket.satellite_release");
    /** Retro thruster bursts, a landing-leg thump and chirps as a probe lands. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_PROBE_LANDING = event("rocket.probe_landing");

    // --- suit and station -----------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> SUIT_OXYGEN_ALARM = event("suit.oxygen_alarm");
    /** Hatch clunk, latch and the rush of air as an airlock cycles (5 s). */
    public static final DeferredHolder<SoundEvent, SoundEvent> AIRLOCK_CYCLE = event("station.airlock_cycle");

    // --- vehicles and machines (seamless 5 s loops; replay from a tickable sound) ----------------
    /** Electric wheel motors and gravel; scale pitch and volume with speed. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_MOTOR_LOOP = event("vehicle.rover_motor_loop");
    /** Transformer hum and bubbling for the electrolyzer while it runs. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTROLYZER_LOOP = event("machine.electrolyzer_loop");
    /** Motor drone and rock grinding for drills and excavators while they run. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DRILL_LOOP = event("machine.drill_loop");
    /** High rotor whine over a soft electric hum for the gas centrifuge cascade while it runs. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CENTRIFUGE_LOOP = event("machine.centrifuge_loop");
    /** Low steady drone with soft engine ticking for the fission surface power unit while it runs. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FISSION_REACTOR_LOOP = event("machine.fission_reactor_loop");

    // --- music (streamed; use in a biome's "music" field) ----------------------------------------
    /** Calm piano and synth pads for drifting in orbit. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_ORBIT = event("music.orbit");
    /** Low drones and glassy bells for the Moon and other airless worlds. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_MOON = event("music.moon");
    /** Warm pads and a lone distant melody over the red desert of Mars. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_MARS = event("music.mars");
    /** Airy glass tones above Venus' clouds over a distant furnace rumble. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_VENUS = event("music.venus");
    /** Amber haze and methane rain: detuned pads and soft falling notes for Titan. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_TITAN = event("music.titan");
    /** Swelling pads and ice chimes under Jupiter for its icy moons. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_JOVIAN = event("music.jovian");
    /** Sparse sub-bass drones, glassy harmonics and far-apart bells for Uranus, Neptune, Pluto and their moons. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_ICE_GIANTS = event("music.ice_giants");
    /** Soft pulsing arpeggio, warm pads and distant metallic tings for the asteroid belt and its dwarf planets. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BELT = event("music.belt");

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
