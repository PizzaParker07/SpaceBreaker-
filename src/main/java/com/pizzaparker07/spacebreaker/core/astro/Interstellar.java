package com.pizzaparker07.spacebreaker.core.astro;

import com.pizzaparker07.spacebreaker.core.astro.galaxy.Galaxy;
import com.pizzaparker07.spacebreaker.core.astro.galaxy.NearbyStars;
import com.pizzaparker07.spacebreaker.core.astro.galaxy.StarNames;
import com.pizzaparker07.spacebreaker.core.astro.galaxy.StarSeed;
import com.pizzaparker07.spacebreaker.core.astro.galaxy.StarSystemGenerator;
import com.pizzaparker07.spacebreaker.core.astro.galaxy.StellarSummary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Star-to-star travel for the star map and Rocketry. Placeholder physics for a "deep space drive"
 * (unlocked by the research tech {@link #REQUIRED_TECH}): constant cruise speed as a fraction of light speed,
 * with relativistic Δv (2·c·atanh β for speeding up and slowing down) and time dilation.
 */
public final class Interstellar {
    /** Research tech id the Rocketry/Research areas should gate star travel behind. */
    public static final String REQUIRED_TECH = "spacebreaker:deep_space_drive";
    public static final double C = 299_792_458.0;

    /**
     * A reachable star.
     *
     * @param star        the star seed (open its system with {@code galaxy.system(star)})
     * @param distanceLy  distance from the origin star
     * @param nameEn      English name
     * @param nameTr      Turkish name
     * @param summary     star type at a glance (no planets generated yet)
     */
    public record Destination(StarSeed star, double distanceLy, String nameEn, String nameTr, StellarSummary summary) {
    }

    /**
     * A trip estimate.
     *
     * @param deltaV        relativistic Δv to reach and shed the cruise speed, m/s
     * @param travelYears   time that passes at home
     * @param shipYears     time that passes on board
     */
    public record Trip(double distanceLy, double cruiseFractionC, double deltaV, double travelYears, double shipYears) {
    }

    /** Stars within {@code radiusLy} of {@code from}, nearest first (the origin itself excluded). */
    public static List<Destination> nearby(Galaxy galaxy, StarSeed from, double radiusLy) {
        double radiusPc = radiusLy * AstroConstants.LIGHT_YEAR / AstroConstants.PARSEC;
        List<Destination> out = new ArrayList<>();
        for (StarSeed s : galaxy.starsWithin(from.position(), radiusPc)) {
            if (s.id() == from.id()) {
                continue;
            }
            double ly = s.position().distance(from.position()) * AstroConstants.PARSEC / AstroConstants.LIGHT_YEAR;
            String en, tr;
            if (s.fixed()) {
                NearbyStars.Entry e = NearbyStars.entry(s.fixedKey()).orElseThrow();
                en = e.nameEn();
                tr = e.nameTr();
            } else {
                en = tr = StarNames.properName(s.id());
            }
            out.add(new Destination(s, ly, en, tr, StellarSummary.of(s)));
        }
        out.sort(Comparator.comparingDouble(Destination::distanceLy));
        return out;
    }

    /** Trip at a cruise speed (fraction of c, 0 &lt; β &lt; 1); acceleration phases are ignored. */
    public static Trip trip(double distanceLy, double beta) {
        if (beta <= 0 || beta >= 1) {
            throw new IllegalArgumentException("cruise speed must be between 0 and c");
        }
        double rapidity = 0.5 * Math.log((1 + beta) / (1 - beta));
        double years = distanceLy / beta;
        double gamma = 1 / Math.sqrt(1 - beta * beta);
        return new Trip(distanceLy, beta, 2 * C * rapidity, years, years / gamma);
    }

    /** Game-friendly travel time for a distance when trips are scaled (e.g. 1 light year = {@code secondsPerLy} simulated s). */
    public static double scaledTravelSeconds(double distanceLy, double secondsPerLy) {
        return distanceLy * secondsPerLy;
    }

    private Interstellar() {
    }
}
