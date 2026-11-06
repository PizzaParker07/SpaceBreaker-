package com.pizzaparker07.spacebreaker.content.mercury;

import com.pizzaparker07.spacebreaker.content.mars.MarsData;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
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

/** Mercury biomes and features, added to the inner-planet datagen. */
public final class MercuryData {
    public static final ResourceKey<Biome> PLAINS = MarsData.biome("mercury_plains");
    public static final ResourceKey<Biome> HIGHLANDS = MarsData.biome("mercury_highlands");
    public static final ResourceKey<Biome> CRATER = MarsData.biome("mercury_crater");
    public static final ResourceKey<Biome> POLAR = MarsData.biome("mercury_polar");
    public static final List<ResourceKey<Biome>> BIOMES = List.of(PLAINS, HIGHLANDS, CRATER, POLAR);

    private MercuryData() {
    }

    static void register() {
        MarsData.EXTRA_CONFIGURED.add(MercuryData::configured);
        MarsData.EXTRA_PLACED.add(MercuryData::placed);
        MarsData.EXTRA_BIOMES.add(MercuryData::biomes);
    }

    static void translations(Lang lang) {
        lang.add("biome.spacebreaker.mercury_plains", "Smooth Plains", "Düz Ovalar");
        lang.add("biome.spacebreaker.mercury_highlands", "Intercrater Plains", "Kraterler Arası Ovalar");
        lang.add("biome.spacebreaker.mercury_crater", "Mercurian Basin", "Merkür Havzası");
        lang.add("biome.spacebreaker.mercury_polar", "Shadowed Pole", "Gölgeli Kutup");
        lang.add("dungeon.spacebreaker.mercury_ice_mine", "Abandoned Polar Ice Mine", "Terk Edilmiş Kutup Buzu Madeni");
        lang.add("structure.spacebreaker.mercury_ice_mine", "Abandoned Polar Ice Mine", "Terk Edilmiş Kutup Buzu Madeni");
    }

    private static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(MarsData.cf("mercury_boulder"), MarsData.boulder(0.8F, 2.0F, 0.45F, MarsData.s(MercuryModule.MERCURIAN_CRUST), 3,
                MarsData.s(MercuryModule.LOW_REFLECTANCE_ROCK), 2));
        var stone = new TagMatchTest(MercuryModule.MERCURY_STONE);
        ctx.register(MarsData.cf("mercury_graphite"), new ConfiguredFeature<>(Feature.ORE,
                new OreConfiguration(stone, MarsData.s(MercuryModule.GRAPHITE_ORE), 9)));
        ctx.register(MarsData.cf("mercury_lrm"), new ConfiguredFeature<>(Feature.ORE,
                new OreConfiguration(stone, MarsData.s(MercuryModule.LOW_REFLECTANCE_ROCK), 48)));
        ctx.register(MarsData.cf("mercury_ice"), new ConfiguredFeature<>(Feature.ORE,
                new OreConfiguration(stone, MoonModule.LUNAR_ICE.get().defaultBlockState(), 24)));
    }

    private static void placed(BootstrapContext<PlacedFeature> ctx) {
        var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        PlacementUtils.register(ctx, MarsData.pf("mercury_boulder"), cf.getOrThrow(MarsData.cf("mercury_boulder")),
                RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
        PlacementUtils.register(ctx, MarsData.pf("mercury_lrm"), cf.getOrThrow(MarsData.cf("mercury_lrm")), CountPlacement.of(6),
                InSquarePlacement.spread(), HeightRangePlacement.uniform(VerticalAnchor.absolute(-60), VerticalAnchor.absolute(120)), BiomeFilter.biome());
        PlacementUtils.register(ctx, MarsData.pf("mercury_graphite"), cf.getOrThrow(MarsData.cf("mercury_graphite")), CountPlacement.of(8),
                InSquarePlacement.spread(), HeightRangePlacement.uniform(VerticalAnchor.absolute(-50), VerticalAnchor.absolute(110)), BiomeFilter.biome());
        PlacementUtils.register(ctx, MarsData.pf("mercury_ice"), cf.getOrThrow(MarsData.cf("mercury_ice")), CountPlacement.of(8),
                InSquarePlacement.spread(), HeightRangePlacement.uniform(VerticalAnchor.absolute(20), VerticalAnchor.absolute(120)), BiomeFilter.biome());
    }

    private static void biomes(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> pf = ctx.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
        for (ResourceKey<Biome> key : BIOMES) {
            BiomeGenerationSettings.Builder gen = new BiomeGenerationSettings.Builder(pf, carvers);
            gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, pf.getOrThrow(MarsData.pf("mercury_lrm")));
            gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, pf.getOrThrow(MarsData.pf("mercury_graphite")));
            if (key == POLAR) {
                gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, pf.getOrThrow(MarsData.pf("mercury_ice")));
            }
            gen.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, pf.getOrThrow(MarsData.pf("mercury_boulder")));
            ctx.register(key, new Biome.BiomeBuilder()
                    .hasPrecipitation(false).temperature(0.5F).downfall(0F)
                    .specialEffects(new BiomeSpecialEffects.Builder()
                            .fogColor(0x000000).skyColor(0x000000).waterColor(0x3F76E4).waterFogColor(0x050533)
                            .grassColorOverride(0x55575C).foliageColorOverride(0x55575C).build())
                    .mobSpawnSettings(MarsData.spawnsFor(key))
                    .generationSettings(gen.build())
                    .build());
        }
    }
}
