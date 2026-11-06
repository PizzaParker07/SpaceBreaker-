package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import java.util.Locale;

/**
 * Deterministic, made-up proper names for generated stars, built from syllables (e.g. "Velorin", "Tasmerá"
 * style but plain ASCII). The same star id always gives the same name; names are not translated
 * (like real star names). Catalogue designations come from {@link StarSystemGenerator#designation}.
 */
public final class StarNames {
    private static final String[] START = {"a", "be", "ca", "da", "e", "fa", "ga", "ha", "i", "ka", "la", "ma", "na",
            "o", "pa", "ra", "sa", "ta", "u", "va", "ya", "za", "bri", "dra", "kre", "tri", "vo", "sel", "mor", "thal",
            "cor", "nym", "ori", "zen", "lu", "qua"};
    private static final String[] MIDDLE = {"ra", "le", "ni", "to", "va", "ri", "sa", "mi", "do", "ke", "lo", "ta",
            "ne", "ru", "si", "mo", "ga", "li", "ve", "ro", "an", "el", "or", "is"};
    private static final String[] END = {"n", "s", "r", "x", "th", "a", "is", "on", "ar", "el", "ia", "us", "or",
            "en", "ix", "um", "ae", "ir", "os", "ea"};

    /** Real star/constellation names and plain words that a generated name must not collide with. */
    private static final java.util.Set<String> RESERVED = java.util.Set.of("orion", "ones", "sirius", "vega", "lyra",
            "draco", "ara", "aries", "taurus", "hydra", "sol", "luna", "terra", "mars", "venus", "pluto", "titan",
            "io", "nova", "atlas", "lens", "sans", "mores", "dras", "dramas", "manis", "sales", "males", "tales",
            "canis", "carina", "corvus", "korea", "kenya", "india", "ira", "ural", "oman", "sudan", "ganymede", "europa");

    /** A pronounceable name of 2–4 syllables for the star with this id (never a reserved word). */
    public static String properName(long id) {
        for (int attempt = 0; ; attempt++) {
            String n = build(Rng.hash(id, 0x4E414D45L, attempt));
            if (!RESERVED.contains(n.toLowerCase(Locale.ROOT)) || attempt > 16) {
                return n;
            }
        }
    }

    private static String build(long seed) {
        Rng rng = new Rng(seed);
        StringBuilder sb = new StringBuilder(START[rng.nextInt(START.length)]);
        int middles = rng.nextInt(3);
        if (sb.length() < 3 && middles == 0) {
            middles = 1; // no two-letter names
        }
        for (int i = 0; i < middles; i++) {
            sb.append(MIDDLE[rng.nextInt(MIDDLE.length)]);
        }
        sb.append(END[rng.nextInt(END.length)]);
        String s = sb.toString().replace("aa", "a").replace("ii", "i").replace("uu", "u");
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    private StarNames() {
    }
}
