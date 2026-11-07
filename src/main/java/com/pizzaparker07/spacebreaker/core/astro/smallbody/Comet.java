package com.pizzaparker07.spacebreaker.core.astro.smallbody;

import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.Kepler;
import com.pizzaparker07.spacebreaker.core.astro.OrbitalElements;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * A comet nucleus on a heliocentric orbit. Pure data plus geometry; activity lives in {@link CometActivity}.
 *
 * @param id               id path, e.g. "churyumov_gerasimenko"
 * @param nameEn           English name
 * @param nameTr           Turkish name
 * @param designation      periodic-comet designation, e.g. "67P"
 * @param semiMajorAxisAu  a, AU
 * @param eccentricity     e
 * @param inclinationDeg   i (above 90° = retrograde)
 * @param nodeDeg          Ω
 * @param periapsisArgDeg  ω
 * @param perihelionDays   time of a perihelion passage, days from J2000
 * @param massKg           nucleus mass
 * @param radiusKm         volume-equivalent nucleus radius
 * @param rotationHours    spin period
 * @param geometricAlbedo  geometric albedo of the nucleus
 * @param activeFraction   fraction of the surface that sublimates (calibrated to observed water production)
 * @param dustToGas        dust-to-gas mass ratio of the outflow
 */
public record Comet(String id, String nameEn, String nameTr, String designation,
                    double semiMajorAxisAu, double eccentricity, double inclinationDeg, double nodeDeg,
                    double periapsisArgDeg, double perihelionDays, double massKg, double radiusKm,
                    double rotationHours, double geometricAlbedo, double activeFraction, double dustToGas) {

    /** 67P/Churyumov–Gerasimenko (Rosetta target), perihelion 2015-08-13. */
    public static final Comet CHURYUMOV_GERASIMENKO = new Comet("churyumov_gerasimenko",
            "67P/Churyumov–Gerasimenko", "67P/Çuryumov–Gerasimenko", "67P",
            3.4630, 0.6410, 7.04, 50.14, 12.78, 5702.59, 9.982e12, 1.65, 12.40, 0.065, 0.20, 1.5);
    /** 1P/Halley, perihelion 1986-02-09. Retrograde. */
    public static final Comet HALLEY = new Comet("halley", "1P/Halley", "1P/Halley", "1P",
            17.834, 0.96714, 162.26, 58.42, 111.33, -5074.04, 2.2e14, 4.4, 52.8, 0.04, 0.30, 1.0);
    /** 2P/Encke, the shortest known period (3.3 y), perihelion 2003-12-29. */
    public static final Comet ENCKE = new Comet("encke", "2P/Encke", "2P/Encke", "2P",
            2.2152, 0.8483, 11.78, 334.57, 186.54, 1458.48, 3.5e13, 2.4, 11.0, 0.05, 0.003, 3.0);

    public static final List<Comet> CATALOGUE = List.of(CHURYUMOV_GERASIMENKO, HALLEY, ENCKE);

    public static Optional<Comet> byId(String id) {
        return CATALOGUE.stream().filter(c -> c.id.equals(id) || c.designation.equalsIgnoreCase(id)).findFirst();
    }

    public double perihelionAu() {
        return semiMajorAxisAu * (1 - eccentricity);
    }

    public double aphelionAu() {
        return semiMajorAxisAu * (1 + eccentricity);
    }

    /** Orbital period, s. */
    public double periodSeconds() {
        return Kepler.period(semiMajorAxisAu * AstroConstants.AU, AstroConstants.MU_SUN);
    }

    public double periodYears() {
        return periodSeconds() / AstroConstants.YEAR;
    }

    /** Elements around the Sun with M₀ at J2000 derived from the perihelion time. */
    public OrbitalElements elements() {
        double a = semiMajorAxisAu * AstroConstants.AU;
        double n = Math.sqrt(AstroConstants.MU_SUN / (a * a * a));
        double m0 = Kepler.wrap(-n * perihelionDays * AstroConstants.DAY);
        return new OrbitalElements(AstroConstants.MU_SUN, a, eccentricity, Math.toRadians(inclinationDeg),
                Math.toRadians(nodeDeg), Math.toRadians(periapsisArgDeg), m0);
    }

    /** Heliocentric position at simulated time t (s from J2000), m. */
    public Vec3 position(double t) {
        return Kepler.stateFromElements(elements(), t).position();
    }

    public double distanceAu(double t) {
        return position(t).length() / AstroConstants.AU;
    }

    /** Simulated time of the next perihelion at or after t, s from J2000. */
    public double nextPerihelion(double t) {
        double p = periodSeconds(), tp = perihelionDays * AstroConstants.DAY;
        return tp + Math.ceil((t - tp) / p) * p;
    }

    public double surfaceAreaM2() {
        double r = radiusKm * 1000;
        return 4 * Math.PI * r * r;
    }

    /** Bulk density, kg/m³ (comets are ≈ 500 kg/m³: porous ice and dust). */
    public double density() {
        double r = radiusKm * 1000;
        return massKg / (4.0 / 3 * Math.PI * r * r * r);
    }

    public double surfaceGravity() {
        double r = radiusKm * 1000;
        return AstroConstants.G * massKg / (r * r);
    }

    public double escapeSpeed() {
        return Math.sqrt(2 * AstroConstants.G * massKg / (radiusKm * 1000));
    }

    public boolean retrograde() {
        return inclinationDeg > 90;
    }
}
