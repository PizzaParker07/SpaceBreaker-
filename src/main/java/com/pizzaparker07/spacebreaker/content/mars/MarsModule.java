package com.pizzaparker07.spacebreaker.content.mars;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;

/** Mars: its dimension, soils, sands, rocks, ices, ores, dust storms, creatures and dungeons. */
public final class MarsModule implements SbModule {
    public static final BlockKit KIT = new BlockKit();
    public static final ResourceKey<Level> MARS = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("mars"));

    // ------------------------------------------------------------------ soils (boot prints, erased by the wind)

    public static final DeferredBlock<MarsSoilBlock> MARTIAN_REGOLITH = soil("martian_regolith", MapColor.COLOR_ORANGE, SoundType.SAND, 0.6F);
    public static final DeferredBlock<MarsSoilBlock> HEMATITE_SOIL = soil("hematite_soil", MapColor.TERRACOTTA_RED, SoundType.SAND, 0.7F);
    public static final DeferredBlock<MarsSoilBlock> PERCHLORATE_SOIL = soil("perchlorate_soil", MapColor.TERRACOTTA_ORANGE, SoundType.SAND, 0.6F);
    public static final DeferredBlock<MarsSoilBlock> MARTIAN_CLAY = soil("martian_clay", MapColor.TERRACOTTA_WHITE, SoundType.MUD, 0.8F);
    public static final DeferredBlock<MarsSoilBlock> CO2_FROST = soil("co2_frost", MapColor.SNOW, SoundType.SNOW, 0.5F);

    // ------------------------------------------------------------------ sands (fall like vanilla sand)

    public static final DeferredBlock<ColoredFallingBlock> MARTIAN_SAND = sand("martian_sand", 0xFF4F4039, MapColor.COLOR_BROWN);
    public static final DeferredBlock<ColoredFallingBlock> OLIVINE_SAND = sand("olivine_sand", 0xFF6A6B3B, MapColor.COLOR_GREEN);

    // ------------------------------------------------------------------ rocks

    public static final BlockKit.Family MARTIAN_BASALT = KIT.family("martian_basalt", MapColor.COLOR_GRAY, SoundType.BASALT, 1.6F, true);
    public static final BlockKit.Family POLISHED_MARTIAN_BASALT = KIT.family("polished_martian_basalt", MapColor.COLOR_GRAY, SoundType.POLISHED_DEEPSLATE, 1.6F, false);
    public static final BlockKit.Family MARTIAN_BASALT_BRICKS = KIT.family("martian_basalt_bricks", MapColor.COLOR_GRAY, SoundType.DEEPSLATE_BRICKS, 1.8F, true);
    public static final BlockKit.Family MARTIAN_SANDSTONE = KIT.family("martian_sandstone", MapColor.TERRACOTTA_ORANGE, SoundType.STONE, 0.9F, true);
    public static final DeferredBlock<Block> MARTIAN_MUDSTONE = KIT.rock("martian_mudstone", MapColor.TERRACOTTA_BROWN, SoundType.TUFF, 1.2F);
    public static final DeferredBlock<Block> GYPSUM_BLOCK = KIT.rock("gypsum_block", MapColor.QUARTZ, SoundType.CALCITE, 0.75F);

    // ------------------------------------------------------------------ ices

    public static final DeferredBlock<Block> MARTIAN_ICE = KIT.block("martian_ice", BlockBehaviour.Properties.of().mapColor(MapColor.ICE)
            .instrument(NoteBlockInstrument.CHIME).friction(0.98F).strength(0.8F).sound(SoundType.GLASS));
    public static final DeferredBlock<Block> DUSTY_ICE = KIT.block("dusty_ice", BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE)
            .instrument(NoteBlockInstrument.CHIME).friction(0.9F).strength(0.9F).sound(SoundType.GLASS));
    public static final DeferredBlock<DryIceBlock> DRY_ICE = KIT.block("dry_ice", DryIceBlock::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.SNOW).instrument(NoteBlockInstrument.CHIME).friction(0.95F).strength(0.6F).sound(SoundType.GLASS).randomTicks());

    // ------------------------------------------------------------------ ores

    public static final DeferredBlock<DropExperienceBlock> HEMATITE_ORE = KIT.block("hematite_ore",
            p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(2.5F, 6.0F).sound(SoundType.STONE));
    public static final DeferredBlock<DropExperienceBlock> GYPSUM_ORE = KIT.block("gypsum_ore",
            p -> new DropExperienceBlock(UniformInt.of(0, 1), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(1.8F, 6.0F).sound(SoundType.TUFF));

    // ------------------------------------------------------------------ Phobos and Deimos (airless: prints stay forever)

    public static final ResourceKey<Level> PHOBOS = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("phobos"));
    public static final ResourceKey<Level> DEIMOS = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("deimos"));
    public static final DeferredBlock<com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock> PHOBOS_REGOLITH = KIT.block("phobos_regolith",
            com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.5F).sound(SoundType.SAND));
    public static final DeferredBlock<com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock> DEIMOS_REGOLITH = KIT.block("deimos_regolith",
            com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.5F).sound(SoundType.SAND));
    public static final DeferredBlock<Block> CARBONACEOUS_CHONDRITE = KIT.rock("carbonaceous_chondrite", MapColor.COLOR_BLACK, SoundType.TUFF, 1.4F);

    public static final DeferredItem<Item> RAW_HEMATITE = KIT.item("raw_hematite");
    public static final DeferredItem<Item> GYPSUM_CRYSTAL = KIT.item("gypsum_crystal");

    // ------------------------------------------------------------------ tags

    public static final TagKey<Block> ORES_HEMATITE = SbTagKeys.commonBlock("ores/hematite");
    public static final TagKey<Item> ITEM_ORES_HEMATITE = SbTagKeys.commonItem("ores/hematite");
    public static final TagKey<Block> ORES_GYPSUM = SbTagKeys.commonBlock("ores/gypsum");
    public static final TagKey<Item> ITEM_ORES_GYPSUM = SbTagKeys.commonItem("ores/gypsum");
    public static final TagKey<Item> RAW_MATERIALS_HEMATITE = SbTagKeys.commonItem("raw_materials/hematite");
    public static final TagKey<Item> GEMS_GYPSUM = SbTagKeys.commonItem("gems/gypsum");
    /** Loose Martian soils (boot prints). */
    public static final TagKey<Block> MARS_SOILS = SbTagKeys.modBlock("mars_soils");
    /** Rocks that Mars ore veins may replace. */
    public static final TagKey<Block> MARS_STONE = SbTagKeys.modBlock("mars_stone");

    private static DeferredBlock<MarsSoilBlock> soil(String name, MapColor color, SoundType sound, float hardness) {
        return KIT.block(name, MarsSoilBlock::new, BlockBehaviour.Properties.of().mapColor(color).strength(hardness).sound(sound));
    }

    private static DeferredBlock<ColoredFallingBlock> sand(String name, int dust, MapColor color) {
        return KIT.block(name, p -> new ColoredFallingBlock(new ColorRGBA(dust), p), BlockBehaviour.Properties.of().mapColor(color)
                .instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND));
    }

    public static List<DeferredBlock<MarsSoilBlock>> soils() {
        return List.of(MARTIAN_REGOLITH, HEMATITE_SOIL, PERCHLORATE_SOIL, MARTIAN_CLAY, CO2_FROST);
    }

    @Override
    public String id() {
        return "mars";
    }

    @Override
    public void register(IEventBus modBus) {
        com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.register(modBus, KIT.items);
        com.pizzaparker07.spacebreaker.content.mars.dungeon.MarsStructures.register(modBus);
        MarsParticles.register(modBus);
        KIT.blocks.register(modBus);
        KIT.items.register(modBus);
        KIT.tab.forEach(SbTabs.PLANETS::add);
        com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.eggs().forEach(SbTabs.PLANETS::add);
        MarsData.register(modBus);
        MarsClimate.register();
        modBus.addListener(RegisterPayloadHandlersEvent.class, e -> e.registrar("1").playToClient(MarsWeather.Payload.TYPE,
                MarsWeather.Payload.STREAM_CODEC, (payload, ctx) -> MarsWeather.receive(payload)));
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, e -> {
            if (e.getLevel() instanceof ServerLevel level) {
                MarsWeather.tick(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                MarsWeather.syncTo(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                MarsWeather.syncTo(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, MarsModule::commands);
    }

    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("spacebreaker").then(Commands.literal("duststorm")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("start").executes(ctx -> storm(ctx.getSource(), 1F))
                        .then(Commands.argument("strength", FloatArgumentType.floatArg(0.05F, 1F))
                                .executes(ctx -> storm(ctx.getSource(), FloatArgumentType.getFloat(ctx, "strength")))))
                .then(Commands.literal("stop").executes(ctx -> storm(ctx.getSource(), 0F)))));
    }

    private static int storm(net.minecraft.commands.CommandSourceStack src, float strength) {
        if (!MarsWeather.dusty(src.getLevel())) {
            src.sendFailure(Component.translatable("commands.spacebreaker.duststorm.none"));
            return 0;
        }
        MarsWeather.force(src.getLevel(), strength, 24000);
        src.sendSuccess(() -> Component.translatable(strength > 0 ? "commands.spacebreaker.duststorm.start" : "commands.spacebreaker.duststorm.stop"), true);
        return 1;
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.add("body.spacebreaker.mars", "Mars", "Mars");
        lang.add("dimension.spacebreaker.mars", "Mars", "Mars");
        lang.block(MARTIAN_REGOLITH, "Martian Regolith", "Mars Regoliti");
        lang.block(HEMATITE_SOIL, "Hematite Spherule Soil", "Hematit Kürecikli Toprak");
        lang.block(PERCHLORATE_SOIL, "Perchlorate Soil", "Perkloratlı Toprak");
        lang.block(MARTIAN_CLAY, "Martian Clay", "Mars Kili");
        lang.block(CO2_FROST, "CO₂ Frost", "CO₂ Kırağısı");
        lang.block(MARTIAN_SAND, "Martian Sand", "Mars Kumu");
        lang.block(OLIVINE_SAND, "Olivine Sand", "Olivin Kumu");
        BlockKit.familyNames(lang, MARTIAN_BASALT, "Martian Basalt", "Mars Bazaltı");
        BlockKit.familyNames(lang, POLISHED_MARTIAN_BASALT, "Polished Martian Basalt", "Cilalı Mars Bazaltı");
        BlockKit.familyNames(lang, MARTIAN_BASALT_BRICKS, "Martian Basalt Brick", "Mars Bazaltı Tuğla");
        BlockKit.familyNames(lang, MARTIAN_SANDSTONE, "Layered Sandstone", "Katmanlı Kumtaşı");
        lang.block(MARTIAN_MUDSTONE, "Martian Mudstone", "Mars Çamurtaşı");
        lang.block(GYPSUM_BLOCK, "Gypsum Block", "Alçıtaşı Bloğu");
        lang.block(MARTIAN_ICE, "Martian Water Ice", "Mars Su Buzu");
        lang.block(DUSTY_ICE, "Dusty Ice", "Tozlu Buz");
        lang.block(DRY_ICE, "Dry Ice", "Kuru Buz");
        lang.block(HEMATITE_ORE, "Hematite Ore", "Hematit Cevheri");
        lang.block(GYPSUM_ORE, "Gypsum Vein", "Alçıtaşı Damarı");
        lang.item(RAW_HEMATITE, "Raw Hematite", "Ham Hematit");
        lang.add("body.spacebreaker.phobos", "Phobos", "Phobos");
        lang.add("body.spacebreaker.deimos", "Deimos", "Deimos");
        lang.add("dimension.spacebreaker.phobos", "Phobos", "Phobos");
        lang.add("dimension.spacebreaker.deimos", "Deimos", "Deimos");
        lang.block(PHOBOS_REGOLITH, "Phobos Regolith", "Phobos Regoliti");
        lang.block(DEIMOS_REGOLITH, "Deimos Regolith", "Deimos Regoliti");
        lang.block(CARBONACEOUS_CHONDRITE, "Carbonaceous Chondrite", "Karbonlu Kondrit");
        lang.item(GYPSUM_CRYSTAL, "Gypsum Crystal", "Alçıtaşı Kristali");
        lang.add("commands.spacebreaker.duststorm.start", "A dust storm sweeps across Mars", "Mars'ta bir toz fırtınası başladı");
        lang.add("commands.spacebreaker.duststorm.stop", "The dust settles", "Toz yatıştı");
        lang.add("commands.spacebreaker.duststorm.none", "This world has no dust storms", "Bu dünyada toz fırtınası olmaz");
        MarsData.translations(lang);
        com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.translations(lang);
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        soils().forEach(s -> loot.self(s.get()));
        loot.self(MARTIAN_SAND.get());
        loot.self(PHOBOS_REGOLITH.get());
        loot.self(DEIMOS_REGOLITH.get());
        loot.self(CARBONACEOUS_CHONDRITE.get());
        loot.self(OLIVINE_SAND.get());
        KIT.familyLoot(loot);
        loot.self(MARTIAN_MUDSTONE.get());
        loot.self(GYPSUM_BLOCK.get());
        loot.self(MARTIAN_ICE.get());
        loot.self(DUSTY_ICE.get());
        loot.custom(DRY_ICE.get(), b -> net.minecraft.world.level.storage.loot.LootTable.lootTable()); // sublimates: needs silk touch
        loot.ore(HEMATITE_ORE.get(), RAW_HEMATITE.get());
        loot.ore(GYPSUM_ORE.get(), GYPSUM_CRYSTAL.get());
    }

    @Override
    public void addTags(SbTags tags) {
        List<Block> pick = new ArrayList<>(List.of(CARBONACEOUS_CHONDRITE.get(), MARTIAN_MUDSTONE.get(), GYPSUM_BLOCK.get(), MARTIAN_ICE.get(), DUSTY_ICE.get(),
                DRY_ICE.get(), HEMATITE_ORE.get(), GYPSUM_ORE.get()));
        for (BlockKit.Family f : KIT.families()) {
            f.all().forEach(b -> pick.add(b.get()));
            tags.block(BlockTags.STAIRS, f.stairs().get());
            tags.block(BlockTags.SLABS, f.slab().get());
            if (f.wall() != null) {
                tags.block(BlockTags.WALLS, f.wall().get());
            }
        }
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, pick.toArray(Block[]::new));
        List<Block> shovel = new ArrayList<>(soils().stream().map(DeferredBlock::get).map(b -> (Block) b).toList());
        shovel.add(MARTIAN_SAND.get());
        shovel.add(OLIVINE_SAND.get());
        shovel.add(PHOBOS_REGOLITH.get());
        shovel.add(DEIMOS_REGOLITH.get());
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, shovel.toArray(Block[]::new));
        tags.block(MARS_SOILS, soils().stream().map(DeferredBlock::get).toArray(Block[]::new));
        tags.block(BlockTags.SAND, MARTIAN_SAND.get(), OLIVINE_SAND.get());
        tags.block(MARS_STONE, MARTIAN_BASALT.base().get(), MARTIAN_SANDSTONE.base().get(), MARTIAN_MUDSTONE.get());
        tags.block(BlockTags.NEEDS_STONE_TOOL, HEMATITE_ORE.get());
        tags.block(BlockTags.ICE, MARTIAN_ICE.get(), DUSTY_ICE.get());
        tags.block(ORES_HEMATITE, HEMATITE_ORE.get());
        tags.block(ORES_GYPSUM, GYPSUM_ORE.get());
        tags.blockTag(Tags.Blocks.ORES, ORES_HEMATITE, ORES_GYPSUM);
        tags.copy(ORES_HEMATITE, ITEM_ORES_HEMATITE);
        tags.copy(ORES_GYPSUM, ITEM_ORES_GYPSUM);
        tags.itemTag(Tags.Items.ORES, ITEM_ORES_HEMATITE, ITEM_ORES_GYPSUM);
        tags.item(RAW_MATERIALS_HEMATITE, RAW_HEMATITE.get());
        tags.itemTag(Tags.Items.RAW_MATERIALS, RAW_MATERIALS_HEMATITE);
        tags.item(GEMS_GYPSUM, GYPSUM_CRYSTAL.get());
        // stairs/slabs/walls block tags are mirrored into item tags once, by the Moon module
        tags.item(ItemTags.SAND, MARTIAN_SAND.get().asItem(), OLIVINE_SAND.get().asItem());
    }

    /** Meteorites lie on Mars too: the Moon's iron-nickel blocks are reused in Mars worldgen. */
    static Block meteorite() {
        return MoonModule.METEORIC_IRON.get();
    }
}
