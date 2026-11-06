package com.pizzaparker07.spacebreaker.core.astro.smallbody;

/**
 * A population of small bodies around the Sun.
 *
 * @param key        id part, e.g. "main_belt"
 * @param nameEn     English name
 * @param nameTr     Turkish name
 * @param aMinAu     semi-major axis range
 * @param aMaxAu
 * @param eSigma     Rayleigh scale of the eccentricity
 * @param eMax       eccentricity cap
 * @param iSigmaDeg  Rayleigh scale of the inclination
 * @param minDiameterKm smallest generated body
 * @param maxDiameterKm largest generated body (real big ones are data files)
 */
public record Region(String key, String nameEn, String nameTr, double aMinAu, double aMaxAu, double eSigma, double eMax,
                     double iSigmaDeg, double minDiameterKm, double maxDiameterKm) {
    public static final Region MAIN_BELT = new Region("main_belt", "Main Belt", "Ana Kuşak", 2.1, 3.3, 0.1, 0.3, 7, 0.05, 100);
    /** Jupiter's L4/L5 Trojans: same orbit as Jupiter, 60° ahead or behind. */
    public static final Region TROJANS = new Region("trojans", "Jupiter Trojans", "Jüpiter Truvalıları", 5.05, 5.35, 0.07, 0.2, 12, 0.5, 100);
    public static final Region KUIPER_BELT = new Region("kuiper_belt", "Kuiper Belt", "Kuiper Kuşağı", 39.0, 48.0, 0.08, 0.3, 8, 1, 300);

    public boolean trojans() {
        return this == TROJANS || key.equals("trojans");
    }
}
