package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Timings for the lunar shuttle and other surface ↔ orbit legs. */
class SurfaceLegTest {
    @Test
    void moonShuttleLikeApollo() {
        AstroBody moon = SolarSystemData.body("moon");
        TransferPlanner.SurfaceLeg up = TransferPlanner.ascentLeg(moon, 2.0);
        assertEquals(TransferPlanner.ascent(moon, 2.0), up.deltaV(), 1e-9);
        assertEquals(7.5, up.burnSeconds() / 60, 1.5, "Apollo ascent stage burned ≈ 7 min to orbit");
        assertEquals(0, up.coastSeconds());
        TransferPlanner.SurfaceLeg down = TransferPlanner.descentLeg(moon, true, 2.0);
        assertEquals(TransferPlanner.descent(moon, true, 2.0), down.deltaV(), 1e-9);
        assertEquals(56, down.coastSeconds() / 60, 3, "half an orbit from the deorbit burn to powered descent");
        assertEquals(7.5, down.burnSeconds() / 60, 1.5);
        assertEquals(down.burnSeconds() + down.coastSeconds(), down.totalSeconds(), 1e-9);
    }

    @Test
    void airAndThrust() {
        AstroBody mars = SolarSystemData.body("mars");
        TransferPlanner.SurfaceLeg withAir = TransferPlanner.descentLeg(mars, true, 1.5);
        TransferPlanner.SurfaceLeg powered = TransferPlanner.descentLeg(mars, false, 1.5);
        assertTrue(withAir.deltaV() < powered.deltaV());
        assertTrue(withAir.burnSeconds() < powered.burnSeconds(), "parachutes do most of the work");
        assertTrue(withAir.coastSeconds() > powered.coastSeconds(), "entry takes time");
        AstroBody moon = SolarSystemData.body("moon");
        assertTrue(TransferPlanner.ascentLeg(moon, 4).burnSeconds() < TransferPlanner.ascentLeg(moon, 1.5).burnSeconds());
        assertEquals(Double.POSITIVE_INFINITY, TransferPlanner.ascentLeg(SolarSystemData.body("earth"), 0.9).burnSeconds(), "cannot lift off");
    }
}
