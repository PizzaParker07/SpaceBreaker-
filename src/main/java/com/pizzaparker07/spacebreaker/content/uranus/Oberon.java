package com.pizzaparker07.spacebreaker.content.uranus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.belt.SmallBodyTerrain;
import com.pizzaparker07.spacebreaker.content.belt.world.CraterFloorFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature;
import com.pizzaparker07.spacebreaker.content.machines.IsruRecipes;
import com.pizzaparker07.spacebreaker.content.mars.BlockKit;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.worldgen.Landforms;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.List;
import java.util.Optional;

import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.cf;
import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.s;

/**
 * Oberon, the outermost of Uranus' large moons: old, heavily cratered reddish-grey ice, where many large craters have
 * floors flooded with something dark; one great chasm, and a mountain about 11 km high standing over the plains (here
 * west of the landing site). Its own small package: three blocks, the Rim Burrower, a lander and a showcase stage.
 */
public final class Oberon {
    public static final ResourceKey<Level> OBERON = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("oberon"));
    private static final BlockKit KIT = UranusModule.KIT;

    public static final DeferredBlock<LunarRegolithBlock> REGOLITH = KIT.block("oberon_regolith", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).strength(0.5F).sound(SoundType.SAND));
    public static final BlockKit.Family ICE = KIT.family("oberon_ice", MapColor.TERRACOTTA_GRAY, SoundType.STONE, 1.7F, true);
    /** The dark material flooding the floors of Oberon's large craters. */
    public static final DeferredBlock<Block> DARK_FLOOR = KIT.block("dark_crater_fill", BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_BLACK).strength(0.8F).sound(SoundType.GRAVEL));
    public static final TagKey<Block> STONE = SbTagKeys.modBlock("oberon_stone");

    public static final ResourceKey<Biome> PLAINS = OuterWorlds.biome("oberon_plains");
    public static final ResourceKey<Biome> CRATER = OuterWorlds.biome("oberon_crater");
    public static final ResourceKey<Biome> POLAR = OuterWorlds.biome("oberon_polar");
    public static final List<ResourceKey<Biome>> BIOMES = List.of(PLAINS, CRATER, POLAR);

    public static final double RADIUS_KM = 761.4;
    /** The great mountain, west of the landing site. */
    public static final double MOUNTAIN_X = -420;
    public static final double MOUNTAIN_Z = 180;
    public static final double MOUNTAIN_HEIGHT = 110;

    private Oberon() {
    }

    /** Loads the block fields before the kit registers. */
    static void load() {
    }

    // ------------------------------------------------------------------ terrain

    public static TerrainProfile profile() {
        var m = new SmallBodyTerrain.Materials(s(REGOLITH), s(ICE.base()), s(ICE.base()), s(REGOLITH),
                Optional.of(s(MarsModule.CO2_FROST)), Optional.of(s(ICE.base())));
        var mountain = new Landforms.Volcanoes(2000, 0.0, 60, 100, 0.9, MOUNTAIN_HEIGHT, 0.0, 0,
                List.of(new Landforms.Anchor(MOUNTAIN_X, MOUNTAIN_Z, 95, MOUNTAIN_HEIGHT)), s(ICE.base()), Optional.of(s(REGOLITH)));
        var lf = new Landforms(Optional.empty(), Optional.empty(), Optional.of(mountain), Optional.empty(), Optional.empty(), Optional.empty(),
                List.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        return SmallBodyTerrain.of(Spacebreaker.id("oberon"), RADIUS_KM).baseHeight(74).roughness(0.9).craters(1.1).age(0.85)
                .iceLatitude(80).materials(m).landforms(lf).build();
    }

    /** Dark floors in more than half of the larger craters. */
    public static CraterFloorFeature.Config darkFloors() {
        return new CraterFloorFeature.Config(s(DARK_FLOOR), 12, 0.6, 0.7, 0, 0, 2, 0, Optional.empty());
    }

    /** Mommur Chasma: one long chasm crossing the plains east of the landing site. */
    public static GrabenFeature.Config chasma() {
        return new GrabenFeature.Config(List.of(new GrabenFeature.Graben(520, 8, 2600, 50, 38, -15)), 10, 3, s(REGOLITH), s(ICE.base()));
    }

    // ------------------------------------------------------------------ data

    static void translations(Lang lang) {
        lang.add("dimension.spacebreaker.oberon", "Oberon", "Oberon");
        lang.block(REGOLITH, "Oberonian Regolith", "Oberon Regoliti");
        BlockKit.familyNames(lang, ICE, "Oberonian Bedrock Ice", "Oberon Ana Kaya Buzu");
        lang.block(DARK_FLOOR, "Dark Crater Fill", "Koyu Krater Dolgusu");
        lang.add("biome.spacebreaker.oberon_plains", "Oberonian Plains", "Oberon Ovaları");
        lang.add("biome.spacebreaker.oberon_crater", "Dark-Floored Crater", "Koyu Tabanlı Krater");
        lang.add("biome.spacebreaker.oberon_polar", "Oberonian Pole", "Oberon Kutbu");
    }

    static void loot(SbBlockLoot loot) {
        loot.self(REGOLITH.get());
        loot.self(DARK_FLOOR.get());
    }

    static void tags(SbTags tags) {
        tags.item(IsruRecipes.WATER_ICES, ICE.base().get().asItem());
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, REGOLITH.get(), DARK_FLOOR.get());
        tags.block(STONE, ICE.base().get());
        tags.block(UranusModule.MIRANDAN_GROUND, REGOLITH.get(), ICE.base().get(), DARK_FLOOR.get());
    }

    static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(cf("oberon_dark_floors"), new ConfiguredFeature<>(OuterWorlds.CRATER_FLOOR.get(), darkFloors()));
        ctx.register(cf("oberon_chasma"), new ConfiguredFeature<>(OuterWorlds.GRABENS.get(), chasma()));
        ctx.register(cf("oberon_block"), OuterWorlds.boulder(0.6F, 1.8F, 0.5F, s(ICE.base()), 3, s(DARK_FLOOR), 1));
    }

    static void placed(BootstrapContext<PlacedFeature> ctx) {
        OuterWorlds.placedWhole(ctx, "oberon_chasma", "oberon_dark_floors");
        OuterWorlds.placedBoulder(ctx, "oberon_block", 4);
    }

    static void biomes(BootstrapContext<Biome> ctx, HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        var raw = GenerationStep.Decoration.RAW_GENERATION;
        for (ResourceKey<Biome> key : BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -2.0F, OuterWorlds.Look.airless(0x6E6560),
                    raw, "oberon_chasma", raw, "oberon_dark_floors", GenerationStep.Decoration.LOCAL_MODIFICATIONS, "oberon_block"));
        }
    }
}
