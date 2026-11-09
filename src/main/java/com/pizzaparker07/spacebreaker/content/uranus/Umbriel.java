package com.pizzaparker07.spacebreaker.content.uranus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.belt.SmallBodyTerrain;
import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.RingFeature;
import com.pizzaparker07.spacebreaker.content.machines.IsruRecipes;
import com.pizzaparker07.spacebreaker.content.mars.BlockKit;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
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
 * Umbriel, the darkest of Uranus' large moons: an old surface, saturated with craters and coated in a soot-dark carbon
 * film, unchanged for billions of years. One thing breaks the gloom: Wunda, a crater near the equator whose floor holds
 * a bright ring of frost. Its own small package: three blocks, the Gloam Hopper, a lander and a showcase stage.
 */
public final class Umbriel {
    public static final ResourceKey<Level> UMBRIEL = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("umbriel"));
    private static final BlockKit KIT = UranusModule.KIT;

    public static final DeferredBlock<LunarRegolithBlock> REGOLITH = KIT.block("umbriel_regolith", LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(0.5F).sound(SoundType.SAND));
    public static final BlockKit.Family ICE = KIT.family("umbriel_ice", MapColor.DEEPSLATE, SoundType.GLASS, 1.7F, true);
    /** Dark ice laced with carbon: smelts into coal. */
    public static final DeferredBlock<Block> SOOTY_ICE = KIT.rock("sooty_ice", MapColor.COLOR_BLACK, SoundType.STONE, 1.5F);
    public static final TagKey<Block> STONE = SbTagKeys.modBlock("umbriel_stone");

    public static final ResourceKey<Biome> PLAINS = OuterWorlds.biome("umbriel_plains");
    public static final ResourceKey<Biome> CRATER = OuterWorlds.biome("umbriel_crater");
    public static final ResourceKey<Biome> POLAR = OuterWorlds.biome("umbriel_polar");
    public static final List<ResourceKey<Biome>> BIOMES = List.of(PLAINS, CRATER, POLAR);

    /** Wunda, the bright-ringed crater, a short walk east of the landing site. */
    public static final double WUNDA_X = 320;
    public static final double WUNDA_LATITUDE = -4;

    private Umbriel() {
    }

    /** Loads the block fields before the kit registers. */
    static void load() {
    }

    // ------------------------------------------------------------------ terrain

    public static TerrainProfile profile() {
        var m = new SmallBodyTerrain.Materials(s(REGOLITH), s(ICE.base()), s(SOOTY_ICE), s(REGOLITH),
                Optional.of(s(MarsModule.CO2_FROST)), Optional.of(s(ICE.base())));
        // old and dark: saturated with craters, no fresh bright rays
        return SmallBodyTerrain.of(Spacebreaker.id("umbriel"), 584.7).baseHeight(76).roughness(0.9).craters(1.2).age(0.95)
                .iceLatitude(82).materials(m).build();
    }

    /** Wunda: a flat-floored crater with a low rim and no central peak. */
    public static BasinFeature.Config wunda() {
        return new BasinFeature.Config(WUNDA_X, WUNDA_LATITUDE, 170, 16, 8, 0, 0.1, 2, 0.22, 0,
                s(ICE.base()), Optional.of(s(REGOLITH)), Optional.empty());
    }

    /** The bright frost ring on Wunda's floor. */
    public static RingFeature.Config ring() {
        return new RingFeature.Config(List.of(new RingFeature.Mark(WUNDA_X, WUNDA_LATITUDE, 105, 26, s(MarsModule.CO2_FROST))));
    }

    // ------------------------------------------------------------------ data

    static void translations(Lang lang) {
        lang.add("dimension.spacebreaker.umbriel", "Umbriel", "Umbriel");
        lang.block(REGOLITH, "Umbrielian Regolith", "Umbriel Regoliti");
        BlockKit.familyNames(lang, ICE, "Umbrielian Bedrock Ice", "Umbriel Ana Kaya Buzu");
        lang.block(SOOTY_ICE, "Sooty Ice", "İsli Buz");
        lang.add("biome.spacebreaker.umbriel_plains", "Gloaming Plains", "Alacakaranlık Ovaları");
        lang.add("biome.spacebreaker.umbriel_crater", "Umbrielian Crater", "Umbriel Krateri");
        lang.add("biome.spacebreaker.umbriel_polar", "Umbrielian Pole", "Umbriel Kutbu");
    }

    static void loot(SbBlockLoot loot) {
        loot.self(REGOLITH.get());
        loot.self(SOOTY_ICE.get());
    }

    static void tags(SbTags tags) {
        tags.item(IsruRecipes.WATER_ICES, ICE.base().get().asItem(), SOOTY_ICE.get().asItem());
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, REGOLITH.get());
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, SOOTY_ICE.get());
        tags.block(STONE, ICE.base().get(), SOOTY_ICE.get());
        tags.block(UranusModule.MIRANDAN_GROUND, REGOLITH.get(), ICE.base().get(), SOOTY_ICE.get());
    }

    static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(cf("umbriel_wunda"), new ConfiguredFeature<>(OuterWorlds.BASIN.get(), wunda()));
        ctx.register(cf("umbriel_wunda_ring"), new ConfiguredFeature<>(OuterWorlds.MARKS.get(), ring()));
        OuterWorlds.ore(ctx, "umbriel_soot", STONE, s(SOOTY_ICE), 14);
        ctx.register(cf("umbriel_block"), OuterWorlds.boulder(0.6F, 1.6F, 0.5F, s(ICE.base()), 3, s(SOOTY_ICE), 1));
    }

    static void placed(BootstrapContext<PlacedFeature> ctx) {
        OuterWorlds.placedWhole(ctx, "umbriel_wunda", "umbriel_wunda_ring");
        OuterWorlds.placedOre(ctx, "umbriel_soot", 6);
        OuterWorlds.placedBoulder(ctx, "umbriel_block", 5);
    }

    static void biomes(BootstrapContext<Biome> ctx, HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        var raw = GenerationStep.Decoration.RAW_GENERATION;
        for (ResourceKey<Biome> key : BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -2.0F, OuterWorlds.Look.airless(0x4A4846),
                    raw, "umbriel_wunda", raw, "umbriel_wunda_ring", GenerationStep.Decoration.UNDERGROUND_ORES, "umbriel_soot",
                    GenerationStep.Decoration.LOCAL_MODIFICATIONS, "umbriel_block"));
        }
    }
}
