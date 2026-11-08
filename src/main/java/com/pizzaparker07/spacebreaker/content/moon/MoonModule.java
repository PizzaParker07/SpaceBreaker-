package com.pizzaparker07.spacebreaker.content.moon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** The Moon: its dimension, rocks, soils, ores, creatures and dungeons. */
public final class MoonModule implements SbModule {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Spacebreaker.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Spacebreaker.MOD_ID);

    public static final ResourceKey<Level> MOON = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("moon"));

    /** Every block item of this module in creative-tab order. */
    private static final List<Supplier<? extends Item>> TAB = new ArrayList<>();

    // ------------------------------------------------------------------ soils (all keep boot prints)

    public static final DeferredBlock<LunarRegolithBlock> LUNAR_REGOLITH = soil("lunar_regolith", MapColor.COLOR_LIGHT_GRAY, SoundType.SAND, 0.6F);
    public static final DeferredBlock<LunarRegolithBlock> MARE_REGOLITH = soil("mare_regolith", MapColor.COLOR_GRAY, SoundType.SAND, 0.6F);
    public static final DeferredBlock<LunarRegolithBlock> VOLCANIC_GLASS_SOIL = soil("volcanic_glass_soil", MapColor.COLOR_BROWN, SoundType.SAND, 0.6F);
    public static final DeferredBlock<LunarRegolithBlock> ICY_REGOLITH = soil("icy_regolith", MapColor.ICE, SoundType.SNOW, 0.8F);

    // ------------------------------------------------------------------ rocks

    public static final DeferredBlock<Block> MARE_BASALT = rock("mare_basalt", MapColor.COLOR_BLACK, SoundType.BASALT, 1.8F);
    public static final StoneFamily ANORTHOSITE = family("anorthosite", MapColor.QUARTZ, SoundType.STONE, 1.5F, true);
    public static final StoneFamily POLISHED_ANORTHOSITE = family("polished_anorthosite", MapColor.QUARTZ, SoundType.STONE, 1.5F, false);
    public static final StoneFamily ANORTHOSITE_BRICKS = family("anorthosite_bricks", MapColor.QUARTZ, SoundType.DEEPSLATE_BRICKS, 1.5F, true);
    public static final DeferredBlock<Block> IMPACT_BRECCIA = rock("impact_breccia", MapColor.COLOR_LIGHT_GRAY, SoundType.TUFF, 2.0F);
    public static final DeferredBlock<Block> LUNAR_ICE = block("lunar_ice", BlockBehaviour.Properties.of().mapColor(MapColor.ICE)
            .instrument(NoteBlockInstrument.CHIME).friction(0.98F).strength(0.8F).sound(SoundType.GLASS));
    public static final DeferredBlock<Block> SINTERED_REGOLITH = rock("sintered_regolith", MapColor.COLOR_GRAY, SoundType.STONE, 2.0F);
    public static final StoneFamily SINTERED_REGOLITH_BRICKS = family("sintered_regolith_bricks", MapColor.COLOR_GRAY, SoundType.MUD_BRICKS, 2.0F, true);
    public static final DeferredBlock<Block> METEORIC_IRON = block("meteoric_iron", BlockBehaviour.Properties.of().mapColor(MapColor.RAW_IRON)
            .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL));

    // ------------------------------------------------------------------ ores

    public static final DeferredBlock<DropExperienceBlock> ILMENITE_ORE = block("ilmenite_ore",
            p -> new DropExperienceBlock(UniformInt.of(1, 3), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(SoundType.BASALT));
    public static final DeferredBlock<DropExperienceBlock> KREEP_ORE = block("kreep_ore",
            p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(SoundType.TUFF));

    public static final DeferredItem<Item> RAW_ILMENITE = ITEMS.registerSimpleItem("raw_ilmenite");
    public static final DeferredItem<Item> KREEP_FRAGMENT = ITEMS.registerSimpleItem("kreep_fragment");
    public static final DeferredItem<Item> GLIMMER_SHELL = ITEMS.registerSimpleItem("glimmer_shell");
    /** A drilled core of polar cold-trap ice: billions of years of layered water ice and frozen volatiles. */
    public static final DeferredItem<Item> ICE_CORE_SAMPLE = ITEMS.registerItem("ice_core_sample", p -> new Item(p) {
        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext ctx, java.util.List<net.minecraft.network.chat.Component> tooltip,
                                    net.minecraft.world.item.TooltipFlag flag) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.spacebreaker.ice_core_sample")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }, new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    /** A lamp of glimmer beetle shells: steady light with no power, for the long lunar night. */
    public static final DeferredBlock<Block> GLIMMER_LAMP = block("glimmer_lamp", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
            .instrument(NoteBlockInstrument.HAT).strength(0.6F).sound(SoundType.GLASS).lightLevel(st -> 14));

    /** A work desk with a switch panel and a screen: base decoration, fitted in the survey outposts. */
    public static final DeferredBlock<ControlDeskBlock> CONTROL_DESK = block("control_desk", ControlDeskBlock::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    // block items (kept as fields for other modules and tests)
    public static final DeferredItem<BlockItem> LUNAR_REGOLITH_ITEM = item(LUNAR_REGOLITH);
    public static final DeferredItem<BlockItem> MARE_BASALT_ITEM = item(MARE_BASALT);
    public static final DeferredItem<BlockItem> ILMENITE_ORE_ITEM = item(ILMENITE_ORE);
    public static final DeferredItem<BlockItem> KREEP_ORE_ITEM = item(KREEP_ORE);

    public static final TagKey<Block> ORES_ILMENITE = SbTagKeys.commonBlock("ores/ilmenite");
    public static final TagKey<Item> ITEM_ORES_ILMENITE = SbTagKeys.commonItem("ores/ilmenite");
    public static final TagKey<Block> ORES_KREEP = SbTagKeys.commonBlock("ores/kreep");
    public static final TagKey<Item> ITEM_ORES_KREEP = SbTagKeys.commonItem("ores/kreep");
    public static final TagKey<Item> RAW_MATERIALS_ILMENITE = SbTagKeys.commonItem("raw_materials/ilmenite");
    /** Soft lunar soils that keep boot prints. */
    public static final TagKey<Block> LUNAR_SOILS = SbTagKeys.modBlock("lunar_soils");
    /** Rocks that Moon ore veins and caves may replace. */
    public static final TagKey<Block> MOON_STONE = SbTagKeys.modBlock("moon_stone");

    /** A rock with optional stairs, slab and wall, like vanilla's stone families. */
    public record StoneFamily(DeferredBlock<Block> base, DeferredBlock<StairBlock> stairs, DeferredBlock<SlabBlock> slab,
                              DeferredBlock<WallBlock> wall) {
        public List<DeferredBlock<? extends Block>> all() {
            List<DeferredBlock<? extends Block>> l = new ArrayList<>(List.of(base, stairs, slab));
            if (wall != null) {
                l.add(wall);
            }
            return l;
        }
    }

    private static BlockBehaviour.Properties rockProps(MapColor color, SoundType sound, float hardness) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(hardness, 6.0F).sound(sound);
    }

    private static <B extends Block> DeferredBlock<B> block(String name, java.util.function.Function<BlockBehaviour.Properties, B> factory,
                                                           BlockBehaviour.Properties props) {
        return BLOCKS.registerBlock(name, factory, props);
    }

    private static DeferredBlock<Block> block(String name, BlockBehaviour.Properties props) {
        return BLOCKS.registerSimpleBlock(name, props);
    }

    private static DeferredBlock<Block> rock(String name, MapColor color, SoundType sound, float hardness) {
        return block(name, rockProps(color, sound, hardness));
    }

    private static DeferredBlock<LunarRegolithBlock> soil(String name, MapColor color, SoundType sound, float hardness) {
        return block(name, LunarRegolithBlock::new, BlockBehaviour.Properties.of().mapColor(color).strength(hardness).sound(sound));
    }

    private static StoneFamily family(String name, MapColor color, SoundType sound, float hardness, boolean wall) {
        String stem = name.endsWith("_bricks") ? name.substring(0, name.length() - 1) : name;
        DeferredBlock<Block> base = rock(name, color, sound, hardness);
        DeferredBlock<StairBlock> stairs = block(stem + "_stairs",
                p -> new StairBlock(base.get().defaultBlockState(), p), rockProps(color, sound, hardness));
        DeferredBlock<SlabBlock> slab = block(stem + "_slab", SlabBlock::new, rockProps(color, sound, hardness));
        DeferredBlock<WallBlock> w = wall ? block(stem + "_wall", WallBlock::new, rockProps(color, sound, hardness).forceSolidOn()) : null;
        return new StoneFamily(base, stairs, slab, w);
    }

    private static <B extends Block> DeferredItem<BlockItem> item(DeferredBlock<B> block) {
        return ITEMS.registerSimpleBlockItem(block);
    }

    static {
        // block items for everything not given a named field above, in tab order
        List<DeferredBlock<? extends Block>> order = new ArrayList<>();
        order.add(LUNAR_REGOLITH);
        order.add(MARE_REGOLITH);
        order.add(VOLCANIC_GLASS_SOIL);
        order.add(ICY_REGOLITH);
        order.add(LUNAR_ICE);
        order.add(MARE_BASALT);
        order.addAll(ANORTHOSITE.all());
        order.addAll(POLISHED_ANORTHOSITE.all());
        order.addAll(ANORTHOSITE_BRICKS.all());
        order.add(IMPACT_BRECCIA);
        order.add(SINTERED_REGOLITH);
        order.addAll(SINTERED_REGOLITH_BRICKS.all());
        order.add(METEORIC_IRON);
        order.add(ILMENITE_ORE);
        order.add(KREEP_ORE);
        order.add(GLIMMER_LAMP);
        order.add(CONTROL_DESK);
        for (DeferredBlock<? extends Block> b : order) {
            DeferredItem<BlockItem> existing = switch (b.getId().getPath()) {
                case "lunar_regolith" -> LUNAR_REGOLITH_ITEM;
                case "mare_basalt" -> MARE_BASALT_ITEM;
                case "ilmenite_ore" -> ILMENITE_ORE_ITEM;
                case "kreep_ore" -> KREEP_ORE_ITEM;
                default -> item(b);
            };
            TAB.add(existing);
        }
        TAB.add(RAW_ILMENITE);
        TAB.add(KREEP_FRAGMENT);
        TAB.add(GLIMMER_SHELL);
        TAB.add(ICE_CORE_SAMPLE);
    }

    public static List<DeferredBlock<LunarRegolithBlock>> soils() {
        return List.of(LUNAR_REGOLITH, MARE_REGOLITH, VOLCANIC_GLASS_SOIL, ICY_REGOLITH);
    }

    public static List<StoneFamily> families() {
        return List.of(ANORTHOSITE, POLISHED_ANORTHOSITE, ANORTHOSITE_BRICKS, SINTERED_REGOLITH_BRICKS);
    }

    @Override
    public String id() {
        return "moon";
    }

    @Override
    public void register(IEventBus modBus) {
        MoonEntities.register(modBus, ITEMS);
        com.pizzaparker07.spacebreaker.content.moon.dungeon.MoonStructures.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SbTabs.PLANETS.icon(LUNAR_REGOLITH_ITEM);
        TAB.forEach(SbTabs.PLANETS::add);
        MoonEntities.eggs().forEach(e -> SbTabs.PLANETS.add(e));
        MoonData.register(modBus);
        // creature spawns per biome (written into the biome JSON by datagen)
        MoonData.SPAWNS.add(new MoonData.BiomeSpawns(MoonData.HIGHLANDS, s -> {
            s.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MoonEntities.HEAT_SIPHON.get(), 30, 1, 2));
            s.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MoonEntities.REGOLITH_TICK.get(), 100, 2, 4));
            s.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(MoonEntities.DUST_SWIMMER.get(), 6, 1, 2));
        }));
        MoonData.SPAWNS.add(new MoonData.BiomeSpawns(MoonData.MARE, s -> {
            s.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MoonEntities.HEAT_SIPHON.get(), 30, 1, 2));
            s.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MoonEntities.REGOLITH_TICK.get(), 60, 1, 3));
            s.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(MoonEntities.DUST_SWIMMER.get(), 14, 2, 3));
        }));
        MoonData.SPAWNS.add(new MoonData.BiomeSpawns(MoonData.CRATER, s -> {
            s.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MoonEntities.HEAT_SIPHON.get(), 20, 1, 2));
            s.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MoonEntities.REGOLITH_TICK.get(), 120, 2, 5));
            s.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MoonEntities.ELDER_REGOLITH_TICK.get(), 4, 1, 1));
            s.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(MoonEntities.GLIMMER_BEETLE.get(), 4, 1, 3));
        }));
        MoonData.SPAWNS.add(new MoonData.BiomeSpawns(MoonData.VOLCANIC, s -> {
            s.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MoonEntities.HEAT_SIPHON.get(), 20, 1, 2));
            s.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(MoonEntities.DUST_SWIMMER.get(), 18, 2, 4));
            s.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MoonEntities.REGOLITH_TICK.get(), 40, 1, 2));
        }));
        MoonData.SPAWNS.add(new MoonData.BiomeSpawns(MoonData.POLAR, s -> {
            s.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MoonEntities.HEAT_SIPHON.get(), 12, 1, 2));
            s.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MoonEntities.REGOLITH_TICK.get(), 30, 1, 2));
            s.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(MoonEntities.GLIMMER_BEETLE.get(), 12, 2, 4));
        }));
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.add("itemGroup.spacebreaker.planets", "Spacebreaker: Worlds", "Spacebreaker: Dünyalar");
        lang.add("body.spacebreaker.moon", "Moon", "Ay");
        lang.add("dimension.spacebreaker.moon", "The Moon", "Ay");
        lang.block(LUNAR_REGOLITH, "Lunar Regolith", "Ay Regoliti");
        lang.block(MARE_REGOLITH, "Mare Regolith", "Mare Regoliti");
        lang.block(VOLCANIC_GLASS_SOIL, "Volcanic Glass Soil", "Volkanik Cam Toprağı");
        lang.block(ICY_REGOLITH, "Icy Regolith", "Buzlu Regolit");
        lang.block(LUNAR_ICE, "Lunar Ice", "Ay Buzu");
        lang.block(MARE_BASALT, "Mare Basalt", "Mare Bazaltı");
        familyNames(lang, ANORTHOSITE, "Anorthosite", "Anortozit");
        familyNames(lang, POLISHED_ANORTHOSITE, "Polished Anorthosite", "Cilalı Anortozit");
        familyNames(lang, ANORTHOSITE_BRICKS, "Anorthosite Brick", "Anortozit Tuğla");
        lang.block(IMPACT_BRECCIA, "Impact Breccia", "Darbe Breşi");
        lang.block(SINTERED_REGOLITH, "Sintered Regolith", "Sinterlenmiş Regolit");
        familyNames(lang, SINTERED_REGOLITH_BRICKS, "Sintered Regolith Brick", "Sinterlenmiş Regolit Tuğla");
        lang.block(METEORIC_IRON, "Meteoric Iron", "Meteor Demiri");
        lang.block(ILMENITE_ORE, "Ilmenite Ore", "İlmenit Cevheri");
        lang.block(KREEP_ORE, "KREEP Ore", "KREEP Cevheri");
        lang.item(RAW_ILMENITE, "Raw Ilmenite", "Ham İlmenit");
        lang.item(KREEP_FRAGMENT, "KREEP Fragment", "KREEP Parçası");
        lang.item(GLIMMER_SHELL, "Glimmer Shell", "Parıltı Kabuğu");
        lang.item(ICE_CORE_SAMPLE, "Ice Core Sample", "Buz Çekirdeği Numunesi");
        lang.add("tooltip.spacebreaker.ice_core_sample", "Ancient cold-trap ice in layers: water and frozen volatiles",
                "Katman katman kadim soğuk tuzak buzu: su ve donmuş uçucular");
        lang.block(GLIMMER_LAMP, "Glimmer Lamp", "Parıltı Lambası");
        lang.block(CONTROL_DESK, "Control Desk", "Kontrol Masası");
        lang.add("biome.spacebreaker.moon_highlands", "Lunar Highlands", "Ay Yaylaları");
        lang.add("biome.spacebreaker.moon_mare", "Lunar Mare", "Ay Denizi (Mare)");
        lang.add("biome.spacebreaker.moon_crater", "Great Crater", "Büyük Krater");
        lang.add("biome.spacebreaker.moon_volcanic", "Pyroclastic Plains", "Piroklastik Ovalar");
        lang.add("biome.spacebreaker.moon_polar", "Lunar Pole", "Ay Kutbu");
        lang.add("structure.spacebreaker.survey_outpost", "Abandoned Survey Outpost", "Terk Edilmiş Keşif Karakolu");
        lang.add("structure.spacebreaker.lava_tube", "Ancient Lava Tube", "Antik Lav Tüpü");
        lang.add("sign.spacebreaker.outpost_vault.0", "VAULT", "KASA");
        lang.add("sign.spacebreaker.outpost_vault.1", "power lost", "güç kesildi");
        lang.add("sign.spacebreaker.outpost_vault.2", "fit a power cell", "güç hücresi tak");
        lang.add("sign.spacebreaker.outpost_vault.3", "above the door", "kapının üstüne");
        MoonData.translations(lang);
        MoonEntities.translations(lang);
    }

    /**
     * Base block named {@code en}/{@code tr}; for brick families the singular stem is given ("Anorthosite Brick")
     * and the base block becomes plural, like vanilla's "Stone Bricks" / "Stone Brick Stairs".
     */
    private static void familyNames(Lang lang, StoneFamily f, String en, String tr) {
        boolean bricks = f.base().getId().getPath().endsWith("_bricks");
        lang.block(f.base(), bricks ? en + "s" : en, tr);
        lang.block(f.stairs(), en + " Stairs", tr + " Merdiven");
        lang.block(f.slab(), en + " Slab", tr + " Basamak");
        if (f.wall() != null) {
            lang.block(f.wall(), en + " Wall", tr + " Duvar");
        }
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        soils().forEach(s -> loot.self(s.get()));
        loot.self(LUNAR_ICE.get());
        loot.self(MARE_BASALT.get());
        loot.self(IMPACT_BRECCIA.get());
        loot.self(SINTERED_REGOLITH.get());
        loot.self(METEORIC_IRON.get());
        loot.self(GLIMMER_LAMP.get());
        loot.self(CONTROL_DESK.get());
        for (StoneFamily f : families()) {
            loot.self(f.base().get());
            loot.self(f.stairs().get());
            loot.slab(f.slab().get());
            if (f.wall() != null) {
                loot.self(f.wall().get());
            }
        }
        loot.ore(ILMENITE_ORE.get(), RAW_ILMENITE.get());
        loot.ore(KREEP_ORE.get(), KREEP_FRAGMENT.get());
    }

    @Override
    public void addTags(SbTags tags) {
        List<Block> pick = new ArrayList<>(List.of(MARE_BASALT.get(), IMPACT_BRECCIA.get(), SINTERED_REGOLITH.get(),
                METEORIC_IRON.get(), ILMENITE_ORE.get(), KREEP_ORE.get(), LUNAR_ICE.get(), GLIMMER_LAMP.get(), CONTROL_DESK.get()));
        for (StoneFamily f : families()) {
            f.all().forEach(b -> pick.add(b.get()));
            tags.block(BlockTags.STAIRS, f.stairs().get());
            tags.block(BlockTags.SLABS, f.slab().get());
            if (f.wall() != null) {
                tags.block(BlockTags.WALLS, f.wall().get());
            }
        }
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, pick.toArray(Block[]::new));
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, soils().stream().map(DeferredBlock::get).toArray(Block[]::new));
        tags.block(LUNAR_SOILS, soils().stream().map(DeferredBlock::get).toArray(Block[]::new));
        tags.block(MOON_STONE, MARE_BASALT.get(), ANORTHOSITE.base().get(), IMPACT_BRECCIA.get());
        tags.block(BlockTags.NEEDS_IRON_TOOL, ILMENITE_ORE.get(), KREEP_ORE.get());
        tags.block(BlockTags.NEEDS_STONE_TOOL, METEORIC_IRON.get());
        tags.block(BlockTags.ICE, LUNAR_ICE.get());
        tags.block(ORES_ILMENITE, ILMENITE_ORE.get());
        tags.block(ORES_KREEP, KREEP_ORE.get());
        tags.blockTag(Tags.Blocks.ORES, ORES_ILMENITE, ORES_KREEP);
        tags.copy(ORES_ILMENITE, ITEM_ORES_ILMENITE);
        tags.copy(ORES_KREEP, ITEM_ORES_KREEP);
        tags.itemTag(Tags.Items.ORES, ITEM_ORES_ILMENITE, ITEM_ORES_KREEP);
        tags.item(RAW_MATERIALS_ILMENITE, RAW_ILMENITE.get());
        tags.itemTag(Tags.Items.RAW_MATERIALS, RAW_MATERIALS_ILMENITE);
        tags.copy(BlockTags.STAIRS, ItemTags.STAIRS);
        tags.copy(BlockTags.SLABS, ItemTags.SLABS);
        tags.copy(BlockTags.WALLS, ItemTags.WALLS);
    }
}
