package com.pizzaparker07.spacebreaker.content.rocketry.entity;

/** Server-side flight phases of a rocket; synced to clients for effects, camera and HUD. */
public enum FlightState {
    /** Standing on a pad or landed; can be boarded, fuelled and launched. */
    IDLE,
    /** T-10 (T-5 on the Moon); engines light at T-3. */
    COUNTDOWN,
    /** Powered ascent from a surface to orbit, with staging. */
    ASCENT,
    /** Parked in an orbit dimension. */
    ORBIT,
    /** A transfer burn in orbit before coasting to the destination. */
    BURN,
    /** Coasting between places (screen fades; the rocket arrives somewhere else). */
    TRANSFER,
    /** Powered descent onto an airless body (retro-burn, legs out, dust). */
    DESCENT,
    /** Capsule falling through an atmosphere behind its heat shield. */
    REENTRY,
    /** Capsule hanging under its parachutes. */
    PARACHUTE;

    public static FlightState byId(int id) {
        FlightState[] all = values();
        return all[Math.floorMod(id, all.length)];
    }

    public boolean flying() {
        return this != IDLE && this != COUNTDOWN;
    }

    /** Engines are visibly burning in these phases (plus the last seconds of the countdown). */
    public boolean powered() {
        return this == ASCENT || this == BURN || this == DESCENT;
    }
}
