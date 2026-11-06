package com.pizzaparker07.spacebreaker.content.jupiter;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.belt.SmallBodyTerrain;
import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.LakeFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.LayerFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.LineaeFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.VentFeature;
import com.pizzaparker07.spacebreaker.content.jupiter.dungeon.JupiterStructures;
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
import net.minecraft.world.level.block.Blocks;
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
import static com.pizzaparker07.spacebreaker.content.jupiter.JupiterModule.*;

/** The Galilean moons: terrain profiles, biomes and features (datagen). */
public final class JupiterData {
    public static final ResourceKey<Biome> EUROPA_PLAINS = OuterWorlds.biome("europa_ridged_plains");
    public static final ResourceKey<Biome> EUROPA_CRATER = OuterWorlds.biome("europa_crater");
    public static final ResourceKey<Biome> EUROPA_POLAR = OuterWorlds.biome("europa_polar");
    public static final ResourceKey<Biome> IO_PLAINS = OuterWorlds.biome("io_plains");
    public static final ResourceKey<Biome> IO_MOUNTAINS = OuterWorlds.biome("io_mountains");
    public static final ResourceKey<Biome> IO_POLAR = OuterWorlds.biome("io_polar");
    public static final ResourceKey<Biome> GANYMEDE_DARK = OuterWorlds.biome("ganymede_dark_terrain");
    public static final ResourceKey<Biome> GANYMEDE_GROOVED = OuterWorlds.biome("ganymede_grooved_terrain");
    public static final ResourceKey<Biome> GANYMEDE_CRATER = OuterWorlds.biome("ganymede_crater");
    public static final ResourceKey<Biome> GANYMEDE_POLAR = OuterWorlds.biome("ganymede_polar");
    public static final ResourceKey<Biome> CALLISTO_PLAINS = OuterWorlds.biome("callisto_plains");
    public static final ResourceKey<Biome> CALLISTO_CRATER = OuterWorlds.biome("callisto_crater");
    public static final ResourceKey<Biome> CALLISTO_POLAR = OuterWorlds.biome("callisto_polar");
    public static final List<ResourceKey<Biome>> EUROPA_BIOMES = List.of(EUROPA_PLAINS, EUROPA_CRATER, EUROPA_POLAR);
    public static final List<ResourceKey<Biome>> IO_BIOMES = List.of(IO_PLAINS, IO_MOUNTAINS, IO_POLAR);
    public static final List<ResourceKey<Biome>> GANYMEDE_BIOMES = List.of(GANYMEDE_DARK, GANYMEDE_GROOVED, GANYMEDE_CRATER, GANYMEDE_POLAR);
    public static final List<ResourceKey<Biome>> CALLISTO_BIOMES = List.of(CALLISTO_PLAINS, CALLISTO_CRATER, CALLISTO_POLAR);

    /** Top of Europa's buried ocean (the ice shell above it is the rest of the world). */
    public static final int OCEAN_TOP = -52;
    public static final int OCEAN_BOTTOM = -63;

    private JupiterData() {
    }

    static ResourceKey<TerrainProfile> terrain(String name) {
        return ResourceKey.create(TerrainProfile.REGISTRY, Spacebreaker.id(name));
    }

    static void translations(Lang lang) {
        lang.add("biome.spacebreaker.europa_ridged_plains", "Ridged Plains", "Sırtlı Ovalar");
        lang.add("biome.spacebreaker.europa_crater", "Europan Crater", "Europa Krateri");
        lang.add("biome.spacebreaker.europa_polar", "Europan Pole", "Europa Kutbu");
        lang.add("biome.spacebreaker.io_plains", "Sulfur Plains", "Kükürt Ovaları");
        lang.add("biome.spacebreaker.io_mountains", "Ionian Mountains", "Io Dağları");
        lang.add("biome.spacebreaker.io_polar", "Ionian Pole", "Io Kutbu");
        lang.add("biome.spacebreaker.ganymede_dark_terrain", "Dark Ancient Terrain", "Koyu Kadim Arazi");
        lang.add("biome.spacebreaker.ganymede_grooved_terrain", "Bright Grooved Terrain", "Parlak Oluklu Arazi");
        lang.add("biome.spacebreaker.ganymede_crater", "Ganymedean Crater", "Ganymede Krateri");
        lang.add("biome.spacebreaker.ganymede_polar", "Ganymedean Polar Cap", "Ganymede Kutup Örtüsü");
        lang.add("biome.spacebreaker.callisto_plains", "Cratered Plains of Callisto", "Kallisto'nun Kraterli Ovaları");
        lang.add("biome.spacebreaker.callisto_crater", "Callistan Crater", "Kallisto Krateri");
        lang.add("biome.spacebreaker.callisto_polar", "Callistan Pole", "Kallisto Kutbu");
        lang.add("dungeon.spacebreaker.europa_probe", "Buried Research Probe", "Gömülü Araştırma Sondası");
        lang.add("structure.spacebreaker.europa_probe", "Buried Research Probe", "Gömülü Araştırma Sondası");
        lang.add("dungeon.spacebreaker.io_observatory", "Abandoned Volcano Observatory", "Terk Edilmiş Volkan Gözlemevi");
        lang.add("structure.spacebreaker.io_observatory", "Abandoned Volcano Observatory", "Terk Edilmiş Volkan Gözlemevi");
        tech(lang, "cryobot_drilling", "Cryobot Drilling",
                "A heated probe that melts its way down through kilometres of ice, paying out a tether behind it: the way into Europa's hidden ocean.",
                "Kriyobot Sondajı",
                "Arkasında bir kablo bırakarak kilometrelerce buzu eritip aşağı inen ısıtmalı sonda: Europa'nın gizli okyanusuna giden yol.");
        tech(lang, "radiation_hardened_electronics", "Radiation-Hardened Electronics",
                "Shielded vaults, error-correcting memory and wide-gap chips that keep working inside Jupiter's radiation belts.",
                "Radyasyona Dayanıklı Elektronik",
                "Jüpiter'in radyasyon kuşaklarının içinde çalışmayı sürdüren zırhlı kutular, hata düzelten bellek ve geniş bant aralıklı yongalar.");
        tech(lang, "volcanic_heat_shielding", "Volcanic Heat Shielding",
                "Reflective multilayer suits and ceramic boots for walking beside Io's lava lakes, where the ground goes from -140 °C to molten in a few steps.",
                "Volkanik Isı Kalkanı",
                "Zeminin birkaç adımda -140 °C'den erimiş kayaya geçtiği Io'nun lav gölleri yanında yürümek için yansıtıcı çok katmanlı giysiler ve seramik botlar.");
    }

    static void tech(Lang lang, String id, String en, String enDesc, String tr, String trDesc) {
        lang.add("tech.spacebreaker." + id, en, tr);
        lang.add("tech.spacebreaker." + id + ".desc", enDesc, trDesc);
    }

    // ------------------------------------------------------------------ terrain

    public static TerrainProfile europaProfile() {
        var m = new SmallBodyTerrain.Materials(s(EUROPAN_FROST), s(EUROPAN_ICE), s(STAINED_ICE), s(EUROPAN_FROST),
                Optional.of(s(EUROPAN_FROST)), Optional.of(s(EUROPAN_ICE)));
        // a young surface: very few craters, low relief, all the drama in the cracks
        return SmallBodyTerrain.of(Spacebreaker.id("europa"), 1560.8).baseHeight(70).relief(6).roughness(0.45).craters(0.3).age(0.12)
                .soilDepth(2).materials(m).build();
    }

    public static LineaeFeature.Config europaLineae() {
        return new LineaeFeature.Config(s(STAINED_ICE), Optional.of(s(EUROPAN_ICE)), 420, 0.035, 2.0, 3, Optional.of(s(REFROZEN_BRINE)),
                950, 0.64, 0, -70, "any");
    }

    public static TerrainProfile ioProfile() {
        var m = new SmallBodyTerrain.Materials(s(SULFUR_SAND), s(IONIAN_BASALT.base()), s(IONIAN_BASALT.base()), s(SULFUR_SAND),
                Optional.of(s(SO2_FROST)), Optional.empty());
        // no impact craters at all: the whole surface is younger than a million years
        var mountains = new Landforms.Volcanoes(2600, 0.35, 70, 160, 0.45, 70, 0.02, 8, List.of(), s(IONIAN_BASALT.base()),
                Optional.of(s(SULFUR_SAND)));
        var lf = new Landforms(Optional.empty(), Optional.empty(), Optional.of(mountains), Optional.empty(), Optional.empty(), Optional.empty(),
                List.of(new Landforms.SoilPatch(s(SO2_FROST), 260, 0.66, "any"), new Landforms.SoilPatch(s(RED_SULFUR), 170, 0.8, "any")),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        return SmallBodyTerrain.of(Spacebreaker.id("io"), 1821.6).baseHeight(72).relief(9).roughness(0.6).craters(0.0).age(0.0)
                .materials(m).landforms(lf).build();
    }

    public static LakeFeature.Config ioPaterae() {
        return new LakeFeature.Config(Blocks.LAVA.defaultBlockState(), Optional.of(s(IONIAN_BASALT.base())), Optional.of(s(IONIAN_BASALT.base())),
                300, 0.45, 9, 36, 4, 0);
    }

    public static VentFeature.Config ioPlumes() {
        return new VentFeature.Config(s(SULFUR_VENT), List.of(s(SO2_FROST), s(RED_SULFUR), s(RED_SULFUR)), 340, 0.4, 24, -80, 80,
                Optional.of(s(IONIAN_BASALT.base())));
    }

    public static TerrainProfile ganymedeProfile() {
        var m = new SmallBodyTerrain.Materials(s(GANYMEDE_REGOLITH), s(DARK_ICE_CRUST), s(DARK_ICE_CRUST), s(BRIGHT_FROST),
                Optional.of(s(BRIGHT_FROST)), Optional.of(s(GROOVED_ICE)));
        var low = new TerrainProfile.Lowlands(0.0, 1700, 5, 0.08, 0, 1.0);
        return SmallBodyTerrain.of(Spacebreaker.id("ganymede"), 2634.1).baseHeight(74).relief(12).roughness(0.7).craters(0.8).age(0.7)
                .lowlands(low, s(GROOVED_ICE), s(GROOVED_ICE)).iceLatitude(70).materials(m).build();
    }

    public static LineaeFeature.Config ganymedeGrooves() {
        return new LineaeFeature.Config(s(GROOVED_ICE), Optional.of(s(BRIGHT_FROST)), 150, 0.07, 3.0, 2, Optional.empty(), 900, 0.6, 0, -70,
                "lowlands");
    }

    public static TerrainProfile callistoProfile() {
        var m = new SmallBodyTerrain.Materials(s(CALLISTO_REGOLITH), s(CALLISTAN_ROCK.base()), s(CALLISTAN_ROCK.base()), s(BRIGHT_FROST),
                Optional.of(s(BRIGHT_FROST)), Optional.of(s(EUROPAN_ICE)));
        return SmallBodyTerrain.of(Spacebreaker.id("callisto"), 2410.3).baseHeight(74).roughness(0.6).craters(1.35).age(1.0)
                .iceLatitude(72).materials(m).build();
    }

    /** Valhalla: a huge multi-ring basin, a bright central plain ringed by concentric troughs and ridges. */
    public static BasinFeature.Config valhalla() {
        return new BasinFeature.Config(1400, 15, 1100, 5, 3, 0, 0.3, 0, 0.08, 4.5, s(CALLISTAN_ROCK.base()), Optional.of(s(BRIGHT_FROST)));
    }

    // ------------------------------------------------------------------ datagen

    static void datapack(SbDatapack dp) {
        dp.add(TerrainProfile.REGISTRY, ctx -> {
            ctx.register(terrain("europa"), europaProfile());
            ctx.register(terrain("io"), ioProfile());
            ctx.register(terrain("ganymede"), ganymedeProfile());
            ctx.register(terrain("callisto"), callistoProfile());
        });
        dp.add(Registries.CONFIGURED_FEATURE, JupiterData::configured);
        dp.add(Registries.PLACED_FEATURE, JupiterData::placed);
        dp.add(Registries.BIOME, JupiterData::biomes);
        dp.add(Registries.STRUCTURE, JupiterStructures::structures);
        dp.add(Registries.STRUCTURE_SET, JupiterStructures::sets);
    }

    private static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        var ice = new TagMatchTest(JOVIAN_ICE);
        ctx.register(cf("europa_lineae"), new ConfiguredFeature<>(OuterWorlds.LINEAE.get(), europaLineae()));
        ctx.register(cf("europa_ocean"), new ConfiguredFeature<>(OuterWorlds.LAYER.get(),
                new LayerFeature.Config(Blocks.WATER.defaultBlockState(), s(EUROPAN_ICE), OCEAN_BOTTOM, OCEAN_TOP)));
        ctx.register(cf("europa_geysers"), new ConfiguredFeature<>(OuterWorlds.VENTS.get(),
                new VentFeature.Config(s(ICE_GEYSER), List.of(s(EUROPAN_FROST), s(EUROPAN_FROST)), 420, 0.3, 7, -90, 90, Optional.of(s(STAINED_ICE)))));
        ctx.register(cf("jovian_salt"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(ice, s(SALT_ORE), 9)));
        ctx.register(cf("europa_stained_lens"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(ice, s(STAINED_ICE), 40)));
        ctx.register(cf("europa_block"), OuterWorlds.boulder(0.7F, 1.6F, 0.55F, s(EUROPAN_ICE), 4, s(STAINED_ICE), 1));
        ctx.register(cf("io_paterae"), new ConfiguredFeature<>(OuterWorlds.LAKES.get(), ioPaterae()));
        ctx.register(cf("io_plumes"), new ConfiguredFeature<>(OuterWorlds.VENTS.get(), ioPlumes()));
        ctx.register(cf("io_sulfur_ore"), new ConfiguredFeature<>(Feature.ORE,
                new OreConfiguration(new TagMatchTest(IO_STONE), s(IONIAN_SULFUR_ORE), 10)));
        ctx.register(cf("io_boulder"), OuterWorlds.boulder(0.7F, 1.8F, 0.5F, s(IONIAN_BASALT.base()), 1));
        ctx.register(cf("ganymede_grooves"), new ConfiguredFeature<>(OuterWorlds.LINEAE.get(), ganymedeGrooves()));
        ctx.register(cf("ganymede_dark_lens"), new ConfiguredFeature<>(Feature.ORE,
                new OreConfiguration(new BlockMatchTest(GROOVED_ICE.get()), s(DARK_ICE_CRUST), 36)));
        ctx.register(cf("ganymede_boulder"), OuterWorlds.boulder(0.7F, 1.7F, 0.5F, s(DARK_ICE_CRUST), 3, s(GROOVED_ICE), 1));
        ctx.register(cf("callisto_valhalla"), new ConfiguredFeature<>(OuterWorlds.BASIN.get(), valhalla()));
        ctx.register(cf("callisto_knobs"), OuterWorlds.boulder(0.8F, 2.0F, 0.4F, s(BRIGHT_FROST), 2, s(CALLISTAN_ROCK.base()), 3));
    }

    private static void placed(BootstrapContext<PlacedFeature> ctx) {
        var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        for (String whole : new String[]{"europa_lineae", "europa_ocean", "europa_geysers", "io_paterae", "io_plumes", "ganymede_grooves",
                "callisto_valhalla"}) {
            PlacementUtils.register(ctx, pf(whole), cf.getOrThrow(cf(whole)), BiomeFilter.biome());
        }
        PlacementUtils.register(ctx, pf("jovian_salt"), cf.getOrThrow(cf("jovian_salt")), CountPlacement.of(8), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-50), VerticalAnchor.absolute(90)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("europa_stained_lens"), cf.getOrThrow(cf("europa_stained_lens")), CountPlacement.of(4),
                InSquarePlacement.spread(), HeightRangePlacement.uniform(VerticalAnchor.absolute(-50), VerticalAnchor.absolute(60)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("europa_block"), cf.getOrThrow(cf("europa_block")), RarityFilter.onAverageOnceEvery(5),
                InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("io_sulfur_ore"), cf.getOrThrow(cf("io_sulfur_ore")), CountPlacement.of(12), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-40), VerticalAnchor.absolute(110)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("io_boulder"), cf.getOrThrow(cf("io_boulder")), RarityFilter.onAverageOnceEvery(4),
                InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("ganymede_dark_lens"), cf.getOrThrow(cf("ganymede_dark_lens")), CountPlacement.of(5),
                InSquarePlacement.spread(), HeightRangePlacement.uniform(VerticalAnchor.absolute(-50), VerticalAnchor.absolute(80)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("ganymede_boulder"), cf.getOrThrow(cf("ganymede_boulder")), RarityFilter.onAverageOnceEvery(3),
                InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("callisto_knobs"), cf.getOrThrow(cf("callisto_knobs")), RarityFilter.onAverageOnceEvery(2),
                InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
    }

    private static void biomes(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> pf = ctx.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
        var raw = GenerationStep.Decoration.RAW_GENERATION;
        var lakes = GenerationStep.Decoration.LAKES;
        var ores = GenerationStep.Decoration.UNDERGROUND_ORES;
        var local = GenerationStep.Decoration.LOCAL_MODIFICATIONS;
        var surface = GenerationStep.Decoration.SURFACE_STRUCTURES;
        for (ResourceKey<Biome> key : EUROPA_BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -1.6F, OuterWorlds.Look.airless(0xB9B2A8),
                    raw, "europa_lineae", raw, "europa_ocean", ores, "jovian_salt", ores, "europa_stained_lens", local, "europa_block",
                    surface, "europa_geysers"));
        }
        for (ResourceKey<Biome> key : IO_BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -1.4F, OuterWorlds.Look.airless(0xB8A050),
                    lakes, "io_paterae", ores, "io_sulfur_ore", local, "io_boulder", surface, "io_plumes"));
        }
        for (ResourceKey<Biome> key : GANYMEDE_BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -1.5F, OuterWorlds.Look.airless(0x8A847C),
                    raw, "ganymede_grooves", ores, "jovian_salt", ores, "ganymede_dark_lens", local, "ganymede_boulder"));
        }
        for (ResourceKey<Biome> key : CALLISTO_BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -1.4F, OuterWorlds.Look.airless(0x5E5850),
                    raw, "callisto_valhalla", local, "callisto_knobs"));
        }
    }
}
