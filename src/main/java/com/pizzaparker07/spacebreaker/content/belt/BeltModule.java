package com.pizzaparker07.spacebreaker.content.belt;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.dungeon.BeltStructures;
import com.pizzaparker07.spacebreaker.content.belt.entity.BeltEntities;
import com.pizzaparker07.spacebreaker.content.mars.BlockKit;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbDatapack;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The asteroid belt: Ceres, a dark dwarf planet of hydrated clays and carbon with bright salt faculae on its crater
 * floors and water ice a few blocks down, and Vesta, a battered protoplanet of basalt (eucrite) over a deep orthopyroxene
 * mantle (diogenite) exposed in the giant Rheasilvia basin at its south pole.
 */
public final class BeltModule implements SbModule {
    public static final BlockKit KIT = new BlockKit();
    public static final ResourceKey<Level> CERES = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("ceres"));
    public static final ResourceKey<Level> VESTA = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("vesta"));

    // ---- Ceres
    public static final DeferredBlock<LunarRegolithBlock> CERES_REGOLITH = KIT.block("ceres_regolith", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.6F).sound(SoundType.SAND));
    public static final DeferredBlock<LunarRegolithBlock> AMMONIATED_CLAY = KIT.block("ammoniated_clay", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).strength(0.7F).sound(SoundType.MUD));
    public static final DeferredBlock<LunarRegolithBlock> CARBONATE_CRUST = KIT.block("carbonate_crust", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.6F).sound(SoundType.CALCITE));
    public static final BlockKit.Family CEREAN_ROCK = KIT.family("cerean_rock", MapColor.DEEPSLATE, SoundType.TUFF, 1.5F, true);
    public static final DeferredBlock<Block> CEREAN_ICE = KIT.block("cerean_ice", BlockBehaviour.Properties.of().mapColor(MapColor.ICE)
            .strength(0.8F).friction(0.98F).sound(SoundType.GLASS).instrument(NoteBlockInstrument.CHIME));
    public static final DeferredBlock<DropExperienceBlock> MAGNETITE_ORE = KIT.block("magnetite_ore", p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
            BlockKit.rockProps(MapColor.DEEPSLATE, SoundType.TUFF, 2.6F));
    public static final DeferredItem<Item> RAW_MAGNETITE = KIT.item("raw_magnetite");

    // ---- Vesta
    public static final DeferredBlock<LunarRegolithBlock> VESTAN_REGOLITH = KIT.block("vestan_regolith", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.6F).sound(SoundType.SAND));
    public static final BlockKit.Family EUCRITE = KIT.family("eucrite", MapColor.STONE, SoundType.STONE, 1.6F, true);
    public static final BlockKit.Family EUCRITE_BRICKS = KIT.family("eucrite_bricks", MapColor.STONE, SoundType.STONE, 1.6F, true);
    public static final DeferredBlock<Block> DIOGENITE = KIT.rock("diogenite", MapColor.TERRACOTTA_YELLOW, SoundType.STONE, 1.8F);
    public static final DeferredBlock<DropExperienceBlock> CHROMITE_ORE = KIT.block("chromite_ore", p -> new DropExperienceBlock(UniformInt.of(1, 3), p),
            BlockKit.rockProps(MapColor.TERRACOTTA_YELLOW, SoundType.STONE, 3.0F));
    public static final DeferredItem<Item> CHROMITE = KIT.item("chromite");

    public static final TagKey<Block> BELT_STONE = SbTagKeys.modBlock("belt_stone");
    public static final TagKey<Block> CERES_GROUND = SbTagKeys.modBlock("ceres_ground");
    public static final TagKey<Block> ORES_MAGNETITE = SbTagKeys.commonBlock("ores/magnetite");
    public static final TagKey<Block> ORES_CHROMITE = SbTagKeys.commonBlock("ores/chromite");
    public static final TagKey<Item> ITEM_ORES_MAGNETITE = SbTagKeys.commonItem("ores/magnetite");
    public static final TagKey<Item> ITEM_ORES_CHROMITE = SbTagKeys.commonItem("ores/chromite");
    public static final TagKey<Item> RAW_MATERIALS_MAGNETITE = SbTagKeys.commonItem("raw_materials/magnetite");

    @Override
    public String id() {
        return "belt";
    }

    @Override
    public void register(IEventBus modBus) {
        BeltEntities.register(modBus, KIT.items);
        BeltStructures.register(modBus);
        KIT.blocks.register(modBus);
        KIT.items.register(modBus);
        KIT.tab.forEach(SbTabs.PLANETS::add);
        BeltEntities.eggs().forEach(SbTabs.PLANETS::add);
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.add("body.spacebreaker.ceres", "Ceres", "Ceres");
        lang.add("body.spacebreaker.vesta", "Vesta", "Vesta");
        lang.add("dimension.spacebreaker.ceres", "Ceres", "Ceres");
        lang.add("dimension.spacebreaker.vesta", "Vesta", "Vesta");
        lang.add("sample.spacebreaker.ceres", "Ceres", "Ceres");
        lang.add("sample.spacebreaker.vesta", "Vesta", "Vesta");
        lang.block(CERES_REGOLITH, "Cerean Regolith", "Ceres Regoliti");
        lang.block(AMMONIATED_CLAY, "Ammoniated Clay", "Amonyaklı Kil");
        lang.block(CARBONATE_CRUST, "Carbonate Salt Crust", "Karbonat Tuz Kabuğu");
        BlockKit.familyNames(lang, CEREAN_ROCK, "Cerean Rock", "Ceres Kayası");
        lang.block(CEREAN_ICE, "Cerean Ice", "Ceres Buzu");
        lang.block(MAGNETITE_ORE, "Magnetite Ore", "Manyetit Cevheri");
        lang.item(RAW_MAGNETITE, "Raw Magnetite", "Ham Manyetit");
        lang.block(VESTAN_REGOLITH, "Vestan Regolith", "Vesta Regoliti");
        BlockKit.familyNames(lang, EUCRITE, "Eucrite", "Ökrit");
        BlockKit.familyNames(lang, EUCRITE_BRICKS, "Eucrite Brick", "Ökrit Tuğla");
        lang.block(DIOGENITE, "Diogenite", "Diyojenit");
        lang.block(CHROMITE_ORE, "Chromite Ore", "Kromit Cevheri");
        lang.item(CHROMITE, "Chromite", "Kromit");
        BeltData.translations(lang);
        BeltEntities.translations(lang);
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        loot.self(CERES_REGOLITH.get());
        loot.self(AMMONIATED_CLAY.get());
        loot.self(CARBONATE_CRUST.get());
        loot.self(CEREAN_ICE.get());
        loot.ore(MAGNETITE_ORE.get(), RAW_MAGNETITE.get());
        loot.self(VESTAN_REGOLITH.get());
        loot.self(DIOGENITE.get());
        loot.ore(CHROMITE_ORE.get(), CHROMITE.get());
        KIT.familyLoot(loot);
    }

    @Override
    public void addTags(SbTags tags) {
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, CERES_REGOLITH.get(), AMMONIATED_CLAY.get(), CARBONATE_CRUST.get(), VESTAN_REGOLITH.get());
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, CEREAN_ICE.get(), MAGNETITE_ORE.get(), DIOGENITE.get(), CHROMITE_ORE.get());
        for (BlockKit.Family f : KIT.families()) {
            f.all().forEach(b -> tags.block(BlockTags.MINEABLE_WITH_PICKAXE, b.get()));
            tags.block(BlockTags.STAIRS, f.stairs().get());
            tags.block(BlockTags.SLABS, f.slab().get());
            if (f.wall() != null) {
                tags.block(BlockTags.WALLS, f.wall().get());
            }
        }
        tags.block(BlockTags.ICE, CEREAN_ICE.get());
        tags.block(BlockTags.NEEDS_STONE_TOOL, MAGNETITE_ORE.get());
        tags.block(BlockTags.NEEDS_IRON_TOOL, CHROMITE_ORE.get());
        tags.block(BELT_STONE, CEREAN_ROCK.base().get(), EUCRITE.base().get(), DIOGENITE.get());
        tags.block(CERES_GROUND, CERES_REGOLITH.get(), AMMONIATED_CLAY.get(), CARBONATE_CRUST.get(), CEREAN_ROCK.base().get(), VESTAN_REGOLITH.get(),
                EUCRITE.base().get(), DIOGENITE.get());
        tags.block(ORES_MAGNETITE, MAGNETITE_ORE.get());
        tags.block(ORES_CHROMITE, CHROMITE_ORE.get());
        tags.blockTag(Tags.Blocks.ORES, ORES_MAGNETITE);
        tags.blockTag(Tags.Blocks.ORES, ORES_CHROMITE);
        tags.copy(ORES_MAGNETITE, ITEM_ORES_MAGNETITE);
        tags.copy(ORES_CHROMITE, ITEM_ORES_CHROMITE);
        tags.itemTag(Tags.Items.ORES, ITEM_ORES_MAGNETITE);
        tags.itemTag(Tags.Items.ORES, ITEM_ORES_CHROMITE);
        tags.item(RAW_MATERIALS_MAGNETITE, RAW_MAGNETITE.get());
        tags.itemTag(Tags.Items.RAW_MATERIALS, RAW_MATERIALS_MAGNETITE);
    }

    @Override
    public void addRecipes(RecipeOutput out, HolderLookup.Provider registries) {
        for (BlockKit.Family f : KIT.families()) {
            Block base = f.base().get();
            stairs(out, f.stairs().get(), base);
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, f.slab().get(), 6).pattern("###").define('#', base)
                    .unlockedBy("has_" + path(base), has(base)).save(out);
            stonecut(out, f.stairs().get(), base, 1);
            stonecut(out, f.slab().get(), base, 2);
            if (f.wall() != null) {
                ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, f.wall().get(), 6).pattern("###").pattern("###").define('#', base)
                        .unlockedBy("has_" + path(base), has(base)).save(out);
                stonecut(out, f.wall().get(), base, 1);
            }
        }
        Block eucrite = EUCRITE.base().get();
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, EUCRITE_BRICKS.base().get(), 4).pattern("##").pattern("##").define('#', eucrite)
                .unlockedBy("has_eucrite", has(eucrite)).save(out);
        stonecut(out, EUCRITE_BRICKS.base().get(), eucrite, 1);
        stonecut(out, EUCRITE_BRICKS.stairs().get(), eucrite, 1);
        stonecut(out, EUCRITE_BRICKS.slab().get(), eucrite, 2);
        stonecut(out, EUCRITE_BRICKS.wall().get(), eucrite, 1);
        for (String kind : new String[]{"smelting", "blasting"}) {
            boolean blast = kind.equals("blasting");
            for (ItemLike in : new ItemLike[]{RAW_MAGNETITE.get(), MAGNETITE_ORE.get()}) {
                (blast ? SimpleCookingRecipeBuilder.blasting(Ingredient.of(in), RecipeCategory.MISC, Items.IRON_INGOT, 0.7F, 100)
                        : SimpleCookingRecipeBuilder.smelting(Ingredient.of(in), RecipeCategory.MISC, Items.IRON_INGOT, 0.7F, 200))
                        .unlockedBy("has_" + path(in), has(in))
                        .save(out, Spacebreaker.id("iron_ingot_from_" + kind + "_" + path(in)));
            }
        }
        // the bright salt crust dries into a white powder: sodium carbonate works as bone meal for greenhouse soils
        net.minecraft.data.recipes.ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.BONE_MEAL, 2).requires(CARBONATE_CRUST.get())
                .unlockedBy("has_carbonate_crust", has(CARBONATE_CRUST.get())).save(out, Spacebreaker.id("bone_meal_from_carbonate_crust"));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(CEREAN_ICE.get()), RecipeCategory.MISC, Items.ICE, 0.1F, 100)
                .unlockedBy("has_cerean_ice", has(CEREAN_ICE.get())).save(out, Spacebreaker.id("ice_from_cerean_ice"));
    }

    private static void stairs(RecipeOutput out, Block stairs, Block base) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, stairs, 4).pattern("#  ").pattern("## ").pattern("###").define('#', base)
                .unlockedBy("has_" + path(base), has(base)).save(out);
    }

    static void stonecut(RecipeOutput out, ItemLike result, ItemLike material, int count) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(material), RecipeCategory.BUILDING_BLOCKS, result, count)
                .unlockedBy("has_" + path(material), has(material))
                .save(out, Spacebreaker.id(path(result) + "_from_" + path(material) + "_stonecutting"));
    }

    static net.minecraft.advancements.Criterion<?> has(ItemLike item) {
        return net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    static String path(ItemLike item) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    @Override
    public void addDatapackEntries(SbDatapack datapack) {
        BeltData.datapack(datapack);
    }
}
