package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.StellarModel;

/** A star at a glance without generating its planets: kind, class, colour, mass and luminosity. */
public record StellarSummary(StarKind kind, StellarModel.SpectralClass spectralClass, int colorRgb, double mass,
                             double luminosity) {
    public static StellarSummary of(StarSeed seed) {
        StellarModel.Star star;
        StarKind kind;
        if (seed.fixed()) {
            NearbyStars.Entry e = NearbyStars.entry(seed.fixedKey()).orElseThrow();
            star = e.star();
            kind = StarKind.MAIN_SEQUENCE;
        } else {
            StarSystemGenerator.StarState st = StarSystemGenerator.starOf(seed);
            star = st.star();
            kind = st.kind();
        }
        return new StellarSummary(kind, star.spectralClass(), star.colorRgb(), star.mass(), star.luminosity());
    }
}
