package com.pizzaparker07.spacebreaker.content.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.worldgen.BoulderFeature;
import com.pizzaparker07.spacebreaker.content.worldgen.WorldgenModule;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.AmbientParticleSettings;
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
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Data of the inner planets' worlds: biomes, features, structures (datagen) and recipes. Dimensions, dimension types
 * and terrain profiles are plain JSON in {@code src/main/resources}.
 */
public final class MarsData {
    // ------------------------------------------------------------------ Mars biomes
    public static final ResourceKey<Biome> HIGHLANDS = biome("mars_highlands");
    public static final ResourceKey<Biome> PLAINS = biome("mars_plains");
    public static final ResourceKey<Biome> CRATER = biome("mars_crater");
    public static final ResourceKey<Biome> POLAR = biome("mars_polar");
    public static final ResourceKey<Biome> DUNES = biome("mars_dunes");
    public static final ResourceKey<Biome> CANYON = biome("mars_canyon");
    public static final ResourceKey<Biome> VOLCANO = biome("mars_volcano");
    public static final ResourceKey<Biome> VALLEY = biome("mars_valley");
    public static final ResourceKey<Biome> ICE_CAP = biome("mars_ice_cap");
    public static final ResourceKey<Biome> PHOBOS_SURFACE = biome("phobos_surface");
    public static final ResourceKey<Biome> PHOBOS_CRATER = biome("phobos_crater");
    public static final ResourceKey<Biome> DEIMOS_SURFACE = biome("deimos_surface");
    public static final List<ResourceKey<Biome>> MARS_BIOMES = List.of(HIGHLANDS, PLAINS, CRATER, POLAR, DUNES, CANYON, VOLCANO, VALLEY, ICE_CAP);

    static final Map<String, ResourceKey<PlacedFeature>> PF = new LinkedHashMap<>();

    /** Extra bootstrap steps (other worlds of this area: Phobos, Deimos, Venus, Mercury). */
    public static final List<Consumer<RegistrySetBuilder>> EXTRA = new ArrayList<>();
    /** Creature spawns per biome, filled by the creature code before datagen runs. */
    public static final List<BiomeSpawns> SPAWNS = new ArrayList<>();
    /** Extra biome builders of the other worlds of this area, run in the same biome bootstrap. */
    public static final List<Consumer<BootstrapContext<Biome>>> EXTRA_BIOMES = new ArrayList<>();
    public static final List<Consumer<BootstrapContext<ConfiguredFeature<?, ?>>>> EXTRA_CONFIGURED = new ArrayList<>();
    public static final List<Consumer<BootstrapContext<PlacedFeature>>> EXTRA_PLACED = new ArrayList<>();
    public static final List<Consumer<BootstrapContext<net.minecraft.world.level.levelgen.structure.Structure>>> EXTRA_STRUCTURES = new ArrayList<>();
    public static final List<Consumer<BootstrapContext<net.minecraft.world.level.levelgen.structure.StructureSet>>> EXTRA_SETS = new ArrayList<>();
    /** Extra recipes of the other worlds of this area. */
    public static final List<Consumer<RecipeOutput>> EXTRA_RECIPES = new ArrayList<>();

    public record BiomeSpawns(ResourceKey<Biome> biome, Consumer<MobSpawnSettings.Builder> spawns) {
    }

    /** Spawn settings collected in {@link #SPAWNS} for a biome. */
    public static MobSpawnSettings spawnsFor(ResourceKey<Biome> key) {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder().creatureGenerationProbability(0.05F);
        for (BiomeSpawns s : SPAWNS) {
            if (s.biome() == key) {
                s.spawns().accept(spawns);
            }
        }
        return spawns.build();
    }

    public static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, Spacebreaker.id(name));
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> cf(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Spacebreaker.id(name));
    }

    public static ResourceKey<PlacedFeature> pf(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Spacebreaker.id(name));
    }

    static void register(IEventBus modBus) {
        modBus.addListener(GatherDataEvent.class, MarsData::gather);
    }

    static void translations(Lang lang) {
        lang.add("biome.spacebreaker.mars_highlands", "Cratered Highlands", "Kraterli Yaylalar");
        lang.add("biome.spacebreaker.mars_plains", "Northern Plains", "Kuzey Ovaları");
        lang.add("biome.spacebreaker.mars_crater", "Martian Crater", "Mars Krateri");
        lang.add("biome.spacebreaker.mars_polar", "Polar Layered Deposits", "Kutup Katmanlı Tortulları");
        lang.add("biome.spacebreaker.mars_dunes", "Dune Sea", "Kumul Denizi");
        lang.add("biome.spacebreaker.mars_canyon", "Great Rift Canyon", "Büyük Yarık Kanyonu");
        lang.add("biome.spacebreaker.mars_volcano", "Shield Volcano", "Kalkan Volkan");
        lang.add("biome.spacebreaker.mars_valley", "Ancient River Valley", "Antik Nehir Vadisi");
        lang.add("biome.spacebreaker.mars_ice_cap", "Polar Ice Cap", "Kutup Buz Örtüsü");
        lang.add("biome.spacebreaker.phobos_surface", "Grooved Plains of Phobos", "Phobos'un Oluklu Ovaları");
        lang.add("biome.spacebreaker.phobos_crater", "Phobos Crater", "Phobos Krateri");
        lang.add("biome.spacebreaker.deimos_surface", "Dusty Plains of Deimos", "Deimos'un Tozlu Ovaları");
        lang.add("dungeon.spacebreaker.mars_research_station", "Abandoned Mars Research Station", "Terk Edilmiş Mars Araştırma İstasyonu");
        lang.add("dungeon.spacebreaker.mars_riverbed_cave", "Ancient Buried Riverbed", "Antik Gömülü Nehir Yatağı");
        lang.add("structure.spacebreaker.mars_research_station", "Abandoned Mars Research Station", "Terk Edilmiş Mars Araştırma İstasyonu");
        lang.add("dungeon.spacebreaker.phobos_probe_wreck", "Crashed Sample-Return Probe", "Düşmüş Numune Dönüş Sondası");
        lang.add("dungeon.spacebreaker.deimos_ice_cache", "Deimos Ice Cache", "Deimos Buz Deposu");
        lang.add("structure.spacebreaker.phobos_probe_wreck", "Crashed Sample-Return Probe", "Düşmüş Numune Dönüş Sondası");
        lang.add("structure.spacebreaker.deimos_ice_cache", "Deimos Ice Cache", "Deimos Buz Deposu");
        lang.add("structure.spacebreaker.mars_riverbed_cave", "Ancient Buried Riverbed", "Antik Gömülü Nehir Yatağı");
        tech(lang, "dust_proof_seals", "Dust-Proof Seals", "Martian dust is finer than flour and full of salts: labyrinth seals and electrostatic wipers keep joints, airlocks and bearings working.",
                "Toz Geçirmez Contalar", "Mars tozu undan incedir ve tuzla doludur: labirent contalar ve elektrostatik silecekler eklemleri, hava kilitlerini ve yatakları çalışır tutar.");
        tech(lang, "heat_resistant_electronics", "Heat-Resistant Electronics", "Silicon carbide chips and ceramic boards that keep thinking at 460 °C, on Venus' surface or Mercury's day side.",
                "Isıya Dayanıklı Elektronik", "460 °C'de bile çalışmayı sürdüren silisyum karbür yongalar ve seramik kartlar: Venüs yüzeyi ya da Merkür'ün gündüz yüzü için.");
        tech(lang, "acid_resistant_coatings", "Acid-Resistant Coatings", "Fluoropolymer skins and gold-plated contacts that survive the sulfuric-acid clouds of Venus.",
                "Aside Dayanıklı Kaplamalar", "Venüs'ün sülfürik asit bulutlarına dayanan floropolimer kaplamalar ve altın kaplı kontaklar.");
        tech(lang, "polar_ice_mining", "Polar Ice Mining", "Heated drills for the permanently shadowed craters, where ice has waited in the cold for billions of years.",
                "Kutup Buzu Madenciliği", "Buzun milyarlarca yıldır soğukta beklediği kalıcı gölgeli kraterler için ısıtmalı matkaplar.");
    }

    static void tech(Lang lang, String id, String en, String enDesc, String tr, String trDesc) {
        lang.add("tech.spacebreaker." + id, en, tr);
        lang.add("tech.spacebreaker." + id + ".desc", enDesc, trDesc);
    }

    // ------------------------------------------------------------------ datagen

    private static void gather(GatherDataEvent event) {
        PackOutput out = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        RegistrySetBuilder builder = new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, ctx -> {
                    configuredFeatures(ctx);
                    EXTRA_CONFIGURED.forEach(c -> c.accept(ctx));
                })
                .add(Registries.PLACED_FEATURE, ctx -> {
                    placedFeatures(ctx);
                    EXTRA_PLACED.forEach(c -> c.accept(ctx));
                })
                .add(Registries.BIOME, ctx -> {
                    biomes(ctx);
                    EXTRA_BIOMES.forEach(c -> c.accept(ctx));
                })
                .add(Registries.STRUCTURE, ctx -> EXTRA_STRUCTURES.forEach(c -> c.accept(ctx)))
                .add(Registries.STRUCTURE_SET, ctx -> EXTRA_SETS.forEach(c -> c.accept(ctx)));
        EXTRA.forEach(c -> c.accept(builder));
        event.getGenerator().addProvider(event.includeServer(),
                new Named("Spacebreaker inner planets worldgen", new DatapackBuiltinEntriesProvider(out, lookup, builder, Set.of(Spacebreaker.MOD_ID))));
        event.getGenerator().addProvider(event.includeServer(), new Named("Spacebreaker inner planets recipes", new Recipes(out, lookup)));
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

    public static BlockState s(DeferredBlock<? extends Block> b) {
        return b.get().defaultBlockState();
    }

    /** Boulder of mixed rocks. */
    public static ConfiguredFeature<?, ?> boulder(float min, float max, float buried, Object... weighted) {
        SimpleWeightedRandomList.Builder<BlockState> list = SimpleWeightedRandomList.builder();
        for (int i = 0; i < weighted.length; i += 2) {
            list.add((BlockState) weighted[i], (Integer) weighted[i + 1]);
        }
        return new ConfiguredFeature<>(WorldgenModule.BOULDER.get(), new BoulderFeature.Config(new WeightedStateProvider(list),
                UniformFloat.of(min, max), buried));
    }

    private static void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        BlockState basalt = s(MarsModule.MARTIAN_BASALT.base());
        BlockState sandstone = s(MarsModule.MARTIAN_SANDSTONE.base());
        BlockState mudstone = s(MarsModule.MARTIAN_MUDSTONE);
        ctx.register(cf("mars_boulder"), boulder(0.8F, 1.9F, 0.45F, basalt, 6, mudstone, 1));
        ctx.register(cf("mars_boulder_sediment"), boulder(0.8F, 1.7F, 0.5F, sandstone, 4, mudstone, 2));
        ctx.register(cf("mars_boulder_large"), boulder(2.0F, 3.4F, 0.4F, basalt, 3, sandstone, 2));
        ctx.register(cf("mars_meteorite"), boulder(0.6F, 1.1F, 0.6F, MarsModule.meteorite().defaultBlockState(), 1));
        ctx.register(cf("moonlet_boulder"), boulder(0.7F, 1.6F, 0.5F, s(MarsModule.CARBONACEOUS_CHONDRITE), 1));
        ctx.register(cf("mars_ice_boulder"), boulder(0.8F, 1.6F, 0.5F, s(MarsModule.DRY_ICE), 2, s(MarsModule.MARTIAN_ICE), 3));

        RuleTest stone = new TagMatchTest(MarsModule.MARS_STONE);
        RuleTest sediment = new BlockMatchTest(MarsModule.MARTIAN_SANDSTONE.base().get());
        RuleTest mud = new BlockMatchTest(MarsModule.MARTIAN_MUDSTONE.get());
        ctx.register(cf("mars_ore_hematite"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(stone, s(MarsModule.HEMATITE_ORE), 8)));
        ctx.register(cf("mars_ore_gypsum"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(List.of(
                OreConfiguration.target(mud, s(MarsModule.GYPSUM_ORE)), OreConfiguration.target(sediment, s(MarsModule.GYPSUM_ORE))), 10)));
        ctx.register(cf("mars_ice_vein"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(stone, s(MarsModule.MARTIAN_ICE), 24)));
        ctx.register(cf("mars_clay_lens"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(stone, s(MarsModule.MARTIAN_CLAY), 30)));
        ctx.register(cf("mars_buried_iron"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(stone, MarsModule.meteorite().defaultBlockState(), 3)));
    }

    private static void placed(BootstrapContext<PlacedFeature> ctx, String name, String feature,
                               net.minecraft.world.level.levelgen.placement.PlacementModifier... modifiers) {
        ResourceKey<PlacedFeature> key = pf(name);
        PF.put(name, key);
        PlacementUtils.register(ctx, key, ctx.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(cf(feature)), modifiers);
    }

    private static void placedFeatures(BootstrapContext<PlacedFeature> ctx) {
        var top = PlacementUtils.HEIGHTMAP_TOP_SOLID;
        placed(ctx, "mars_boulder", "mars_boulder", RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(), top, BiomeFilter.biome());
        placed(ctx, "mars_boulder_rare", "mars_boulder", RarityFilter.onAverageOnceEvery(9), InSquarePlacement.spread(), top, BiomeFilter.biome());
        placed(ctx, "mars_boulder_sediment", "mars_boulder_sediment", RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(), top, BiomeFilter.biome());
        placed(ctx, "mars_boulder_large", "mars_boulder_large", RarityFilter.onAverageOnceEvery(4), InSquarePlacement.spread(), top, BiomeFilter.biome());
        placed(ctx, "mars_meteorite", "mars_meteorite", RarityFilter.onAverageOnceEvery(70), InSquarePlacement.spread(), top, BiomeFilter.biome());
        placed(ctx, "mars_ice_boulder", "mars_ice_boulder", RarityFilter.onAverageOnceEvery(5), InSquarePlacement.spread(), top, BiomeFilter.biome());
        placed(ctx, "moonlet_boulder", "moonlet_boulder", RarityFilter.onAverageOnceEvery(4), InSquarePlacement.spread(), top, BiomeFilter.biome());
        placed(ctx, "mars_ore_hematite", "mars_ore_hematite", CountPlacement.of(6), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-40), VerticalAnchor.absolute(140)), BiomeFilter.biome());
        placed(ctx, "mars_ore_hematite_rich", "mars_ore_hematite", CountPlacement.of(14), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(20), VerticalAnchor.absolute(140)), BiomeFilter.biome());
        placed(ctx, "mars_ore_gypsum", "mars_ore_gypsum", CountPlacement.of(5), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(160)), BiomeFilter.biome());
        placed(ctx, "mars_ice_vein", "mars_ice_vein", CountPlacement.of(8), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(10), VerticalAnchor.absolute(160)), BiomeFilter.biome());
        placed(ctx, "mars_clay_lens", "mars_clay_lens", CountPlacement.of(3), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(30), VerticalAnchor.absolute(120)), BiomeFilter.biome());
        placed(ctx, "mars_buried_iron", "mars_buried_iron", CountPlacement.of(1), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-60), VerticalAnchor.absolute(100)), BiomeFilter.biome());
    }

    /** Features in one fixed order: every biome must list shared features in the same order (vanilla rule). */
    private static final List<String> ORE_ORDER = List.of("mars_clay_lens", "mars_ore_hematite", "mars_ore_hematite_rich", "mars_ore_gypsum",
            "mars_ice_vein", "mars_buried_iron");
    private static final List<String> SURFACE_ORDER = List.of("mars_boulder_large", "mars_boulder", "mars_boulder_rare", "mars_boulder_sediment",
            "mars_ice_boulder", "mars_meteorite", "moonlet_boulder");

    private static Consumer<BiomeGenerationSettings.Builder> features(HolderGetter<PlacedFeature> pf, String... wanted) {
        Set<String> w = Set.of(wanted);
        return g -> {
            ORE_ORDER.stream().filter(w::contains).forEach(k -> g.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, pf.getOrThrow(pf(k))));
            SURFACE_ORDER.stream().filter(w::contains).forEach(k -> g.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, pf.getOrThrow(pf(k))));
        };
    }

    private static void biomes(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> pf = ctx.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
        ctx.register(HIGHLANDS, marsBiome(pf, carvers, HIGHLANDS, -0.6F, features(pf, "mars_ore_hematite", "mars_ore_gypsum", "mars_buried_iron",
                "mars_boulder", "mars_meteorite")));
        ctx.register(PLAINS, marsBiome(pf, carvers, PLAINS, -0.6F, features(pf, "mars_ore_hematite_rich", "mars_ice_vein", "mars_boulder_rare",
                "mars_meteorite")));
        ctx.register(CRATER, marsBiome(pf, carvers, CRATER, -0.6F, features(pf, "mars_ore_hematite", "mars_ore_gypsum", "mars_buried_iron",
                "mars_boulder_large", "mars_boulder", "mars_meteorite")));
        ctx.register(POLAR, marsBiome(pf, carvers, POLAR, -1.2F, features(pf, "mars_ore_hematite", "mars_ice_vein", "mars_boulder_rare",
                "mars_ice_boulder")));
        ctx.register(DUNES, marsBiome(pf, carvers, DUNES, -0.6F, features(pf, "mars_ore_hematite")));
        ctx.register(CANYON, marsBiome(pf, carvers, CANYON, -0.5F, features(pf, "mars_clay_lens", "mars_ore_hematite", "mars_ore_gypsum",
                "mars_boulder_large", "mars_boulder_sediment")));
        ctx.register(VOLCANO, marsBiome(pf, carvers, VOLCANO, -0.9F, features(pf, "mars_ore_hematite", "mars_boulder")));
        ctx.register(VALLEY, marsBiome(pf, carvers, VALLEY, -0.6F, features(pf, "mars_clay_lens", "mars_ore_gypsum", "mars_ice_vein",
                "mars_boulder_sediment", "mars_meteorite")));
        ctx.register(ICE_CAP, marsBiome(pf, carvers, ICE_CAP, -1.5F, features(pf, "mars_ice_vein", "mars_ice_boulder")));
        ctx.register(PHOBOS_SURFACE, airless(pf, carvers, features(pf, "mars_buried_iron", "moonlet_boulder")));
        ctx.register(PHOBOS_CRATER, airless(pf, carvers, features(pf, "mars_buried_iron", "moonlet_boulder", "mars_meteorite")));
        ctx.register(DEIMOS_SURFACE, airless(pf, carvers, features(pf, "mars_buried_iron")));
    }

    /** Biome of an airless little moon: black sky, no weather, no life. */
    public static Biome airless(HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers,
                                Consumer<BiomeGenerationSettings.Builder> features) {
        BiomeGenerationSettings.Builder gen = new BiomeGenerationSettings.Builder(pf, carvers);
        features.accept(gen);
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false).temperature(0.5F).downfall(0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .fogColor(0x000000).skyColor(0x000000).waterColor(0x3F76E4).waterFogColor(0x050533)
                        .grassColorOverride(0x6A6460).foliageColorOverride(0x6A6460).build())
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(gen.build())
                .build();
    }

    /** Daytime sky colours (shader packs read these): butterscotch dust haze. */
    public static final int MARS_SKY = 0xC9A27A;
    public static final int MARS_FOG = 0xB88E68;

    private static Biome marsBiome(HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers, ResourceKey<Biome> key,
                                   float temperature, Consumer<BiomeGenerationSettings.Builder> features) {
        BiomeGenerationSettings.Builder gen = new BiomeGenerationSettings.Builder(pf, carvers);
        features.accept(gen);
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder().creatureGenerationProbability(0.05F);
        for (BiomeSpawns s : SPAWNS) {
            if (s.biome() == key) {
                s.spawns().accept(spawns);
            }
        }
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(temperature)
                .downfall(0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .fogColor(MARS_FOG).skyColor(MARS_SKY).waterColor(0x4F6A7A).waterFogColor(0x2A3036)
                        .grassColorOverride(0x9A6A48).foliageColorOverride(0x9A6A48)
                        .ambientParticle(new AmbientParticleSettings(MarsParticles.DUST_MOTE.get(), 0.0015F))
                        .build())
                .mobSpawnSettings(spawns.build())
                .generationSettings(gen.build())
                .build();
    }

    // ------------------------------------------------------------------ recipes

    static final class Recipes extends RecipeProvider {
        Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        static void stonecut(RecipeOutput out, RecipeCategory category, ItemLike result, ItemLike material, int count) {
            net.minecraft.data.recipes.SingleItemRecipeBuilder.stonecutting(Ingredient.of(material), category, result, count)
                    .unlockedBy(getHasName(material), has(material))
                    .save(out, Spacebreaker.id(getConversionRecipeName(result, material) + "_stonecutting"));
        }

        /** Stairs, slab and wall recipes (crafting and stonecutter) for every family of a kit. */
        static void families(RecipeOutput out, BlockKit kit) {
            for (BlockKit.Family f : kit.families()) {
                ItemLike base = f.base().get();
                stairBuilder(f.stairs().get(), Ingredient.of(base)).unlockedBy(getHasName(base), has(base)).save(out);
                slab(out, RecipeCategory.BUILDING_BLOCKS, f.slab().get(), base);
                stonecut(out, RecipeCategory.BUILDING_BLOCKS, f.stairs().get(), base, 1);
                stonecut(out, RecipeCategory.BUILDING_BLOCKS, f.slab().get(), base, 2);
                if (f.wall() != null) {
                    wall(out, RecipeCategory.DECORATIONS, f.wall().get(), base);
                    stonecut(out, RecipeCategory.DECORATIONS, f.wall().get(), base, 1);
                }
            }
        }

        static void smeltOre(RecipeOutput out, ItemLike input, ItemLike result, float xp, String name) {
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, result, xp, 200)
                    .unlockedBy(getHasName(input), has(input)).save(out, Spacebreaker.id(name + "_smelting"));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, result, xp, 100)
                    .unlockedBy(getHasName(input), has(input)).save(out, Spacebreaker.id(name + "_blasting"));
        }

        @Override
        protected void buildRecipes(RecipeOutput out) {
            families(out, MarsModule.KIT);
            BlockKit.Family basalt = MarsModule.MARTIAN_BASALT;
            BlockKit.Family pol = MarsModule.POLISHED_MARTIAN_BASALT;
            BlockKit.Family bri = MarsModule.MARTIAN_BASALT_BRICKS;
            polished(out, RecipeCategory.BUILDING_BLOCKS, pol.base().get(), basalt.base().get());
            polished(out, RecipeCategory.BUILDING_BLOCKS, bri.base().get(), pol.base().get());
            stonecut(out, RecipeCategory.BUILDING_BLOCKS, pol.base().get(), basalt.base().get(), 1);
            stonecut(out, RecipeCategory.BUILDING_BLOCKS, bri.base().get(), basalt.base().get(), 1);
            stonecut(out, RecipeCategory.BUILDING_BLOCKS, bri.base().get(), pol.base().get(), 1);
            // sand packs into sandstone, like vanilla
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MarsModule.MARTIAN_SANDSTONE.base().get())
                    .define('#', MarsModule.MARTIAN_SAND.get()).pattern("##").pattern("##")
                    .unlockedBy(getHasName(MarsModule.MARTIAN_SAND.get()), has(MarsModule.MARTIAN_SAND.get())).save(out);
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MarsModule.GYPSUM_BLOCK.get())
                    .define('#', MarsModule.GYPSUM_CRYSTAL.get()).pattern("##").pattern("##")
                    .unlockedBy(getHasName(MarsModule.GYPSUM_CRYSTAL.get()), has(MarsModule.GYPSUM_CRYSTAL.get())).save(out);
            // hematite is iron oxide: smelt it into iron
            smeltOre(out, MarsModule.RAW_HEMATITE.get(), Items.IRON_INGOT, 0.7F, "iron_ingot_from_raw_hematite");
            smeltOre(out, MarsModule.HEMATITE_ORE.get(), Items.IRON_INGOT, 0.7F, "iron_ingot_from_hematite_ore");
            EXTRA_RECIPES.forEach(c -> c.accept(out));
        }
    }

    static String path(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    private MarsData() {
    }
}
