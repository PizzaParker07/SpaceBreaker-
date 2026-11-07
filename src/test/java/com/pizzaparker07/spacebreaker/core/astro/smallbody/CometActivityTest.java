package com.pizzaparker07.spacebreaker.core.astro.smallbody;

import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.Activity;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CometActivityTest {
    private static final Comet C67P = Comet.CHURYUMOV_GERASIMENKO;
    private static final double DAY = AstroConstants.DAY;

    @Test
    void orbitsMatchObservedComets() {
        assertEquals(6.44, C67P.periodYears(), 0.03);
        assertEquals(1.243, C67P.perihelionAu(), 0.01);
        assertEquals(75.3, Comet.HALLEY.periodYears(), 0.6);
        assertEquals(0.586, Comet.HALLEY.perihelionAu(), 0.01);
        assertTrue(Comet.HALLEY.retrograde());
        assertEquals(3.30, Comet.ENCKE.periodYears(), 0.03);
        assertEquals(0.336, Comet.ENCKE.perihelionAu(), 0.005);
        assertEquals(2530, Comet.HALE_BOPP.periodYears(), 30);
        assertEquals(0.914, Comet.HALE_BOPP.perihelionAu(), 0.005);
        assertEquals(5.51, Comet.TEMPEL_1.periodYears(), 0.05);
        assertEquals(1.506, Comet.TEMPEL_1.perihelionAu(), 0.01);
        for (Comet c : Comet.CATALOGUE) {
            assertTrue(c.density() > 300 && c.density() < 800, c.id() + " density " + c.density());
            assertEquals(c, Comet.byId(c.id()).orElseThrow());
            assertEquals(c, Comet.byId(c.designation().toLowerCase()).orElseThrow());
        }
        assertTrue(Comet.byId("nope").isEmpty());
        assertEquals(0.9, C67P.escapeSpeed(), 0.3, "67P escape speed ≈ 1 m/s");
    }

    @Test
    void perihelionTimeIsPerihelion() {
        for (Comet c : Comet.CATALOGUE) {
            double tp = c.perihelionDays() * DAY;
            assertEquals(c.perihelionAu(), c.distanceAu(tp), 1e-6, c.id());
            assertTrue(c.distanceAu(tp + 30 * DAY) > c.perihelionAu());
            double next = c.nextPerihelion(tp + DAY);
            assertEquals(c.periodSeconds(), next - tp, 1);
            assertEquals(c.perihelionAu(), c.distanceAu(next), 1e-6);
            assertEquals(tp, c.nextPerihelion(tp), 1e-3);
            assertEquals(c.aphelionAu(), c.distanceAu(tp + c.periodSeconds() / 2), 1e-6);
        }
        // Rosetta: 67P at ≈ 3.5 AU on 2014-08-06 (arrival), 5330 days after J2000
        assertEquals(3.5, C67P.distanceAu(5330.5 * DAY), 0.15);
    }

    @Test
    void sublimationLaw() {
        assertEquals(1.0, CometActivity.sublimation(1.0), 2e-3);
        double prev = Double.POSITIVE_INFINITY;
        for (double r = 0.1; r < 10; r += 0.05) {
            double g = CometActivity.sublimation(r);
            assertTrue(g < prev, "monotonic at " + r);
            prev = g;
        }
        assertEquals(Math.pow(0.5, -2.15), CometActivity.sublimation(0.5), 0.15, "≈ r^-2.15 inside 1 AU");
        assertTrue(CometActivity.sublimation(5) < 1e-3 * CometActivity.sublimation(2), "steep fall beyond 3 AU");
    }

    @Test
    void productionMatchesObservations() {
        // 67P at perihelion: ≈ 1–3·10²⁸ water molecules/s (Rosetta)
        double q67 = CometActivity.waterProduction(C67P, C67P.perihelionAu());
        assertTrue(q67 > 5e27 && q67 < 4e28, "67P Q " + q67);
        // Halley at 0.89 AU (Giotto, 1986): ≈ 7·10²⁹
        double qh = CometActivity.waterProduction(Comet.HALLEY, 0.89);
        assertTrue(qh > 2e29 && qh < 2e30, "Halley Q " + qh);
        // Hale–Bopp near perihelion: ≈ 10³¹ s⁻¹; Tempel 1 during Deep Impact (1.5 AU): ≈ 10²⁸ s⁻¹
        double qhb = CometActivity.waterProduction(Comet.HALE_BOPP, Comet.HALE_BOPP.perihelionAu());
        assertTrue(qhb > 3e30 && qhb < 3e31, "Hale-Bopp Q " + qhb);
        double qt = CometActivity.waterProduction(Comet.TEMPEL_1, Comet.TEMPEL_1.perihelionAu());
        assertTrue(qt > 3e27 && qt < 3e28, "Tempel 1 Q " + qt);
        assertEquals(Level.GREAT, CometActivity.at(Comet.HALE_BOPP, Comet.HALE_BOPP.perihelionDays() * DAY).level());
        // the real Hale–Bopp was active at 7 AU (CO, not water); the water-only model switches it on later
        assertTrue(CometActivity.activationDistance(Comet.HALE_BOPP) > CometActivity.activationDistance(Comet.HALLEY));
        double qe = CometActivity.waterProduction(Comet.ENCKE, Comet.ENCKE.perihelionAu());
        assertTrue(qe > 2e27 && qe < 3e28, "Encke Q " + qe);
        // Rosetta saw 67P switch on between 4 and 3 AU
        double on = CometActivity.activationDistance(C67P);
        assertTrue(on > 2.5 && on < 4.5, "67P activation " + on);
        assertTrue(CometActivity.activationDistance(Comet.HALLEY) > on, "bigger nucleus switches on farther out");
    }

    @Test
    void massLossPerOrbit() {
        // 67P lost ≈ 6·10⁹ kg of water over the 2015 apparition
        double w = CometActivity.waterLossPerOrbit(C67P);
        assertTrue(w > 2e9 && w < 2e10, "67P water loss " + w);
        double erosion = CometActivity.erosionPerOrbit(C67P);
        assertTrue(erosion > 0.2 && erosion < 3, "67P erosion " + erosion + " m");
        assertTrue(w / C67P.massKg() < 0.01, "a comet survives many passages");
    }

    @Test
    void activityFollowsTheOrbit() {
        double tp = C67P.perihelionDays() * DAY;
        Activity peri = CometActivity.at(C67P, tp);
        Activity aph = CometActivity.at(C67P, tp + C67P.periodSeconds() / 2);
        Activity mid = CometActivity.at(C67P, tp + 150 * DAY);
        assertEquals(Level.ACTIVE, peri.level());
        assertEquals(Level.DORMANT, aph.level());
        assertFalse(aph.active());
        assertEquals(0, aph.comaRadiusKm());
        assertEquals(0, aph.tailLengthKm());
        assertTrue(mid.waterPerSecond() < peri.waterPerSecond() && mid.waterPerSecond() > aph.waterPerSecond());
        assertTrue(peri.comaRadiusKm() > 1e4 && peri.comaRadiusKm() < 1e6, "coma " + peri.comaRadiusKm());
        assertTrue(peri.tailLengthKm() > 3e5 && peri.tailLengthKm() < 1e7, "tail " + peri.tailLengthKm());
        assertTrue(peri.comaRadiusKm() > mid.comaRadiusKm());
        assertEquals(peri.gasKgPerSecond() * C67P.dustToGas(), peri.dustKgPerSecond(), 1e-9);
        assertEquals(peri.gasKgPerSecond() + peri.dustKgPerSecond(), peri.totalKgPerSecond(), 1e-9);
        Vec3 pos = C67P.position(tp);
        assertEquals(1, peri.tailDirection().dot(pos.normalize()), 1e-9, "tail points away from the Sun");

        Activity halley = CometActivity.at(Comet.HALLEY, Comet.HALLEY.perihelionDays() * DAY + 30 * DAY);
        assertEquals(Level.GREAT, halley.level());
        assertTrue(halley.tailLengthKm() > 1e7, "Halley tail " + halley.tailLengthKm());
    }

    @Test
    void haserComa() {
        double q = 1e28, r = 1.3;
        assertTrue(CometActivity.haserDensity(q, r, 1e5) > CometActivity.haserDensity(q, r, 1e6));
        double edge = CometActivity.comaRadius(q, r, 2000);
        assertEquals(CometActivity.COMA_EDGE_DENSITY, CometActivity.haserDensity(q, r, edge), 1e6);
        assertEquals(0, CometActivity.comaRadius(1e15, r, 2000));
        assertTrue(CometActivity.comaRadius(1e29, r, 2000) > edge);
        assertEquals(850, CometActivity.outflowSpeed(1), 1e-9);
        assertEquals(850 * 8.3e4, CometActivity.scaleLength(1), 1e-3);
    }

    @Test
    void levelsAndNames() {
        assertEquals(Level.DORMANT, Level.of(1e20));
        assertEquals(Level.WEAK, Level.of(1e25));
        assertEquals(Level.ACTIVE, Level.of(1e28));
        assertEquals(Level.GREAT, Level.of(1e30));
        for (Level l : Level.values()) assertFalse(l.nameEn.isBlank() || l.nameTr.isBlank());
        for (Comet c : Comet.CATALOGUE) assertFalse(c.nameTr().isBlank());
    }
}
