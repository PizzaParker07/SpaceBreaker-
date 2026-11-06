package com.pizzaparker07.spacebreaker.content.earth;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.data.SbDatapack;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

/**
 * Overworld geology for the industry ores.
 * <ul>
 *   <li>Bauxite: lateritic weathering crust, shallow, only in warm biomes.</li>
 *   <li>Rutile: deep and rare (heavy-mineral rich metamorphic rock).</li>
 *   <li>Pentlandite: nickel sulphide at mid depth.</li>
 *   <li>Spodumene: lithium pegmatites high up in mountains.</li>
 *   <li>Wolframite: deep greisen veins.</li>
 *   <li>Uraninite: small pitchblende pods, deepest and rarest.</li>
 *   <li>Crude oil: reservoirs trapped underground, marked by surface seeps.</li>
 * </ul>
 */
public final class EarthWorldgen {
    public record Vein(Metal metal, int size, int count, int minY, int maxY, boolean triangle, float airDiscard, TagKey<Biome> biomes, String suffix) {
        public Vein(Metal metal, int size, int count, int minY, int maxY, boolean triangle, float airDiscard, TagKey<Biome> biomes) {
            this(metal, size, count, minY, maxY, triangle, airDiscard, biomes, "");
        }

        public ResourceKey<ConfiguredFeature<?, ?>> configured() {
            return ResourceKey.create(Registries.CONFIGURED_FEATURE, Spacebreaker.id("ore_" + metal.oreName + suffix));
        }

        public ResourceKey<PlacedFeature> placed() {
            return ResourceKey.create(Registries.PLACED_FEATURE, Spacebreaker.id("ore_" + metal.oreName + suffix));
        }

        public ResourceKey<BiomeModifier> modifier() {
            return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Spacebreaker.id("add_ore_" + metal.oreName + suffix));
        }
    }

    public static final List<Vein> VEINS = List.of(
            new Vein(EarthModule.ALUMINUM, 10, 9, 40, 128, false, 0.0F, Tags.Biomes.IS_HOT_OVERWORLD),
            // old lateritic patches everywhere else, so nobody is stuck before finding a warm biome
            new Vein(EarthModule.ALUMINUM, 6, 2, 48, 100, false, 0.0F, BiomeTags.IS_OVERWORLD, "_sparse"),
            new Vein(EarthModule.TITANIUM, 5, 3, -64, 8, true, 0.4F, BiomeTags.IS_OVERWORLD),
            new Vein(EarthModule.NICKEL, 8, 6, -32, 48, true, 0.2F, BiomeTags.IS_OVERWORLD),
            new Vein(EarthModule.LITHIUM, 7, 10, 40, 220, false, 0.0F, Tags.Biomes.IS_MOUNTAIN),
            new Vein(EarthModule.TUNGSTEN, 6, 3, -64, -16, true, 0.3F, BiomeTags.IS_OVERWORLD),
            new Vein(EarthModule.URANIUM, 4, 1, -64, -32, true, 0.5F, BiomeTags.IS_OVERWORLD));

    public static final ResourceKey<ConfiguredFeature<?, ?>> OIL_CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE, Spacebreaker.id("oil_deposit"));
    public static final ResourceKey<PlacedFeature> OIL_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, Spacebreaker.id("oil_deposit"));
    public static final ResourceKey<BiomeModifier> OIL_MODIFIER = ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Spacebreaker.id("add_oil_deposit"));

    static void bootstrap(SbDatapack dp) {
        dp.add(Registries.CONFIGURED_FEATURE, ctx -> {
            for (Vein v : VEINS) {
                List<OreConfiguration.TargetBlockState> targets = List.of(
                        OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), v.metal().ore.get().defaultBlockState()),
                        OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), v.metal().deepslateOre.get().defaultBlockState()));
                ctx.register(v.configured(), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(targets, v.size(), v.airDiscard())));
            }
            ctx.register(OIL_CONFIGURED, new ConfiguredFeature<>(EarthModule.OIL_DEPOSIT.get(), NoneFeatureConfiguration.INSTANCE));
        });
        dp.add(Registries.PLACED_FEATURE, ctx -> {
            HolderGetter<ConfiguredFeature<?, ?>> configured = ctx.lookup(Registries.CONFIGURED_FEATURE);
            for (Vein v : VEINS) {
                VerticalAnchor lo = VerticalAnchor.absolute(v.minY());
                VerticalAnchor hi = VerticalAnchor.absolute(v.maxY());
                List<PlacementModifier> mods = List.of(CountPlacement.of(v.count()), InSquarePlacement.spread(),
                        v.triangle() ? HeightRangePlacement.triangle(lo, hi) : HeightRangePlacement.uniform(lo, hi), BiomeFilter.biome());
                ctx.register(v.placed(), new PlacedFeature(configured.getOrThrow(v.configured()), mods));
            }
            ctx.register(OIL_PLACED, new PlacedFeature(configured.getOrThrow(OIL_CONFIGURED),
                    List.of(RarityFilter.onAverageOnceEvery(28), InSquarePlacement.spread(), BiomeFilter.biome())));
        });
        dp.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ctx -> {
            HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
            HolderGetter<PlacedFeature> placed = ctx.lookup(Registries.PLACED_FEATURE);
            for (Vein v : VEINS) {
                ctx.register(v.modifier(), new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(v.biomes()),
                        HolderSet.direct(placed.getOrThrow(v.placed())), GenerationStep.Decoration.UNDERGROUND_ORES));
            }
            ctx.register(OIL_MODIFIER, new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(Tags.Biomes.IS_DRY_OVERWORLD),
                    HolderSet.direct(placed.getOrThrow(OIL_PLACED)), GenerationStep.Decoration.LAKES));
        });
    }

    private EarthWorldgen() {
    }
}
