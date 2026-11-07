package com.pizzaparker07.spacebreaker.core.astro.flight;

import com.pizzaparker07.spacebreaker.core.astro.BodyLook;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;

/**
 * Latitude, longitude and altitude over a turning body, from a position relative to it (ecliptic frame) and its
 * {@link BodyLook} at that moment, and back. Longitude 0 is the body's prime meridian, east positive.
 */
public record SurfacePoint(double latitudeDeg, double longitudeDeg, double altitude) {

    public static SurfacePoint of(Vec3 relEcliptic, BodyLook look, double bodyRadius) {
        Vec3 b = look.toBodyFixed(relEcliptic);
        double r = b.length();
        double lat = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, b.z() / r))));
        double lon = Math.toDegrees(Math.atan2(b.y(), b.x()));
        return new SurfacePoint(lat, lon, r - bodyRadius);
    }

    /** Position relative to the body (ecliptic frame) of this point. */
    public Vec3 toEcliptic(BodyLook look, double bodyRadius) {
        double la = Math.toRadians(latitudeDeg), lo = Math.toRadians(longitudeDeg), r = bodyRadius + altitude;
        Vec3 b = new Vec3(r * Math.cos(la) * Math.cos(lo), r * Math.cos(la) * Math.sin(lo), r * Math.sin(la));
        return look.toEcliptic(b);
    }

    /** Block coordinates on a surface map where 1° of longitude spans {@code blocksPerDegree} at the equator (x east, z south). */
    public long[] mapXZ(double blocksPerDegree) {
        return new long[]{Math.round(longitudeDeg * blocksPerDegree), Math.round(-latitudeDeg * blocksPerDegree)};
    }
}
