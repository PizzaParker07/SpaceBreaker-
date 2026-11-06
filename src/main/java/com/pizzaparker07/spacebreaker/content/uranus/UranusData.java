package com.pizzaparker07.spacebreaker.content.uranus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.belt.SmallBodyTerrain;
import com.pizzaparker07.spacebreaker.content.belt.world.CoronaFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.RupesFeature;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.data.SbDatapack;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;
import java.util.Optional;

import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.cf;
import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.pf;
import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.s;
import static com.pizzaparker07.spacebreaker.content.uranus.UranusModule.*;

/** Miranda: terrain profile, biomes and features (datagen). */
public final class UranusData {
    public static final ResourceKey<Biome> MIRANDA_PLAINS = OuterWorlds.biome("miranda_plains");
    public static final ResourceKey<Biome> MIRANDA_CRATER = OuterWorlds.biome("miranda_crater");
    public static final ResourceKey<Biome> MIRANDA_POLAR = OuterWorlds.biome("miranda_polar");
    public static final List<ResourceKey<Biome>> MIRANDA_BIOMES = List.of(MIRANDA_PLAINS, MIRANDA_CRATER, MIRANDA_POLAR);

    private UranusData() {
    }

    static ResourceKey<TerrainProfile> terrain(String name) {
        return ResourceKey.create(TerrainProfile.REGISTRY, Spacebreaker.id(name));
    }

    static void translations(Lang lang) {
        lang.add("biome.spacebreaker.miranda_plains", "Patchwork Plains", "Yamalı Ovalar");
        lang.add("biome.spacebreaker.miranda_crater", "Mirandan Crater", "Miranda Krateri");
        lang.add("biome.spacebreaker.miranda_polar", "Mirandan Pole", "Miranda Kutbu");
        lang.add("tech.spacebreaker.vertical_exploration", "Vertical Exploration", "Dikey Keşif");
        lang.add("tech.spacebreaker.vertical_exploration.desc",
                "Rope anchors in ice, slow controlled descents and the patience to fall for minutes: how to get down Miranda's great cliff and back up again.",
                "Buza çakılan halat çapaları, yavaş ve denetimli inişler ve dakikalarca düşmeye yetecek sabır: Miranda'nın dev uçurumundan nasıl inilir ve yine nasıl çıkılır.");
    }

    // ------------------------------------------------------------------ terrain

    public static TerrainProfile mirandaProfile() {
        var m = new SmallBodyTerrain.Materials(s(MIRANDAN_REGOLITH), s(MIRANDAN_ICE.base()), s(MIRANDAN_ICE.base()), s(MIRANDAN_REGOLITH),
                Optional.of(s(MIRANDAN_REGOLITH)), Optional.of(s(BRIGHT_RIDGE_ICE)));
        return SmallBodyTerrain.of(Spacebreaker.id("miranda"), 235.8).baseHeight(76).roughness(1.0).craters(1.0).age(0.8)
                .iceLatitude(84).materials(m).build();
    }

    /** The great fault cliff, a short walk from the landing site. */
    public static RupesFeature.Config cliff() {
        return new RupesFeature.Config(380, -8, 1000, 18, 96, 7, s(MIRANDAN_ICE.base()), Optional.of(s(DARK_BAND_ICE)));
    }

    /** Three racetrack coronae of bright and dark ridges; the cliff runs along the edge of the first. */
    public static CoronaFeature.Config coronae() {
        return new CoronaFeature.Config(List.of(
                new CoronaFeature.Corona(380, -18, 420, 240, 18),
                new CoronaFeature.Corona(-760, 28, 300, 260, -30),
                new CoronaFeature.Corona(1300, 30, 380, 210, 60)),
                16, 5, s(BRIGHT_RIDGE_ICE), s(DARK_BAND_ICE));
    }

    // ------------------------------------------------------------------ datagen

    static void datapack(SbDatapack dp) {
        dp.add(TerrainProfile.REGISTRY, ctx -> ctx.register(terrain("miranda"), mirandaProfile()));
        dp.add(Registries.CONFIGURED_FEATURE, UranusData::configured);
        dp.add(Registries.PLACED_FEATURE, UranusData::placed);
        dp.add(Registries.BIOME, UranusData::biomes);
    }

    private static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(cf("miranda_coronae"), new ConfiguredFeature<>(OuterWorlds.CORONAE.get(), coronae()));
        ctx.register(cf("miranda_cliff"), new ConfiguredFeature<>(OuterWorlds.RUPES.get(), cliff()));
        ctx.register(cf("miranda_carbon"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(new TagMatchTest(MIRANDA_STONE),
                s(CARBON_ORE), 10)));
        ctx.register(cf("miranda_block"), OuterWorlds.boulder(0.6F, 1.8F, 0.5F, s(MIRANDAN_ICE.base()), 3, s(DARK_BAND_ICE), 1));
    }

    private static void placed(BootstrapContext<PlacedFeature> ctx) {
        var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        for (String whole : new String[]{"miranda_coronae", "miranda_cliff"}) {
            PlacementUtils.register(ctx, pf(whole), cf.getOrThrow(cf(whole)), BiomeFilter.biome());
        }
        PlacementUtils.register(ctx, pf("miranda_carbon"), cf.getOrThrow(cf("miranda_carbon")), CountPlacement.of(10), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-40), VerticalAnchor.absolute(100)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("miranda_block"), cf.getOrThrow(cf("miranda_block")), RarityFilter.onAverageOnceEvery(4),
                InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
    }

    private static void biomes(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> pf = ctx.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
        var raw = GenerationStep.Decoration.RAW_GENERATION;
        var ores = GenerationStep.Decoration.UNDERGROUND_ORES;
        var local = GenerationStep.Decoration.LOCAL_MODIFICATIONS;
        for (ResourceKey<Biome> key : MIRANDA_BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -2.0F, OuterWorlds.Look.airless(0x9A9A98),
                    raw, "miranda_coronae", raw, "miranda_cliff", ores, "miranda_carbon", local, "miranda_block"));
        }
    }
}
