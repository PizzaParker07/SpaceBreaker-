package com.pizzaparker07.spacebreaker.content.saturn;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.mars.BlockKit;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.saturn.entity.SaturnEntities;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Saturn's moons. Titan: a world under a thick orange nitrogen sky, 1.5 bar and -179 °C, where water ice is the bedrock,
 * hydrocarbon sand piles into long dunes, methane falls as rain and pools in lakes near the poles - and where the thick
 * air and weak gravity let you glide. Enceladus: the brightest body in the Solar System, fresh ice everywhere, with four
 * warm fractures at its south pole spraying its ocean into space.
 */
public final class SaturnModule implements SbModule {
    public static final BlockKit KIT = new BlockKit();
    public static final ResourceKey<Level> TITAN = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("titan"));
    public static final ResourceKey<Level> ENCELADUS = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("enceladus"));

    // ---- Titan
    public static final DeferredBlock<LunarRegolithBlock> THOLIN_SAND = KIT.block("tholin_sand", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(0.5F).sound(SoundType.SAND));
    public static final DeferredBlock<LunarRegolithBlock> THOLIN_MUD = KIT.block("tholin_mud", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.6F).sound(SoundType.MUD));
    public static final BlockKit.Family TITAN_ICE = KIT.family("titan_ice", MapColor.TERRACOTTA_WHITE, SoundType.GLASS, 1.6F, true);
    public static final DeferredBlock<Block> ICE_COBBLES = KIT.block("ice_cobbles", BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE)
            .strength(1.2F).sound(SoundType.GRAVEL).instrument(NoteBlockInstrument.BASEDRUM));
    public static final DeferredBlock<DropExperienceBlock> THOLIN_ORE = KIT.block("tholin_ore", p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
            BlockKit.rockProps(MapColor.TERRACOTTA_WHITE, SoundType.STONE, 2.0F));
    public static final DeferredItem<Item> THOLIN = KIT.item("tholin");
    /** Methane lakes: the fluid of the industry area, held in a block that does not boil away at Titan's -179 °C. */
    public static final DeferredBlock<LiquidBlock> TITAN_METHANE = KIT.blocks.registerBlock("titan_methane",
            p -> new LiquidBlock(FluidsModule.LIQUID_METHANE.source.get(), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).replaceable().noCollission().strength(100F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    // ---- Enceladus
    public static final DeferredBlock<LunarRegolithBlock> ENCELADUS_SNOW = KIT.block("enceladus_snow", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.4F).sound(SoundType.POWDER_SNOW));
    public static final DeferredBlock<Block> ENCELADUS_ICE = KIT.block("enceladus_ice", BlockBehaviour.Properties.of().mapColor(MapColor.ICE)
            .strength(0.9F).friction(0.98F).sound(SoundType.GLASS).instrument(NoteBlockInstrument.CHIME));
    public static final DeferredBlock<Block> SULCUS_ICE = KIT.block("sulcus_ice", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
            .strength(1.0F).friction(0.95F).sound(SoundType.GLASS).instrument(NoteBlockInstrument.CHIME).lightLevel(s -> 3));

    public static final TagKey<Block> TITAN_STONE = SbTagKeys.modBlock("titan_stone");
    public static final TagKey<Block> SATURNIAN_GROUND = SbTagKeys.modBlock("saturnian_ground");
    public static final TagKey<Block> ORES_THOLIN = SbTagKeys.commonBlock("ores/tholin");
    public static final TagKey<Item> ITEM_ORES_THOLIN = SbTagKeys.commonItem("ores/tholin");

    @Override
    public String id() {
        return "saturn";
    }

    @Override
    public void register(IEventBus modBus) {
        SaturnEntities.register(modBus, KIT.items);
        com.pizzaparker07.spacebreaker.content.saturn.dungeon.SaturnStructures.register(modBus);
        KIT.blocks.register(modBus);
        SaturnParticles.register(modBus);
        KIT.items.register(modBus);
        KIT.tab.forEach(SbTabs.PLANETS::add);
        SaturnEntities.eggs().forEach(SbTabs.PLANETS::add);
        TitanAir.register();
    }

    @Override
    public void addTranslations(Lang lang) {
        for (String[] w : new String[][]{{"saturn", "Saturn", "Satürn"}, {"titan", "Titan", "Titan"}, {"enceladus", "Enceladus", "Enceladus"}}) {
            lang.add("body.spacebreaker." + w[0], w[1], w[2]);
            if (!w[0].equals("saturn")) {
                lang.add("dimension.spacebreaker." + w[0], w[1], w[2]);
            }
        }
        lang.block(THOLIN_SAND, "Tholin Sand", "Tolin Kumu");
        lang.block(THOLIN_MUD, "Tholin Mud", "Tolin Çamuru");
        BlockKit.familyNames(lang, TITAN_ICE, "Titan Bedrock Ice", "Titan Ana Kaya Buzu");
        lang.block(ICE_COBBLES, "Ice Cobbles", "Buz Çakılları");
        lang.block(THOLIN_ORE, "Tholin Deposit", "Tolin Yatağı");
        lang.item(THOLIN, "Tholin", "Tolin");
        lang.block(TITAN_METHANE, "Liquid Methane", "Sıvı Metan");
        lang.block(ENCELADUS_SNOW, "Enceladan Snow", "Enceladus Karı");
        lang.block(ENCELADUS_ICE, "Enceladan Ice", "Enceladus Buzu");
        lang.block(SULCUS_ICE, "Tiger Stripe Ice", "Kaplan Çizgisi Buzu");
        SaturnData.translations(lang);
        SaturnEntities.translations(lang);
        TitanAir.translations(lang);
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        for (DeferredBlock<? extends Block> b : java.util.List.of(THOLIN_SAND, THOLIN_MUD, ICE_COBBLES, ENCELADUS_SNOW, ENCELADUS_ICE, SULCUS_ICE)) {
            loot.self(b.get());
        }
        loot.ore(THOLIN_ORE.get(), THOLIN.get());
        KIT.familyLoot(loot);
    }

    @Override
    public void addTags(SbTags tags) {
        // water ice for the ice melter: water, then hydrogen and oxygen for rocket fuel
        tags.item(com.pizzaparker07.spacebreaker.content.machines.IsruRecipes.WATER_ICES, TITAN_ICE.base().get().asItem(), ICE_COBBLES.get().asItem(), ENCELADUS_ICE.get().asItem());
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, THOLIN_SAND.get(), THOLIN_MUD.get(), ICE_COBBLES.get(), ENCELADUS_SNOW.get());
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, THOLIN_ORE.get(), ENCELADUS_ICE.get(), SULCUS_ICE.get());
        for (BlockKit.Family f : KIT.families()) {
            f.all().forEach(b -> tags.block(BlockTags.MINEABLE_WITH_PICKAXE, b.get()));
            tags.block(BlockTags.STAIRS, f.stairs().get());
            tags.block(BlockTags.SLABS, f.slab().get());
            tags.block(BlockTags.WALLS, f.wall().get());
        }
        tags.block(BlockTags.ICE, ENCELADUS_ICE.get(), SULCUS_ICE.get());
        tags.block(BlockTags.NEEDS_STONE_TOOL, THOLIN_ORE.get());
        tags.block(TITAN_STONE, TITAN_ICE.base().get());
        tags.block(SATURNIAN_GROUND, THOLIN_SAND.get(), THOLIN_MUD.get(), TITAN_ICE.base().get(), ICE_COBBLES.get(), ENCELADUS_SNOW.get(),
                ENCELADUS_ICE.get(), SULCUS_ICE.get());
        tags.block(ORES_THOLIN, THOLIN_ORE.get());
        tags.blockTag(Tags.Blocks.ORES, ORES_THOLIN);
        tags.copy(ORES_THOLIN, ITEM_ORES_THOLIN);
        tags.itemTag(Tags.Items.ORES, ITEM_ORES_THOLIN);
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
        // tholin: the organic haze that settled out of Titan's sky; heated it cracks into a black, plastic-like tar
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.BLACK_DYE, 2).pattern("#").define('#', THOLIN.get())
                .unlockedBy("has_tholin", InventoryChangeTrigger.TriggerInstance.hasItems(THOLIN.get())).save(out, Spacebreaker.id("black_dye_from_tholin"));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(THOLIN_ORE.get()), RecipeCategory.MISC, THOLIN.get(), 0.3F, 200)
                .unlockedBy("has_tholin_ore", InventoryChangeTrigger.TriggerInstance.hasItems(THOLIN_ORE.get()))
                .save(out, Spacebreaker.id("tholin_from_smelting_tholin_ore"));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ENCELADUS_ICE.get()), RecipeCategory.MISC, Items.ICE, 0.1F, 100)
                .unlockedBy("has_enceladus_ice", InventoryChangeTrigger.TriggerInstance.hasItems(ENCELADUS_ICE.get()))
                .save(out, Spacebreaker.id("ice_from_enceladus_ice"));
    }

    static String path(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    @Override
    public void addDatapackEntries(SbDatapack datapack) {
        SaturnData.datapack(datapack);
    }
}
