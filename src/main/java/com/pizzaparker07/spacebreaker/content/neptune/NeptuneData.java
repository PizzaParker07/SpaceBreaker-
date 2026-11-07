package com.pizzaparker07.spacebreaker.content.neptune;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.belt.SmallBodyTerrain;
import com.pizzaparker07.spacebreaker.content.belt.world.CellsFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.VentFeature;
import com.pizzaparker07.spacebreaker.content.worldgen.Landforms;
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
import static com.pizzaparker07.spacebreaker.content.neptune.NeptuneModule.*;

/** Triton: terrain profile, biomes and features (datagen). */
public final class NeptuneData {
    public static final ResourceKey<Biome> TRITON_CANTALOUPE = OuterWorlds.biome("triton_cantaloupe");
    public static final ResourceKey<Biome> TRITON_CRATER = OuterWorlds.biome("triton_crater");
    public static final ResourceKey<Biome> TRITON_POLAR = OuterWorlds.biome("triton_polar_cap");
    public static final List<ResourceKey<Biome>> TRITON_BIOMES = List.of(TRITON_CANTALOUPE, TRITON_CRATER, TRITON_POLAR);

    private NeptuneData() {
    }

    static ResourceKey<TerrainProfile> terrain(String name) {
        return ResourceKey.create(TerrainProfile.REGISTRY, Spacebreaker.id(name));
    }

    static void translations(Lang lang) {
        lang.add("dungeon.spacebreaker.triton_geyser_station", "Geyser Watch Station", "Gayzer Gözlem İstasyonu");
        lang.add("structure.spacebreaker.triton_geyser_station", "Geyser Watch Station", "Gayzer Gözlem İstasyonu");
        lang.add("biome.spacebreaker.triton_cantaloupe", "Cantaloupe Terrain", "Kavun Kabuğu Arazisi");
        lang.add("biome.spacebreaker.triton_crater", "Tritonian Crater", "Triton Krateri");
        lang.add("biome.spacebreaker.triton_polar_cap", "Pink Polar Cap", "Pembe Kutup Başlığı");
        lang.add("tech.spacebreaker.cryovolcanism", "Cryovolcanism", "Kriyovolkanizma");
        lang.add("tech.spacebreaker.cryovolcanism.desc",
                "Sunlight warms the nitrogen under Triton's clear polar ice until it bursts out in geysers kilometres high: a volcano that runs on frost.",
                "Güneş ışığı Triton'un saydam kutup buzunun altındaki azotu ısıtır, ta ki kilometrelerce yükselen gayzerlerle fışkırana kadar: kırağıyla çalışan bir volkan.");
    }

    // ------------------------------------------------------------------ terrain

    public static TerrainProfile tritonProfile() {
        var m = new SmallBodyTerrain.Materials(s(TRITON_FROST), s(TRITON_ICE.base()), s(TRITON_ICE.base()), s(TRITON_FROST),
                Optional.of(s(PINK_NITROGEN_FROST)), Optional.of(s(TRITON_ICE.base())));
        // the pink polar cap of nitrogen frost, reaching far toward the equator in the long southern summer
        var cap = new Landforms.IceCap(48, 2, 52, 0, s(PINK_NITROGEN_FROST), Optional.of(s(PINK_NITROGEN_FROST)), Optional.empty());
        var lf = new Landforms(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(cap), List.of(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        // a young surface: very few craters
        return SmallBodyTerrain.of(Spacebreaker.id("triton"), 1353.4).baseHeight(74).roughness(0.5).craters(0.25).age(0.15)
                .iceLatitude(80).materials(m).landforms(lf).build();
    }

    /** Cantaloupe terrain: round pits a few blocks deep with sharp ridges between them, between the caps. */
    public static CellsFeature.Config cantaloupe() {
        return new CellsFeature.Config("dimples", 30, 4, 2.0, Optional.of(s(TRITON_ICE.base())), Optional.empty(), 0, 0, 0, -44, 44,
                Optional.empty());
    }

    /** Nitrogen geysers in the polar caps, each with a dark streak of fallen dust. */
    public static VentFeature.Config geysers() {
        return new VentFeature.Config(s(NITROGEN_GEYSER), List.of(s(PLUME_DUST)), 130, 0.55, 7, -90, -50, Optional.empty());
    }

    // ------------------------------------------------------------------ datagen

    static void datapack(SbDatapack dp) {
        dp.add(TerrainProfile.REGISTRY, ctx -> {
            ctx.register(terrain("triton"), tritonProfile());
            Proteus.terrain(ctx);
        });
        dp.add(Registries.CONFIGURED_FEATURE, NeptuneData::configured);
        dp.add(Registries.PLACED_FEATURE, NeptuneData::placed);
        dp.add(Registries.BIOME, NeptuneData::biomes);
        dp.add(Registries.STRUCTURE, com.pizzaparker07.spacebreaker.content.neptune.dungeon.NeptuneStructures::structures);
        dp.add(Registries.STRUCTURE_SET, com.pizzaparker07.spacebreaker.content.neptune.dungeon.NeptuneStructures::sets);
    }

    private static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        Proteus.configured(ctx);
        ctx.register(cf("triton_cantaloupe"), new ConfiguredFeature<>(OuterWorlds.CELLS.get(), cantaloupe()));
        ctx.register(cf("triton_geysers"), new ConfiguredFeature<>(OuterWorlds.VENTS.get(), geysers()));
        ctx.register(cf("triton_clathrate"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(new TagMatchTest(TRITON_STONE),
                s(CLATHRATE_ORE), 9)));
        ctx.register(cf("triton_block"), OuterWorlds.boulder(0.6F, 1.5F, 0.5F, s(TRITON_ICE.base()), 1));
    }

    private static void placed(BootstrapContext<PlacedFeature> ctx) {
        Proteus.placed(ctx);
        var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        for (String whole : new String[]{"triton_cantaloupe", "triton_geysers"}) {
            PlacementUtils.register(ctx, pf(whole), cf.getOrThrow(cf(whole)), BiomeFilter.biome());
        }
        PlacementUtils.register(ctx, pf("triton_clathrate"), cf.getOrThrow(cf("triton_clathrate")), CountPlacement.of(9), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-40), VerticalAnchor.absolute(100)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("triton_block"), cf.getOrThrow(cf("triton_block")), RarityFilter.onAverageOnceEvery(6),
                InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
    }

    private static void biomes(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> pf = ctx.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
        Proteus.biomes(ctx, pf, carvers);
        var raw = GenerationStep.Decoration.RAW_GENERATION;
        var ores = GenerationStep.Decoration.UNDERGROUND_ORES;
        var local = GenerationStep.Decoration.LOCAL_MODIFICATIONS;
        var surface = GenerationStep.Decoration.SURFACE_STRUCTURES;
        for (ResourceKey<Biome> key : TRITON_BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -2.0F, OuterWorlds.Look.airless(0xA9B3A6),
                    raw, "triton_cantaloupe", ores, "triton_clathrate", local, "triton_block", surface, "triton_geysers"));
        }
    }
}
