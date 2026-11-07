package com.pizzaparker07.spacebreaker.core.astro;

/** Two-body orbital mechanics: Kepler's equation, anomalies, state vectors, vis-viva and periods. */
public final class Kepler {
    public static final double TWO_PI = 2 * Math.PI;
    private static final int MAX_ITER = 60;
    private static final double TOL = 1e-14;

    /** Wraps an angle into [0, 2π). */
    public static double wrap(double angle) {
        double a = angle % TWO_PI;
        return a < 0 ? a + TWO_PI : a;
    }

    /** Wraps an angle into (−π, π]. */
    public static double wrapSigned(double angle) {
        double a = wrap(angle);
        return a > Math.PI ? a - TWO_PI : a;
    }

    /**
     * Solves Kepler's equation M = E − e sin E for the eccentric anomaly E (elliptic orbits, 0 ≤ e < 1).
     * Newton–Raphson with a robust starting value; converges for every e below 1.
     */
    public static double eccentricAnomaly(double meanAnomaly, double e) {
        if (e < 0 || e >= 1) {
            throw new IllegalArgumentException("elliptic eccentricity required, got " + e);
        }
        double m = wrapSigned(meanAnomaly);
        double E = e < 0.8 ? m : Math.copySign(Math.PI, m);
        if (m == 0) {
            return 0;
        }
        for (int i = 0; i < MAX_ITER; i++) {
            double f = E - e * Math.sin(E) - m;
            double d = f / (1 - e * Math.cos(E));
            E -= d;
            if (Math.abs(d) < TOL) {
                break;
            }
        }
        return E + (meanAnomaly - m);
    }

    /** Solves the hyperbolic Kepler equation M = e sinh H − H for H (e &gt; 1). */
    public static double hyperbolicAnomaly(double meanAnomaly, double e) {
        if (e <= 1) {
            throw new IllegalArgumentException("hyperbolic eccentricity required, got " + e);
        }
        double H = Math.log(2 * Math.abs(meanAnomaly) / e + 1.8) * Math.signum(meanAnomaly);
        if (meanAnomaly == 0) {
            return 0;
        }
        for (int i = 0; i < MAX_ITER * 2; i++) {
            double f = e * Math.sinh(H) - H - meanAnomaly;
            double d = f / (e * Math.cosh(H) - 1);
            H -= d;
            if (Math.abs(d) < TOL * Math.max(1, Math.abs(H))) {
                break;
            }
        }
        return H;
    }

    /** True anomaly from eccentric anomaly (elliptic). */
    public static double trueFromEccentric(double E, double e) {
        return 2 * Math.atan2(Math.sqrt(1 + e) * Math.sin(E / 2), Math.sqrt(1 - e) * Math.cos(E / 2));
    }

    /** Eccentric anomaly from true anomaly (elliptic). */
    public static double eccentricFromTrue(double nu, double e) {
        return 2 * Math.atan2(Math.sqrt(1 - e) * Math.sin(nu / 2), Math.sqrt(1 + e) * Math.cos(nu / 2));
    }

    /** Mean anomaly from true anomaly (elliptic). */
    public static double meanFromTrue(double nu, double e) {
        double E = eccentricFromTrue(nu, e);
        return E - e * Math.sin(E);
    }

    /** True anomaly from mean anomaly, elliptic or hyperbolic. */
    public static double trueAnomaly(double meanAnomaly, double e) {
        if (e < 1) {
            return trueFromEccentric(eccentricAnomaly(meanAnomaly, e), e);
        }
        double H = hyperbolicAnomaly(meanAnomaly, e);
        return 2 * Math.atan(Math.sqrt((e + 1) / (e - 1)) * Math.tanh(H / 2));
    }

    /** Orbital period of an ellipse with semi-major axis a (m) around mu, s. */
    public static double period(double a, double mu) {
        return TWO_PI * Math.sqrt(a * a * a / mu);
    }

    /** Semi-major axis giving a period T (s) around mu, m. */
    public static double semiMajorAxisForPeriod(double periodS, double mu) {
        double n = TWO_PI / periodS;
        return Math.cbrt(mu / (n * n));
    }

    /** Vis-viva: speed at radius r on an orbit of semi-major axis a, m/s. */
    public static double visViva(double mu, double r, double a) {
        return Math.sqrt(mu * (2 / r - 1 / a));
    }

    /** Circular orbit speed at radius r, m/s. */
    public static double circularSpeed(double mu, double r) {
        return Math.sqrt(mu / r);
    }

    /** Escape speed at radius r, m/s. */
    public static double escapeSpeed(double mu, double r) {
        return Math.sqrt(2 * mu / r);
    }

    /** Radius at true anomaly nu. */
    public static double radiusAt(double a, double e, double nu) {
        return a * (1 - e * e) / (1 + e * Math.cos(nu));
    }

    /** Radius of the sphere of influence of a body of mass m orbiting a primary of mass M at distance a. */
    public static double sphereOfInfluence(double a, double m, double bigM) {
        return a * Math.pow(m / bigM, 0.4);
    }

    /** Hill sphere radius. */
    public static double hillRadius(double a, double e, double m, double bigM) {
        return a * (1 - e) * Math.cbrt(m / (3 * bigM));
    }

    /** State vector from Keplerian elements at time t (s since epoch). */
    public static StateVector stateFromElements(OrbitalElements el, double t) {
        double e = el.eccentricity();
        double mu = el.mu();
        double a = el.semiMajorAxis();
        double nu = trueAnomaly(el.meanAnomaly(t), e);
        double p = a * (1 - e * e);
        double r = p / (1 + e * Math.cos(nu));
        // perifocal frame
        double xp = r * Math.cos(nu);
        double yp = r * Math.sin(nu);
        double k = Math.sqrt(mu / p);
        double vxp = -k * Math.sin(nu);
        double vyp = k * (e + Math.cos(nu));
        double node = el.longitudeAscendingNode(t), arg = el.argumentOfPeriapsis(t);
        return new StateVector(el.toEcliptic(perifocalToInertial(xp, yp, node, arg, el)),
                el.toEcliptic(perifocalToInertial(vxp, vyp, node, arg, el)));
    }

    private static Vec3 perifocalToInertial(double x, double y, double node, double arg, OrbitalElements el) {
        double cO = Math.cos(node), sO = Math.sin(node);
        double cw = Math.cos(arg), sw = Math.sin(arg);
        double ci = Math.cos(el.inclination()), si = Math.sin(el.inclination());
        double r11 = cO * cw - sO * sw * ci, r12 = -cO * sw - sO * cw * ci;
        double r21 = sO * cw + cO * sw * ci, r22 = -sO * sw + cO * cw * ci;
        double r31 = sw * si, r32 = cw * si;
        return new Vec3(r11 * x + r12 * y, r21 * x + r22 * y, r31 * x + r32 * y);
    }

    /** Keplerian elements from a state vector; mean anomaly refers to the state's own time (t = 0). */
    public static OrbitalElements elementsFromState(StateVector s, double mu) {
        Vec3 r = s.position(), v = s.velocity();
        double rn = r.length();
        Vec3 h = r.cross(v);
        double hn = h.length();
        Vec3 n = new Vec3(-h.y(), h.x(), 0);
        double nn = n.length();
        Vec3 eVec = r.scale(v.dot(v) - mu / rn).sub(v.scale(r.dot(v))).scale(1 / mu);
        double e = eVec.length();
        double energy = v.dot(v) / 2 - mu / rn;
        double a = Math.abs(e - 1) < 1e-12 ? Double.POSITIVE_INFINITY : -mu / (2 * energy);
        double i = Math.acos(clamp(h.z() / hn));
        double raan = nn < 1e-12 ? 0 : Math.acos(clamp(n.x() / nn));
        if (n.y() < 0) {
            raan = TWO_PI - raan;
        }
        double argp;
        double nu;
        if (e < 1e-11) {
            // circular: measure from ascending node (or x axis if equatorial)
            argp = 0;
            if (nn < 1e-12) {
                nu = Math.atan2(r.y(), r.x());
                if (h.z() < 0) {
                    nu = -nu;
                }
            } else {
                nu = Math.acos(clamp(n.dot(r) / (nn * rn)));
                if (r.z() < 0) {
                    nu = TWO_PI - nu;
                }
            }
        } else {
            if (nn < 1e-12) {
                argp = Math.atan2(eVec.y(), eVec.x());
                if (h.z() < 0) {
                    argp = TWO_PI - argp;
                }
            } else {
                argp = Math.acos(clamp(n.dot(eVec) / (nn * e)));
                if (eVec.z() < 0) {
                    argp = TWO_PI - argp;
                }
            }
            nu = Math.acos(clamp(eVec.dot(r) / (e * rn)));
            if (r.dot(v) < 0) {
                nu = TWO_PI - nu;
            }
        }
        double m;
        if (e < 1) {
            m = meanFromTrue(nu, e);
        } else {
            double H = 2 * atanh(Math.sqrt((e - 1) / (e + 1)) * Math.tan(nu / 2));
            m = e * Math.sinh(H) - H;
        }
        return new OrbitalElements(mu, a, e, i, wrap(raan), wrap(argp), e < 1 ? wrap(m) : m);
    }

    private static double atanh(double x) {
        return 0.5 * Math.log((1 + x) / (1 - x));
    }

    private static double clamp(double x) {
        return Math.max(-1, Math.min(1, x));
    }

    private Kepler() {
    }
}
