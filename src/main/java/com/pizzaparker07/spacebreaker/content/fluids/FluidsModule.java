package com.pizzaparker07.spacebreaker.content.fluids;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

/** Process fluids: crude oil, rocket propellants and the gases they are made from. */
public final class FluidsModule implements SbModule {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Spacebreaker.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Spacebreaker.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Spacebreaker.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Spacebreaker.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Spacebreaker.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENT =
            COMPONENTS.registerComponentType("fluid", b -> b.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    public static final TagKey<Fluid> TAG_CRUDE_OIL = SbTagKeys.commonFluid("crude_oil");
    public static final TagKey<Fluid> TAG_RP1 = SbTagKeys.commonFluid("rp1");
    public static final TagKey<Fluid> TAG_LOX = SbTagKeys.commonFluid("liquid_oxygen");
    public static final TagKey<Fluid> TAG_LH2 = SbTagKeys.commonFluid("liquid_hydrogen");
    public static final TagKey<Fluid> TAG_LCH4 = SbTagKeys.commonFluid("liquid_methane");
    public static final TagKey<Fluid> TAG_OXYGEN = SbTagKeys.commonFluid("oxygen");
    public static final TagKey<Fluid> TAG_HYDROGEN = SbTagKeys.commonFluid("hydrogen");
    public static final TagKey<Fluid> TAG_METHANE = SbTagKeys.commonFluid("methane");
    public static final TagKey<Fluid> TAG_CO2 = SbTagKeys.commonFluid("carbon_dioxide");
    public static final TagKey<Fluid> TAG_SULFURIC_ACID = SbTagKeys.commonFluid("sulfuric_acid");
    public static final TagKey<Fluid> TAG_HELIUM_3 = SbTagKeys.commonFluid("helium_3");

    //                                       name, en, tr, kind, tint, kelvin, density, viscosity, tags
    public static final SbFluid CRUDE_OIL = new SbFluid("crude_oil", "Crude Oil", "Ham Petrol", SbFluid.Kind.LIQUID,
            0xF0201812, 300, 870, 6000, List.of(TAG_CRUDE_OIL));
    public static final SbFluid RP1 = new SbFluid("rp1", "RP-1 Kerosene", "RP-1 Kerosen", SbFluid.Kind.LIQUID,
            0xC0D8C27A, 293, 810, 1500, List.of(TAG_RP1));
    public static final SbFluid LIQUID_OXYGEN = new SbFluid("liquid_oxygen", "Liquid Oxygen", "Sıvı Oksijen", SbFluid.Kind.CRYOGENIC,
            0xB07FB8F0, 90, 1141, 200, List.of(TAG_LOX));
    public static final SbFluid LIQUID_HYDROGEN = new SbFluid("liquid_hydrogen", "Liquid Hydrogen", "Sıvı Hidrojen", SbFluid.Kind.CRYOGENIC,
            0x90DCEBF5, 20, 71, 100, List.of(TAG_LH2));
    public static final SbFluid LIQUID_METHANE = new SbFluid("liquid_methane", "Liquid Methane", "Sıvı Metan", SbFluid.Kind.CRYOGENIC,
            0xA0C9D8E6, 112, 422, 150, List.of(TAG_LCH4));
    public static final SbFluid OXYGEN = new SbFluid("oxygen", "Oxygen", "Oksijen", SbFluid.Kind.GAS,
            0x80A9D4F2, 293, -1, 20, List.of(TAG_OXYGEN));
    public static final SbFluid HYDROGEN = new SbFluid("hydrogen", "Hydrogen", "Hidrojen", SbFluid.Kind.GAS,
            0x80F2F2F2, 293, -10, 10, List.of(TAG_HYDROGEN));
    public static final SbFluid METHANE = new SbFluid("methane", "Methane", "Metan", SbFluid.Kind.GAS,
            0x80E6E0C8, 293, -5, 12, List.of(TAG_METHANE));

    public static final SbFluid CARBON_DIOXIDE = new SbFluid("carbon_dioxide", "Carbon Dioxide", "Karbondioksit", SbFluid.Kind.GAS,
            0x80D8D2C8, 293, -2, 15, List.of(TAG_CO2));

    /** Venus cloud droplets; a dense, oily, faintly yellow liquid (no world block: it stays in tanks and pipes). */
    public static final SbFluid SULFURIC_ACID = new SbFluid("sulfuric_acid", "Sulfuric Acid", "Sülfürik Asit", SbFluid.Kind.PROCESS,
            0xC8E8DC8C, 293, 1830, 2400, List.of(TAG_SULFURIC_ACID));
    /** Solar-wind helium implanted in Mercury and Moon regolith. */
    public static final SbFluid HELIUM_3 = new SbFluid("helium_3", "Helium-3", "Helyum-3", SbFluid.Kind.GAS,
            0x80F0E6FF, 293, -12, 8, List.of(TAG_HELIUM_3));

    public static final List<SbFluid> ALL = List.of(CRUDE_OIL, RP1, LIQUID_OXYGEN, LIQUID_HYDROGEN, LIQUID_METHANE, OXYGEN, HYDROGEN, METHANE,
            CARBON_DIOXIDE, SULFURIC_ACID, HELIUM_3);

    public static final DeferredItem<CryogenicCanisterItem> CANISTER = ITEMS.registerItem("cryogenic_canister", CryogenicCanisterItem::new);

    @Override
    public String id() {
        return "fluids";
    }

    @Override
    public void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener(FluidsModule::capabilities);
        SbTabs.MATERIALS.add(CANISTER);
        for (SbFluid f : ALL) {
            if (f.bucket != null) {
                SbTabs.MATERIALS.add(f.bucket);
            }
        }
        for (SbFluid f : ALL) {
            if (f.kind != SbFluid.Kind.LIQUID) {
                SbTabs.MATERIALS.addStack(() -> CryogenicCanisterItem.filled(CANISTER.get(), f.stack(CryogenicCanisterItem.CAPACITY)));
            }
        }
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new FluidHandlerItemStack(FLUID_CONTENT, stack, CryogenicCanisterItem.CAPACITY), CANISTER.get());
    }

    public static SbFluid of(Fluid fluid) {
        for (SbFluid f : ALL) {
            if (f.source.get() == fluid || f.flowing.get() == fluid) {
                return f;
            }
        }
        return null;
    }

    @Override
    public void addTranslations(Lang lang) {
        for (SbFluid f : ALL) {
            lang.add("fluid_type.spacebreaker." + f.name, f.en, f.tr);
            if (f.block != null) {
                lang.block(f.block, f.en, f.tr);
                lang.item(f.bucket, f.en + " Bucket", f.tr + " Kovası");
            }
        }
        lang.item(CANISTER, "Cryogenic Canister", "Kriyojenik Kap");
        lang.add("tooltip.spacebreaker.empty", "Empty", "Boş");
        lang.add("tooltip.spacebreaker.fluid_amount", "%s: %s / %s mB", "%s: %s / %s mB");
    }

    @Override
    public void addTags(SbTags tags) {
        for (SbFluid f : ALL) {
            for (TagKey<Fluid> tag : f.tags) {
                tags.fluid(tag, f.source.get(), f.flowing.get());
            }
            if (f.bucket != null) {
                tags.item(Tags.Items.BUCKETS, f.bucket.get());
            }
        }
    }

    @Override
    public void addRecipes(RecipeOutput out, HolderLookup.Provider registries) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, CANISTER.get())
                .pattern(" I ").pattern("PGP").pattern("PPP")
                .define('I', Items.IRON_INGOT).define('P', com.pizzaparker07.spacebreaker.content.earth.EarthModule.ALUMINUM.plateTag)
                .define('G', com.pizzaparker07.spacebreaker.content.earth.EarthModule.GOLD_FOIL_INSULATION.get())
                .unlockedBy("has_plate", net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(
                        com.pizzaparker07.spacebreaker.content.earth.EarthModule.ALUMINUM_PLATE.get()))
                .save(out);
    }
}
