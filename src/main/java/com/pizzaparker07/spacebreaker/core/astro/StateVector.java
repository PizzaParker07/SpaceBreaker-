package com.pizzaparker07.spacebreaker.core.astro;

/** Position (m) and velocity (m/s) relative to the central body. */
public record StateVector(Vec3 position, Vec3 velocity) {
    public double radius() {
        return position.length();
    }

    public double speed() {
        return velocity.length();
    }
}
