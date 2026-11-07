package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.PlanetModel;
import com.pizzaparker07.spacebreaker.core.astro.TerrainHints;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the star map's "nearby stars" view shows about one system, as plain data: the star, its companion,
 * and each planet with whether it is confirmed or only "not yet discovered". Pure Java; build one with
 * {@link #of(Galaxy, StarSeed)}.
 *
 * @param nameEn              English name
 * @param nameTr              Turkish name
 * @param designation         catalogue designation (real stars: their name)
 * @param distanceLy          from the Sun
 * @param summary             star kind, class, colour, mass, luminosity
 * @param binary              true when a companion star orbits the primary
 * @param knownPlanets        confirmed planets (show this count)
 * @param undiscoveredPlanets planets the generator added that nobody has found yet
 * @param habitableZoneInnerAu conservative habitable zone
 * @param habitableZoneOuterAu
 * @param planets             every planet, innermost first
 */
public record SystemCard(String nameEn, String nameTr, String designation, double distanceLy, StellarSummary summary,
                         boolean binary, int knownPlanets, int undiscoveredPlanets, double habitableZoneInnerAu,
                         double habitableZoneOuterAu, List<PlanetCard> planets) {

    /**
     * One planet.
     *
     * @param discovered        a confirmed real planet (false: "not yet discovered")
     * @param equilibriumTempK  equilibrium temperature with its albedo
     * @param inHabitableZone   orbit inside the conservative habitable zone
     * @param moons             number of moons
     * @param rings             has rings
     */
    public record PlanetCard(String id, String nameEn, String nameTr, AstroBody.Kind kind, boolean discovered,
                             double massEarths, double radiusEarths, double semiMajorAxisAu, double periodDays,
                             double equilibriumTempK, boolean inHabitableZone, boolean landable, int moons, boolean rings,
                             TerrainHints.ColorFamily look) {
    }

    /** The card of a star; for the Sun (whose system is the data registry) the planet list is empty. */
    public static SystemCard of(Galaxy galaxy, StarSeed seed) {
        StellarSummary sum = StellarSummary.of(seed);
        String en, tr, des;
        if (seed.fixed()) {
            NearbyStars.Entry e = NearbyStars.entry(seed.fixedKey()).orElseThrow();
            en = e.nameEn();
            tr = e.nameTr();
            des = e.nameEn();
        } else {
            en = tr = StarNames.properName(seed.id());
            des = StarSystemGenerator.designation(seed.id());
        }
        double ly = seed.distanceFromSun() * AstroConstants.PARSEC / AstroConstants.LIGHT_YEAR;
        double hzIn = PlanetModel.habitableZoneInner(sum.luminosity());
        double hzOut = PlanetModel.habitableZoneOuter(sum.luminosity());
        StarSystem sys = galaxy.system(seed).orElse(null);
        if (sys == null) {
            return new SystemCard(en, tr, des, ly, sum, false, 0, 0, hzIn, hzOut, List.of());
        }
        List<PlanetCard> cards = new ArrayList<>();
        int known = 0, unknown = 0;
        List<AstroBody> planets = new ArrayList<>(sys.planets());
        planets.sort((a, b) -> Double.compare(a.orbit().semiMajorAxis(), b.orbit().semiMajorAxis()));
        for (AstroBody p : planets) {
            StarSystem.Details d = sys.details(p.id());
            double au = p.orbit().semiMajorAxis() / AstroConstants.AU;
            if (d.discovered()) {
                known++;
            } else {
                unknown++;
            }
            cards.add(new PlanetCard(p.id(), d.nameEn(), d.nameTr(), p.kind(), d.discovered(), p.mass() / AstroConstants.EARTH_MASS,
                    p.radius() / AstroConstants.EARTH_RADIUS, au, p.orbit().period() / AstroConstants.DAY,
                    PlanetModel.equilibriumTemperature(d.irradiance(), p.albedo()), au >= hzIn && au <= hzOut,
                    p.landable(), sys.bodies().children(p.id()).size(), d.hasRings(), sys.terrainHints(p.id()).colorFamily()));
        }
        return new SystemCard(en, tr, des, ly, sum, sys.binary().isPresent(), known, unknown, hzIn, hzOut, List.copyOf(cards));
    }

    /** Planets in the habitable zone. */
    public List<PlanetCard> habitableZonePlanets() {
        return planets.stream().filter(PlanetCard::inHabitableZone).toList();
    }
}
