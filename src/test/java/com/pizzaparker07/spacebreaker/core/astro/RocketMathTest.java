package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RocketMathTest {
    @Test
    void tsiolkovsky() {
        // Isp 300 s, mass ratio e -> Δv = 300 * 9.80665
        assertEquals(2941.995, RocketMath.deltaV(300, Math.E * 1000, 1000), 1e-3);
        assertEquals(0, RocketMath.deltaV(450, 1000, 1000), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> RocketMath.deltaV(300, 10, 20));
    }

    @Test
    void massRatioAndPropellantInvertTheEquation() {
        double dv = 9400, isp = 350;
        double mr = RocketMath.massRatio(dv, isp);
        assertEquals(dv, RocketMath.deltaV(isp, mr * 1000, 1000), 1e-6);
        double prop = RocketMath.propellantFor(dv, isp, 1000);
        assertEquals(dv, RocketMath.deltaV(isp, 1000 + prop, 1000), 1e-6);
        assertEquals(15.4, mr, 0.1);
    }

    @Test
    void burnTimeAndFlow() {
        // 1 MN at Isp 300 s burns ~340 kg/s
        assertEquals(339.9, RocketMath.massFlow(1e6, 300), 0.1);
        assertEquals(100000 / 339.9, RocketMath.burnTime(100000, 1e6, 300), 0.1);
        double m0 = 100000;
        double t = RocketMath.burnTimeForDeltaV(3000, m0, 1e6, 300);
        double used = t * RocketMath.massFlow(1e6, 300);
        assertEquals(3000, RocketMath.deltaV(300, m0, m0 - used), 1e-6);
    }

    @Test
    void thrustToWeight() {
        assertEquals(1.5, RocketMath.thrustToWeight(1.5 * 1000 * 9.80665, 1000, 9.80665), 1e-12);
        assertEquals(1.5 / 0.1652, RocketMath.thrustToWeight(1.5 * 1000 * 9.80665, 1000, 1.62), 0.05);
    }

    @Test
    void combinedAndPressureIsp() {
        assertEquals(300, RocketMath.combinedIsp(new double[]{1, 2}, new double[]{300, 300}), 1e-9);
        double mix = RocketMath.combinedIsp(new double[]{1e5, 1e5}, new double[]{300, 450});
        assertTrue(mix > 300 && mix < 375, "thrust-weighted harmonic mean");
        assertEquals(360, RocketMath.combinedIsp(new double[]{1e5, 1e5}, new double[]{300, 450}), 1e-9);
        assertEquals(282, RocketMath.ispAtPressure(282, 311, 101.325), 1e-9);
        assertEquals(311, RocketMath.ispAtPressure(282, 311, 0), 1e-9);
        assertEquals(311, RocketMath.ispAtPressure(282, 311, -5), 1e-9);
    }

    @Test
    void twoStageStack() {
        RocketMath.Stage first = new RocketMath.Stage(20000, 380000, 7.6e6, 290);
        RocketMath.Stage second = new RocketMath.Stage(4000, 90000, 9.3e5, 345);
        double payload = 10000;
        RocketMath.StageResult[] r = RocketMath.stack(List.of(first, second), payload, 9.80665);
        assertEquals(504000, r[0].initialMass(), 1e-6);
        assertEquals(124000, r[0].finalMass(), 1e-6);
        assertEquals(104000, r[1].initialMass(), 1e-6);
        assertEquals(14000, r[1].finalMass(), 1e-6);
        double dv1 = 290 * 9.80665 * Math.log(504000.0 / 124000.0);
        double dv2 = 345 * 9.80665 * Math.log(104000.0 / 14000.0);
        assertEquals(dv1, r[0].deltaV(), 1e-6);
        assertEquals(dv2, r[1].deltaV(), 1e-6);
        assertEquals(dv1 + dv2, RocketMath.totalDeltaV(List.of(first, second), payload), 1e-6);
        assertTrue(r[0].initialTwr() > 1.5 && r[0].finalTwr() > r[0].initialTwr());
        assertTrue(dv1 + dv2 > 9400, "this stack reaches orbit");
    }

    @Test
    void stagingBeatsSingleStage() {
        double total = 400000;
        RocketMath.Stage single = new RocketMath.Stage(40000, total - 40000, 1e7, 320);
        RocketMath.Stage a = new RocketMath.Stage(30000, 270000, 1e7, 320);
        RocketMath.Stage b = new RocketMath.Stage(10000, 90000, 2e6, 320);
        assertTrue(RocketMath.totalDeltaV(List.of(a, b), 5000) > RocketMath.totalDeltaV(List.of(single), 5000));
    }
}
