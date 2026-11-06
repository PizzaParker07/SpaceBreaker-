package com.pizzaparker07.spacebreaker.content.suit;

/**
 * What a sealed suit can survive. Outside the temperature range the suit's thermal control can no
 * longer keep up and the wearer slowly heats up or cools down.
 *
 * @param level           1 = Mk1, 2 = Mk2, ...
 * @param minTempC        coldest ambient temperature the suit fully compensates, °C
 * @param maxTempC        hottest ambient temperature the suit fully compensates, °C
 * @param radiationFactor fraction of the ambient dose rate that reaches the wearer
 */
public record SuitTier(int level, int minTempC, int maxTempC, double radiationFactor) {
    /** Mk1: lunar day and night, light shielding (roughly a real EVA suit). */
    public static final SuitTier MK1 = new SuitTier(1, -180, 130, 0.7);
}
