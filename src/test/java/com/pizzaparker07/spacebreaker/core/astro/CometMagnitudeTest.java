package com.pizzaparker07.spacebreaker.core.astro;

import com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity;
import org.junit.jupiter.api.Test;

import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.DAY;
import static org.junit.jupiter.api.Assertions.*;

/** Comet brightness seen from Earth against real apparitions. */
class CometMagnitudeTest {
    private static final BodySystem SYS = SolarSystemData.system();
    private static final AstroBody EARTH = SYS.require("spacebreaker:earth");

    static double fromEarth(Comet c, double days) {
        double t = days * DAY;
        return CometActivity.apparentMagnitude(c, c.position(t), SYS.position(EARTH, t));
    }

    @Test
    void realApparitions() {
        // Hale–Bopp at perihelion (1997-04-01): about −0.8, a great naked-eye comet
        double hb = fromEarth(Comet.HALE_BOPP, Comet.HALE_BOPP.perihelionDays());
        assertEquals(-0.8, hb, 1.0, "Hale-Bopp " + hb);
        assertTrue(CometActivity.nakedEye(hb));
        // 67P at its 2015 perihelion: about 11–12, telescopic
        double c67 = fromEarth(Comet.CHURYUMOV_GERASIMENKO, Comet.CHURYUMOV_GERASIMENKO.perihelionDays());
        assertTrue(c67 > 9.5 && c67 < 13.5, "67P " + c67);
        assertFalse(CometActivity.nakedEye(c67));
        // Halley in March 1986 (about 2.5–4), naked-eye
        double halley = fromEarth(Comet.HALLEY, -5074.04 + 35);
        assertTrue(halley > 1 && halley < 5, "Halley 1986 " + halley);
        // Halley in 2000, near 25 AU: a bare 11 km nucleus, far beyond any naked eye
        double far = fromEarth(Comet.HALLEY, 0);
        assertTrue(far > 25, "Halley in 2000 " + far);
    }

    @Test
    void magnitudeRelations() {
        // Jorda et al.: Q = 10²⁸ s⁻¹ ↔ m_H ≈ 10.9
        assertEquals(10.9, CometActivity.comaHeliocentricMagnitude(1e28), 0.05);
        assertTrue(CometActivity.comaHeliocentricMagnitude(1e30) < CometActivity.comaHeliocentricMagnitude(1e28));
        // H = 5 log₁₀(1329 km / (D √p)): 67P (D = 3.3 km, p = 0.065) ≈ 16.0
        assertEquals(16.0, CometActivity.nucleusAbsoluteMagnitude(Comet.CHURYUMOV_GERASIMENKO), 0.1);
        // farther away is fainter
        Comet c = Comet.ENCKE;
        double t = c.perihelionDays() * DAY;
        Vec3 pos = c.position(t);
        assertTrue(CometActivity.apparentMagnitude(c, pos, pos.scale(0.5)) < CometActivity.apparentMagnitude(c, pos, pos.scale(-1)));
    }
}
