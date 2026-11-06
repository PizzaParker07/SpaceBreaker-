package com.pizzaparker07.spacebreaker.content.rocketry.entity;

import com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line;
import org.jetbrains.annotations.Nullable;

/**
 * Which mission control line a change in the ridden rocket's synced state calls for. Pure logic: the client
 * feeds it what it saw last tick and what it sees now (the snapshot survives the rocket being re-created in a
 * new dimension), so only the crew hears the calls, and the tests can check every transition.
 */
public final class MissionCalls {
    /**
     * What the crew can see of the flight.
     *
     * @param canReturn whether the fuel on board still gets the crew home (from where they are)
     */
    public record Snapshot(FlightState state, int timer, int stage, boolean canReturn) {
    }

    private MissionCalls() {
    }

    /** Seconds the countdown shows for this timer value (T-n). */
    public static int secondsLeft(int timer) {
        return (int) Math.ceil(timer / 20.0);
    }

    /** The line to speak for the change from {@code prev} to {@code now}, or null for none. */
    @Nullable
    public static Line call(@Nullable Snapshot prev, Snapshot now) {
        FlightState was = prev == null ? null : prev.state();
        FlightState is = now.state();
        if (is == FlightState.COUNTDOWN) {
            boolean crossedIgnition = was == FlightState.COUNTDOWN && prev.timer() > RocketEntity.IGNITION_TICKS
                    && now.timer() <= RocketEntity.IGNITION_TICKS;
            if (crossedIgnition) {
                return Line.IGNITION;
            }
            int n = secondsLeft(now.timer());
            boolean newSecond = was != FlightState.COUNTDOWN || secondsLeft(prev.timer()) != n;
            boolean ignited = now.timer() <= RocketEntity.IGNITION_TICKS && n == secondsLeft(RocketEntity.IGNITION_TICKS);
            return newSecond && !ignited ? Line.countdown(n) : null;
        }
        if (was == FlightState.COUNTDOWN && is == FlightState.ASCENT) {
            return Line.LIFTOFF;
        }
        if (was == FlightState.COUNTDOWN && is == FlightState.IDLE) {
            return Line.COUNTDOWN_HOLD;
        }
        if (was != null && was != is) {
            switch (is) {
                case ORBIT -> {
                    return Line.ORBIT_REACHED;
                }
                case REENTRY -> {
                    return Line.ENTRY_INTERFACE;
                }
                case PARACHUTE -> {
                    return Line.PARACHUTES;
                }
                case IDLE -> {
                    if (was == FlightState.DESCENT || was == FlightState.PARACHUTE || was == FlightState.REENTRY) {
                        return Line.TOUCHDOWN;
                    }
                }
                default -> {
                }
            }
        }
        if (prev != null && now.stage() > prev.stage() && (is == FlightState.ASCENT || is == FlightState.BURN || is == FlightState.DESCENT)) {
            return Line.STAGE_SEPARATION;
        }
        if (prev != null && prev.canReturn() && !now.canReturn()) {
            return Line.FUEL_LOW;
        }
        return null;
    }
}
