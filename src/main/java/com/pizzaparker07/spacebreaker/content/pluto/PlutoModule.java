package com.pizzaparker07.spacebreaker.content.pluto;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.BlockKit;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.pluto.entity.PlutoEntities;
import com.pizzaparker07.spacebreaker.content.saturn.SaturnModule;
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
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
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

import java.util.List;

/**
 * Pluto and Charon, a double world 40 AU out. Pluto: a great heart-shaped plain of soft nitrogen ice, slowly overturning
 * in cells; mountains of water ice standing in it like icebergs; dark red tholin lowlands along the equator; bright
 * methane frost on the heights; a thin blue haze in layers above the horizon, and Charon hanging still in the sky.
 * Charon: grey water ice split by a belt of chasms, its north pole stained dark red by gas leaking from Pluto - and
 * Pluto, seven degrees wide, never moving in its sky.
 */
public final class PlutoModule implements SbModule {
    public static final BlockKit KIT = new BlockKit();
    public static final ResourceKey<Level> PLUTO = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("pluto"));
    public static final ResourceKey<Level> CHARON = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("charon"));

    // ---- Pluto
    public static final DeferredBlock<LunarRegolithBlock> PLUTONIAN_FROST = KIT.block("plutonian_frost", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.5F).sound(SoundType.POWDER_SNOW));
    public static final DeferredBlock<LunarRegolithBlock> THOLIN_REGOLITH = KIT.block("tholin_regolith", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).strength(0.6F).sound(SoundType.SAND));
    public static final DeferredBlock<Block> NITROGEN_ICE = KIT.block("nitrogen_ice", BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
            .strength(0.6F).friction(0.99F).sound(SoundType.GLASS).instrument(NoteBlockInstrument.CHIME));
    public static final DeferredBlock<Block> METHANE_FROST = KIT.block("methane_frost", BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
            .strength(0.3F).sound(SoundType.POWDER_SNOW));
    public static final BlockKit.Family PLUTONIAN_ICE = KIT.family("plutonian_ice", MapColor.TERRACOTTA_WHITE, SoundType.STONE, 1.7F, true);
    public static final DeferredBlock<DropExperienceBlock> PLUTONIAN_THOLIN_ORE = KIT.block("plutonian_tholin_ore",
            p -> new DropExperienceBlock(UniformInt.of(0, 2), p), BlockKit.rockProps(MapColor.TERRACOTTA_WHITE, SoundType.STONE, 2.0F));

    // ---- Charon
    public static final DeferredBlock<LunarRegolithBlock> CHARON_REGOLITH = KIT.block("charon_regolith", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.5F).sound(SoundType.SAND));
    public static final DeferredBlock<Block> CHARON_ICE = KIT.rock("charon_ice", MapColor.COLOR_GRAY, SoundType.STONE, 1.6F);
    public static final DeferredBlock<LunarRegolithBlock> RED_POLAR_FROST = KIT.block("red_polar_frost", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).strength(0.4F).sound(SoundType.POWDER_SNOW));
    public static final DeferredBlock<DropExperienceBlock> AMMONIA_HYDRATE_ORE = KIT.block("ammonia_hydrate_ore",
            p -> new DropExperienceBlock(UniformInt.of(1, 3), p), BlockKit.rockProps(MapColor.COLOR_GRAY, SoundType.STONE, 2.2F));
    public static final DeferredItem<Item> AMMONIA_CRYSTAL = KIT.item("ammonia_crystal");

    public static final TagKey<Block> PLUTO_STONE = SbTagKeys.modBlock("pluto_stone");
    public static final TagKey<Block> CHARON_STONE = SbTagKeys.modBlock("charon_stone");
    public static final TagKey<Block> PLUTONIAN_GROUND = SbTagKeys.modBlock("plutonian_ground");
    public static final TagKey<Block> ORES_AMMONIA = SbTagKeys.commonBlock("ores/ammonia");
    public static final TagKey<Item> ITEM_ORES_AMMONIA = SbTagKeys.commonItem("ores/ammonia");

    @Override
    public String id() {
        return "pluto";
    }

    @Override
    public void register(IEventBus modBus) {
        PlutoEntities.register(modBus, KIT.items);
        KIT.blocks.register(modBus);
        KIT.items.register(modBus);
        KIT.tab.forEach(SbTabs.PLANETS::add);
        PlutoEntities.eggs().forEach(SbTabs.PLANETS::add);
    }

    @Override
    public void addTranslations(Lang lang) {
        for (String[] w : new String[][]{{"pluto", "Pluto", "Plüton"}, {"charon", "Charon", "Charon"}}) {
            lang.add("body.spacebreaker." + w[0], w[1], w[2]);
            lang.add("dimension.spacebreaker." + w[0], w[1], w[2]);
        }
        lang.block(PLUTONIAN_FROST, "Plutonian Frost", "Plüton Kırağısı");
        lang.block(THOLIN_REGOLITH, "Red Tholin Regolith", "Kızıl Tolin Regoliti");
        lang.block(NITROGEN_ICE, "Nitrogen Ice", "Azot Buzu");
        lang.block(METHANE_FROST, "Methane Frost", "Metan Kırağısı");
        BlockKit.familyNames(lang, PLUTONIAN_ICE, "Plutonian Bedrock Ice", "Plüton Ana Kaya Buzu");
        lang.block(PLUTONIAN_THOLIN_ORE, "Plutonian Tholin Deposit", "Plüton Tolin Yatağı");
        lang.block(CHARON_REGOLITH, "Charon Regolith", "Charon Regoliti");
        lang.block(CHARON_ICE, "Charon Ice", "Charon Buzu");
        lang.block(RED_POLAR_FROST, "Red Polar Frost", "Kızıl Kutup Kırağısı");
        lang.block(AMMONIA_HYDRATE_ORE, "Ammonia Hydrate Deposit", "Amonyak Hidrat Yatağı");
        lang.item(AMMONIA_CRYSTAL, "Ammonia Crystal", "Amonyak Kristali");
        PlutoData.translations(lang);
        PlutoEntities.translations(lang);
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        for (DeferredBlock<? extends Block> b : List.of(PLUTONIAN_FROST, THOLIN_REGOLITH, NITROGEN_ICE, METHANE_FROST, CHARON_REGOLITH, CHARON_ICE,
                RED_POLAR_FROST)) {
            loot.self(b.get());
        }
        loot.ore(PLUTONIAN_THOLIN_ORE.get(), SaturnModule.THOLIN.get());
        loot.ore(AMMONIA_HYDRATE_ORE.get(), AMMONIA_CRYSTAL.get());
        KIT.familyLoot(loot);
    }

    @Override
    public void addTags(SbTags tags) {
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, PLUTONIAN_FROST.get(), THOLIN_REGOLITH.get(), METHANE_FROST.get(), CHARON_REGOLITH.get(),
                RED_POLAR_FROST.get());
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, NITROGEN_ICE.get(), PLUTONIAN_THOLIN_ORE.get(), CHARON_ICE.get(), AMMONIA_HYDRATE_ORE.get());
        for (BlockKit.Family f : KIT.families()) {
            f.all().forEach(b -> tags.block(BlockTags.MINEABLE_WITH_PICKAXE, b.get()));
            tags.block(BlockTags.STAIRS, f.stairs().get());
            tags.block(BlockTags.SLABS, f.slab().get());
            tags.block(BlockTags.WALLS, f.wall().get());
        }
        tags.block(BlockTags.ICE, NITROGEN_ICE.get());
        tags.block(BlockTags.NEEDS_STONE_TOOL, PLUTONIAN_THOLIN_ORE.get(), AMMONIA_HYDRATE_ORE.get());
        tags.block(PLUTO_STONE, PLUTONIAN_ICE.base().get());
        tags.block(CHARON_STONE, CHARON_ICE.get());
        tags.block(PLUTONIAN_GROUND, PLUTONIAN_FROST.get(), THOLIN_REGOLITH.get(), NITROGEN_ICE.get(), METHANE_FROST.get(), PLUTONIAN_ICE.base().get(),
                CHARON_REGOLITH.get(), CHARON_ICE.get(), RED_POLAR_FROST.get());
        tags.block(SaturnModule.ORES_THOLIN, PLUTONIAN_THOLIN_ORE.get());
        tags.block(ORES_AMMONIA, AMMONIA_HYDRATE_ORE.get());
        tags.blockTag(Tags.Blocks.ORES, ORES_AMMONIA);
        tags.copy(ORES_AMMONIA, ITEM_ORES_AMMONIA);
        tags.itemTag(Tags.Items.ORES, ITEM_ORES_AMMONIA);
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
        var hasAmmonia = InventoryChangeTrigger.TriggerInstance.hasItems(AMMONIA_CRYSTAL.get());
        // ammonia: a nitrogen fertiliser - two crystals make bone meal's worth of plant food
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.BONE_MEAL, 3).requires(AMMONIA_CRYSTAL.get()).requires(AMMONIA_CRYSTAL.get())
                .unlockedBy("has_ammonia_crystal", hasAmmonia).save(out, Spacebreaker.id("bone_meal_from_ammonia_crystal"));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(AMMONIA_HYDRATE_ORE.get()), RecipeCategory.MISC, AMMONIA_CRYSTAL.get(), 0.4F, 200)
                .unlockedBy("has_ammonia_hydrate_ore", InventoryChangeTrigger.TriggerInstance.hasItems(AMMONIA_HYDRATE_ORE.get()))
                .save(out, Spacebreaker.id("ammonia_crystal_from_smelting_ammonia_hydrate_ore"));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(PLUTONIAN_THOLIN_ORE.get()), RecipeCategory.MISC, SaturnModule.THOLIN.get(), 0.3F, 200)
                .unlockedBy("has_plutonian_tholin_ore", InventoryChangeTrigger.TriggerInstance.hasItems(PLUTONIAN_THOLIN_ORE.get()))
                .save(out, Spacebreaker.id("tholin_from_smelting_plutonian_tholin_ore"));
    }

    static String path(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    @Override
    public void addDatapackEntries(SbDatapack datapack) {
        PlutoData.datapack(datapack);
    }
}
