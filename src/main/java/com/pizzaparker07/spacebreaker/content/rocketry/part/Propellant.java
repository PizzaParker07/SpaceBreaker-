package com.pizzaparker07.spacebreaker.content.rocketry.part;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * What an engine burns with liquid oxygen. A stage's tanks hold the propellant of the stage's engine, so the
 * same tank parts serve kerosene and methane stages.
 */
public enum Propellant implements StringRepresentable {
    /** RP-1 kerosene, mixture ratio O/F 2.56. */
    KEROLOX(1.0 / 3.56, 0.81, "rp1", 0),
    /** Liquid methane, mixture ratio O/F 3.6 (made on Mars by the Sabatier process). */
    METHALOX(1.0 / 4.6, 0.422, "liquid_methane", 0.0002),
    /** Liquid hydrogen, mixture ratio O/F 6: the highest specific impulse, but very bulky and very cold. */
    HYDROLOX(1.0 / 7.0, 0.071, "liquid_hydrogen", 0.001),
    /**
     * Liquid hydrogen alone, heated by a fission reactor in a nuclear thermal engine: no oxidiser at all, so the
     * whole propellant mass is hydrogen. It shares the hydrogen supply with hydrolox stages.
     */
    NUCLEAR(1.0, 0.071, "liquid_hydrogen", 0.001);

    public static final Codec<Propellant> CODEC = StringRepresentable.fromEnum(Propellant::values);
    /** Liquid oxygen, the oxidiser of every propellant. */
    public static final TagKey<Fluid> LOX_TAG = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "liquid_oxygen"));
    public static final double LOX_DENSITY = 1.141;

    private final double fuelFraction;
    private final double fuelDensity;
    private final TagKey<Fluid> fuelTag;
    private final double fuelBoilOff;
    /** Liquid oxygen lost to boil-off per day in a passively insulated tank (share of what is left). */
    public static final double LOX_BOIL_OFF = 0.0002;

    Propellant(double fuelFraction, double fuelDensity, String fuelTag, double fuelBoilOff) {
        this.fuelFraction = fuelFraction;
        this.fuelDensity = fuelDensity;
        this.fuelBoilOff = fuelBoilOff;
        this.fuelTag = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", fuelTag));
    }

    /** Fuel share of the propellant mass (the rest is liquid oxygen). */
    public double fuelFraction() {
        return fuelFraction;
    }

    /** Fuel density in kg per litre (1 mB = 1 L). */
    public double fuelDensity() {
        return fuelDensity;
    }

    public TagKey<Fluid> fuelTag() {
        return fuelTag;
    }

    /** Whether stages of this propellant take {@code fuel} (the same fuel liquid, e.g. hydrogen for hydrolox and nuclear). */
    public boolean takes(Propellant fuel) {
        return fuelTag.equals(fuel.fuelTag);
    }

    /**
     * Share of a stage's usable propellant lost per day to boil-off: the faster-boiling of its fuel and its
     * oxygen limits it (what boils away leaves the other liquid without a partner). Liquid hydrogen 0.1 %/day,
     * liquid oxygen and methane 0.02 %/day, RP-1 none.
     */
    public double boilOffPerDay() {
        return Math.max(fuelBoilOff, needsOxygen() ? LOX_BOIL_OFF : 0);
    }

    /** Whether this propellant needs liquid oxygen at all. */
    public boolean needsOxygen() {
        return fuelFraction < 1.0;
    }

    /** Litres (mB) of fuel in this much propellant. */
    public int fuelMillibuckets(double propellantKg) {
        return (int) Math.round(propellantKg * fuelFraction / fuelDensity);
    }

    /** Litres (mB) of liquid oxygen in this much propellant. */
    public int loxMillibuckets(double propellantKg) {
        return (int) Math.round(propellantKg * (1 - fuelFraction) / LOX_DENSITY);
    }

    public Component displayName() {
        return Component.translatable("propellant.spacebreaker." + getSerializedName());
    }

    /** The fuel this fluid is, or null (also for liquid oxygen). */
    @Nullable
    public static Propellant forFuel(FluidStack stack) {
        for (Propellant p : values()) {
            if (stack.is(p.fuelTag)) {
                return p;
            }
        }
        return null;
    }

    /** Whether a rocket takes this fluid: any fuel or liquid oxygen. */
    public static boolean isRocketLiquid(FluidStack stack) {
        return !stack.isEmpty() && (stack.is(LOX_TAG) || forFuel(stack) != null);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
