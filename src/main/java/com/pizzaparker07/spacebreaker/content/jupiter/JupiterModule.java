package com.pizzaparker07.spacebreaker.content.jupiter;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.belt.block.VentBlock;
import com.pizzaparker07.spacebreaker.content.jupiter.dungeon.JupiterStructures;
import com.pizzaparker07.spacebreaker.content.jupiter.entity.JupiterEntities;
import com.pizzaparker07.spacebreaker.content.mars.BlockKit;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbDatapack;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.advancements.Criterion;
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
 * Jupiter's Galilean moons. Europa: a young shell of water ice cracked by long stained ridges and broken into chaos
 * terrain over a salty ocean, under a lethal radiation dose. Io: the most volcanic world known - sulfur plains in
 * yellow, white and red, lava lakes and towering plumes. Ganymede: bright grooved ice cutting through dark ancient crust.
 * Callisto: the oldest, most cratered surface in the Solar System, dark and quiet, far enough out to be a safe base.
 */
public final class JupiterModule implements SbModule {
    public static final BlockKit KIT = new BlockKit();
    public static final ResourceKey<Level> EUROPA = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("europa"));
    public static final ResourceKey<Level> IO = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("io"));
    public static final ResourceKey<Level> GANYMEDE = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("ganymede"));
    public static final ResourceKey<Level> CALLISTO = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("callisto"));

    private static BlockBehaviour.Properties ice(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(0.9F).friction(0.98F).sound(SoundType.GLASS)
                .instrument(NoteBlockInstrument.CHIME);
    }

    private static BlockBehaviour.Properties soil(MapColor color, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(0.5F).sound(sound);
    }

    // ---- Europa
    public static final DeferredBlock<LunarRegolithBlock> EUROPAN_FROST = KIT.block("europan_frost", LunarRegolithBlock::new,
            soil(MapColor.SNOW, SoundType.POWDER_SNOW));
    public static final DeferredBlock<Block> EUROPAN_ICE = KIT.block("europan_ice", ice(MapColor.ICE));
    public static final BlockKit.Family EUROPAN_ICE_BRICKS = KIT.family("europan_ice_bricks", MapColor.ICE, SoundType.GLASS, 1.0F, true);
    public static final DeferredBlock<Block> STAINED_ICE = KIT.block("stained_ice", ice(MapColor.TERRACOTTA_BROWN));
    public static final DeferredBlock<Block> REFROZEN_BRINE = KIT.block("refrozen_brine", ice(MapColor.TERRACOTTA_ORANGE).friction(0.8F));
    public static final DeferredBlock<DropExperienceBlock> SALT_ORE = KIT.block("salt_ore", p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
            BlockKit.rockProps(MapColor.ICE, SoundType.GLASS, 1.4F));
    public static final DeferredItem<Item> OCEAN_SALT = KIT.item("ocean_salt");
    public static final DeferredBlock<VentBlock> ICE_GEYSER = KIT.block("ice_geyser",
            p -> new VentBlock(new VentBlock.Kind(VentBlock.Style.ICE, 0.55, 0F, 600, 220, 34), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(2.5F, 9F).sound(SoundType.GLASS).requiresCorrectToolForDrops());

    // ---- Io
    public static final DeferredBlock<LunarRegolithBlock> SULFUR_SAND = KIT.block("sulfur_sand", LunarRegolithBlock::new,
            soil(MapColor.COLOR_YELLOW, SoundType.SAND));
    public static final DeferredBlock<LunarRegolithBlock> SO2_FROST = KIT.block("so2_frost", LunarRegolithBlock::new,
            soil(MapColor.SNOW, SoundType.POWDER_SNOW));
    public static final DeferredBlock<LunarRegolithBlock> RED_SULFUR = KIT.block("red_sulfur", LunarRegolithBlock::new,
            soil(MapColor.COLOR_ORANGE, SoundType.SAND));
    public static final BlockKit.Family IONIAN_BASALT = KIT.family("ionian_basalt", MapColor.COLOR_BLACK, SoundType.BASALT, 1.8F, true);
    public static final DeferredBlock<DropExperienceBlock> IONIAN_SULFUR_ORE = KIT.block("ionian_sulfur_ore",
            p -> new DropExperienceBlock(UniformInt.of(0, 2), p), BlockKit.rockProps(MapColor.COLOR_BLACK, SoundType.BASALT, 2.2F));
    public static final DeferredBlock<VentBlock> SULFUR_VENT = KIT.block("sulfur_vent",
            p -> new VentBlock(new VentBlock.Kind(VentBlock.Style.SULFUR, 0.35, 2F, 900, 520, 48), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(3F, 9F).sound(SoundType.BASALT).lightLevel(s -> 7)
                    .requiresCorrectToolForDrops());

    // ---- Ganymede
    public static final DeferredBlock<LunarRegolithBlock> GANYMEDE_REGOLITH = KIT.block("ganymede_regolith", LunarRegolithBlock::new,
            soil(MapColor.COLOR_LIGHT_GRAY, SoundType.SAND));
    public static final DeferredBlock<Block> GROOVED_ICE = KIT.block("grooved_ice", ice(MapColor.ICE));
    public static final DeferredBlock<Block> DARK_ICE_CRUST = KIT.rock("dark_ice_crust", MapColor.TERRACOTTA_BROWN, SoundType.TUFF, 1.5F);

    // ---- Callisto
    public static final DeferredBlock<LunarRegolithBlock> CALLISTO_REGOLITH = KIT.block("callisto_regolith", LunarRegolithBlock::new,
            soil(MapColor.TERRACOTTA_BLACK, SoundType.SAND));
    public static final DeferredBlock<LunarRegolithBlock> BRIGHT_FROST = KIT.block("bright_frost", LunarRegolithBlock::new,
            soil(MapColor.SNOW, SoundType.POWDER_SNOW));
    public static final BlockKit.Family CALLISTAN_ROCK = KIT.family("callistan_rock", MapColor.TERRACOTTA_GRAY, SoundType.DEEPSLATE, 1.7F, true);

    public static final TagKey<Block> JOVIAN_ICE = SbTagKeys.modBlock("jovian_ice");
    public static final TagKey<Block> JOVIAN_GROUND = SbTagKeys.modBlock("jovian_ground");
    public static final TagKey<Block> IO_STONE = SbTagKeys.modBlock("io_stone");
    public static final TagKey<Block> ORES_SALT = SbTagKeys.commonBlock("ores/salt");
    public static final TagKey<Item> ITEM_ORES_SALT = SbTagKeys.commonItem("ores/salt");
    public static final TagKey<Block> ORES_SULFUR = SbTagKeys.commonBlock("ores/sulfur");
    public static final TagKey<Item> ITEM_ORES_SULFUR = SbTagKeys.commonItem("ores/sulfur");

    static {
        OuterWorlds.VENT_BLOCKS.add(ICE_GEYSER);
        OuterWorlds.VENT_BLOCKS.add(SULFUR_VENT);
    }

    @Override
    public String id() {
        return "jupiter";
    }

    @Override
    public void register(IEventBus modBus) {
        JupiterEntities.register(modBus, KIT.items);
        JupiterStructures.register(modBus);
        KIT.blocks.register(modBus);
        KIT.items.register(modBus);
        KIT.tab.forEach(SbTabs.PLANETS::add);
        JupiterEntities.eggs().forEach(SbTabs.PLANETS::add);
        JupiterRules.register();
    }

    @Override
    public void addTranslations(Lang lang) {
        for (String[] w : new String[][]{{"jupiter", "Jupiter", "Jüpiter"}, {"io", "Io", "Io"}, {"europa", "Europa", "Europa"},
                {"ganymede", "Ganymede", "Ganymede"}, {"callisto", "Callisto", "Kallisto"}}) {
            lang.add("body.spacebreaker." + w[0], w[1], w[2]);
            if (!w[0].equals("jupiter")) {
                lang.add("dimension.spacebreaker." + w[0], w[1], w[2]);
            }
        }
        lang.block(EUROPAN_FROST, "Europan Frost", "Europa Kırağısı");
        lang.block(EUROPAN_ICE, "Europan Ice", "Europa Buzu");
        lang.block(EUROPAN_ICE_BRICKS.base(), "Europan Ice Bricks", "Europa Buz Tuğlası");
        lang.block(EUROPAN_ICE_BRICKS.stairs(), "Europan Ice Brick Stairs", "Europa Buz Tuğlası Merdiveni");
        lang.block(EUROPAN_ICE_BRICKS.slab(), "Europan Ice Brick Slab", "Europa Buz Tuğlası Basamağı");
        lang.block(EUROPAN_ICE_BRICKS.wall(), "Europan Ice Brick Wall", "Europa Buz Tuğlası Duvarı");
        lang.block(STAINED_ICE, "Stained Ice", "Lekeli Buz");
        lang.block(REFROZEN_BRINE, "Refrozen Brine", "Yeniden Donmuş Tuzlu Su");
        lang.block(SALT_ORE, "Salt Ore", "Tuz Cevheri");
        lang.item(OCEAN_SALT, "Ocean Salt", "Okyanus Tuzu");
        lang.block(ICE_GEYSER, "Ice Geyser", "Buz Gayzeri");
        lang.block(SULFUR_SAND, "Sulfur Sand", "Kükürt Kumu");
        lang.block(SO2_FROST, "Sulfur Dioxide Frost", "Kükürt Dioksit Kırağısı");
        lang.block(RED_SULFUR, "Red Sulfur", "Kırmızı Kükürt");
        BlockKit.familyNames(lang, IONIAN_BASALT, "Ionian Basalt", "Io Bazaltı");
        lang.block(IONIAN_SULFUR_ORE, "Ionian Sulfur Ore", "Io Kükürt Cevheri");
        lang.block(SULFUR_VENT, "Sulfur Plume Vent", "Kükürt Bulutu Bacası");
        lang.block(GANYMEDE_REGOLITH, "Ganymedean Regolith", "Ganymede Regoliti");
        lang.block(GROOVED_ICE, "Grooved Ice", "Oluklu Buz");
        lang.block(DARK_ICE_CRUST, "Dark Ice Crust", "Koyu Buz Kabuğu");
        lang.block(CALLISTO_REGOLITH, "Callistan Regolith", "Kallisto Regoliti");
        lang.block(BRIGHT_FROST, "Bright Frost", "Parlak Kırağı");
        BlockKit.familyNames(lang, CALLISTAN_ROCK, "Callistan Rock", "Kallisto Kayası");
        JupiterData.translations(lang);
        JupiterEntities.translations(lang);
        JupiterRules.translations(lang);
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        for (DeferredBlock<? extends Block> b : java.util.List.of(EUROPAN_FROST, EUROPAN_ICE, STAINED_ICE, REFROZEN_BRINE, ICE_GEYSER, SULFUR_SAND, SO2_FROST,
                RED_SULFUR, SULFUR_VENT, GANYMEDE_REGOLITH, GROOVED_ICE, DARK_ICE_CRUST, CALLISTO_REGOLITH, BRIGHT_FROST)) {
            loot.self(b.get());
        }
        loot.ore(SALT_ORE.get(), OCEAN_SALT.get());
        loot.ore(IONIAN_SULFUR_ORE.get(), com.pizzaparker07.spacebreaker.content.venus.VenusModule.SULFUR.get());
        KIT.familyLoot(loot);
    }

    @Override
    public void addTags(SbTags tags) {
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, EUROPAN_FROST.get(), SULFUR_SAND.get(), SO2_FROST.get(), RED_SULFUR.get(), GANYMEDE_REGOLITH.get(),
                CALLISTO_REGOLITH.get(), BRIGHT_FROST.get());
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, EUROPAN_ICE.get(), STAINED_ICE.get(), REFROZEN_BRINE.get(), SALT_ORE.get(), ICE_GEYSER.get(),
                IONIAN_SULFUR_ORE.get(), SULFUR_VENT.get(), GROOVED_ICE.get(), DARK_ICE_CRUST.get());
        for (BlockKit.Family f : KIT.families()) {
            f.all().forEach(b -> tags.block(BlockTags.MINEABLE_WITH_PICKAXE, b.get()));
            tags.block(BlockTags.STAIRS, f.stairs().get());
            tags.block(BlockTags.SLABS, f.slab().get());
            tags.block(BlockTags.WALLS, f.wall().get());
        }
        tags.block(BlockTags.ICE, EUROPAN_ICE.get(), STAINED_ICE.get(), REFROZEN_BRINE.get(), GROOVED_ICE.get());
        tags.block(BlockTags.NEEDS_STONE_TOOL, SALT_ORE.get(), IONIAN_SULFUR_ORE.get(), ICE_GEYSER.get(), SULFUR_VENT.get());
        tags.block(JOVIAN_ICE, EUROPAN_ICE.get(), STAINED_ICE.get(), REFROZEN_BRINE.get(), GROOVED_ICE.get(), DARK_ICE_CRUST.get(),
                CALLISTAN_ROCK.base().get());
        tags.block(IO_STONE, IONIAN_BASALT.base().get());
        tags.block(JOVIAN_GROUND, EUROPAN_FROST.get(), EUROPAN_ICE.get(), STAINED_ICE.get(), SULFUR_SAND.get(), SO2_FROST.get(), RED_SULFUR.get(),
                IONIAN_BASALT.base().get(), GANYMEDE_REGOLITH.get(), GROOVED_ICE.get(), DARK_ICE_CRUST.get(), CALLISTO_REGOLITH.get(),
                BRIGHT_FROST.get(), CALLISTAN_ROCK.base().get());
        tags.block(ORES_SALT, SALT_ORE.get());
        tags.block(ORES_SULFUR, IONIAN_SULFUR_ORE.get());
        tags.blockTag(Tags.Blocks.ORES, ORES_SALT);
        tags.blockTag(Tags.Blocks.ORES, ORES_SULFUR);
        tags.copy(ORES_SALT, ITEM_ORES_SALT);
        tags.copy(ORES_SULFUR, ITEM_ORES_SULFUR);
        tags.itemTag(Tags.Items.ORES, ITEM_ORES_SALT);
        tags.itemTag(Tags.Items.ORES, ITEM_ORES_SULFUR);
    }

    @Override
    public void addRecipes(RecipeOutput out, HolderLookup.Provider registries) {
        for (BlockKit.Family f : KIT.families()) {
            Block base = f.base().get();
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, f.stairs().get(), 4).pattern("#  ").pattern("## ").pattern("###")
                    .define('#', base).unlockedBy("has_" + path(base), has(base)).save(out);
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, f.slab().get(), 6).pattern("###").define('#', base)
                    .unlockedBy("has_" + path(base), has(base)).save(out);
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, f.wall().get(), 6).pattern("###").pattern("###").define('#', base)
                    .unlockedBy("has_" + path(base), has(base)).save(out);
            stonecut(out, f.stairs().get(), base, 1);
            stonecut(out, f.slab().get(), base, 2);
            stonecut(out, f.wall().get(), base, 1);
        }
        Block ice = EUROPAN_ICE.get();
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, EUROPAN_ICE_BRICKS.base().get(), 4).pattern("##").pattern("##").define('#', ice)
                .unlockedBy("has_europan_ice", has(ice)).save(out);
        stonecut(out, EUROPAN_ICE_BRICKS.base().get(), ice, 1);
        for (ItemLike in : new ItemLike[]{EUROPAN_ICE.get(), GROOVED_ICE.get()}) {
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(in), RecipeCategory.MISC, Items.ICE, 0.1F, 100)
                    .unlockedBy("has_" + path(in), has(in)).save(out, Spacebreaker.id("ice_from_" + path(in)));
        }
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(SALT_ORE.get()), RecipeCategory.MISC, OCEAN_SALT.get(), 0.4F, 200)
                .unlockedBy("has_salt_ore", has(SALT_ORE.get())).save(out, Spacebreaker.id("ocean_salt_from_smelting_salt_ore"));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(IONIAN_SULFUR_ORE.get()), RecipeCategory.MISC,
                        com.pizzaparker07.spacebreaker.content.venus.VenusModule.SULFUR.get(), 0.4F, 200)
                .unlockedBy("has_ionian_sulfur_ore", has(IONIAN_SULFUR_ORE.get())).save(out, Spacebreaker.id("sulfur_from_smelting_ionian_sulfur_ore"));
    }

    static void stonecut(RecipeOutput out, ItemLike result, ItemLike material, int count) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(material), RecipeCategory.BUILDING_BLOCKS, result, count)
                .unlockedBy("has_" + path(material), has(material))
                .save(out, Spacebreaker.id(path(result) + "_from_" + path(material) + "_stonecutting"));
    }

    static Criterion<?> has(ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    static String path(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    @Override
    public void addDatapackEntries(SbDatapack datapack) {
        JupiterData.datapack(datapack);
    }
}
