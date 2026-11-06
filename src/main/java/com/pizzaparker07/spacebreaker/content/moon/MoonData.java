package com.pizzaparker07.spacebreaker.content.moon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.worldgen.BoulderFeature;
import com.pizzaparker07.spacebreaker.content.worldgen.WorldgenModule;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Moon data: biome, feature and structure keys, their datagen bootstrap, and recipes. The dimension, dimension
 * type and terrain profile are plain JSON in {@code src/main/resources} so they stay easy to tweak.
 */
public final class MoonData {
    public static final ResourceKey<Biome> HIGHLANDS = biome("moon_highlands");
    public static final ResourceKey<Biome> MARE = biome("moon_mare");
    public static final ResourceKey<Biome> CRATER = biome("moon_crater");
    public static final ResourceKey<Biome> VOLCANIC = biome("moon_volcanic");
    public static final ResourceKey<Biome> POLAR = biome("moon_polar");

    static final ResourceKey<ConfiguredFeature<?, ?>> CF_BOULDER_HIGHLAND = cf("moon_boulder_highland");
    static final ResourceKey<ConfiguredFeature<?, ?>> CF_BOULDER_MARE = cf("moon_boulder_mare");
    static final ResourceKey<ConfiguredFeature<?, ?>> CF_BOULDER_LARGE = cf("moon_boulder_large");
    static final ResourceKey<ConfiguredFeature<?, ?>> CF_METEORITE = cf("moon_meteorite");
    static final ResourceKey<ConfiguredFeature<?, ?>> CF_ICE_BOULDER = cf("moon_ice_boulder");
    static final ResourceKey<ConfiguredFeature<?, ?>> CF_ILMENITE = cf("moon_ore_ilmenite");
    static final ResourceKey<ConfiguredFeature<?, ?>> CF_KREEP = cf("moon_ore_kreep");
    static final ResourceKey<ConfiguredFeature<?, ?>> CF_ICE_VEIN = cf("moon_ice_vein");
    static final ResourceKey<ConfiguredFeature<?, ?>> CF_BRECCIA = cf("moon_breccia_blob");
    static final ResourceKey<ConfiguredFeature<?, ?>> CF_BURIED_IRON = cf("moon_buried_iron");

    static final ResourceKey<PlacedFeature> PF_BOULDER_HIGHLAND = pf("moon_boulder_highland");
    static final ResourceKey<PlacedFeature> PF_BOULDER_MARE = pf("moon_boulder_mare");
    static final ResourceKey<PlacedFeature> PF_BOULDER_LARGE = pf("moon_boulder_large");
    static final ResourceKey<PlacedFeature> PF_METEORITE = pf("moon_meteorite");
    static final ResourceKey<PlacedFeature> PF_METEORITE_RARE = pf("moon_meteorite_rare");
    static final ResourceKey<PlacedFeature> PF_ICE_BOULDER = pf("moon_ice_boulder");
    static final ResourceKey<PlacedFeature> PF_ILMENITE = pf("moon_ore_ilmenite");
    static final ResourceKey<PlacedFeature> PF_ILMENITE_RICH = pf("moon_ore_ilmenite_rich");
    static final ResourceKey<PlacedFeature> PF_KREEP = pf("moon_ore_kreep");
    static final ResourceKey<PlacedFeature> PF_ICE_VEIN = pf("moon_ice_vein");
    static final ResourceKey<PlacedFeature> PF_BRECCIA = pf("moon_breccia_blob");
    static final ResourceKey<PlacedFeature> PF_BURIED_IRON = pf("moon_buried_iron");

    /** Extra bootstrap steps added by other Moon classes (creatures, dungeons) before datagen runs. */
    static final List<Consumer<RegistrySetBuilder>> EXTRA = new ArrayList<>();
    /** Extra mob spawns per biome, filled by the creature code. */
    static final List<BiomeSpawns> SPAWNS = new ArrayList<>();

    record BiomeSpawns(ResourceKey<Biome> biome, Consumer<MobSpawnSettings.Builder> spawns) {
    }

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, Spacebreaker.id(name));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> cf(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Spacebreaker.id(name));
    }

    private static ResourceKey<PlacedFeature> pf(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Spacebreaker.id(name));
    }

    static void register(IEventBus modBus) {
        modBus.addListener(GatherDataEvent.class, MoonData::gather);
    }

    static void translations(Lang lang) {
    }

    // ------------------------------------------------------------------ datagen

    private static void gather(GatherDataEvent event) {
        PackOutput out = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        RegistrySetBuilder builder = new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, MoonData::configuredFeatures)
                .add(Registries.PLACED_FEATURE, MoonData::placedFeatures)
                .add(Registries.BIOME, MoonData::biomes);
        EXTRA.forEach(c -> c.accept(builder));
        event.getGenerator().addProvider(event.includeServer(),
                new Named("Spacebreaker Moon worldgen", new DatapackBuiltinEntriesProvider(out, lookup, builder, Set.of(Spacebreaker.MOD_ID))));
        event.getGenerator().addProvider(event.includeServer(), new Named("Spacebreaker Moon recipes", new Recipes(out, lookup)));
    }

    /** Gives a provider a unique name so several modules can each add their own. */
    record Named(String name, DataProvider delegate) implements DataProvider {
        @Override
        public CompletableFuture<?> run(CachedOutput output) {
            return delegate.run(output);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private static BlockState s(net.neoforged.neoforge.registries.DeferredBlock<? extends Block> b) {
        return b.get().defaultBlockState();
    }

    private static void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        BoulderFeature boulder = WorldgenModule.BOULDER.get();
        ctx.register(CF_BOULDER_HIGHLAND, new ConfiguredFeature<>(boulder, new BoulderFeature.Config(new WeightedStateProvider(
                SimpleWeightedRandomList.<BlockState>builder().add(s(MoonModule.ANORTHOSITE.base()), 5).add(s(MoonModule.IMPACT_BRECCIA), 3)),
                UniformFloat.of(0.9F, 2.2F), 0.45F)));
        ctx.register(CF_BOULDER_MARE, new ConfiguredFeature<>(boulder, new BoulderFeature.Config(new WeightedStateProvider(
                SimpleWeightedRandomList.<BlockState>builder().add(s(MoonModule.MARE_BASALT), 5).add(s(MoonModule.IMPACT_BRECCIA), 1)),
                UniformFloat.of(0.8F, 1.8F), 0.45F)));
        ctx.register(CF_BOULDER_LARGE, new ConfiguredFeature<>(boulder, new BoulderFeature.Config(new WeightedStateProvider(
                SimpleWeightedRandomList.<BlockState>builder().add(s(MoonModule.IMPACT_BRECCIA), 4).add(s(MoonModule.ANORTHOSITE.base()), 3)
                        .add(s(MoonModule.MARE_BASALT), 1)),
                UniformFloat.of(2.0F, 3.6F), 0.4F)));
        ctx.register(CF_METEORITE, new ConfiguredFeature<>(boulder, new BoulderFeature.Config(new WeightedStateProvider(
                SimpleWeightedRandomList.<BlockState>builder().add(s(MoonModule.METEORIC_IRON), 4).add(s(MoonModule.IMPACT_BRECCIA), 1)),
                UniformFloat.of(0.7F, 1.4F), 0.55F)));
        ctx.register(CF_ICE_BOULDER, new ConfiguredFeature<>(boulder, new BoulderFeature.Config(new WeightedStateProvider(
                SimpleWeightedRandomList.<BlockState>builder().add(s(MoonModule.LUNAR_ICE), 2).add(s(MoonModule.ANORTHOSITE.base()), 3)),
                UniformFloat.of(0.8F, 1.6F), 0.5F)));

        RuleTest basalt = new BlockMatchTest(MoonModule.MARE_BASALT.get());
        RuleTest moonStone = new TagMatchTest(MoonModule.MOON_STONE);
        RuleTest anorthosite = new BlockMatchTest(MoonModule.ANORTHOSITE.base().get());
        ctx.register(CF_ILMENITE, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(basalt, s(MoonModule.ILMENITE_ORE), 9)));
        ctx.register(CF_KREEP, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(List.of(
                OreConfiguration.target(new BlockMatchTest(MoonModule.IMPACT_BRECCIA.get()), s(MoonModule.KREEP_ORE)),
                OreConfiguration.target(moonStone, s(MoonModule.KREEP_ORE))), 6)));
        ctx.register(CF_ICE_VEIN, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(List.of(
                OreConfiguration.target(anorthosite, s(MoonModule.LUNAR_ICE)),
                OreConfiguration.target(new BlockMatchTest(MoonModule.IMPACT_BRECCIA.get()), s(MoonModule.LUNAR_ICE)),
                OreConfiguration.target(new BlockMatchTest(MoonModule.LUNAR_REGOLITH.get()), s(MoonModule.ICY_REGOLITH))), 26)));
        ctx.register(CF_BRECCIA, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(anorthosite, s(MoonModule.IMPACT_BRECCIA), 40)));
        ctx.register(CF_BURIED_IRON, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(moonStone, s(MoonModule.METEORIC_IRON), 4)));
    }

    private static void placedFeatures(BootstrapContext<PlacedFeature> ctx) {
        HolderGetter<ConfiguredFeature<?, ?>> cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        PlacementUtils.register(ctx, PF_BOULDER_HIGHLAND, cf.getOrThrow(CF_BOULDER_HIGHLAND),
                RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
        PlacementUtils.register(ctx, PF_BOULDER_MARE, cf.getOrThrow(CF_BOULDER_MARE),
                RarityFilter.onAverageOnceEvery(4), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
        PlacementUtils.register(ctx, PF_BOULDER_LARGE, cf.getOrThrow(CF_BOULDER_LARGE),
                RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
        PlacementUtils.register(ctx, PF_METEORITE, cf.getOrThrow(CF_METEORITE),
                RarityFilter.onAverageOnceEvery(18), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
        PlacementUtils.register(ctx, PF_METEORITE_RARE, cf.getOrThrow(CF_METEORITE),
                RarityFilter.onAverageOnceEvery(90), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
        PlacementUtils.register(ctx, PF_ICE_BOULDER, cf.getOrThrow(CF_ICE_BOULDER),
                RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
        PlacementUtils.register(ctx, PF_ILMENITE, cf.getOrThrow(CF_ILMENITE), CountPlacement.of(4), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-20), VerticalAnchor.absolute(120)), BiomeFilter.biome());
        PlacementUtils.register(ctx, PF_ILMENITE_RICH, cf.getOrThrow(CF_ILMENITE), CountPlacement.of(10), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(20), VerticalAnchor.absolute(120)), BiomeFilter.biome());
        PlacementUtils.register(ctx, PF_KREEP, cf.getOrThrow(CF_KREEP), CountPlacement.of(3), InSquarePlacement.spread(),
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-50), VerticalAnchor.absolute(90)), BiomeFilter.biome());
        PlacementUtils.register(ctx, PF_ICE_VEIN, cf.getOrThrow(CF_ICE_VEIN), CountPlacement.of(7), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(10), VerticalAnchor.absolute(130)), BiomeFilter.biome());
        PlacementUtils.register(ctx, PF_BRECCIA, cf.getOrThrow(CF_BRECCIA), CountPlacement.of(5), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-60), VerticalAnchor.absolute(120)), BiomeFilter.biome());
        PlacementUtils.register(ctx, PF_BURIED_IRON, cf.getOrThrow(CF_BURIED_IRON), CountPlacement.of(2), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-60), VerticalAnchor.absolute(100)), BiomeFilter.biome());
    }

    /** Features in one fixed order: every biome must list shared features in the same order (vanilla rule). */
    private static final List<ResourceKey<PlacedFeature>> ORE_ORDER = List.of(PF_BRECCIA, PF_ILMENITE, PF_ILMENITE_RICH, PF_KREEP,
            PF_ICE_VEIN, PF_BURIED_IRON);
    private static final List<ResourceKey<PlacedFeature>> SURFACE_ORDER = List.of(PF_BOULDER_LARGE, PF_BOULDER_HIGHLAND, PF_BOULDER_MARE,
            PF_ICE_BOULDER, PF_METEORITE, PF_METEORITE_RARE);

    private static Consumer<BiomeGenerationSettings.Builder> features(HolderGetter<PlacedFeature> pf, Set<ResourceKey<PlacedFeature>> wanted) {
        return g -> {
            ORE_ORDER.stream().filter(wanted::contains).forEach(k -> g.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, pf.getOrThrow(k)));
            SURFACE_ORDER.stream().filter(wanted::contains).forEach(k -> g.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, pf.getOrThrow(k)));
        };
    }

    private static void biomes(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> pf = ctx.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
        ctx.register(HIGHLANDS, moonBiome(pf, carvers, HIGHLANDS, features(pf, Set.of(PF_BRECCIA, PF_KREEP, PF_BURIED_IRON,
                PF_BOULDER_HIGHLAND, PF_METEORITE_RARE))));
        ctx.register(MARE, moonBiome(pf, carvers, MARE, features(pf, Set.of(PF_ILMENITE, PF_KREEP, PF_BOULDER_MARE, PF_METEORITE_RARE))));
        ctx.register(CRATER, moonBiome(pf, carvers, CRATER, features(pf, Set.of(PF_BRECCIA, PF_KREEP, PF_ILMENITE, PF_BURIED_IRON,
                PF_BOULDER_LARGE, PF_BOULDER_HIGHLAND, PF_METEORITE))));
        ctx.register(VOLCANIC, moonBiome(pf, carvers, VOLCANIC, features(pf, Set.of(PF_ILMENITE_RICH, PF_BOULDER_MARE))));
        ctx.register(POLAR, moonBiome(pf, carvers, POLAR, features(pf, Set.of(PF_BRECCIA, PF_ICE_VEIN, PF_KREEP, PF_BOULDER_HIGHLAND,
                PF_ICE_BOULDER))));
    }

    private static Biome moonBiome(HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers, ResourceKey<Biome> key,
                                   Consumer<BiomeGenerationSettings.Builder> features) {
        BiomeGenerationSettings.Builder gen = new BiomeGenerationSettings.Builder(pf, carvers);
        features.accept(gen);
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder().creatureGenerationProbability(0.04F);
        for (BiomeSpawns s : SPAWNS) {
            if (s.biome() == key) {
                s.spawns().accept(spawns);
            }
        }
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5F)
                .downfall(0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .fogColor(0x000000).skyColor(0x000000).waterColor(0x3F76E4).waterFogColor(0x050533)
                        .grassColorOverride(0x8A8A80).foliageColorOverride(0x8A8A80).build())
                .mobSpawnSettings(spawns.build())
                .generationSettings(gen.build())
                .build();
    }

    // ------------------------------------------------------------------ recipes

    static final class Recipes extends RecipeProvider {
        Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        /** Stonecutter recipe in our own namespace (vanilla's helper would name it minecraft:...). */
        private static void stonecut(RecipeOutput out, RecipeCategory category, ItemLike result, ItemLike material) {
            stonecut(out, category, result, material, 1);
        }

        private static void stonecut(RecipeOutput out, RecipeCategory category, ItemLike result, ItemLike material, int count) {
            net.minecraft.data.recipes.SingleItemRecipeBuilder.stonecutting(Ingredient.of(material), category, result, count)
                    .unlockedBy(getHasName(material), has(material))
                    .save(out, Spacebreaker.id(getConversionRecipeName(result, material) + "_stonecutting"));
        }

        @Override
        protected void buildRecipes(RecipeOutput out) {
            MoonModule.StoneFamily an = MoonModule.ANORTHOSITE;
            MoonModule.StoneFamily pol = MoonModule.POLISHED_ANORTHOSITE;
            MoonModule.StoneFamily bri = MoonModule.ANORTHOSITE_BRICKS;
            MoonModule.StoneFamily sin = MoonModule.SINTERED_REGOLITH_BRICKS;
            for (MoonModule.StoneFamily f : MoonModule.families()) {
                ItemLike base = f.base().get();
                stairBuilder(f.stairs().get(), Ingredient.of(base)).unlockedBy(getHasName(base), has(base)).save(out);
                slab(out, RecipeCategory.BUILDING_BLOCKS, f.slab().get(), base);
                if (f.wall() != null) {
                    wall(out, RecipeCategory.DECORATIONS, f.wall().get(), base);
                }
                stonecut(out, RecipeCategory.BUILDING_BLOCKS, f.stairs().get(), base);
                stonecut(out, RecipeCategory.BUILDING_BLOCKS, f.slab().get(), base, 2);
                if (f.wall() != null) {
                    stonecut(out, RecipeCategory.DECORATIONS, f.wall().get(), base);
                }
            }
            polished(out, RecipeCategory.BUILDING_BLOCKS, pol.base().get(), an.base().get());
            polished(out, RecipeCategory.BUILDING_BLOCKS, bri.base().get(), pol.base().get());
            stonecut(out, RecipeCategory.BUILDING_BLOCKS, pol.base().get(), an.base().get());
            stonecut(out, RecipeCategory.BUILDING_BLOCKS, bri.base().get(), an.base().get());
            stonecut(out, RecipeCategory.BUILDING_BLOCKS, bri.base().get(), pol.base().get());
            // derived shapes straight from raw anorthosite in the stonecutter
            stonecut(out, RecipeCategory.BUILDING_BLOCKS, pol.stairs().get(), an.base().get());
            stonecut(out, RecipeCategory.BUILDING_BLOCKS, pol.slab().get(), an.base().get(), 2);
            stonecut(out, RecipeCategory.BUILDING_BLOCKS, bri.stairs().get(), an.base().get());
            stonecut(out, RecipeCategory.BUILDING_BLOCKS, bri.slab().get(), an.base().get(), 2);
            stonecut(out, RecipeCategory.DECORATIONS, bri.wall().get(), an.base().get());

            // sintering: regolith heated into a hard ceramic, the first building material of a lunar base
            for (ItemLike soil : List.<ItemLike>of(MoonModule.LUNAR_REGOLITH.get(), MoonModule.MARE_REGOLITH.get())) {
                SimpleCookingRecipeBuilder.smelting(Ingredient.of(soil), RecipeCategory.BUILDING_BLOCKS, MoonModule.SINTERED_REGOLITH.get(), 0.1F, 200)
                        .unlockedBy(getHasName(soil), has(soil))
                        .save(out, Spacebreaker.id("sintered_regolith_from_" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(soil.asItem()).getPath()));
            }
            polished(out, RecipeCategory.BUILDING_BLOCKS, sin.base().get(), MoonModule.SINTERED_REGOLITH.get());
            stonecut(out, RecipeCategory.BUILDING_BLOCKS, sin.base().get(), MoonModule.SINTERED_REGOLITH.get());

            // meteoric iron: almost pure iron-nickel
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.RAW_IRON, 4).requires(MoonModule.METEORIC_IRON.get())
                    .unlockedBy(getHasName(MoonModule.METEORIC_IRON.get()), has(MoonModule.METEORIC_IRON.get()))
                    .save(out, Spacebreaker.id("raw_iron_from_meteoric_iron"));
            // drying icy regolith gives plain regolith back
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(MoonModule.ICY_REGOLITH.get()), RecipeCategory.BUILDING_BLOCKS,
                            MoonModule.LUNAR_REGOLITH.get(), 0.05F, 100)
                    .unlockedBy(getHasName(MoonModule.ICY_REGOLITH.get()), has(MoonModule.ICY_REGOLITH.get()))
                    .save(out, Spacebreaker.id("dry_lunar_regolith"));
        }
    }

    private MoonData() {
    }
}
