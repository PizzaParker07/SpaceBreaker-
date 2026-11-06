package com.pizzaparker07.spacebreaker.content.worldgen;

import net.minecraft.util.Mth;
import org.joml.Vector3d;

/**
 * Where things are in another world's sky, in Minecraft's local frame (+x east, +y up, -z north).
 * Shared by the sky renderer and tests, so the maths can be checked without a client.
 */
public final class SkyMath {
    /** Mean longitude (degrees) of the point that faces the parent planet, relative to x = 0. */
    public static final double SUB_PARENT_LONGITUDE = -35;
    /** Optical libration amplitudes of the Moon (degrees): the parent wobbles slightly in the sky. */
    public static final double LIBRATION_LON = 6.3;
    public static final double LIBRATION_LAT = 5.5;

    /**
     * Direction to the sun. The sun follows vanilla's east-west arc for {@code timeOfDay}
     * (vanilla convention, 0 = noon) tilted toward the equator by the observer's latitude,
     * so near the poles it only skims the horizon.
     */
    public static Vector3d sunDirection(double timeOfDay, double latitudeDeg) {
        double theta = timeOfDay * Mth.TWO_PI;
        double phi = Math.toRadians(latitudeDeg);
        double x = -Math.sin(theta);
        double y = Math.cos(theta) * Math.cos(phi);
        double z = Math.cos(theta) * Math.sin(phi);
        return new Vector3d(x, y, z);
    }

    /** Sun elevation above the horizon in degrees. */
    public static double sunElevation(double timeOfDay, double latitudeDeg) {
        return Math.toDegrees(Math.asin(Mth.clamp(sunDirection(timeOfDay, latitudeDeg).y, -1, 1)));
    }

    /**
     * Direction to the parent planet for a tidally locked moon: fixed in the sky, overhead at the
     * sub-planet point, on the horizon at the limb and below it on the far side.
     *
     * @param latitudeDeg  observer latitude
     * @param longitudeDeg observer longitude
     * @param dayFraction  local day fraction, drives the small libration wobble
     */
    public static Vector3d parentDirection(double latitudeDeg, double longitudeDeg, double dayFraction) {
        double lib = dayFraction * Mth.TWO_PI;
        double lon0 = Math.toRadians(SUB_PARENT_LONGITUDE + LIBRATION_LON * Math.sin(lib));
        double lat0 = Math.toRadians(LIBRATION_LAT * Math.cos(lib));
        // axis toward the planet in the body frame (x: lon 0, y: lon 90 E, z: north)
        double ax = Math.cos(lat0) * Math.cos(lon0);
        double ay = Math.cos(lat0) * Math.sin(lon0);
        double az = Math.sin(lat0);
        double phi = Math.toRadians(latitudeDeg);
        double lam = Math.toRadians(longitudeDeg);
        double up = ax * Math.cos(phi) * Math.cos(lam) + ay * Math.cos(phi) * Math.sin(lam) + az * Math.sin(phi);
        double east = -ax * Math.sin(lam) + ay * Math.cos(lam);
        double north = -ax * Math.sin(phi) * Math.cos(lam) - ay * Math.sin(phi) * Math.sin(lam) + az * Math.cos(phi);
        return new Vector3d(east, up, -north).normalize();
    }

    /** Lit fraction (0 = new, 1 = full) of a planet seen in direction {@code planet} while the sun is in direction {@code sun}. */
    public static double illuminatedFraction(Vector3d sun, Vector3d planet) {
        return (1 - sun.dot(planet)) / 2;
    }

    /** Apparent angular diameter in degrees of a sphere with radius r at distance d (same units). */
    public static double angularDiameter(double radius, double distance) {
        return Math.toDegrees(2 * Math.atan(radius / distance));
    }

    private SkyMath() {
    }
}
