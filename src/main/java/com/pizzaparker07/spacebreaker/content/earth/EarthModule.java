package com.pizzaparker07.spacebreaker.content.earth;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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
import net.neoforged.neoforge.registries.DeferredRegister;

/** Earth ores and the metals refined from them. */
public final class EarthModule implements SbModule {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Spacebreaker.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Spacebreaker.MOD_ID);

    public static final DeferredBlock<DropExperienceBlock> BAUXITE_ORE = BLOCKS.registerBlock("bauxite_ore",
            p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(3.0F, 3.0F));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_BAUXITE_ORE = BLOCKS.registerBlock("deepslate_bauxite_ore",
            p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> RAW_BAUXITE_BLOCK = BLOCKS.registerSimpleBlock("raw_bauxite_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F));

    public static final DeferredItem<Item> RAW_BAUXITE = ITEMS.registerSimpleItem("raw_bauxite");
    public static final DeferredItem<Item> ALUMINUM_INGOT = ITEMS.registerSimpleItem("aluminum_ingot");
    public static final DeferredItem<Item> ALUMINUM_PLATE = ITEMS.registerSimpleItem("aluminum_plate");
    public static final DeferredItem<BlockItem> BAUXITE_ORE_ITEM = ITEMS.registerSimpleBlockItem(BAUXITE_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_BAUXITE_ORE_ITEM = ITEMS.registerSimpleBlockItem(DEEPSLATE_BAUXITE_ORE);
    public static final DeferredItem<BlockItem> RAW_BAUXITE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(RAW_BAUXITE_BLOCK);

    public static final TagKey<net.minecraft.world.level.block.Block> ORES_BAUXITE = SbTagKeys.commonBlock("ores/bauxite");
    public static final TagKey<Item> ITEM_ORES_BAUXITE = SbTagKeys.commonItem("ores/bauxite");
    public static final TagKey<Item> RAW_MATERIALS_BAUXITE = SbTagKeys.commonItem("raw_materials/bauxite");
    public static final TagKey<Item> INGOTS_ALUMINUM = SbTagKeys.commonItem("ingots/aluminum");
    public static final TagKey<Item> PLATES_ALUMINUM = SbTagKeys.commonItem("plates/aluminum");

    @Override
    public String id() {
        return "earth";
    }

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SbTabs.MATERIALS.icon(ALUMINUM_INGOT)
                .add(BAUXITE_ORE_ITEM).add(DEEPSLATE_BAUXITE_ORE_ITEM).add(RAW_BAUXITE).add(RAW_BAUXITE_BLOCK_ITEM)
                .add(ALUMINUM_INGOT).add(ALUMINUM_PLATE);
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.add("itemGroup.spacebreaker.materials", "Spacebreaker: Materials", "Spacebreaker: Malzemeler");
        lang.block(BAUXITE_ORE, "Bauxite Ore", "Boksit Cevheri");
        lang.block(DEEPSLATE_BAUXITE_ORE, "Deepslate Bauxite Ore", "Derin Kayrak Boksit Cevheri");
        lang.block(RAW_BAUXITE_BLOCK, "Block of Raw Bauxite", "Ham Boksit Bloğu");
        lang.item(RAW_BAUXITE, "Raw Bauxite", "Ham Boksit");
        lang.item(ALUMINUM_INGOT, "Aluminum Ingot", "Alüminyum Külçe");
        lang.item(ALUMINUM_PLATE, "Aluminum Plate", "Alüminyum Levha");
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        loot.ore(BAUXITE_ORE.get(), RAW_BAUXITE.get());
        loot.ore(DEEPSLATE_BAUXITE_ORE.get(), RAW_BAUXITE.get());
        loot.self(RAW_BAUXITE_BLOCK.get());
    }

    @Override
    public void addTags(SbTags tags) {
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, BAUXITE_ORE.get(), DEEPSLATE_BAUXITE_ORE.get(), RAW_BAUXITE_BLOCK.get());
        tags.block(BlockTags.NEEDS_STONE_TOOL, BAUXITE_ORE.get(), DEEPSLATE_BAUXITE_ORE.get(), RAW_BAUXITE_BLOCK.get());
        tags.block(ORES_BAUXITE, BAUXITE_ORE.get(), DEEPSLATE_BAUXITE_ORE.get());
        tags.blockTag(Tags.Blocks.ORES, ORES_BAUXITE);
        tags.copy(ORES_BAUXITE, ITEM_ORES_BAUXITE);
        tags.itemTag(Tags.Items.ORES, ITEM_ORES_BAUXITE);
        tags.item(RAW_MATERIALS_BAUXITE, RAW_BAUXITE.get());
        tags.itemTag(Tags.Items.RAW_MATERIALS, RAW_MATERIALS_BAUXITE);
        tags.item(INGOTS_ALUMINUM, ALUMINUM_INGOT.get());
        tags.itemTag(Tags.Items.INGOTS, INGOTS_ALUMINUM);
        tags.item(PLATES_ALUMINUM, ALUMINUM_PLATE.get());
    }
}
