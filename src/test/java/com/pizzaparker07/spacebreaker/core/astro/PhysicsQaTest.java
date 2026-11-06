package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.DAY;
import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.YEAR;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Physics QA: every shipped body against independent reference values (NASA planetary and satellite fact
 * sheets), the Δv the game uses against published Δv-budget values, and transfer times and launch-window
 * periods against textbook numbers. Writes {@code build/astro/physics_qa.md}. Tolerances: docs/ASTRO.md.
 */
class PhysicsQaTest {
    /** name, mass kg, mean radius km, sidereal period days, solar day hours, mean temp °C, pressure kPa (NaN = skip). */
    private static final Object[][] BODIES = {
            {"mercury", 3.301e23, 2439.7, 87.969, 4222.6, 167.0, 0.0},
            {"venus", 4.867e24, 6051.8, 224.701, 2802.0, 464.0, 9200.0},
            {"earth", 5.972e24, 6371.0, 365.256, 24.0, 15.0, 101.4},
            {"moon", 7.346e22, 1737.4, 27.3217, 708.7, -20.0, 0.0},
            {"mars", 6.417e23, 3389.5, 686.98, 24.6597, -65.0, 0.636},
            {"phobos", 1.0659e16, 11.08, 0.31891, Double.NaN, Double.NaN, 0.0},
            {"deimos", 1.4762e15, 6.27, 1.26244, Double.NaN, Double.NaN, 0.0},
            {"ceres", 9.3839e20, 469.7, 1681.6, 9.074, -105.0, 0.0},
            {"vesta", 2.590e20, 262.7, 1325.75, 5.342, -100.0, 0.0},
            {"jupiter", 1.89813e27, 69911.0, 4332.59, 9.925, -110.0, Double.NaN},
            {"io", 8.932e22, 1821.6, 1.769138, 42.46, -163.0, 0.0},
            {"europa", 4.800e22, 1560.8, 3.551181, 85.23, -171.0, 0.0},
            {"ganymede", 1.4819e23, 2634.1, 7.154553, 171.71, -163.0, 0.0},
            {"callisto", 1.0759e23, 2410.3, 16.689018, 400.54, -139.0, 0.0},
            {"saturn", 5.6834e26, 58232.0, 10759.22, 10.656, -140.0, Double.NaN},
            {"titan", 1.3452e23, 2574.73, 15.945, 382.68, -179.5, 146.7},
            {"enceladus", 1.0802e20, 252.1, 1.370218, 32.89, -201.0, 0.0},
            {"uranus", 8.681e25, 25362.0, 30685.4, 17.24, -195.0, Double.NaN},
            {"miranda", 6.59e19, 235.8, 1.413479, 33.92, -187.0, 0.0},
            {"neptune", 1.02409e26, 24622.0, 60189.0, 16.11, -200.0, Double.NaN},
            {"triton", 2.139e22, 1353.4, 5.876854, 141.04, -235.0, 0.0014},
            {"pluto", 1.303e22, 1188.3, 90560.0, 153.29, -229.0, 0.001},
            {"charon", 1.586e21, 606.0, 6.38723, 153.29, -220.0, 0.0},
    };

    private static final StringBuilder REPORT = new StringBuilder();

    private static AstroBody b(String n) {
        return SolarSystemData.body(n);
    }

    @Test
    void bodiesMatchReferenceValues() throws Exception {
        StringBuilder r = new StringBuilder("| Body | mass | radius | period | day | mean T | pressure |\n|---|---|---|---|---|---|---|\n");
        for (Object[] row : BODIES) {
            String n = (String) row[0];
            AstroBody x = b(n);
            double mass = (Double) row[1], radius = (Double) row[2], period = (Double) row[3], day = (Double) row[4];
            double temp = (Double) row[5], p = (Double) row[6];
            assertEquals(mass, x.mass(), mass * 0.02, n + " mass");
            assertEquals(radius * 1000, x.radius(), radius * 10, n + " radius");
            assertEquals(period, x.orbit().period() / DAY, period * 0.01, n + " orbital period");
            if (!Double.isNaN(day)) {
                assertEquals(day, x.rotationPeriod() / 3600, day * 0.02, n + " day length");
            }
            double tolT = x.hasAtmosphere() && x.surfacePressureKpa() > 10 ? 15 : 60; // airless: noon/pre-dawn average vs global mean
            if (!Double.isNaN(temp)) {
                assertEquals(temp + 273.15, x.meanTempK(), tolT, n + " mean temperature");
            }
            if (!Double.isNaN(p)) {
                assertEquals(p, x.surfacePressureKpa(), Math.max(0.0005, p * 0.1), n + " pressure");
            }
            r.append(String.format(Locale.ROOT, "| %s | %.4g kg | %.1f km | %.4g d | %s | %.0f °C (ref %s) | %s |\n", n, x.mass(),
                    x.radius() / 1000, x.orbit().period() / DAY, Double.isNaN(day) ? "locked" : String.format(Locale.ROOT, "%.2f h", x.rotationPeriod() / 3600),
                    x.meanTempK() - 273.15, Double.isNaN(temp) ? "—" : String.format(Locale.ROOT, "%.0f", temp),
                    Double.isNaN(p) ? "1 bar ref." : String.format(Locale.ROOT, "%.4g kPa", x.surfacePressureKpa())));
        }
        section("Bodies vs NASA fact sheets (all within tolerance)", r.toString());
        // surface gravity and escape velocity
        assertEquals(9.80, b("earth").surfaceGravity(), 0.05);
        assertEquals(3.72, b("mars").surfaceGravity(), 0.03);
        assertEquals(1.62, b("moon").surfaceGravity(), 0.01);
        assertEquals(1.35, b("titan").surfaceGravity(), 0.02);
        assertEquals(4.25, b("mercury").escapeVelocity() / 1000, 0.02);
        assertEquals(10.36, b("venus").escapeVelocity() / 1000, 0.03);
        assertEquals(36.1, b("saturn").escapeVelocity() / 1000, 0.3);
    }

    /** label, real Δv (m/s) from the library, reference (m/s), tolerance (m/s). */
    private static void dv(StringBuilder r, String label, double value, double ref, double tol) {
        String status = Math.abs(value - ref) <= tol ? "ok" : "OUT";
        r.append(String.format(Locale.ROOT, "| %s | %.2f | %.2f ± %.2f | %.2f | %.2f | %s |\n", label, value / 1000, ref / 1000,
                tol / 1000, value * 0.35 / 1000, value * 0.25 / 1000, status));
        assertEquals(ref, value, tol, label);
    }

    @Test
    void deltaVMatchesPublishedBudgets() throws Exception {
        BodySystem s = SolarSystemData.system();
        StringBuilder r = new StringBuilder("| Leg | library (km/s) | reference | BALANCED ×0.35 | EASY ×0.25 | |\n|---|---|---|---|---|---|\n");
        dv(r, "Earth surface → LEO", TransferPlanner.ascent(b("earth")), 9400, 600);
        dv(r, "LEO → Earth surface (deorbit)", TransferPlanner.descent(b("earth"), true), 100, 80);
        dv(r, "LEO → escape", TransferPlanner.escape(b("earth")), 3220, 60);
        TransferPlanner.Plan moon = TransferPlanner.plan(s, "spacebreaker:earth", "spacebreaker:moon", false);
        dv(r, "Trans-lunar injection", moon.deltaV("departure"), 3130, 100);
        dv(r, "Lunar orbit insertion", moon.deltaV("capture"), 850, 100);
        dv(r, "Low lunar orbit → Moon surface", TransferPlanner.descent(b("moon"), true), 1870, 200);
        dv(r, "Moon surface → low lunar orbit", TransferPlanner.ascent(b("moon")), 1870, 200);
        TransferPlanner.Plan mars = TransferPlanner.plan(s, "spacebreaker:earth", "spacebreaker:mars", false);
        TransferPlanner.Plan marsAero = TransferPlanner.plan(s, "spacebreaker:earth", "spacebreaker:mars", true);
        dv(r, "Trans-Mars injection", mars.deltaV("departure"), 3600, 120);
        dv(r, "Mars orbit insertion (propulsive)", mars.deltaV("capture"), 2100, 200);
        dv(r, "Mars orbit insertion (aerobraking)", marsAero.deltaV("capture"), 100, 100);
        dv(r, "Low Mars orbit → surface (aero + powered)", TransferPlanner.descent(b("mars"), true), 750, 300);
        dv(r, "Mars surface → low Mars orbit", TransferPlanner.ascent(b("mars")), 4100, 300);
        TransferPlanner.Plan venus = TransferPlanner.plan(s, "spacebreaker:earth", "spacebreaker:venus", false);
        dv(r, "Trans-Venus injection", venus.deltaV("departure"), 3500, 150);
        dv(r, "Venus orbit insertion (propulsive)", venus.deltaV("capture"), 3300, 300);
        TransferPlanner.Plan merc = TransferPlanner.plan(s, "spacebreaker:earth", "spacebreaker:mercury", false);
        dv(r, "Trans-Mercury injection", merc.deltaV("departure"), 5500, 300);
        dv(r, "Mercury orbit insertion", merc.deltaV("capture"), 7500, 600);
        TransferPlanner.Plan jup = TransferPlanner.plan(s, "spacebreaker:earth", "spacebreaker:jupiter", false);
        dv(r, "Trans-Jupiter injection", jup.deltaV("departure"), 6300, 200);
        dv(r, "Low Jupiter orbit insertion", jup.deltaV("capture"), 17200, 1000);
        dv(r, "Phobos surface → orbit", TransferPlanner.ascent(b("phobos")), 10, 10);
        section("Δv vs published budgets (real values; gameplay = real × realism factor − fin/RCS bonuses)", r.toString());
    }

    @Test
    void windowsAndTransferTimes() throws Exception {
        BodySystem s = SolarSystemData.system();
        StringBuilder r = new StringBuilder("| Target | transfer | ref | window every | ref |\n|---|---|---|---|---|\n");
        Object[][] refs = {
                {"mercury", 105.5, 115.9}, {"venus", 146.1, 583.9}, {"mars", 258.9, 779.9}, {"ceres", 472.0, 466.7},
                {"jupiter", 2.73 * 365.25, 398.9}, {"saturn", 6.05 * 365.25, 378.1}, {"uranus", 16.0 * 365.25, 369.7},
                {"neptune", 30.6 * 365.25, 367.5}, {"pluto", 45.5 * 365.25, 366.7},
        };
        for (Object[] ref : refs) {
            String n = (String) ref[0];
            TransferPlanner.Plan p = TransferPlanner.plan(s, "spacebreaker:earth", "spacebreaker:" + n, false);
            double t = p.transferTime() / DAY, syn = p.synodicPeriod() / DAY;
            assertEquals((Double) ref[1], t, (Double) ref[1] * 0.02, n + " transfer time");
            assertEquals((Double) ref[2], syn, (Double) ref[2] * 0.01, n + " synodic period");
            r.append(String.format(Locale.ROOT, "| %s | %s | %s | %s | %s |\n", n, DeltaVMap.duration(p.transferTime()),
                    DeltaVMap.duration((Double) ref[1] * DAY), DeltaVMap.duration(p.synodicPeriod()), DeltaVMap.duration((Double) ref[2] * DAY)));
        }
        TransferPlanner.Plan moon = TransferPlanner.plan(s, "spacebreaker:earth", "spacebreaker:moon", false);
        assertEquals(5.0, moon.transferTime() / DAY, 0.3, "Hohmann to the Moon ≈ 5 days (Apollo's faster free return: 3 days)");
        r.append(String.format(Locale.ROOT, "| moon | %s | ≈ 5 d (Hohmann) | always | — |\n", DeltaVMap.duration(moon.transferTime())));
        // game time: at the default 1 real second per simulated hour, a Mars transfer takes ≈ 1.7 real hours
        double real = GameClock.realSeconds(TransferPlanner.plan(s, "spacebreaker:earth", "spacebreaker:mars", false).transferTime(), 1.0);
        assertEquals(1.73, real / 3600, 0.05);
        r.append(String.format(Locale.ROOT, "\nAt the default orbitalTimeScale (1 real s per simulated hour) a Mars transfer coasts %.1f real hours and a Mars window comes every %.1f real hours.\n",
                real / 3600, GameClock.realSeconds(780 * DAY, 1.0) / 3600));
        section("Transfer times and launch windows", r.toString());
        assertTrue(YEAR > 0);
    }

    private static synchronized void section(String title, String body) throws Exception {
        REPORT.append("### ").append(title).append("\n\n").append(body).append('\n');
        Path out = Path.of("build/astro/physics_qa.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, "## Physics QA report\n\n" + REPORT, StandardCharsets.UTF_8);
    }
}
