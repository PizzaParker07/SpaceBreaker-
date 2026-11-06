package com.pizzaparker07.spacebreaker.content.belt;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.dungeon.BeltStructures;
import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.CraterFloorFeature;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;
import java.util.Optional;

import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.cf;
import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.pf;
import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.s;

/** Ceres and Vesta: terrain profiles, biomes and features (datagen). */
public final class BeltData {
    public static final ResourceKey<Biome> CERES_PLAINS = OuterWorlds.biome("ceres_plains");
    public static final ResourceKey<Biome> CERES_CRATER = OuterWorlds.biome("ceres_crater");
    public static final ResourceKey<Biome> CERES_DOME = OuterWorlds.biome("ceres_dome");
    public static final ResourceKey<Biome> CERES_POLAR = OuterWorlds.biome("ceres_polar");
    public static final ResourceKey<Biome> VESTA_HIGHLANDS = OuterWorlds.biome("vesta_highlands");
    public static final ResourceKey<Biome> VESTA_CRATER = OuterWorlds.biome("vesta_crater");
    public static final ResourceKey<Biome> VESTA_POLAR = OuterWorlds.biome("vesta_polar");
    public static final List<ResourceKey<Biome>> CERES_BIOMES = List.of(CERES_PLAINS, CERES_CRATER, CERES_DOME, CERES_POLAR);
    public static final List<ResourceKey<Biome>> VESTA_BIOMES = List.of(VESTA_HIGHLANDS, VESTA_CRATER, VESTA_POLAR);

    public static final ResourceKey<TerrainProfile> CERES_TERRAIN = ResourceKey.create(TerrainProfile.REGISTRY, Spacebreaker.id("ceres"));
    public static final ResourceKey<TerrainProfile> VESTA_TERRAIN = ResourceKey.create(TerrainProfile.REGISTRY, Spacebreaker.id("vesta"));

    /** Rheasilvia: the south-polar basin of Vesta. */
    public static BasinFeature.Config rheasilvia() {
        return new BasinFeature.Config(0, -90, 1000, 36, 18, 84, 0.22, 4, 0.16, s(BeltModule.DIOGENITE), Optional.empty());
    }

    /** Occator-like salt deposits on Ceres' crater floors. */
    public static CraterFloorFeature.Config faculae() {
        return new CraterFloorFeature.Config(s(BeltModule.CARBONATE_CRUST), 16, 0.4, 0.3, 0.8, 0, 2);
    }

    private BeltData() {
    }

    static void translations(Lang lang) {
        lang.add("biome.spacebreaker.ceres_plains", "Cratered Plains of Ceres", "Ceres'in Kraterli Ovaları");
        lang.add("biome.spacebreaker.ceres_crater", "Cerean Crater", "Ceres Krateri");
        lang.add("biome.spacebreaker.ceres_dome", "Cryovolcanic Dome", "Kriyovolkanik Kubbe");
        lang.add("biome.spacebreaker.ceres_polar", "Cerean Pole", "Ceres Kutbu");
        lang.add("biome.spacebreaker.vesta_highlands", "Vestan Highlands", "Vesta Yaylaları");
        lang.add("biome.spacebreaker.vesta_crater", "Vestan Crater", "Vesta Krateri");
        lang.add("biome.spacebreaker.vesta_polar", "Vestan Pole", "Vesta Kutbu");
        lang.add("dungeon.spacebreaker.ceres_mining_outpost", "Abandoned Ice Mining Outpost", "Terk Edilmiş Buz Madenciliği Karakolu");
        lang.add("structure.spacebreaker.ceres_mining_outpost", "Abandoned Ice Mining Outpost", "Terk Edilmiş Buz Madenciliği Karakolu");
        tech(lang, "microgravity_mining", "Microgravity Mining",
                "On a world where a hard swing throws you off the ground, drills need anchors: harpoons, screw piles and counter-rotating cutters that push nothing away.",
                "Mikro Yerçekiminde Madencilik",
                "Sert bir vuruşun seni yerden fırlattığı bir dünyada matkapların çapaya ihtiyacı vardır: zıpkınlar, vidalı kazıklar ve hiçbir şeyi itmeyen ters dönen kesiciler.");
        tech(lang, "brine_chemistry", "Brine Chemistry",
                "Salts left behind by drying brines (sodium carbonate, ammonium chloride) are feedstock for soils, glass and coolants far from Earth.",
                "Tuzlu Su Kimyası",
                "Kuruyan tuzlu sulardan geriye kalan tuzlar (sodyum karbonat, amonyum klorür), Dünya'dan çok uzakta toprak, cam ve soğutucu için hammaddedir.");
    }

    static void tech(Lang lang, String id, String en, String enDesc, String tr, String trDesc) {
        lang.add("tech.spacebreaker." + id, en, tr);
        lang.add("tech.spacebreaker." + id + ".desc", enDesc, trDesc);
    }

    // ------------------------------------------------------------------ terrain

    public static TerrainProfile ceresProfile() {
        var m = new SmallBodyTerrain.Materials(s(BeltModule.CERES_REGOLITH), s(BeltModule.CEREAN_ROCK.base()), s(BeltModule.AMMONIATED_CLAY),
                s(BeltModule.AMMONIATED_CLAY), Optional.of(s(BeltModule.CERES_REGOLITH)), Optional.of(s(BeltModule.CEREAN_ICE)));
        // Ahuna Mons-like domes: steep, flat-topped piles of salty ice that rose through the crust
        var domes = new Landforms.Volcanoes(1800, 0.35, 45, 80, 0.6, 46, 0.02, 0, List.of(), s(BeltModule.CEREAN_ROCK.base()),
                Optional.of(s(BeltModule.CARBONATE_CRUST)));
        var lf = new Landforms(Optional.empty(), Optional.empty(), Optional.of(domes), Optional.empty(), Optional.empty(), Optional.empty(),
                List.of(new Landforms.SoilPatch(s(BeltModule.AMMONIATED_CLAY), 240, 0.66, "any")),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        return SmallBodyTerrain.of(Spacebreaker.id("ceres"), 469.7).baseHeight(72).roughness(0.9).craters(1.0).age(0.8)
                .iceLatitude(76).materials(m).landforms(lf).build();
    }

    public static TerrainProfile vestaProfile() {
        var m = new SmallBodyTerrain.Materials(s(BeltModule.VESTAN_REGOLITH), s(BeltModule.EUCRITE.base()), s(BeltModule.DIOGENITE),
                s(BeltModule.VESTAN_REGOLITH), Optional.empty(), Optional.empty());
        // dark carbon-rich material splashed in by old impacts
        var lf = new Landforms(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                List.of(new Landforms.SoilPatch(s(MarsModule.CARBONACEOUS_CHONDRITE), 170, 0.8, "any")),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        return SmallBodyTerrain.of(Spacebreaker.id("vesta"), 262.7).baseHeight(74).roughness(1.05).craters(1.15).age(1.0)
                .materials(m).landforms(lf).build();
    }

    // ------------------------------------------------------------------ datagen

    static void datapack(SbDatapack dp) {
        dp.add(TerrainProfile.REGISTRY, ctx -> {
            ctx.register(CERES_TERRAIN, ceresProfile());
            ctx.register(VESTA_TERRAIN, vestaProfile());
        });
        dp.add(Registries.CONFIGURED_FEATURE, BeltData::configured);
        dp.add(Registries.PLACED_FEATURE, BeltData::placed);
        dp.add(Registries.BIOME, BeltData::biomes);
        dp.add(Registries.STRUCTURE, BeltStructures::structures);
        dp.add(Registries.STRUCTURE_SET, BeltStructures::sets);
    }

    private static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        var stone = new TagMatchTest(BeltModule.BELT_STONE);
        ctx.register(cf("ceres_faculae"), new ConfiguredFeature<>(OuterWorlds.CRATER_FLOOR.get(), faculae()));
        ctx.register(cf("ceres_magnetite"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(stone, s(BeltModule.MAGNETITE_ORE), 8)));
        ctx.register(cf("ceres_ice_lens"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(stone, s(BeltModule.CEREAN_ICE), 40)));
        ctx.register(cf("ceres_clay_lens"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(stone, s(BeltModule.AMMONIATED_CLAY), 32)));
        ctx.register(cf("ceres_boulder"), OuterWorlds.boulder(0.7F, 1.8F, 0.5F, s(BeltModule.CEREAN_ROCK.base()), 5, s(BeltModule.CEREAN_ICE), 1));
        ctx.register(cf("vesta_rheasilvia"), new ConfiguredFeature<>(OuterWorlds.BASIN.get(), rheasilvia()));
        ctx.register(cf("vesta_diogenite_mass"), new ConfiguredFeature<>(Feature.ORE,
                new OreConfiguration(new BlockMatchTest(BeltModule.EUCRITE.base().get()), s(BeltModule.DIOGENITE), 56)));
        ctx.register(cf("vesta_chromite"), new ConfiguredFeature<>(Feature.ORE,
                new OreConfiguration(new BlockMatchTest(BeltModule.DIOGENITE.get()), s(BeltModule.CHROMITE_ORE), 7)));
        ctx.register(cf("vesta_boulder"), OuterWorlds.boulder(0.8F, 2.2F, 0.45F, s(BeltModule.EUCRITE.base()), 5, s(BeltModule.DIOGENITE), 2));
    }

    private static void placed(BootstrapContext<PlacedFeature> ctx) {
        var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        PlacementUtils.register(ctx, pf("ceres_faculae"), cf.getOrThrow(cf("ceres_faculae")), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("ceres_magnetite"), cf.getOrThrow(cf("ceres_magnetite")), CountPlacement.of(9), InSquarePlacement.spread(),
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-40), VerticalAnchor.absolute(90)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("ceres_ice_lens"), cf.getOrThrow(cf("ceres_ice_lens")), CountPlacement.of(7), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-30), VerticalAnchor.absolute(58)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("ceres_polar_ice"), cf.getOrThrow(cf("ceres_ice_lens")), CountPlacement.of(14), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(20), VerticalAnchor.absolute(80)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("ceres_clay_lens"), cf.getOrThrow(cf("ceres_clay_lens")), CountPlacement.of(5), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-20), VerticalAnchor.absolute(80)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("ceres_boulder"), cf.getOrThrow(cf("ceres_boulder")), RarityFilter.onAverageOnceEvery(3),
                InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("vesta_rheasilvia"), cf.getOrThrow(cf("vesta_rheasilvia")), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("vesta_diogenite_mass"), cf.getOrThrow(cf("vesta_diogenite_mass")), CountPlacement.of(6),
                InSquarePlacement.spread(), HeightRangePlacement.uniform(VerticalAnchor.absolute(-60), VerticalAnchor.absolute(36)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("vesta_chromite"), cf.getOrThrow(cf("vesta_chromite")), CountPlacement.of(10), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-60), VerticalAnchor.absolute(100)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("vesta_boulder"), cf.getOrThrow(cf("vesta_boulder")), RarityFilter.onAverageOnceEvery(2),
                InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
    }

    private static void biomes(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> pf = ctx.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
        var raw = GenerationStep.Decoration.RAW_GENERATION;
        var ores = GenerationStep.Decoration.UNDERGROUND_ORES;
        var local = GenerationStep.Decoration.LOCAL_MODIFICATIONS;
        var ceres = OuterWorlds.Look.airless(0x3E3B38);
        for (ResourceKey<Biome> key : CERES_BIOMES) {
            boolean polar = key == CERES_POLAR;
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -1.2F, ceres,
                    raw, "ceres_faculae", ores, "ceres_clay_lens", ores, "ceres_magnetite", ores, polar ? "ceres_polar_ice" : "ceres_ice_lens",
                    local, "ceres_boulder"));
        }
        var vesta = OuterWorlds.Look.airless(0x6A6660);
        for (ResourceKey<Biome> key : VESTA_BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -0.9F, vesta,
                    raw, "vesta_rheasilvia", ores, "vesta_diogenite_mass", ores, "vesta_chromite", local, "vesta_boulder"));
        }
    }
}
