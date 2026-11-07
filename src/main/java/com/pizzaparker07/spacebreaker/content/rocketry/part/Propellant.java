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
    KEROLOX(1.0 / 3.56, 0.81, "rp1"),
    /** Liquid methane, mixture ratio O/F 3.6 (made on Mars by the Sabatier process). */
    METHALOX(1.0 / 4.6, 0.422, "liquid_methane"),
    /** Liquid hydrogen, mixture ratio O/F 6: the highest specific impulse, but very bulky and very cold. */
    HYDROLOX(1.0 / 7.0, 0.071, "liquid_hydrogen");

    public static final Codec<Propellant> CODEC = StringRepresentable.fromEnum(Propellant::values);
    /** Liquid oxygen, the oxidiser of every propellant. */
    public static final TagKey<Fluid> LOX_TAG = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "liquid_oxygen"));
    public static final double LOX_DENSITY = 1.141;

    private final double fuelFraction;
    private final double fuelDensity;
    private final TagKey<Fluid> fuelTag;

    Propellant(double fuelFraction, double fuelDensity, String fuelTag) {
        this.fuelFraction = fuelFraction;
        this.fuelDensity = fuelDensity;
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
