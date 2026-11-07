package com.pizzaparker07.spacebreaker.core.astro;

import java.util.Map;

/**
 * How a body looks from a viewpoint at a moment, for drawing it as a model: its spin axis, how far it has turned,
 * where the Sun lights it from and its phase. Directions are ecliptic J2000 unit vectors unless stated.
 *
 * <p>Body-fixed frame: z along the spin axis (right-hand rule), x on the prime meridian. A model drawn in that frame
 * and rotated by {@link #toEcliptic} shows the right face, tilt and lighting.
 *
 * @param spinAxis               spin axis (north by the right-hand rule)
 * @param rotationAngle          angle of the prime meridian from the axis's reference direction, rad
 * @param siderealRotationSeconds one turn relative to the stars (negative never; see {@code retrograde})
 * @param retrograde             spins backwards relative to its orbit (Venus, Uranus, Pluto)
 * @param axialTiltDeg           angle between the spin axis and the orbit normal
 * @param sunDirection           from the body to the Sun
 * @param viewDirection          from the body to the observer
 * @param phaseAngleRad          Sun–body–observer angle (0 = full)
 * @param illuminatedFraction    lit fraction of the disc seen by the observer, (1 + cos α) / 2
 */
public record BodyLook(Vec3 spinAxis, double rotationAngle, double siderealRotationSeconds, boolean retrograde,
                       double axialTiltDeg, Vec3 sunDirection, Vec3 viewDirection, double phaseAngleRad,
                       double illuminatedFraction) {

    /**
     * Spin poles (ICRF RA/Dec, IAU WGCCRE 2015) for bodies whose data file has none because their moons' orbits are
     * kept on the ecliptic (Earth) or that have no moons. Planets with moons carry their pole in the data.
     */
    public static final Map<String, double[]> KNOWN_POLES = Map.of(
            "spacebreaker:mercury", new double[]{281.0103, 61.4155},
            "spacebreaker:venus", new double[]{92.76, -67.16}, // antipode of the IAU north pole: Venus spins retrograde
            "spacebreaker:earth", new double[]{0.0, 90.0},
            "spacebreaker:moon", new double[]{269.9949, 66.5392});

    /** Unit vector of the prime meridian in ecliptic coordinates. */
    public Vec3 primeMeridian() {
        Vec3[] f = frame(spinAxis);
        return f[0].scale(Math.cos(rotationAngle)).add(f[1].scale(Math.sin(rotationAngle)));
    }

    /** Rotates a body-fixed vector (x prime meridian, z spin axis) to ecliptic coordinates. */
    public Vec3 toEcliptic(Vec3 bodyFixed) {
        Vec3 x = primeMeridian(), z = spinAxis, y = z.cross(x);
        return x.scale(bodyFixed.x()).add(y.scale(bodyFixed.y())).add(z.scale(bodyFixed.z()));
    }

    /** Expresses an ecliptic vector in the body-fixed frame. */
    public Vec3 toBodyFixed(Vec3 ecliptic) {
        Vec3 x = primeMeridian(), z = spinAxis, y = z.cross(x);
        return new Vec3(ecliptic.dot(x), ecliptic.dot(y), ecliptic.dot(z));
    }

    /** Sun direction in the body-fixed frame (for lighting a model). */
    public Vec3 sunInBodyFrame() {
        return toBodyFixed(sunDirection);
    }

    /** Observer direction in the body-fixed frame (which face is seen). */
    public Vec3 viewInBodyFrame() {
        return toBodyFixed(viewDirection);
    }

    /** Reference axes {x0, y0} perpendicular to a spin axis: x0 on the axis's ascending node on the ecliptic. */
    static Vec3[] frame(Vec3 axis) {
        Vec3 x = new Vec3(0, 0, 1).cross(axis);
        x = x.length() < 1e-12 ? new Vec3(1, 0, 0) : x.normalize();
        return new Vec3[]{x, axis.cross(x)};
    }

    /**
     * @param pole         spin axis from the data (ecliptic unit vector), or null: then {@link #KNOWN_POLES}, then the
     *                     orbit normal tilted by {@code axialTiltDeg} about the orbit's line of nodes
     * @param axialTiltDeg the data's axial tilt
     * @param observer     observer position (heliocentric, m)
     * @param t            simulated time, s from J2000
     */
    public static BodyLook of(BodySystem sys, AstroBody body, Vec3 pole, double axialTiltDeg, Vec3 observer, double t) {
        Vec3 pos = sys.position(body, t);
        Vec3 axis = spinAxis(sys, body, pole, axialTiltDeg, t);
        // the orbit that sets the solar day: the body's own around the Sun, or its planet's for a moon
        AstroBody root = body;
        for (AstroBody a : sys.ancestry(body)) {
            if (a.orbit() != null && sys.parent(a).map(AstroBody::isStar).orElse(false)) root = a;
        }
        Vec3 orbitNormal = root.orbit() == null ? new Vec3(0, 0, 1) : normal(root.orbit(), t);
        double tilt = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, axis.dot(orbitNormal)))));
        boolean retro = tilt > 90;
        double solar = body.rotationPeriod();
        double year = root.orbit() == null ? Double.POSITIVE_INFINITY : root.orbit().period();
        double sidereal = solar <= 0 ? Double.POSITIVE_INFINITY
                : 1 / (1 / solar + (retro ? -1 : 1) / year);
        double angle;
        if (body.tidallyLocked() && body.orbit() != null) {
            // the prime meridian faces the parent
            Vec3 toParent = sys.position(sys.parent(body).orElseThrow(), t).sub(pos);
            Vec3[] f = frame(axis);
            angle = Math.atan2(toParent.dot(f[1]), toParent.dot(f[0]));
        } else {
            angle = Double.isFinite(sidereal) ? Kepler.wrap((retro ? -1 : 1) * Kepler.TWO_PI * t / Math.abs(sidereal)) : 0;
        }
        Vec3 sun = pos.scale(-1).normalize();
        Vec3 view = observer.sub(pos).normalize();
        double alpha = Math.acos(Math.max(-1, Math.min(1, sun.dot(view))));
        return new BodyLook(axis, angle, Math.abs(sidereal), retro, tilt, sun, view, alpha, (1 + Math.cos(alpha)) / 2);
    }

    static Vec3 spinAxis(BodySystem sys, AstroBody body, Vec3 pole, double axialTiltDeg, double t) {
        if (pole != null) return pole.normalize();
        double[] known = KNOWN_POLES.get(body.id());
        if (known != null) return Frames.eclipticFromEquatorial(known[0], known[1]);
        if (body.orbit() == null) return new Vec3(0, 0, 1);
        Vec3 n = normal(body.orbit(), t);
        Vec3 node = new Vec3(0, 0, 1).cross(n);
        node = node.length() < 1e-12 ? new Vec3(1, 0, 0) : node.normalize();
        double a = Math.toRadians(axialTiltDeg);
        // Rodrigues rotation of the orbit normal about the line of nodes
        return n.scale(Math.cos(a)).add(node.cross(n).scale(Math.sin(a))).add(node.scale(node.dot(n) * (1 - Math.cos(a)))).normalize();
    }

    static Vec3 normal(OrbitalElements o, double t) {
        StateVector s = o.stateAt(t);
        return s.position().cross(s.velocity()).normalize();
    }
}
