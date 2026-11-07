package com.pizzaparker07.spacebreaker.content.machines;

import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipeBuilder;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

import static com.pizzaparker07.spacebreaker.content.machines.MachineRecipes.has;
import static com.pizzaparker07.spacebreaker.content.machines.MachineRecipes.shaped;
import static com.pizzaparker07.spacebreaker.content.machines.MachinesModule.*;

/**
 * Off-world industry. Feedstock is matched through common tags so the planet areas can add their blocks
 * without code dependencies: {@code c:regoliths}, {@code c:ices}, {@code c:dry_ices}, {@code c:perchlorate_soils}.
 */
public final class IsruRecipes {
    public static final TagKey<Item> REGOLITHS = SbTagKeys.commonItem("regoliths");
    public static final TagKey<Item> ICES = SbTagKeys.commonItem("ices");
    public static final TagKey<Item> DRY_ICES = SbTagKeys.commonItem("dry_ices");
    public static final TagKey<Item> PERCHLORATE_SOILS = SbTagKeys.commonItem("perchlorate_soils");
    /** Filled by the planet areas (crushed perchlorate soil, water-ice blocks). */
    public static final TagKey<Item> PERCHLORATE_DUSTS = SbTagKeys.commonItem("dusts/perchlorate");
    public static final TagKey<Item> WATER_ICES = SbTagKeys.commonItem("ices/water");
    /** Regolith soaked in solar-wind volatiles (Mercury, the Moon): filled by the planet areas. */
    public static final TagKey<Item> SOLAR_WIND_REGOLITHS = SbTagKeys.commonItem("regoliths/solar_wind");
    public static final TagKey<Item> SULFUR = SbTagKeys.commonItem("dusts/sulfur");
    /** Carbon-rich rubble (C-type asteroids, carbonaceous chondrite): clay minerals that hold water, and organics. */
    /** Fuel a nuclear reactor or nuclear thermal rocket accepts (spacebreaker:reactor_fuel). */
    public static final TagKey<Item> REACTOR_FUEL = SbTagKeys.modItem("reactor_fuel");
    public static final TagKey<Item> CARBONACEOUS_REGOLITHS = SbTagKeys.commonItem("regoliths/carbonaceous");
    /** Stony (S-type) asteroid rubble with grains of meteoritic iron-nickel. */
    public static final TagKey<Item> STONY_REGOLITHS = SbTagKeys.commonItem("regoliths/stony");

    /** Known feedstock from other areas, added by id only when it exists. */
    private static final List<String> REGOLITH_IDS = List.of("lunar_regolith", "mare_regolith", "martian_regolith",
            "phobos_regolith", "deimos_regolith", "venus_soil", "hematite_soil");
    private static final List<String> CARBONACEOUS_IDS = List.of("bennu_regolith", "bennu_boulder", "ryugu_regolith", "ryugu_rock",
            "carbonaceous_chondrite");
    private static final List<String> STONY_IDS = List.of("eros_regolith", "eros_chondrite", "itokawa_gravel", "itokawa_rock");
    private static final List<String> ICE_IDS = List.of("lunar_ice", "icy_regolith", "martian_ice", "dusty_ice", "mercury_ice");
    private static final List<String> DRY_ICE_IDS = List.of("dry_ice");
    private static final List<String> PERCHLORATE_IDS = List.of("perchlorate_soil");

    private IsruRecipes() {
    }

    static void tags(SbTags tags) {
        tags.item(REGOLITHS, byId(REGOLITH_IDS));
        tags.item(CARBONACEOUS_REGOLITHS, byId(CARBONACEOUS_IDS));
        tags.item(REACTOR_FUEL, NUCLEAR_FUEL_ELEMENT.get());
        tags.item(STONY_REGOLITHS, byId(STONY_IDS));
        // asteroid rubble is regolith too: it sinters, and stony rubble gives oxygen in the heater like any other
        tags.item(REGOLITHS, byId(STONY_IDS));
        tags.item(ICES, concat(byId(ICE_IDS), Items.ICE, Items.PACKED_ICE, Items.BLUE_ICE, Items.SNOW_BLOCK));
        tags.item(DRY_ICES, byId(DRY_ICE_IDS));
        tags.item(PERCHLORATE_SOILS, byId(PERCHLORATE_IDS));
    }

    private static Item[] byId(List<String> ids) {
        return ids.stream().map(id -> BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("spacebreaker", id)))
                .flatMap(java.util.Optional::stream).toArray(Item[]::new);
    }

    private static Item[] concat(Item[] a, Item... b) {
        Item[] out = java.util.Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    static void add(RecipeOutput out) {
        // CO2 + 4 H2 -> CH4 + 2 H2O
        ProcessingRecipeBuilder.of(SABATIER).fluidIn(FluidsModule.TAG_CO2, 500).fluidIn(FluidsModule.TAG_HYDROGEN, 2000)
                .fluidOut(FluidsModule.METHANE.get(), 500).fluidOut(Fluids.WATER, 250).time(80).energy(60).save(out, "methane");
        // molten regolith electrolysis; regolith is ~40 % oxygen by mass, metals stay behind
        ProcessingRecipeBuilder.of(REGOLITH_PROCESSING).in(REGOLITHS).fluidOut(FluidsModule.OXYGEN.get(), 250)
                .bonus(EarthModule.IRON.dust.get(), 1, 0.2F).time(160).energy(120).save(out, "regolith");
        // carbonaceous asteroids: heating drives water out of their clays (Bennu's rock is several % water) and burns
        // off a little carbon as CO2; the char left behind is carbon
        ProcessingRecipeBuilder.of(REGOLITH_PROCESSING).in(CARBONACEOUS_REGOLITHS).fluidOut(Fluids.WATER, 200)
                .fluidOut(FluidsModule.CARBON_DIOXIDE.get(), 50).bonus(EarthModule.COAL_DUST.get(), 1, 0.3F).time(160).energy(100)
                .priority(2).save(out, "carbonaceous_water");
        // stony asteroids: crushed rubble gives up its iron-nickel grains
        ProcessingRecipeBuilder.of(CRUSHING).in(STONY_REGOLITHS).out(EarthModule.IRON.dust.get())
                .bonus(EarthModule.NICKEL.dust.get(), 1, 0.25F).time(80).energy(16).save(out, "stony_asteroid_metal");
        // perchlorate decomposition (ClO4- -> Cl- + 2 O2)
        ProcessingRecipeBuilder.of(REGOLITH_PROCESSING).in(PERCHLORATE_SOILS).fluidOut(FluidsModule.OXYGEN.get(), 500)
                .time(100).energy(80).save(out, "perchlorate");
        // perchlorate salt (crushed soil) is richer than the soil itself
        ProcessingRecipeBuilder.of(REGOLITH_PROCESSING).in(PERCHLORATE_DUSTS).fluidOut(FluidsModule.OXYGEN.get(), 750)
                .time(80).energy(80).save(out, "perchlorate_salt");
        // solar-wind volatiles baked out of the grains: mostly hydrogen, a trace of helium-3
        // airless worlds' soil: oxygen bound in the minerals plus a trace of solar-wind helium-3 (hydrogen comes from their ice)
        ProcessingRecipeBuilder.of(REGOLITH_PROCESSING).in(SOLAR_WIND_REGOLITHS).fluidOut(FluidsModule.OXYGEN.get(), 250)
                .fluidOut(FluidsModule.HELIUM_3.get(), 20).time(160).energy(120).priority(1).save(out, "solar_wind_volatiles");
        // wet sulfuric acid process: S + O2 + H2O (via SO2/SO3) -> H2SO4
        ProcessingRecipeBuilder.of(CHEMICAL_REACTION).in(SULFUR).fluidIn(net.neoforged.neoforge.common.Tags.Fluids.WATER, 250)
                .fluidIn(FluidsModule.TAG_OXYGEN, 750).fluidOut(FluidsModule.SULFURIC_ACID.get(), 250).time(100).energy(60).save(out, "sulfuric_acid");
        ProcessingRecipeBuilder.of(MELTING).in(ICES).fluidOut(Fluids.WATER, 1000).time(60).energy(30).save(out, "ice");
        ProcessingRecipeBuilder.of(MELTING).in(WATER_ICES).fluidOut(Fluids.WATER, 1000).time(60).energy(30).save(out, "water_ice");
        ProcessingRecipeBuilder.of(MELTING).in(DRY_ICES).fluidOut(FluidsModule.CARBON_DIOXIDE.get(), 1000).time(40).energy(20).save(out, "dry_ice");

        shaped(out, ATMOSPHERE_PROCESSOR.item().get(), 1, has(EarthModule.ELECTRIC_MOTOR.get()), "AFA", "MCM", "APA",
                'A', EarthModule.ALUMINUM.plateTag, 'F', Items.IRON_BARS, 'M', EarthModule.ELECTRIC_MOTOR.get(),
                'C', EarthModule.ADVANCED_CIRCUIT.get(), 'P', GAS_PIPE.get());
        shaped(out, SABATIER_REACTOR.item().get(), 1, has(EarthModule.NICKEL.plateTag), "NPN", "SCS", "NPN",
                'N', EarthModule.NICKEL.plateTag, 'P', GAS_PIPE.get(), 'S', EarthModule.STEEL.plateTag, 'C', EarthModule.ADVANCED_CIRCUIT.get());
        shaped(out, REGOLITH_HEATER.item().get(), 1, has(EarthModule.TUNGSTEN.rodTag), "THT", "WFW", "TCT",
                'T', EarthModule.TITANIUM.plateTag, 'H', Items.HOPPER, 'W', EarthModule.TUNGSTEN.rodTag, 'F', Items.BLAST_FURNACE,
                'C', EarthModule.ADVANCED_CIRCUIT.get());
        shaped(out, CHEMICAL_REACTOR.item().get(), 1, has(EarthModule.STEEL.plateTag), "SGS", "PCP", "SMS",
                'S', EarthModule.STEEL.plateTag, 'G', Tags.Items.GLASS_BLOCKS_COLORLESS, 'P', FLUID_PIPE.get(),
                'C', EarthModule.ADVANCED_CIRCUIT.get(), 'M', EarthModule.ELECTRIC_MOTOR.get());
        // sintering: any planet's regolith (the Moon's, Mars', Mercury's...) fused into a ceramic block for base walls
        ProcessingRecipeBuilder.of(SINTERING).in(REGOLITHS).out(com.pizzaparker07.spacebreaker.content.moon.MoonModule.SINTERED_REGOLITH.get())
                .time(100).energy(60).save(out, "regolith");
        ProcessingRecipeBuilder.of(SINTERING).in(SOLAR_WIND_REGOLITHS).out(com.pizzaparker07.spacebreaker.content.moon.MoonModule.SINTERED_REGOLITH.get())
                .time(100).energy(60).save(out, "solar_wind_regolith");
        shaped(out, SINTERING_FURNACE.item().get(), 1, has(EarthModule.TUNGSTEN.rodTag), "BWB", "BCB", "SPS",
                'B', Items.BRICKS, 'W', EarthModule.TUNGSTEN.wireTag, 'C', EarthModule.BASIC_CIRCUIT.get(), 'S', EarthModule.STEEL.plateTag,
                'P', EarthModule.STEEL.plateTag);
        // excavator: tungsten-toothed bucket wheel on a motor, steel frame
        shaped(out, REGOLITH_EXCAVATOR.item().get(), 1, has(EarthModule.TUNGSTEN.rodTag), "SMS", "TCT", "T T",
                'S', EarthModule.STEEL.plateTag, 'M', EarthModule.ELECTRIC_MOTOR.get(), 'T', EarthModule.TUNGSTEN.rodTag,
                'C', EarthModule.ADVANCED_CIRCUIT.get());
        // RTG: shielded heat-source cavity (tungsten), silicon thermocouples, aluminium radiator fins
        shaped(out, RTG.item().get(), 1, has(EarthModule.TUNGSTEN.plateTag), "ATA", "SCS", "ATA",
                'A', EarthModule.ALUMINUM.plateTag, 'T', EarthModule.TUNGSTEN.plateTag, 'S', EarthModule.SILICON_WAFER.get(),
                'C', EarthModule.ADVANCED_CIRCUIT.get());
        // yellowcake: leach crushed uraninite in sulfuric acid and precipitate the oxide concentrate
        ProcessingRecipeBuilder.of(CHEMICAL_REACTION).in(EarthModule.URANIUM.dustTag).fluidIn(FluidsModule.TAG_SULFURIC_ACID, 125)
                .out(YELLOWCAKE.get()).time(80).energy(60).save(out, "yellowcake");
        // RTG pellet: the oxide pressed and sintered into a clad pellet
        ProcessingRecipeBuilder.of(SINTERING).in(YELLOWCAKE.get(), 4).out(RTG_FUEL_PELLET.get()).time(200).energy(80).save(out, "rtg_fuel_pellet");
        // enrichment: a slow, power-hungry spin; one part enriched to three parts depleted
        ProcessingRecipeBuilder.of(CENTRIFUGING).in(YELLOWCAKE.get(), 4).out(ENRICHED_URANIUM.get()).out(DEPLETED_URANIUM.get(), 3)
                .time(800).energy(160).save(out, "enriched_uranium");
        // reactor fuel: carbothermic reduction of enriched uranium to carbide kernels, sealed in a tungsten cermet clad
        ProcessingRecipeBuilder.of(ARC_SMELTING).in(ENRICHED_URANIUM.get(), 2).in(EarthModule.COAL_DUST_TAG, 2)
                .in(EarthModule.TUNGSTEN.rodTag, 2).out(NUCLEAR_FUEL_ELEMENT.get()).time(400).energy(120).save(out, "nuclear_fuel_element");
        // gas centrifuge: a tall rotor on magnetic bearings in a vacuum casing
        shaped(out, GAS_CENTRIFUGE.item().get(), 1, has(EarthModule.TITANIUM.plateTag), "SRS", "MCM", "SAS",
                'S', EarthModule.STEEL.plateTag, 'R', EarthModule.TITANIUM.rodTag, 'M', EarthModule.ELECTRIC_MOTOR.get(),
                'C', EarthModule.ADVANCED_CIRCUIT.get(), 'A', EarthModule.ALUMINUM.plateTag);
        shaped(out, ICE_MELTER.item().get(), 1, has(EarthModule.ALUMINUM.plateTag), "AHA", "GKG", "ACA",
                'A', EarthModule.ALUMINUM.plateTag, 'H', Items.HOPPER, 'G', Tags.Items.GLASS_BLOCKS_COLORLESS, 'K', Items.CAULDRON,
                'C', EarthModule.BASIC_CIRCUIT.get());
    }
}
