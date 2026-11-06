package com.pizzaparker07.spacebreaker.content.saturn;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.belt.SmallBodyTerrain;
import com.pizzaparker07.spacebreaker.content.belt.world.LakeFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.LineaeFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.VentFeature;
import com.pizzaparker07.spacebreaker.content.jupiter.JupiterModule;
import com.pizzaparker07.spacebreaker.content.saturn.dungeon.SaturnStructures;
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
import static com.pizzaparker07.spacebreaker.content.saturn.SaturnModule.*;

/** Titan and Enceladus: terrain profiles, biomes and features (datagen). */
public final class SaturnData {
    public static final ResourceKey<Biome> TITAN_PLAINS = OuterWorlds.biome("titan_plains");
    public static final ResourceKey<Biome> TITAN_DUNES = OuterWorlds.biome("titan_dunes");
    public static final ResourceKey<Biome> TITAN_CRATER = OuterWorlds.biome("titan_crater");
    public static final ResourceKey<Biome> TITAN_LAKES = OuterWorlds.biome("titan_lake_district");
    public static final ResourceKey<Biome> ENCELADUS_PLAINS = OuterWorlds.biome("enceladus_plains");
    public static final ResourceKey<Biome> ENCELADUS_CRATER = OuterWorlds.biome("enceladus_crater");
    public static final ResourceKey<Biome> ENCELADUS_POLAR = OuterWorlds.biome("enceladus_polar");
    public static final List<ResourceKey<Biome>> TITAN_BIOMES = List.of(TITAN_PLAINS, TITAN_DUNES, TITAN_CRATER, TITAN_LAKES);
    public static final List<ResourceKey<Biome>> ENCELADUS_BIOMES = List.of(ENCELADUS_PLAINS, ENCELADUS_CRATER, ENCELADUS_POLAR);
    /** Titan's hazy daytime sky and fog (shader packs read these). */
    public static final int TITAN_SKY = 0xB0783C;
    public static final int TITAN_FOG = 0xA87440;

    private SaturnData() {
    }

    static ResourceKey<TerrainProfile> terrain(String name) {
        return ResourceKey.create(TerrainProfile.REGISTRY, Spacebreaker.id(name));
    }

    static void translations(Lang lang) {
        lang.add("biome.spacebreaker.titan_plains", "Organic Plains", "Organik Ovalar");
        lang.add("biome.spacebreaker.titan_dunes", "Hydrocarbon Dune Sea", "Hidrokarbon Kumul Denizi");
        lang.add("biome.spacebreaker.titan_crater", "Titanian Crater", "Titan Krateri");
        lang.add("biome.spacebreaker.titan_lake_district", "Methane Lake District", "Metan Gölleri Bölgesi");
        lang.add("biome.spacebreaker.enceladus_plains", "Cratered Snow Plains", "Kraterli Kar Ovaları");
        lang.add("biome.spacebreaker.enceladus_crater", "Enceladan Crater", "Enceladus Krateri");
        lang.add("biome.spacebreaker.enceladus_polar", "Polar Ice Plains", "Kutup Buz Ovaları");
        lang.add("dungeon.spacebreaker.titan_hangar", "Abandoned Rotorcraft Hangar", "Terk Edilmiş Döner Kanat Hangarı");
        lang.add("structure.spacebreaker.titan_hangar", "Abandoned Rotorcraft Hangar", "Terk Edilmiş Döner Kanat Hangarı");
        tech(lang, "aerial_mobility", "Aerial Mobility",
                "On Titan the air is four times denser than Earth's and gravity a seventh: a small rotorcraft can hop between dunes and lakes on a battery.",
                "Hava Hareketliliği",
                "Titan'da hava Dünya'dakinden dört kat yoğun, yerçekimi ise yedide bir: küçük bir döner kanat, bataryayla kumullar ve göller arasında sıçrayabilir.");
        tech(lang, "hydrocarbon_processing", "Hydrocarbon Processing",
                "Methane and ethane straight from the lakes, tholins from the dunes: fuel and plastics lying on the ground, waiting for oxygen to burn with.",
                "Hidrokarbon İşleme",
                "Göllerden doğrudan metan ve etan, kumullardan tolinler: yerde duran yakıt ve plastik - yanmak için yalnızca oksijen bekliyor.");
        tech(lang, "plume_sampling", "Plume Sampling",
                "Fly through the geysers of Enceladus and catch grains of its ocean: salt, silica and organic molecules, sampled without drilling a metre.",
                "Gayzer Örneklemesi",
                "Enceladus'un gayzerlerinin içinden uçup okyanusunun tanelerini yakala: tuz, silika ve organik moleküller - tek metre delmeden örneklenir.");
    }

    static void tech(Lang lang, String id, String en, String enDesc, String tr, String trDesc) {
        lang.add("tech.spacebreaker." + id, en, tr);
        lang.add("tech.spacebreaker." + id + ".desc", enDesc, trDesc);
    }

    // ------------------------------------------------------------------ terrain

    public static TerrainProfile titanProfile() {
        var m = new SmallBodyTerrain.Materials(s(THOLIN_MUD), s(TITAN_ICE.base()), s(ICE_COBBLES), s(ICE_COBBLES), Optional.of(s(THOLIN_MUD)),
                Optional.of(s(TITAN_ICE.base())));
        // long dunes of hydrocarbon sand, with patches of rounded ice cobbles where rivers once ran
        var dunes = new Landforms.Dunes(900, 0.12, 34, 6, 95, false, s(THOLIN_SAND), Optional.empty(), 0.0);
        var lf = new Landforms(Optional.of(dunes), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                List.of(new Landforms.SoilPatch(s(ICE_COBBLES), 160, 0.8, "any")),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        return SmallBodyTerrain.of(Spacebreaker.id("titan"), 2574.73).baseHeight(70).relief(10).roughness(0.35).craters(0.15).age(0.1)
                .iceLatitude(66).materials(m).landforms(lf).build();
    }

    public static LakeFeature.Config titanLakes() {
        return new LakeFeature.Config(s(TITAN_METHANE), Optional.of(s(THOLIN_MUD)), Optional.of(s(THOLIN_MUD)), 240, 0.65, 12, 60, 6, 58);
    }

    public static TerrainProfile enceladusProfile() {
        var m = new SmallBodyTerrain.Materials(s(ENCELADUS_SNOW), s(ENCELADUS_ICE), s(ENCELADUS_ICE), s(ENCELADUS_SNOW), Optional.of(s(ENCELADUS_SNOW)),
                Optional.of(s(ENCELADUS_ICE)));
        return SmallBodyTerrain.of(Spacebreaker.id("enceladus"), 252.1).baseHeight(74).roughness(0.7).craters(0.9).age(0.55)
                .iceLatitude(90).materials(m).build();
    }

    /** Tiger stripes: four long parallel fractures across the south pole, warm blue ice in their trenches. */
    public static LineaeFeature.Config tigerStripes() {
        // the crack sets are kept to the (absent) lowlands: only the polar stripes are cut
        return new LineaeFeature.Config(s(SULCUS_ICE), Optional.of(s(ENCELADUS_SNOW)), 300, 0.06, 3.0, 1, Optional.empty(), 900, 0.6, 150, -64,
                "lowlands");
    }

    public static VentFeature.Config southGeysers() {
        return new VentFeature.Config(s(JupiterModule.ICE_GEYSER), List.of(s(ENCELADUS_SNOW)), 110, 0.55, 6, -90, -64, Optional.of(s(SULCUS_ICE)));
    }

    // ------------------------------------------------------------------ datagen

    static void datapack(SbDatapack dp) {
        dp.add(TerrainProfile.REGISTRY, ctx -> {
            ctx.register(terrain("titan"), titanProfile());
            ctx.register(terrain("enceladus"), enceladusProfile());
        });
        dp.add(Registries.CONFIGURED_FEATURE, SaturnData::configured);
        dp.add(Registries.PLACED_FEATURE, SaturnData::placed);
        dp.add(Registries.BIOME, SaturnData::biomes);
        dp.add(Registries.STRUCTURE, SaturnStructures::structures);
        dp.add(Registries.STRUCTURE_SET, SaturnStructures::sets);
    }

    private static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(cf("titan_lakes"), new ConfiguredFeature<>(OuterWorlds.LAKES.get(), titanLakes()));
        ctx.register(cf("titan_tholin"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(new TagMatchTest(TITAN_STONE), s(THOLIN_ORE), 10)));
        ctx.register(cf("titan_cobbles"), OuterWorlds.boulder(0.6F, 1.3F, 0.6F, s(ICE_COBBLES), 3, s(TITAN_ICE.base()), 1));
        ctx.register(cf("enceladus_stripes"), new ConfiguredFeature<>(OuterWorlds.LINEAE.get(), tigerStripes()));
        ctx.register(cf("enceladus_geysers"), new ConfiguredFeature<>(OuterWorlds.VENTS.get(), southGeysers()));
        ctx.register(cf("enceladus_block"), OuterWorlds.boulder(0.6F, 1.5F, 0.5F, s(ENCELADUS_ICE), 1));
    }

    private static void placed(BootstrapContext<PlacedFeature> ctx) {
        var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        for (String whole : new String[]{"titan_lakes", "enceladus_stripes", "enceladus_geysers"}) {
            PlacementUtils.register(ctx, pf(whole), cf.getOrThrow(cf(whole)), BiomeFilter.biome());
        }
        PlacementUtils.register(ctx, pf("titan_tholin"), cf.getOrThrow(cf("titan_tholin")), CountPlacement.of(10), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-40), VerticalAnchor.absolute(100)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("titan_cobbles"), cf.getOrThrow(cf("titan_cobbles")), RarityFilter.onAverageOnceEvery(6),
                InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("enceladus_block"), cf.getOrThrow(cf("enceladus_block")), RarityFilter.onAverageOnceEvery(5),
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
        var titan = new OuterWorlds.Look(TITAN_SKY, TITAN_FOG, 0x8A5A2E, null);
        for (ResourceKey<Biome> key : TITAN_BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -1.8F, titan, lakes, "titan_lakes", ores, "titan_tholin", local, "titan_cobbles"));
        }
        for (ResourceKey<Biome> key : ENCELADUS_BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -2.0F, OuterWorlds.Look.airless(0xDDE4EA),
                    raw, "enceladus_stripes", local, "enceladus_block", surface, "enceladus_geysers"));
        }
    }
}
