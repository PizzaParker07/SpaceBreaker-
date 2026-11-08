package com.pizzaparker07.spacebreaker.content.machines;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.content.earth.Metal;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipeBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

import static com.pizzaparker07.spacebreaker.content.earth.EarthModule.*;
import static com.pizzaparker07.spacebreaker.content.machines.MachinesModule.*;

/**
 * The processing chain. Progression: hand-made plates and basic circuits -> combustion generator, crusher, arc
 * furnace -> steel -> press and dies -> pumpjack and refinery (RP-1, plastic) -> fabricator -> electrolysis and
 * cryogenic liquefaction (LOX, LH2, LCH4).
 */
final class MachineRecipes {
    private MachineRecipes() {
    }

    static void add(RecipeOutput out) {
        processing(out);
        machines(out);
    }

    // ------------------------------------------------------------------ machine recipes

    private static void processing(RecipeOutput out) {
        // crushing: ore -> 2 dust, raw -> 1 dust (+50 %), ingot -> dust
        for (Metal m : METALS) {
            if (m.dust == null) {
                continue;
            }
            if (m.hasOre()) {
                ProcessingRecipeBuilder.of(CRUSHING).in(m.oreItemTag).out(m.dust.get(), 2).time(120).energy(20).save(out, m.oreName + "_ore");
                ProcessingRecipeBuilder.of(CRUSHING).in(m.rawTag).out(m.dust.get()).bonus(m.dust.get(), 1, 0.5F).time(100).energy(20).save(out, "raw_" + m.oreName);
            }
            ProcessingRecipeBuilder.of(CRUSHING).in(m.ingotTag).out(m.dust.get()).time(80).energy(16).save(out, m.name + "_ingot");
        }
        vanillaOre(out, EarthModule.IRON, Tags.Items.ORES_IRON, Tags.Items.RAW_MATERIALS_IRON);
        vanillaOre(out, EarthModule.COPPER, Tags.Items.ORES_COPPER, Tags.Items.RAW_MATERIALS_COPPER);
        vanillaOre(out, EarthModule.GOLD, Tags.Items.ORES_GOLD, Tags.Items.RAW_MATERIALS_GOLD);
        ProcessingRecipeBuilder.of(CRUSHING).in(Items.COAL).out(COAL_DUST.get()).time(60).energy(12).save(out, "coal");
        ProcessingRecipeBuilder.of(CRUSHING).in(Tags.Items.COBBLESTONES_NORMAL).out(Items.GRAVEL).time(60).energy(12).save(out, "cobblestone");
        ProcessingRecipeBuilder.of(CRUSHING).in(Tags.Items.GRAVELS).out(Items.SAND).bonus(Items.FLINT, 1, 0.1F).time(60).energy(12).save(out, "gravel");

        // arc smelting: refractory metals, dusts, alloys, silicon
        for (Metal m : METALS) {
            if (m.dust != null) {
                ProcessingRecipeBuilder.of(ARC_SMELTING).in(m.dustTag).out(m.ingotItem()).time(80).energy(40).save(out, m.name + "_from_dust");
            }
            if (m.hasOre() && !m.furnace) {
                ProcessingRecipeBuilder.of(ARC_SMELTING).in(m.rawTag).out(m.ingotItem()).time(160).energy(80).save(out, m.name + "_from_raw");
                ProcessingRecipeBuilder.of(ARC_SMELTING).in(m.oreItemTag).out(m.ingotItem()).time(160).energy(80).save(out, m.name + "_from_ore");
            }
        }
        ProcessingRecipeBuilder.of(ARC_SMELTING).in(Tags.Items.INGOTS_IRON).in(COAL_DUST_TAG).out(STEEL.ingotItem()).time(160).energy(60).save(out, "steel");
        ProcessingRecipeBuilder.of(ARC_SMELTING).in(ALUMINUM.ingotTag, 3).in(LITHIUM.ingotTag).out(AL_LI.ingotItem(), 4).time(200).energy(80).save(out, "aluminum_lithium");
        ProcessingRecipeBuilder.of(ARC_SMELTING).in(NICKEL.ingotTag, 3).in(TITANIUM.ingotTag).in(STEEL.ingotTag).out(SUPERALLOY.ingotItem(), 4)
                .time(240).energy(100).save(out, "nickel_superalloy");
        ProcessingRecipeBuilder.of(ARC_SMELTING).in(Tags.Items.SANDS, 2).in(COAL_DUST_TAG).out(SILICON.get()).time(200).energy(80).save(out, "silicon_from_sand");
        ProcessingRecipeBuilder.of(ARC_SMELTING).in(Tags.Items.GEMS_QUARTZ).in(COAL_DUST_TAG).out(SILICON.get(), 2).time(160).energy(80).save(out, "silicon_from_quartz");

        // pressing
        for (Metal m : METALS) {
            if (m.plate != null) {
                ProcessingRecipeBuilder.of(PRESSING).in(m.ingotTag).tool(PLATE_DIE.get()).out(m.plate.get()).time(60).energy(20).save(out, m.name + "_plate");
            }
            if (m.wire != null) {
                ProcessingRecipeBuilder.of(PRESSING).in(m.ingotTag).tool(WIRE_DIE.get()).out(m.wire.get(), 2).time(60).energy(20).save(out, m.name + "_wire");
            }
            if (m.rod != null) {
                ProcessingRecipeBuilder.of(PRESSING).in(m.ingotTag).tool(ROD_DIE.get()).out(m.rod.get()).time(60).energy(20).save(out, m.name + "_rod");
            }
        }
        ProcessingRecipeBuilder.of(PRESSING).in(SILICON_TAG).tool(PLATE_DIE.get()).out(SILICON_WAFER.get(), 2).time(100).energy(30).save(out, "silicon_wafer");

        // fabricating (parts; Rocketry and Suit add their own)
        ProcessingRecipeBuilder.of(FABRICATING).in(SILICON_WAFER.get()).in(COPPER.wireTag).in(Tags.Items.GLASS_PANES_COLORLESS)
                .out(SOLAR_CELL.get()).time(120).energy(40).save(out, "solar_cell");
        ProcessingRecipeBuilder.of(FABRICATING).in(BASIC_CIRCUIT.get(), 2).in(SILICON_WAFER.get(), 2).in(GOLD.wireTag).in(PLASTIC_TAG)
                .out(ADVANCED_CIRCUIT.get()).time(200).energy(60).save(out, "advanced_circuit");
        ProcessingRecipeBuilder.of(FABRICATING).in(PLASTIC_TAG, 2).in(COAL_DUST_TAG, 2).in(TITANIUM.plateTag)
                .out(ABLATIVE_SHIELD_PLATE.get(), 2).time(160).energy(40).save(out, "ablative_shield_plate");
        ProcessingRecipeBuilder.of(FABRICATING).in(GOLD.plateTag).in(PLASTIC_TAG)
                .out(GOLD_FOIL_INSULATION.get(), 4).time(80).energy(30).save(out, "gold_foil_insulation");
        ProcessingRecipeBuilder.of(FABRICATING).in(PLASTIC_TAG, 2).in(Tags.Items.DYES_ORANGE)
                .out(ORANGE_FOAM_INSULATION.get(), 4).time(80).energy(30).save(out, "orange_foam_insulation");
        ProcessingRecipeBuilder.of(FABRICATING).in(STEEL.plateTag, 2).in(COPPER.wireTag, 4).in(STEEL.rodTag)
                .out(ELECTRIC_MOTOR.get(), 2).time(120).energy(40).save(out, "electric_motor");

        // fluids: refinery, electrolysis, liquefaction
        ProcessingRecipeBuilder.of(REFINING).fluidIn(FluidsModule.TAG_CRUDE_OIL, 1000)
                .fluidOut(FluidsModule.RP1.get(), 500).fluidOut(FluidsModule.METHANE.get(), 250).out(PLASTIC_SHEET.get())
                .time(100).energy(60).save(out, "crude_oil");
        ProcessingRecipeBuilder.of(ELECTROLYSIS).fluidIn(net.neoforged.neoforge.common.Tags.Fluids.WATER, 500)
                .fluidOut(FluidsModule.HYDROGEN.get(), 1000).fluidOut(FluidsModule.OXYGEN.get(), 500)
                .time(40).energy(120).save(out, "water");
        ProcessingRecipeBuilder.of(LIQUEFACTION).fluidIn(FluidsModule.TAG_OXYGEN, 1000).fluidOut(FluidsModule.LIQUID_OXYGEN.get(), 500)
                .time(60).energy(100).save(out, "oxygen");
        ProcessingRecipeBuilder.of(LIQUEFACTION).fluidIn(FluidsModule.TAG_HYDROGEN, 1000).fluidOut(FluidsModule.LIQUID_HYDROGEN.get(), 250)
                .time(80).energy(100).save(out, "hydrogen");
        ProcessingRecipeBuilder.of(LIQUEFACTION).fluidIn(FluidsModule.TAG_METHANE, 1000).fluidOut(FluidsModule.LIQUID_METHANE.get(), 500)
                .time(60).energy(100).save(out, "methane");
    }

    private static void vanillaOre(RecipeOutput out, Metal m, TagKey<Item> ore, TagKey<Item> raw) {
        ProcessingRecipeBuilder.of(CRUSHING).in(ore).out(m.dust.get(), 2).time(120).energy(20).save(out, m.name + "_ore");
        ProcessingRecipeBuilder.of(CRUSHING).in(raw).out(m.dust.get()).bonus(m.dust.get(), 1, 0.5F).time(100).energy(20).save(out, "raw_" + m.name);
    }

    // ------------------------------------------------------------------ crafting the machines

    private static void machines(RecipeOutput out) {
        shaped(out, COMBUSTION_GENERATOR.item().get(), 1, has(IRON.plateTag), "PPP", "WFW", "PCP",
                'P', IRON.plateTag, 'W', COPPER.wireTag, 'F', Items.FURNACE, 'C', BASIC_CIRCUIT.get());
        shaped(out, BATTERY_BANK.item().get(), 1, has(LITHIUM.ingotTag), "PWP", "LCL", "PWP",
                'P', IRON.plateTag, 'W', COPPER.wireTag, 'L', LITHIUM.plateTag, 'C', BASIC_CIRCUIT.get());
        shaped(out, CRUSHER.item().get(), 1, has(ELECTRIC_MOTOR.get()), "PHP", "FMF", "PCP",
                'P', IRON.plateTag, 'H', Items.HOPPER, 'F', Items.FLINT, 'M', ELECTRIC_MOTOR.get(), 'C', BASIC_CIRCUIT.get());
        shaped(out, ARC_FURNACE.item().get(), 1, has(BASIC_CIRCUIT.get()), "RWR", "BKB", "PCP",
                'R', Items.REDSTONE, 'W', COPPER.wireTag, 'B', Items.BRICKS, 'K', Items.CAULDRON, 'P', IRON.plateTag, 'C', BASIC_CIRCUIT.get());
        shaped(out, PRESS.item().get(), 1, has(STEEL.plateTag), "SPS", "AMA", "SCS",
                'S', STEEL.plateTag, 'P', Items.PISTON, 'A', Items.ANVIL, 'M', ELECTRIC_MOTOR.get(), 'C', BASIC_CIRCUIT.get());
        shaped(out, FABRICATOR.item().get(), 1, has(STEEL.plateTag), "SCS", "MTM", "SCS",
                'S', STEEL.plateTag, 'C', BASIC_CIRCUIT.get(), 'M', ELECTRIC_MOTOR.get(), 'T', Items.CRAFTING_TABLE);
        shaped(out, ELECTROLYZER.item().get(), 1, has(STEEL.plateTag), "SGS", "WKW", "SCS",
                'S', STEEL.plateTag, 'G', Tags.Items.GLASS_BLOCKS_COLORLESS, 'W', COPPER.wireTag, 'K', Items.CAULDRON, 'C', BASIC_CIRCUIT.get());
        shaped(out, LIQUEFIER.item().get(), 1, has(ADVANCED_CIRCUIT.get()), "AFA", "MKM", "ACA",
                'A', ALUMINUM.plateTag, 'F', GOLD_FOIL_INSULATION.get(), 'M', ELECTRIC_MOTOR.get(), 'K', Items.CAULDRON, 'C', ADVANCED_CIRCUIT.get());
        shaped(out, SOLAR_PANEL_ITEM.get(), 1, has(SOLAR_CELL.get()), "LLL", "ARA", " C ",
                'L', SOLAR_CELL.get(), 'A', ALUMINUM.plateTag, 'R', STEEL.rodTag, 'C', BASIC_CIRCUIT.get());
        shaped(out, SOLAR_ARRAY_ITEM.get(), 1, has(SOLAR_PANEL_ITEM.get()), "PPP", "ARA", " C ",
                'P', SOLAR_PANEL_ITEM.get(), 'A', AL_LI.plateTag, 'R', STEEL.rodTag, 'C', ADVANCED_CIRCUIT.get());
        shaped(out, POWER_MONITOR_ITEM.get(), 1, has(BATTERY_BANK.item().get()), "PGP", "WCW", "PPP",
                'P', IRON.plateTag, 'G', Tags.Items.GLASS_PANES_COLORLESS, 'W', COPPER.wireTag, 'C', BASIC_CIRCUIT.get());
        shaped(out, COPPER_CABLE.get(), 6, has(COPPER.wireTag), "KKK", "WWW", "KKK", 'K', Items.DRIED_KELP, 'W', COPPER.wireTag);
        shaped(out, HV_CABLE.get(), 6, has(ALUMINUM.wireTag), "PPP", "WWW", "PPP", 'P', PLASTIC_TAG, 'W', ALUMINUM.wireTag);
        shaped(out, FLUID_PIPE.get(), 8, has(IRON.plateTag), "PPP", "   ", "PPP", 'P', IRON.plateTag);
        shaped(out, GAS_PIPE.get(), 8, has(COPPER.plateTag), "PPP", " S ", "PPP", 'P', COPPER.plateTag, 'S', SILICONE_SEAL.get());
        shaped(out, ITEM_DUCT.get(), 8, has(IRON.plateTag), "PPP", " H ", "PPP", 'P', IRON.plateTag, 'H', Items.HOPPER);
        MachineRecipesExtra.add(out);
    }

    static Criterion<?> has(TagKey<Item> tag) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(tag));
    }

    static Criterion<?> has(ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    /** pattern rows, then pairs of (char, TagKey or ItemLike). */
    @SuppressWarnings("unchecked")
    static void shaped(RecipeOutput out, ItemLike result, int count, Criterion<?> unlock, String r1, String r2, String r3, Object... keys) {
        ShapedRecipeBuilder b = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result, count).pattern(r1).pattern(r2).pattern(r3);
        for (int i = 0; i < keys.length; i += 2) {
            char c = (Character) keys[i];
            Object v = keys[i + 1];
            if (v instanceof TagKey<?> tag) {
                b.define(c, (TagKey<Item>) tag);
            } else {
                b.define(c, (ItemLike) v);
            }
        }
        b.unlockedBy("has_item", unlock).save(out);
    }

}
