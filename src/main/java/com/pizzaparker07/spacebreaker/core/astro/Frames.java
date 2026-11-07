package com.pizzaparker07.spacebreaker.core.astro;

/** Reference-frame conversions (J2000). */
public final class Frames {
    /** Obliquity of the ecliptic at J2000, degrees (IAU 2006). */
    public static final double OBLIQUITY_J2000_DEG = 23.439279;

    private Frames() {
    }

    /** Unit vector in ecliptic J2000 coordinates for a direction given in equatorial (ICRF) right ascension and declination. */
    public static Vec3 eclipticFromEquatorial(double raDeg, double decDeg) {
        double ra = Math.toRadians(raDeg), dec = Math.toRadians(decDeg), e = Math.toRadians(OBLIQUITY_J2000_DEG);
        double x = Math.cos(dec) * Math.cos(ra), y = Math.cos(dec) * Math.sin(ra), z = Math.sin(dec);
        return new Vec3(x, y * Math.cos(e) + z * Math.sin(e), -y * Math.sin(e) + z * Math.cos(e));
    }

    /**
     * Gives every body whose parent has a spin pole an orbit measured in that parent's equator
     * ({@link OrbitalElements#withReferencePole}).
     *
     * @param poles spin-axis direction (ecliptic unit vector) by body id
     */
    public static java.util.List<AstroBody> equatorialMoonOrbits(java.util.Collection<AstroBody> bodies,
                                                                   java.util.Map<String, Vec3> poles) {
        java.util.List<AstroBody> out = new java.util.ArrayList<>(bodies.size());
        for (AstroBody b : bodies) {
            Vec3 pole = b.parent() == null ? null : poles.get(b.parent());
            out.add(pole != null && b.orbit() != null && b.orbit().referencePole() == null
                    ? b.withOrbit(b.orbit().withReferencePole(pole)) : b);
        }
        return out;
    }
}
