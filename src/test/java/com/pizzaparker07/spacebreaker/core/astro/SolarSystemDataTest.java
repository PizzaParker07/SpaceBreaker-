package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Sanity checks on every shipped celestial_body JSON file against basic physics. */
class SolarSystemDataTest {
    private static final Set<String> EXPECTED = Set.of("sun", "mercury", "venus", "earth", "moon", "mars", "phobos",
            "deimos", "ceres", "vesta", "jupiter", "io", "europa", "ganymede", "callisto", "saturn", "titan",
            "enceladus", "uranus", "miranda", "ariel", "neptune", "triton", "pluto", "charon");

    @Test
    void everyBodyIsPresent() {
        for (String n : EXPECTED) {
            assertTrue(SolarSystemData.system().get("spacebreaker:" + n).isPresent(), n + ".json missing");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void filesAreConsistent() {
        BodySystem sys = SolarSystemData.system();
        AstroBody sun = sys.require("spacebreaker:sun");
        for (Path p : SolarSystemData.files()) {
            String id = SolarSystemData.id(p);
            String name = id.substring("spacebreaker:".length());
            Map<String, Object> j = SolarSystemData.json(p);
            AstroBody b = sys.require(id);
            assertTrue(b.mass() > 0 && b.radius() > 0, id);
            assertTrue(b.dayTempK() >= b.nightTempK(), id + ": day must not be colder than night");
            assertTrue(b.albedo() >= 0 && b.albedo() <= 1, id);
            Map<String, Object> phys = (Map<String, Object>) j.get("physical");
            if (phys.containsKey("surface_gravity")) {
                double g = (Double) phys.get("surface_gravity");
                // giants: NASA quotes gravity at the 1-bar equator including rotation, so allow more
                double tol = b.kind().isGiant() ? 0.10 : 0.03;
                assertEquals(b.surfaceGravity(), g, tol * g, id + ": surface_gravity disagrees with mass and radius");
            }
            Map<String, Object> atm = (Map<String, Object>) j.get("atmosphere");
            if (atm != null && atm.containsKey("composition")) {
                double sum = ((Map<String, Double>) (Map<?, ?>) atm.get("composition")).values().stream().mapToDouble(Double::doubleValue).sum();
                assertEquals(1.0, sum, 0.02, id + ": composition fractions");
                for (String gas : ((Map<String, Object>) atm.get("composition")).keySet()) {
                    assertTrue(AstroBody.MOLAR_MASS.containsKey(gas), id + ": unknown gas " + gas);
                }
            }
            if (b.isStar()) {
                assertNull(b.parent());
                assertFalse(b.landable());
                continue;
            }
            AstroBody parent = sys.require(b.parent());
            assertNotNull(b.orbit(), id + " needs an orbit");
            // Kepler's third law: the stated period matches a and the masses
            double muTrue = AstroConstants.G * (parent.mass() + b.mass());
            assertEquals(muTrue, b.orbit().mu(), muTrue * 0.02, id + ": period inconsistent with semi-major axis");
            // irradiance at the star distance
            double d = parent.isStar() ? b.orbit().semiMajorAxis() : parent.orbit().semiMajorAxis();
            double expected = AstroConstants.SOLAR_CONSTANT * Math.pow(AstroConstants.AU / d, 2);
            double irr = (Double) j.get("solar_irradiance");
            assertEquals(expected, irr, expected * 0.03, id + ": solar_irradiance");
            // landable bodies use spacebreaker:<name> (Earth is the overworld); giants are never landable
            if (b.kind().isGiant()) {
                assertFalse(b.landable(), id + ": gas and ice giants are not landable");
            } else if (!name.equals("earth")) {
                assertEquals("spacebreaker:" + name, j.get("dimension"), id + ": dimension id");
            }
            // moons sit well inside the parent's Hill sphere and outside its radius
            if (!parent.isStar()) {
                double hill = Kepler.hillRadius(parent.orbit().semiMajorAxis(), parent.orbit().eccentricity(), parent.mass(), sun.mass());
                assertTrue(b.orbit().semiMajorAxis() < 0.5 * hill, id + " outside the stable Hill region");
                assertTrue(b.orbit().periapsis() > parent.radius(), id + " inside its parent");
            }
        }
    }

    @Test
    void knownFacts() {
        assertEquals(11.19e3, SolarSystemData.body("earth").escapeVelocity(), 10);
        assertEquals(2.38e3, SolarSystemData.body("moon").escapeVelocity(), 10);
        assertEquals(5.03e3, SolarSystemData.body("mars").escapeVelocity(), 20);
        assertEquals(60.2e3, SolarSystemData.body("jupiter").escapeVelocity(), 300); // at the mean radius
        assertEquals(5514, SolarSystemData.body("earth").density(), 10);
        assertEquals(1.62, SolarSystemData.body("moon").surfaceGravity(), 0.01);
        AstroBody earth = SolarSystemData.body("earth");
        assertEquals(255, PlanetModel.equilibriumTemperature(1361, earth.albedo()), 1.5);
        assertTrue(SolarSystemData.body("triton").orbit().inclination() > Math.PI / 2, "Triton orbits backwards");
        assertEquals(PlanetModel.Composition.ICE_GIANT,
                PlanetModel.composition(SolarSystemData.body("neptune").mass() / AstroConstants.EARTH_MASS, SolarSystemData.body("neptune").density()));
        assertEquals(PlanetModel.Composition.OCEAN,
                PlanetModel.composition(SolarSystemData.body("europa").mass() / AstroConstants.EARTH_MASS, SolarSystemData.body("europa").density()));
    }

    @Test
    void atmospheresAreRetainedWhereTheyExist() {
        for (String n : new String[]{"earth", "venus", "mars", "titan"}) {
            AstroBody b = SolarSystemData.body(n);
            double flux = 1361 * Math.pow(AstroConstants.AU / SolarSystemData.system().distanceToStar(b, 0), 2);
            double teq = PlanetModel.equilibriumTemperature(flux, b.albedo());
            assertTrue(PlanetModel.retainsGas(b.escapeVelocity(), PlanetModel.exosphereTemperature(teq, flux), b.meanMolarMass()), n);
        }
        AstroBody moon = SolarSystemData.body("moon");
        assertFalse(PlanetModel.retainsGas(moon.escapeVelocity(), PlanetModel.exosphereTemperature(270, 1361), 28));
    }

    @Test
    void tidalLockFlagsMatchTheModel() {
        BodySystem sys = SolarSystemData.system();
        for (String n : new String[]{"moon", "phobos", "io", "europa", "ganymede", "callisto", "titan", "triton", "charon"}) {
            AstroBody b = SolarSystemData.body(n);
            AstroBody p = sys.require(b.parent());
            assertTrue(b.tidallyLocked(), n);
            assertTrue(PlanetModel.tidallyLocked(b.orbit().semiMajorAxis(), b.mass(), b.radius(), p.mass(), 4.5e9), n + " model");
        }
        assertFalse(SolarSystemData.body("mars").tidallyLocked());
    }
}
