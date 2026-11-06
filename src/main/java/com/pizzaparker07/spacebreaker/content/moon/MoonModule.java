package com.pizzaparker07.spacebreaker.content.moon;

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

/** The Moon: its rocks, soils, ores (and later its dimension, creatures and dungeons). */
public final class MoonModule implements SbModule {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Spacebreaker.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Spacebreaker.MOD_ID);

    public static final DeferredBlock<LunarRegolithBlock> LUNAR_REGOLITH = BLOCKS.registerBlock("lunar_regolith",
            LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.6F).sound(SoundType.SAND));
    public static final DeferredBlock<Block> MARE_BASALT = BLOCKS.registerSimpleBlock("mare_basalt",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(1.8F, 6.0F).sound(SoundType.BASALT));
    public static final DeferredBlock<DropExperienceBlock> ILMENITE_ORE = BLOCKS.registerBlock("ilmenite_ore",
            p -> new DropExperienceBlock(UniformInt.of(1, 3), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(SoundType.BASALT));

    public static final DeferredItem<BlockItem> LUNAR_REGOLITH_ITEM = ITEMS.registerSimpleBlockItem(LUNAR_REGOLITH);
    public static final DeferredItem<BlockItem> MARE_BASALT_ITEM = ITEMS.registerSimpleBlockItem(MARE_BASALT);
    public static final DeferredItem<BlockItem> ILMENITE_ORE_ITEM = ITEMS.registerSimpleBlockItem(ILMENITE_ORE);

    public static final TagKey<Block> ORES_ILMENITE = SbTagKeys.commonBlock("ores/ilmenite");
    public static final TagKey<Item> ITEM_ORES_ILMENITE = SbTagKeys.commonItem("ores/ilmenite");

    @Override
    public String id() {
        return "moon";
    }

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SbTabs.PLANETS.icon(LUNAR_REGOLITH_ITEM).add(LUNAR_REGOLITH_ITEM).add(MARE_BASALT_ITEM).add(ILMENITE_ORE_ITEM);
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.add("itemGroup.spacebreaker.planets", "Spacebreaker: Worlds", "Spacebreaker: Dünyalar");
        lang.add("body.spacebreaker.moon", "Moon", "Ay");
        lang.block(LUNAR_REGOLITH, "Lunar Regolith", "Ay Regoliti");
        lang.block(MARE_BASALT, "Mare Basalt", "Mare Bazaltı");
        lang.block(ILMENITE_ORE, "Ilmenite Ore", "İlmenit Cevheri");
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        loot.self(LUNAR_REGOLITH.get());
        loot.self(MARE_BASALT.get());
        loot.self(ILMENITE_ORE.get());
    }

    @Override
    public void addTags(SbTags tags) {
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, MARE_BASALT.get(), ILMENITE_ORE.get());
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, LUNAR_REGOLITH.get());
        tags.block(BlockTags.NEEDS_IRON_TOOL, ILMENITE_ORE.get());
        tags.block(ORES_ILMENITE, ILMENITE_ORE.get());
        tags.blockTag(Tags.Blocks.ORES, ORES_ILMENITE);
        tags.copy(ORES_ILMENITE, ITEM_ORES_ILMENITE);
        tags.itemTag(Tags.Items.ORES, ITEM_ORES_ILMENITE);
    }
}
