package com.pizzaparker07.spacebreaker.content.belt;

import com.pizzaparker07.spacebreaker.content.belt.block.CometJetBlock;
import com.pizzaparker07.spacebreaker.content.worldgen.Landforms;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.Optional;

import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.cf;
import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.pf;
import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.s;

/**
 * Comets, each a small package on the shared {@link TinyWorld} base, sharing the comet jet block. 67P/Churyumov-Gerasimenko: a dark, two-lobed
 * nucleus a few kilometres long, its ice hidden under a crust of dust darker than charcoal, with jets of gas and dust
 * rising from the sunlit ground (visual only). Encke: small, old and nearly worn out, dusty and hummocky. Halley: a long,
 * coal-black nucleus with strong jets. Tempel 1: a small nucleus with smooth, flat plains ending at low scarps. Hale-Bopp:
 * a giant nucleus some 60 km across, rugged and deeply cold, far out on a 2500-year orbit. Their ice is water ice: refuel
 * stops.
 */
public final class Comets {
    public static final TinyWorld CHURYUMOV = new TinyWorld(BeltModule.KIT, "churyumov_gerasimenko", "comet_dust", MapColor.COLOR_BLACK,
            "comet_ice", MapColor.DEEPSLATE, true);
    /** Encke: small, old and nearly worn out, its short orbit baking it every 3.3 years; a dusty, hummocky surface. */
    public static final TinyWorld ENCKE = new TinyWorld(BeltModule.KIT, "encke", "encke_dust", MapColor.TERRACOTTA_BLACK, "encke_ice",
            MapColor.COLOR_GRAY, true);
    /** Halley: a long, peanut-shaped nucleus blacker than coal, the most famous comet, with strong jets. */
    public static final TinyWorld HALLEY = new TinyWorld(BeltModule.KIT, "halley", "halley_dust", MapColor.COLOR_BLACK, "halley_ice",
            MapColor.DEEPSLATE, true);
    /** Tempel 1: small and dark, with smooth flat plains bounded by low scarps among the rough, pitted ground. */
    public static final TinyWorld TEMPEL = new TinyWorld(BeltModule.KIT, "tempel_1", "tempel_dust", MapColor.TERRACOTTA_GRAY, "tempel_ice",
            MapColor.COLOR_GRAY, true);
    /** Hale-Bopp: a giant nucleus, rugged and bitterly cold, with big boulders. */
    public static final TinyWorld HALE_BOPP = new TinyWorld(BeltModule.KIT, "hale_bopp", "hale_bopp_dust", MapColor.COLOR_BLACK,
            "hale_bopp_ice", MapColor.DEEPSLATE, true);
    /** Where a jet rises when the sun is up. */
    public static final DeferredBlock<CometJetBlock> JET = BeltModule.KIT.block("comet_jet", CometJetBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.8F).sound(SoundType.GRAVEL));

    private Comets() {
    }

    static void load() {
    }

    public static java.util.List<TinyWorld> all() {
        return java.util.List.of(CHURYUMOV, ENCKE, HALLEY, TEMPEL, HALE_BOPP);
    }

    public static TerrainProfile enckeProfile() {
        return ENCKE.profile(2.4, 14, Optional.empty(), Landforms.NONE);
    }

    public static TerrainProfile halleyProfile() {
        return HALLEY.profile(4.4, 28, Optional.empty(), Landforms.NONE);
    }

    public static TerrainProfile tempelProfile() {
        return TEMPEL.profile(2.83, 16, Optional.empty(), Landforms.NONE);
    }

    public static TerrainProfile haleBoppProfile() {
        return HALE_BOPP.profile(30.0, 40, Optional.empty(), Landforms.NONE);
    }

    /**
     * Tempel 1's smooth plain: a broad, flat-floored hollow a few blocks deep with a steep low scarp for an edge, no rim,
     * no central peak, floored with the dust.
     */
    public static com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature.Config tempelPlain() {
        return new com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature.Config(60, 20, 150, 5, 0, 0, 0.1, 1, 0.05, 0,
                s(TEMPEL.rock), Optional.of(s(TEMPEL.soil)), Optional.empty());
    }

    public static TerrainProfile churyumovProfile() {
        return CHURYUMOV.profile(1.65, 22, Optional.empty(), Landforms.NONE);
    }

    static void translations(Lang lang) {
        CHURYUMOV.translations(lang, "67P/Churyumov-Gerasimenko", "67P/Çuryumov-Gerasimenko", "Comet Dust", "Kuyruklu Yıldız Tozu",
                "Comet Ice", "Kuyruklu Yıldız Buzu");
        ENCKE.translations(lang, "Comet Encke", "Encke Kuyruklu Yıldızı", "Encke Dust", "Encke Tozu", "Encke Ice", "Encke Buzu");
        HALLEY.translations(lang, "Halley's Comet", "Halley Kuyruklu Yıldızı", "Halley Dust", "Halley Tozu", "Halley Ice", "Halley Buzu");
        TEMPEL.translations(lang, "Comet Tempel 1", "Tempel 1 Kuyruklu Yıldızı", "Tempel Dust", "Tempel Tozu", "Tempel Ice", "Tempel Buzu");
        HALE_BOPP.translations(lang, "Comet Hale-Bopp", "Hale-Bopp Kuyruklu Yıldızı", "Hale-Bopp Dust", "Hale-Bopp Tozu", "Hale-Bopp Ice",
                "Hale-Bopp Buzu");
        lang.block(JET, "Comet Jet", "Kuyruklu Yıldız Jeti");
    }

    static void loot(SbBlockLoot loot) {
        all().forEach(w -> w.loot(loot));
        loot.self(JET.get());
    }

    static void tags(SbTags tags) {
        all().forEach(w -> w.tags(tags, NearEarthAsteroids.GROUND));
        // comet dust is carbon-rich rubble too: the Regolith Heater's carbonaceous recipe accepts it (the richer own
        // recipe below takes precedence)
        all().forEach(w -> tags.item(com.pizzaparker07.spacebreaker.content.machines.IsruRecipes.CARBONACEOUS_REGOLITHS, w.soil.get().asItem()));
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, JET.get());
    }

    /**
     * Regolith Heater: comet dust is richer than asteroid rubble - more water (ice grains inside the dust), more CO2
     * and now and then a lump of the dark organic residue (tholin) that colours comet nuclei.
     */
    static void recipes(net.minecraft.data.recipes.RecipeOutput out) {
        for (TinyWorld w : all()) {
            com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipeBuilder
                    .of(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.REGOLITH_PROCESSING).in(w.soil.get())
                    .fluidOut(net.minecraft.world.level.material.Fluids.WATER, 300)
                    .fluidOut(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.CARBON_DIOXIDE.get(), 100)
                    .bonus(com.pizzaparker07.spacebreaker.content.saturn.SaturnModule.THOLIN.get(), 1, 0.15F)
                    .time(160).energy(100).priority(3).save(out, w.soil.getId().getPath());
        }
    }

    static void terrain(BootstrapContext<TerrainProfile> ctx) {
        ctx.register(CHURYUMOV.terrainKey(), churyumovProfile());
        ctx.register(ENCKE.terrainKey(), enckeProfile());
        ctx.register(HALLEY.terrainKey(), halleyProfile());
        ctx.register(TEMPEL.terrainKey(), tempelProfile());
        ctx.register(HALE_BOPP.terrainKey(), haleBoppProfile());
    }

    static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        CHURYUMOV.configured(ctx, s(CHURYUMOV.soil), 0.8F, 2.4F);
        ENCKE.configured(ctx, s(ENCKE.soil), 0.6F, 1.6F);
        HALLEY.configured(ctx, s(HALLEY.soil), 0.8F, 2.6F);
        TEMPEL.configured(ctx, s(TEMPEL.soil), 0.6F, 2.0F);
        HALE_BOPP.configured(ctx, s(HALE_BOPP.soil), 1.2F, 3.6F);
        ctx.register(cf("tempel_smooth_plain"), new ConfiguredFeature<>(OuterWorlds.BASIN.get(), tempelPlain()));
        ctx.register(cf("comet_jets"), new ConfiguredFeature<>(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(JET.get()))));
    }

    static void placed(BootstrapContext<PlacedFeature> ctx) {
        CHURYUMOV.placed(ctx, 2);
        ENCKE.placed(ctx, 4);
        HALLEY.placed(ctx, 2);
        TEMPEL.placed(ctx, 3);
        HALE_BOPP.placed(ctx, 1);
        OuterWorlds.placedWhole(ctx, "tempel_smooth_plain");
        // a jet every few chunks, set into the surface
        PlacementUtils.register(ctx, pf("comet_jets"), ctx.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(cf("comet_jets")),
                RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
    }

    static void biomes(BootstrapContext<Biome> ctx, HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        CHURYUMOV.biomes(ctx, pf, carvers, 0x2B2927, GenerationStep.Decoration.SURFACE_STRUCTURES, "comet_jets");
        // Encke is nearly worn out: few jets (the rare ones it has come from the shared placement too)
        ENCKE.biomes(ctx, pf, carvers, 0x3A3532, GenerationStep.Decoration.SURFACE_STRUCTURES, "comet_jets");
        HALLEY.biomes(ctx, pf, carvers, 0x232120, GenerationStep.Decoration.SURFACE_STRUCTURES, "comet_jets");
        TEMPEL.biomes(ctx, pf, carvers, 0x35322F, GenerationStep.Decoration.RAW_GENERATION, "tempel_smooth_plain",
                GenerationStep.Decoration.SURFACE_STRUCTURES, "comet_jets");
        HALE_BOPP.biomes(ctx, pf, carvers, 0x211F1E, GenerationStep.Decoration.SURFACE_STRUCTURES, "comet_jets");
    }
}
