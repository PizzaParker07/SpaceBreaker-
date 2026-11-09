package com.pizzaparker07.spacebreaker.content.neptune;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.belt.block.VentBlock;
import com.pizzaparker07.spacebreaker.content.mars.BlockKit;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.neptune.entity.NeptuneEntities;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Triton, Neptune's big moon - a captured dwarf planet going round backwards. The coldest surface measured in the
 * Solar System (-235 °C): grey-green "cantaloupe" ground of dimpled cells, a pink polar cap of nitrogen frost, and in the
 * cap, geysers throwing dark columns of nitrogen and dust kilometres up, where the thin wind bends them into long
 * streaks. Neptune hangs blue in the black sky.
 */
public final class NeptuneModule implements SbModule {
    public static final BlockKit KIT = new BlockKit();
    public static final ResourceKey<Level> TRITON = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("triton"));

    public static final DeferredBlock<LunarRegolithBlock> TRITON_FROST = KIT.block("triton_frost", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).strength(0.5F).sound(SoundType.POWDER_SNOW));
    public static final DeferredBlock<LunarRegolithBlock> PINK_NITROGEN_FROST = KIT.block("pink_nitrogen_frost", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_PINK).strength(0.4F).sound(SoundType.POWDER_SNOW));
    public static final DeferredBlock<LunarRegolithBlock> PLUME_DUST = KIT.block("plume_dust", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.4F).sound(SoundType.SAND));
    public static final BlockKit.Family TRITON_ICE = KIT.family("triton_ice", MapColor.TERRACOTTA_LIGHT_GREEN, SoundType.GLASS, 1.6F, true);
    public static final DeferredBlock<VentBlock> NITROGEN_GEYSER = KIT.block("nitrogen_geyser",
            p -> new VentBlock(new VentBlock.Kind(VentBlock.Style.NITROGEN, 0.45, 0F, 700, 260, 40), () -> NeptuneModule.PLUME_DUST.get(), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.5F, 9F).sound(SoundType.GLASS).requiresCorrectToolForDrops());
    public static final DeferredBlock<DropExperienceBlock> CLATHRATE_ORE = KIT.block("methane_clathrate_ore",
            p -> new DropExperienceBlock(UniformInt.of(0, 2), p), BlockKit.rockProps(MapColor.TERRACOTTA_LIGHT_GREEN, SoundType.STONE, 2.0F));
    /** Methane locked in a cage of water ice: burns in a furnace as long as coal. */
    public static final DeferredItem<Item> METHANE_CLATHRATE = KIT.item("methane_clathrate", () -> new Item(new Item.Properties()) {
        @Override
        public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
            return 1600;
        }
    });

    public static final TagKey<Block> TRITON_STONE = SbTagKeys.modBlock("triton_stone");
    public static final TagKey<Block> TRITON_GROUND = SbTagKeys.modBlock("triton_ground");
    public static final TagKey<Block> ORES_CLATHRATE = SbTagKeys.commonBlock("ores/methane_clathrate");
    public static final TagKey<Item> ITEM_ORES_CLATHRATE = SbTagKeys.commonItem("ores/methane_clathrate");

    @Override
    public String id() {
        return "neptune";
    }

    @Override
    public void register(IEventBus modBus) {
        Proteus.load();
        Nereid.load();
        NeptuneEntities.register(modBus, KIT.items);
        KIT.blocks.register(modBus);
        KIT.items.register(modBus);
        KIT.tab.forEach(SbTabs.PLANETS::add);
        NeptuneEntities.eggs().forEach(SbTabs.PLANETS::add);
        OuterWorlds.VENT_BLOCKS.add(NITROGEN_GEYSER);
        com.pizzaparker07.spacebreaker.content.neptune.dungeon.NeptuneStructures.register(modBus);
    }

    @Override
    public void addTranslations(Lang lang) {
        Proteus.translations(lang);
        Nereid.translations(lang);
        lang.add("body.spacebreaker.neptune", "Neptune", "Neptün");
        lang.add("body.spacebreaker.triton", "Triton", "Triton");
        lang.add("dimension.spacebreaker.triton", "Triton", "Triton");
        lang.block(TRITON_FROST, "Tritonian Frost", "Triton Kırağısı");
        lang.block(PINK_NITROGEN_FROST, "Pink Nitrogen Frost", "Pembe Azot Kırağısı");
        lang.block(PLUME_DUST, "Plume Dust", "Gayzer Tozu");
        BlockKit.familyNames(lang, TRITON_ICE, "Tritonian Bedrock Ice", "Triton Ana Kaya Buzu");
        lang.block(NITROGEN_GEYSER, "Nitrogen Geyser", "Azot Gayzeri");
        lang.block(CLATHRATE_ORE, "Methane Clathrate Deposit", "Metan Klatrat Yatağı");
        lang.item(METHANE_CLATHRATE, "Methane Clathrate", "Metan Klatrat");
        NeptuneData.translations(lang);
        NeptuneEntities.translations(lang);
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        Proteus.loot(loot);
        Nereid.loot(loot);
        for (DeferredBlock<? extends Block> b : List.of(TRITON_FROST, PINK_NITROGEN_FROST, PLUME_DUST, NITROGEN_GEYSER)) {
            loot.self(b.get());
        }
        loot.ore(CLATHRATE_ORE.get(), METHANE_CLATHRATE.get());
        KIT.familyLoot(loot);
    }

    @Override
    public void addTags(SbTags tags) {
        Proteus.tags(tags);
        Nereid.tags(tags);
        // water ice for the ice melter: water, then hydrogen and oxygen for rocket fuel
        tags.item(com.pizzaparker07.spacebreaker.content.machines.IsruRecipes.WATER_ICES, TRITON_ICE.base().get().asItem());
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, TRITON_FROST.get(), PINK_NITROGEN_FROST.get(), PLUME_DUST.get());
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, NITROGEN_GEYSER.get(), CLATHRATE_ORE.get());
        for (BlockKit.Family f : KIT.families()) {
            f.all().forEach(b -> tags.block(BlockTags.MINEABLE_WITH_PICKAXE, b.get()));
            tags.block(BlockTags.STAIRS, f.stairs().get());
            tags.block(BlockTags.SLABS, f.slab().get());
            tags.block(BlockTags.WALLS, f.wall().get());
        }
        tags.block(BlockTags.NEEDS_STONE_TOOL, NITROGEN_GEYSER.get(), CLATHRATE_ORE.get());
        tags.block(TRITON_STONE, TRITON_ICE.base().get());
        tags.block(TRITON_GROUND, TRITON_FROST.get(), PINK_NITROGEN_FROST.get(), PLUME_DUST.get(), TRITON_ICE.base().get());
        tags.block(ORES_CLATHRATE, CLATHRATE_ORE.get());
        tags.blockTag(Tags.Blocks.ORES, ORES_CLATHRATE);
        tags.copy(ORES_CLATHRATE, ITEM_ORES_CLATHRATE);
        tags.itemTag(Tags.Items.ORES, ITEM_ORES_CLATHRATE);
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
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(CLATHRATE_ORE.get()), RecipeCategory.MISC, METHANE_CLATHRATE.get(), 0.3F, 200)
                .unlockedBy("has_methane_clathrate_ore", InventoryChangeTrigger.TriggerInstance.hasItems(CLATHRATE_ORE.get()))
                .save(out, Spacebreaker.id("methane_clathrate_from_smelting_methane_clathrate_ore"));
    }

    static String path(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    @Override
    public void addDatapackEntries(SbDatapack datapack) {
        NeptuneData.datapack(datapack);
    }
}
