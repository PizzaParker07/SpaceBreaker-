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

    /** Known feedstock from other areas, added by id only when it exists. */
    private static final List<String> REGOLITH_IDS = List.of("lunar_regolith", "mare_regolith", "martian_regolith",
            "phobos_regolith", "deimos_regolith", "venus_soil", "hematite_soil");
    private static final List<String> ICE_IDS = List.of("lunar_ice", "icy_regolith", "martian_ice", "dusty_ice", "mercury_ice");
    private static final List<String> DRY_ICE_IDS = List.of("dry_ice");
    private static final List<String> PERCHLORATE_IDS = List.of("perchlorate_soil");

    private IsruRecipes() {
    }

    static void tags(SbTags tags) {
        tags.item(REGOLITHS, byId(REGOLITH_IDS));
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
        // perchlorate decomposition (ClO4- -> Cl- + 2 O2)
        ProcessingRecipeBuilder.of(REGOLITH_PROCESSING).in(PERCHLORATE_SOILS).fluidOut(FluidsModule.OXYGEN.get(), 500)
                .time(100).energy(80).save(out, "perchlorate");
        // perchlorate salt (crushed soil) is richer than the soil itself
        ProcessingRecipeBuilder.of(REGOLITH_PROCESSING).in(PERCHLORATE_DUSTS).fluidOut(FluidsModule.OXYGEN.get(), 750)
                .time(80).energy(80).save(out, "perchlorate_salt");
        // solar-wind volatiles baked out of the grains: mostly hydrogen, a trace of helium-3
        ProcessingRecipeBuilder.of(REGOLITH_PROCESSING).in(SOLAR_WIND_REGOLITHS).fluidOut(FluidsModule.HYDROGEN.get(), 200)
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
        // RTG: shielded heat-source cavity (tungsten), silicon thermocouples, aluminium radiator fins
        shaped(out, RTG.item().get(), 1, has(EarthModule.TUNGSTEN.plateTag), "ATA", "SCS", "ATA",
                'A', EarthModule.ALUMINUM.plateTag, 'T', EarthModule.TUNGSTEN.plateTag, 'S', EarthModule.SILICON_WAFER.get(),
                'C', EarthModule.ADVANCED_CIRCUIT.get());
        // pellet: leach crushed pitchblende in acid, precipitate the oxide and sinter it into a clad pellet
        ProcessingRecipeBuilder.of(CHEMICAL_REACTION).in(EarthModule.URANIUM.dustTag, 4).fluidIn(FluidsModule.TAG_SULFURIC_ACID, 500)
                .out(RTG_FUEL_PELLET.get()).time(200).energy(80).save(out, "rtg_fuel_pellet");
        shaped(out, ICE_MELTER.item().get(), 1, has(EarthModule.ALUMINUM.plateTag), "AHA", "GKG", "ACA",
                'A', EarthModule.ALUMINUM.plateTag, 'H', Items.HOPPER, 'G', Tags.Items.GLASS_BLOCKS_COLORLESS, 'K', Items.CAULDRON,
                'C', EarthModule.BASIC_CIRCUIT.get());
    }
}
