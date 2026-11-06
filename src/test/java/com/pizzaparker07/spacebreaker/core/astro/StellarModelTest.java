package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StellarModelTest {
    @Test
    void theSunIsOneSolarUnitEverywhere() {
        StellarModel.Star sun = StellarModel.mainSequence(1.0);
        assertEquals(1.0, sun.luminosity(), 1e-12);
        assertEquals(1.0, sun.radius(), 1e-12);
        assertEquals(5772, sun.temperature(), 1e-9);
        assertEquals(1e10, sun.lifetimeYears(), 1);
        assertEquals(StellarModel.SpectralClass.G, sun.spectralClass());
    }

    @Test
    void luminosityGrowsSteeplyWithMass() {
        double prev = 0;
        for (double m = 0.08; m < 100; m *= 1.1) {
            double l = StellarModel.luminosity(m);
            assertTrue(l > prev, "monotonic at " + m);
            prev = l;
        }
        // Sirius A, 2.06 M☉ -> ~25 L☉ real; the fit gives the right order of magnitude
        assertEquals(25, StellarModel.luminosity(2.06), 8);
        // a 0.1 M☉ red dwarf is ~1e-3 L☉
        assertTrue(StellarModel.luminosity(0.1) < 3e-3);
    }

    @Test
    void lifetimes() {
        assertTrue(StellarModel.lifetimeYears(0.2) > 1e11, "red dwarfs outlive the universe");
        assertTrue(StellarModel.lifetimeYears(10) < 1e8, "massive stars die young");
    }

    @Test
    void spectralClassesFollowMass() {
        assertEquals(StellarModel.SpectralClass.M, StellarModel.mainSequence(0.2).spectralClass());
        assertEquals(StellarModel.SpectralClass.K, StellarModel.mainSequence(0.75).spectralClass());
        assertEquals(StellarModel.SpectralClass.A, StellarModel.mainSequence(2.0).spectralClass());
        assertEquals(StellarModel.SpectralClass.B, StellarModel.mainSequence(5.0).spectralClass());
        assertEquals(StellarModel.SpectralClass.O, StellarModel.mainSequence(30).spectralClass());
    }

    @Test
    void blackbodyColours() {
        int sun = StellarModel.blackbodyColor(5772);
        int red = StellarModel.blackbodyColor(3000);
        int blue = StellarModel.blackbodyColor(20000);
        assertTrue(r(sun) == 255 && g(sun) > 230 && b(sun) > 200, "sun is nearly white: " + Integer.toHexString(sun));
        assertTrue(r(red) > g(red) && g(red) > b(red), "cool stars are orange-red");
        assertTrue(b(blue) > r(blue), "hot stars are blue-white");
        assertEquals(0xFFFFFF, StellarModel.blackbodyColor(6600) | 0x0F0F0F);
    }

    @Test
    void inverseLuminosity() {
        for (double m : new double[]{0.1, 0.5, 1, 3, 20}) {
            assertEquals(m, StellarModel.massForLuminosity(StellarModel.luminosity(m)), m * 1e-6);
        }
    }

    private static int r(int c) {
        return c >> 16 & 0xFF;
    }

    private static int g(int c) {
        return c >> 8 & 0xFF;
    }

    private static int b(int c) {
        return c & 0xFF;
    }
}
