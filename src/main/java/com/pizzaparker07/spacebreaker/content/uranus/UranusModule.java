package com.pizzaparker07.spacebreaker.content.uranus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.BlockKit;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.uranus.entity.UranusEntities;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbDatapack;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.List;

/**
 * Miranda, the smallest of Uranus' round moons and its strangest: a patchwork world, as if broken apart and put back
 * together. Old cratered grey plains; three great racetrack-shaped coronae of bright and dark parallel ridges; and the
 * tallest cliff known, a fault scarp so high that in the feeble gravity a fall from its top takes minutes. Uranus hangs
 * pale cyan in the sky with its thin dark rings.
 */
public final class UranusModule implements SbModule {
    public static final BlockKit KIT = new BlockKit();
    public static final ResourceKey<Level> MIRANDA = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("miranda"));

    public static final DeferredBlock<LunarRegolithBlock> MIRANDAN_REGOLITH = KIT.block("mirandan_regolith", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.5F).sound(SoundType.SAND));
    public static final BlockKit.Family MIRANDAN_ICE = KIT.family("mirandan_ice", MapColor.COLOR_GRAY, SoundType.STONE, 1.6F, true);
    public static final DeferredBlock<Block> BRIGHT_RIDGE_ICE = KIT.rock("bright_ridge_ice", MapColor.SNOW, SoundType.STONE, 1.4F);
    public static final DeferredBlock<Block> DARK_BAND_ICE = KIT.rock("dark_band_ice", MapColor.COLOR_GRAY, SoundType.STONE, 1.4F);
    public static final DeferredBlock<DropExperienceBlock> CARBON_ORE = KIT.block("mirandan_carbon_ore",
            p -> new DropExperienceBlock(UniformInt.of(0, 2), p), BlockKit.rockProps(MapColor.COLOR_GRAY, SoundType.STONE, 2.0F));

    public static final TagKey<Block> MIRANDA_STONE = SbTagKeys.modBlock("miranda_stone");
    public static final TagKey<Block> MIRANDAN_GROUND = SbTagKeys.modBlock("mirandan_ground");

    @Override
    public String id() {
        return "uranus";
    }

    @Override
    public void register(IEventBus modBus) {
        UranusEntities.register(modBus, KIT.items);
        KIT.blocks.register(modBus);
        KIT.items.register(modBus);
        KIT.tab.forEach(SbTabs.PLANETS::add);
        UranusEntities.eggs().forEach(SbTabs.PLANETS::add);
    }

    @Override
    public void addTranslations(Lang lang) {
        // body names of Uranus and Miranda come from the rocketry star map
        lang.add("dimension.spacebreaker.miranda", "Miranda", "Miranda");
        lang.block(MIRANDAN_REGOLITH, "Mirandan Regolith", "Miranda Regoliti");
        BlockKit.familyNames(lang, MIRANDAN_ICE, "Mirandan Bedrock Ice", "Miranda Ana Kaya Buzu");
        lang.block(BRIGHT_RIDGE_ICE, "Bright Ridge Ice", "Parlak Sırt Buzu");
        lang.block(DARK_BAND_ICE, "Dark Band Ice", "Koyu Bant Buzu");
        lang.block(CARBON_ORE, "Mirandan Carbon Deposit", "Miranda Karbon Yatağı");
        UranusData.translations(lang);
        UranusEntities.translations(lang);
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        for (DeferredBlock<? extends Block> b : List.of(MIRANDAN_REGOLITH, BRIGHT_RIDGE_ICE, DARK_BAND_ICE)) {
            loot.self(b.get());
        }
        loot.ore(CARBON_ORE.get(), Items.COAL);
        KIT.familyLoot(loot);
    }

    @Override
    public void addTags(SbTags tags) {
        // water ice for the ice melter: water, then hydrogen and oxygen for rocket fuel
        tags.item(com.pizzaparker07.spacebreaker.content.machines.IsruRecipes.WATER_ICES, MIRANDAN_ICE.base().get().asItem(), BRIGHT_RIDGE_ICE.get().asItem());
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, MIRANDAN_REGOLITH.get());
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, BRIGHT_RIDGE_ICE.get(), DARK_BAND_ICE.get(), CARBON_ORE.get());
        for (BlockKit.Family f : KIT.families()) {
            f.all().forEach(b -> tags.block(BlockTags.MINEABLE_WITH_PICKAXE, b.get()));
            tags.block(BlockTags.STAIRS, f.stairs().get());
            tags.block(BlockTags.SLABS, f.slab().get());
            tags.block(BlockTags.WALLS, f.wall().get());
        }
        tags.block(BlockTags.NEEDS_STONE_TOOL, CARBON_ORE.get());
        tags.block(MIRANDA_STONE, MIRANDAN_ICE.base().get());
        tags.block(MIRANDAN_GROUND, MIRANDAN_REGOLITH.get(), MIRANDAN_ICE.base().get(), BRIGHT_RIDGE_ICE.get(), DARK_BAND_ICE.get());
        tags.block(BlockTags.COAL_ORES, CARBON_ORE.get());
    }

    @Override
    public void addRecipes(RecipeOutput out, HolderLookup.Provider registries) {
        for (BlockKit.Family f : KIT.families()) {
            Block base = f.base().get();
            String has = "has_" + path(base);
            var crit = InventoryChangeTrigger.TriggerInstance.hasItems(base);
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, f.stairs().get(), 4).pattern("#  ").pattern("## ").pattern("###")
                    .define('#', base).unlockedBy(has, crit).save(out);
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, f.slab().get(), 6).pattern("###").define('#', base).unlockedBy(has, crit).save(out);
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, f.wall().get(), 6).pattern("###").pattern("###").define('#', base)
                    .unlockedBy(has, crit).save(out);
            for (ItemLike r : new ItemLike[]{f.stairs().get(), f.wall().get()}) {
                SingleItemRecipeBuilder.stonecutting(Ingredient.of(base), RecipeCategory.BUILDING_BLOCKS, r, 1).unlockedBy(has, crit)
                        .save(out, Spacebreaker.id(path(r) + "_from_" + path(base) + "_stonecutting"));
            }
            SingleItemRecipeBuilder.stonecutting(Ingredient.of(base), RecipeCategory.BUILDING_BLOCKS, f.slab().get(), 2).unlockedBy(has, crit)
                    .save(out, Spacebreaker.id(path(f.slab().get()) + "_from_" + path(base) + "_stonecutting"));
        }
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(CARBON_ORE.get()), RecipeCategory.MISC, Items.COAL, 0.1F, 200)
                .unlockedBy("has_mirandan_carbon_ore", InventoryChangeTrigger.TriggerInstance.hasItems(CARBON_ORE.get()))
                .save(out, Spacebreaker.id("coal_from_smelting_mirandan_carbon_ore"));
    }

    static String path(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    @Override
    public void addDatapackEntries(SbDatapack datapack) {
        UranusData.datapack(datapack);
    }
}
