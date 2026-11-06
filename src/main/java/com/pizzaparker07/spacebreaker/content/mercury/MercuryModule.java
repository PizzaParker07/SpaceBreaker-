package com.pizzaparker07.spacebreaker.content.mercury;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.BlockKit;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
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
 * Mercury: the innermost world. A huge sun, a black sky, days that scorch the ground to 430 °C and nights at -180 °C,
 * a dark graphite-rich crust wrinkled by lobate scarps where the planet shrank as it cooled, bright hollows, and ice
 * hidden in the permanent shadow of polar craters.
 */
public final class MercuryModule implements SbModule {
    public static final BlockKit KIT = new BlockKit();
    public static final ResourceKey<Level> MERCURY = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("mercury"));

    public static final DeferredBlock<LunarRegolithBlock> MERCURY_REGOLITH = KIT.block("mercury_regolith", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.6F).sound(SoundType.SAND));
    public static final DeferredBlock<LunarRegolithBlock> HOLLOW_DEPOSIT = KIT.block("hollow_deposit", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.5F).sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> MERCURIAN_CRUST = KIT.rock("mercurian_crust", MapColor.COLOR_GRAY, SoundType.STONE, 1.7F);
    public static final DeferredBlock<Block> LOW_REFLECTANCE_ROCK = KIT.rock("low_reflectance_rock", MapColor.COLOR_BLACK, SoundType.DEEPSLATE, 2.0F);
    public static final DeferredBlock<DropExperienceBlock> GRAPHITE_ORE = KIT.block("graphite_ore", p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(2.5F, 6.0F).sound(SoundType.DEEPSLATE));
    public static final DeferredItem<Item> GRAPHITE = KIT.item("graphite");

    public static final TagKey<Block> MERCURY_STONE = SbTagKeys.modBlock("mercury_stone");
    public static final TagKey<Block> ORES_GRAPHITE = SbTagKeys.commonBlock("ores/graphite");
    public static final TagKey<Item> ITEM_ORES_GRAPHITE = SbTagKeys.commonItem("ores/graphite");

    @Override
    public String id() {
        return "mercury";
    }

    @Override
    public void register(IEventBus modBus) {
        MercuryEntities.register(modBus, KIT.items);
        MercuryStructures.register(modBus);
        KIT.blocks.register(modBus);
        KIT.items.register(modBus);
        KIT.tab.forEach(SbTabs.PLANETS::add);
        MercuryEntities.eggs().forEach(SbTabs.PLANETS::add);
        MercuryData.register();
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.add("body.spacebreaker.mercury", "Mercury", "Merkür");
        lang.add("dimension.spacebreaker.mercury", "Mercury", "Merkür");
        lang.add("dimension.spacebreaker.mercury_orbit", "Mercury Orbit", "Merkür Yörüngesi");
        lang.block(MERCURY_REGOLITH, "Mercurian Regolith", "Merkür Regoliti");
        lang.block(HOLLOW_DEPOSIT, "Hollow Deposit", "Oyuk Tortusu");
        lang.block(MERCURIAN_CRUST, "Mercurian Crust", "Merkür Kabuğu");
        lang.block(LOW_REFLECTANCE_ROCK, "Low-Reflectance Rock", "Düşük Yansıtmalı Kaya");
        lang.block(GRAPHITE_ORE, "Graphite Ore", "Grafit Cevheri");
        lang.item(GRAPHITE, "Graphite", "Grafit");
        MercuryData.translations(lang);
        MercuryEntities.translations(lang);
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        loot.self(MERCURY_REGOLITH.get());
        loot.self(HOLLOW_DEPOSIT.get());
        loot.self(MERCURIAN_CRUST.get());
        loot.self(LOW_REFLECTANCE_ROCK.get());
        loot.ore(GRAPHITE_ORE.get(), GRAPHITE.get());
    }

    @Override
    public void addTags(SbTags tags) {
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, MERCURIAN_CRUST.get(), LOW_REFLECTANCE_ROCK.get(), GRAPHITE_ORE.get());
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, MERCURY_REGOLITH.get(), HOLLOW_DEPOSIT.get());
        tags.block(BlockTags.NEEDS_STONE_TOOL, GRAPHITE_ORE.get());
        tags.block(MERCURY_STONE, MERCURIAN_CRUST.get(), LOW_REFLECTANCE_ROCK.get());
        tags.block(ORES_GRAPHITE, GRAPHITE_ORE.get());
        tags.blockTag(Tags.Blocks.ORES, ORES_GRAPHITE);
        tags.copy(ORES_GRAPHITE, ITEM_ORES_GRAPHITE);
        tags.itemTag(Tags.Items.ORES, ITEM_ORES_GRAPHITE);
    }
}
