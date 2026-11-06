package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What a sealed suit can survive. Outside the temperature range the suit's thermal control can no
 * longer keep up and the wearer slowly heats up or cools down; in a thick atmosphere (Titan, Venus) the
 * gas carries heat much faster than vacuum, so the same excess hurts more (see LifeSupport).
 * Ranges follow real suits where they exist (an EMU is rated about -157 to +121 °C) and are stretched a
 * little for play.
 *
 * @param id              short name, also the item/texture prefix ("eva", "thermal", ...)
 * @param level           1 = Mk1, 2 = Mk2, 3 = Mk3
 * @param minTempC        coldest ambient temperature the suit fully compensates, °C
 * @param maxTempC        hottest ambient temperature the suit fully compensates, °C
 * @param radiationFactor fraction of the ambient dose rate that reaches the wearer
 * @param tech            technology the wearer must know for the tier's protection (null = always)
 */
public record SuitTier(String id, int level, int minTempC, int maxTempC, double radiationFactor, @Nullable ResourceLocation tech) {
    /** Mk1 EVA suit: lunar day and most of the lunar night, light shielding. */
    public static final SuitTier MK1 = new SuitTier("eva", 1, -160, 125, 0.7, null);
    /** Mk2 thermal suit: gold-foil reflective layers and a liquid cooling loop for Mercury's day and Venus. */
    public static final SuitTier THERMAL = new SuitTier("thermal", 2, -170, 500, 0.6, Spacebreaker.id("thermal_suits"));
    /** Mk2 heated suit: thick insulation and battery heaters for Titan's -180 °C nitrogen air and polar nights. */
    public static final SuitTier HEATED = new SuitTier("heated", 2, -250, 80, 0.6, Spacebreaker.id("heated_suits"));
    /** Mk3 radiation-hardened suit: layered polyethylene and tungsten plates for Jupiter's moons. */
    public static final SuitTier RADIATION = new SuitTier("radiation", 3, -190, 130, 0.04, Spacebreaker.id("radiation_hardened_suits"));

    public static final List<SuitTier> ALL = List.of(MK1, THERMAL, HEATED, RADIATION);

    /** Shielding in percent, for tooltips and the HUD. */
    public int shieldingPercent() {
        return (int) Math.round((1.0 - radiationFactor) * 100);
    }
}
