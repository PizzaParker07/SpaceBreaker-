package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class KeplerTest {
    @Test
    void keplerEquationSolvesForAllEccentricities() {
        for (double e = 0; e < 0.999; e += 0.0371) {
            for (double m = -7; m < 7; m += 0.173) {
                double E = Kepler.eccentricAnomaly(m, e);
                assertEquals(m, E - e * Math.sin(E), 1e-11, "e=" + e + " M=" + m);
            }
        }
    }

    @Test
    void knownKeplerSolution() {
        // Curtis, Orbital Mechanics example 3.1: e = 0.37255, M = 3.6029 rad -> E = 3.4794 rad
        assertEquals(3.4794, Kepler.eccentricAnomaly(3.6029, 0.37255), 1e-4);
    }

    @Test
    void hyperbolicKeplerEquation() {
        for (double e = 1.05; e < 6; e += 0.37) {
            for (double m = -40; m < 40; m += 1.7) {
                double H = Kepler.hyperbolicAnomaly(m, e);
                assertEquals(m, e * Math.sinh(H) - H, 1e-9 * Math.max(1, Math.abs(m)));
            }
        }
    }

    @Test
    void anomalyConversionsRoundTrip() {
        for (double e : new double[]{0, 0.1, 0.5, 0.9}) {
            for (double nu = -3; nu < 3; nu += 0.25) {
                double m = Kepler.meanFromTrue(nu, e);
                assertEquals(nu, Kepler.trueAnomaly(m, e), 1e-10);
            }
        }
    }

    @Test
    void leoPeriodIsAbout92Minutes() {
        double r = EARTH_RADIUS + 400e3;
        assertEquals(92.6, Kepler.period(r, MU_EARTH) / 60, 0.3);
    }

    @Test
    void geostationaryRadius() {
        double sidereal = 86164.0905;
        assertEquals(42164e3, Kepler.semiMajorAxisForPeriod(sidereal, MU_EARTH), 2e3);
    }

    @Test
    void earthEscapeAndOrbitalSpeeds() {
        assertEquals(11186, Kepler.escapeSpeed(MU_EARTH, EARTH_RADIUS), 5);
        assertEquals(7905, Kepler.circularSpeed(MU_EARTH, EARTH_RADIUS), 5);
        assertEquals(29.78e3, Kepler.circularSpeed(MU_SUN, AU), 30);
    }

    @Test
    void visVivaMatchesCircularAndEscape() {
        double r = 7000e3;
        assertEquals(Kepler.circularSpeed(MU_EARTH, r), Kepler.visViva(MU_EARTH, r, r), 1e-9);
        assertEquals(Kepler.escapeSpeed(MU_EARTH, r), Kepler.visViva(MU_EARTH, r, Double.POSITIVE_INFINITY), 1e-9);
    }

    @Test
    void earthOrbitalPeriodIsOneYear() {
        assertEquals(365.256, Kepler.period(1.000001018 * AU, MU_SUN + MU_EARTH * 1.0123) / DAY, 0.01);
    }

    @Test
    void stateVectorsRoundTripThroughElements() {
        OrbitalElements el = new OrbitalElements(MU_EARTH, 12000e3, 0.3, Math.toRadians(28.5), 1.1, 2.3, 0.7);
        for (double t = 0; t < el.period(); t += el.period() / 13) {
            StateVector s = el.stateAt(t);
            // vis-viva holds everywhere on the orbit
            assertEquals(Kepler.visViva(MU_EARTH, s.radius(), el.semiMajorAxis()), s.speed(), 1e-6);
            OrbitalElements back = Kepler.elementsFromState(s, MU_EARTH);
            assertEquals(el.semiMajorAxis(), back.semiMajorAxis(), 1e-3);
            assertEquals(el.eccentricity(), back.eccentricity(), 1e-10);
            assertEquals(el.inclination(), back.inclination(), 1e-10);
            assertEquals(el.longitudeAscendingNode(), back.longitudeAscendingNode(), 1e-9);
            assertEquals(el.argumentOfPeriapsis(), back.argumentOfPeriapsis(), 1e-8);
            assertEquals(el.meanAnomaly(t), back.meanAnomalyAtEpoch(), 1e-8);
        }
    }

    @Test
    void periapsisAndApoapsisRadii() {
        OrbitalElements el = new OrbitalElements(MU_EARTH, 10000e3, 0.2, 0, 0, 0, 0);
        assertEquals(8000e3, el.stateAt(0).radius(), 1e-3);
        assertEquals(12000e3, el.stateAt(el.period() / 2).radius(), 1);
        assertEquals(el.periapsis(), 8000e3, 1e-6);
        assertEquals(el.apoapsis(), 12000e3, 1e-6);
    }

    @Test
    void angularMomentumIsConserved() {
        OrbitalElements el = new OrbitalElements(MU_SUN, 1.5 * AU, 0.6, 0.4, 0.2, 0.9, 0);
        double h0 = el.stateAt(0).position().cross(el.stateAt(0).velocity()).length();
        for (double t = 0; t < el.period(); t += el.period() / 31) {
            StateVector s = el.stateAt(t);
            assertEquals(h0, s.position().cross(s.velocity()).length(), h0 * 1e-10);
        }
    }

    @Test
    void sphereOfInfluenceOfEarth() {
        assertEquals(924e6, Kepler.sphereOfInfluence(AU, EARTH_MASS, SOLAR_MASS), 5e6);
        assertEquals(1.5e9, Kepler.hillRadius(AU, 0, EARTH_MASS, SOLAR_MASS), 0.02e9);
    }

    @Test
    void wrapAngles() {
        assertEquals(0.5, Kepler.wrap(0.5 + 4 * Math.PI), 1e-12);
        assertEquals(2 * Math.PI - 0.5, Kepler.wrap(-0.5), 1e-12);
        assertEquals(-0.5, Kepler.wrapSigned(-0.5 + 6 * Math.PI), 1e-12);
    }
}
