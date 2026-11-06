package com.pizzaparker07.spacebreaker.content.venus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.content.mars.MarsData;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
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

/** Venus biomes, features and recipes, added to the inner-planet datagen of {@link MarsData}. */
public final class VenusData {
    public static final ResourceKey<Biome> PLAINS = MarsData.biome("venus_plains");
    public static final ResourceKey<Biome> HIGHLANDS = MarsData.biome("venus_tesserae");
    public static final ResourceKey<Biome> VOLCANO = MarsData.biome("venus_volcano");
    public static final ResourceKey<Biome> CRATER = MarsData.biome("venus_crater");
    public static final List<ResourceKey<Biome>> BIOMES = List.of(PLAINS, HIGHLANDS, VOLCANO, CRATER);

    /** Haze colours (shader packs read these). */
    public static final int SKY = 0xD9B36A;
    public static final int FOG = 0xB98A44;

    private VenusData() {
    }

    static void register() {
        MarsData.EXTRA_CONFIGURED.add(VenusData::configured);
        MarsData.EXTRA_PLACED.add(VenusData::placed);
        MarsData.EXTRA_BIOMES.add(VenusData::biomes);
        MarsData.EXTRA_RECIPES.add(VenusData::recipes);
    }

    static void translations(Lang lang) {
        lang.add("biome.spacebreaker.venus_plains", "Volcanic Plains", "Volkanik Ovalar");
        lang.add("biome.spacebreaker.venus_tesserae", "Tessera Highlands", "Tessera Yaylaları");
        lang.add("biome.spacebreaker.venus_volcano", "Venusian Shield Volcano", "Venüs Kalkan Volkanı");
        lang.add("biome.spacebreaker.venus_crater", "Venusian Crater", "Venüs Krateri");
        lang.add("dungeon.spacebreaker.venus_aerostat", "Derelict Research Aerostat", "Terk Edilmiş Araştırma Balonu");
        lang.add("structure.spacebreaker.venus_aerostat", "Derelict Research Aerostat", "Terk Edilmiş Araştırma Balonu");
    }

    private static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(MarsData.cf("venus_boulder"), MarsData.boulder(0.8F, 1.8F, 0.5F, MarsData.s(VenusModule.VENUS_BASALT), 4,
                MarsData.s(VenusModule.TESSERA_ROCK), 1));
        ctx.register(MarsData.cf("venus_sulfur"), new ConfiguredFeature<>(Feature.ORE,
                new OreConfiguration(new TagMatchTest(VenusModule.VENUS_STONE), MarsData.s(VenusModule.SULFUR_CRUST), 12)));
    }

    private static void placed(BootstrapContext<PlacedFeature> ctx) {
        var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        PlacementUtils.register(ctx, MarsData.pf("venus_boulder"), cf.getOrThrow(MarsData.cf("venus_boulder")),
                RarityFilter.onAverageOnceEvery(5), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
        PlacementUtils.register(ctx, MarsData.pf("venus_sulfur"), cf.getOrThrow(MarsData.cf("venus_sulfur")),
                CountPlacement.of(6), InSquarePlacement.spread(), HeightRangePlacement.uniform(VerticalAnchor.absolute(-30), VerticalAnchor.absolute(150)),
                BiomeFilter.biome());
        PlacementUtils.register(ctx, MarsData.pf("venus_sulfur_rich"), cf.getOrThrow(MarsData.cf("venus_sulfur")),
                CountPlacement.of(16), InSquarePlacement.spread(), HeightRangePlacement.uniform(VerticalAnchor.absolute(10), VerticalAnchor.absolute(200)),
                BiomeFilter.biome());
    }

    private static void biomes(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> pf = ctx.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
        ctx.register(PLAINS, biome(pf, carvers, false));
        ctx.register(HIGHLANDS, biome(pf, carvers, false));
        ctx.register(VOLCANO, biome(pf, carvers, true));
        ctx.register(CRATER, biome(pf, carvers, false));
    }

    private static Biome biome(HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers, boolean rich) {
        BiomeGenerationSettings.Builder gen = new BiomeGenerationSettings.Builder(pf, carvers);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, pf.getOrThrow(MarsData.pf(rich ? "venus_sulfur_rich" : "venus_sulfur")));
        gen.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, pf.getOrThrow(MarsData.pf("venus_boulder")));
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false).temperature(2.0F).downfall(0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .fogColor(FOG).skyColor(SKY).waterColor(0x7A6A2E).waterFogColor(0x3A3015)
                        .grassColorOverride(0x8A7040).foliageColorOverride(0x8A7040)
                        .backgroundMusic(new net.minecraft.sounds.Music(com.pizzaparker07.spacebreaker.content.audio.SbSounds.MUSIC_VENUS, 3600, 12000, false))
                        .build())
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(gen.build())
                .build();
    }

    private static void recipes(RecipeOutput out) {
        var has = net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(VenusModule.SULFUR.get());
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, VenusModule.ACID_COATING.get(), 2)
                .requires(EarthModule.PLASTIC_SHEET.get()).requires(VenusModule.SULFUR.get()).requires(Items.HONEYCOMB)
                .unlockedBy("has_sulfur", has).save(out, Spacebreaker.id("acid_coating"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, VenusModule.COMPOSITE_PANEL.get(), 4)
                .define('P', EarthModule.PLASTIC_SHEET.get()).define('A', EarthModule.ALUMINUM_PLATE.get())
                .pattern("PA").pattern("AP")
                .unlockedBy("has_plastic", net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(EarthModule.PLASTIC_SHEET.get()))
                .save(out, Spacebreaker.id("composite_panel"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, VenusModule.AEROGEL.get(), 4)
                .define('G', Items.GLASS).define('S', EarthModule.SILICON.get())
                .pattern(" G ").pattern("GSG").pattern(" G ")
                .unlockedBy("has_silicon", net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(EarthModule.SILICON.get()))
                .save(out, Spacebreaker.id("aerogel"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, VenusModule.ENVELOPE_FABRIC.get(), 4)
                .define('W', Items.WHITE_WOOL).define('P', EarthModule.PLASTIC_SHEET.get())
                .pattern("WW").pattern("WP")
                .unlockedBy("has_plastic", net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(EarthModule.PLASTIC_SHEET.get()))
                .save(out, Spacebreaker.id("envelope_fabric"));
    }
}
