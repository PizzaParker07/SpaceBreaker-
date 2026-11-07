package com.pizzaparker07.spacebreaker.content.vehicles;

import net.minecraft.util.Mth;

/**
 * Pure driving rules for the rover, kept free of game state so GameTests can check them directly.
 * Speeds are blocks per tick (0.45 b/t = 9 m/s, about the top speed of the Apollo rover with a tail wind).
 */
public final class RoverPhysics {
    public static final double MAX_SPEED = 0.45;
    public static final double MAX_REVERSE = 0.15;
    public static final double ACCEL = 0.012;
    public static final double BRAKE = 0.03;
    /** Turning rate at speed, degrees per tick at full lock. */
    public static final float TURN_RATE = 4.5F;
    /** Landing speed below which the suspension soaks up the impact without a bounce. */
    public static final double BOUNCE_THRESHOLD = 0.18;
    public static final double RESTITUTION = 0.35;
    /** FE per block driven (all four hub motors). */
    public static final int FE_PER_BLOCK = 50;

    private RoverPhysics() {
    }

    /**
     * Grip of the wheels: in low gravity the wheels press less on the ground, so they push (and brake) less.
     * Earth = 1, the Moon about 0.4, never below 0.25 so the rover stays drivable on small bodies.
     */
    public static double traction(double gravityFactor) {
        return Mth.clamp(Math.sqrt(Math.max(0, gravityFactor)), 0.25, 1.0);
    }

    /** Speed after one tick. throttle -1..1 (forward positive); airborne wheels neither push nor brake. */
    public static double speed(double speed, double throttle, boolean onGround, double traction) {
        if (onGround) {
            if (throttle != 0 && Math.signum(throttle) != Math.signum(speed) && Math.abs(speed) > 1e-4) {
                double brake = BRAKE * traction * Math.abs(throttle);
                speed = Math.abs(speed) <= brake ? 0 : speed - Math.signum(speed) * brake;
            } else {
                speed += throttle * ACCEL * traction;
            }
            // rolling resistance grows with how hard the wheels press on the ground
            speed *= throttle == 0 ? 1 - 0.06 * traction : 1 - 0.015 * traction;
        } else {
            speed *= 0.995;
        }
        if (Math.abs(speed) < 1e-4) {
            speed = 0;
        }
        return Mth.clamp(speed, -MAX_REVERSE, MAX_SPEED);
    }

    /** Change of heading in degrees for a steering input (-1 right .. 1 left, the player's strafe axis). */
    public static float yawChange(double speed, float steer) {
        float grip = (float) Math.min(1.0, Math.abs(speed) / 0.08);
        return -steer * TURN_RATE * grip * (speed < 0 ? -1 : 1);
    }

    /** Vertical speed after gravity and a little drag; vanilla Earth gravity is 0.08 b/t². */
    public static double fall(double vy, double gravityFactor) {
        return (vy - 0.08 * gravityFactor) * 0.98;
    }

    /** Upward speed after touching down at {@code vy} (negative = falling); 0 when the springs absorb it. */
    public static double bounce(double vy) {
        return vy < -BOUNCE_THRESHOLD ? -vy * RESTITUTION : 0;
    }

    /** Energy for a stretch of driving. */
    public static int driveCost(double blocks) {
        return (int) Math.ceil(blocks * FE_PER_BLOCK);
    }
}
