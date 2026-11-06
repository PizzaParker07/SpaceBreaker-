package com.pizzaparker07.spacebreaker.content.pluto;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.belt.SmallBodyTerrain;
import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.CellsFeature;
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
import static com.pizzaparker07.spacebreaker.content.pluto.PlutoModule.*;

/** Pluto and Charon: terrain profiles, biomes and features (datagen). */
public final class PlutoData {
    public static final ResourceKey<Biome> PLUTO_PLAINS = OuterWorlds.biome("pluto_plains");
    public static final ResourceKey<Biome> PLUTO_CRATER = OuterWorlds.biome("pluto_crater");
    public static final ResourceKey<Biome> PLUTO_MOUNTAINS = OuterWorlds.biome("pluto_ice_mountains");
    public static final ResourceKey<Biome> PLUTO_POLAR = OuterWorlds.biome("pluto_polar");
    public static final ResourceKey<Biome> CHARON_PLAINS = OuterWorlds.biome("charon_plains");
    public static final ResourceKey<Biome> CHARON_CRATER = OuterWorlds.biome("charon_crater");
    public static final ResourceKey<Biome> CHARON_CHASMS = OuterWorlds.biome("charon_chasms");
    public static final ResourceKey<Biome> CHARON_POLAR = OuterWorlds.biome("charon_polar");
    public static final List<ResourceKey<Biome>> PLUTO_BIOMES = List.of(PLUTO_PLAINS, PLUTO_CRATER, PLUTO_MOUNTAINS, PLUTO_POLAR);
    public static final List<ResourceKey<Biome>> CHARON_BIOMES = List.of(CHARON_PLAINS, CHARON_CRATER, CHARON_CHASMS, CHARON_POLAR);
    /** The great nitrogen-ice basin: centre (block x, latitude) and radius. */
    public static final double HEART_X = 900;
    public static final double HEART_LATITUDE = 22;
    public static final double HEART_RADIUS = 650;
    /** Surface level of the nitrogen-ice plain. */
    public static final int HEART_LEVEL = 63;

    private PlutoData() {
    }

    static ResourceKey<TerrainProfile> terrain(String name) {
        return ResourceKey.create(TerrainProfile.REGISTRY, Spacebreaker.id(name));
    }

    static void translations(Lang lang) {
        lang.add("biome.spacebreaker.pluto_plains", "Plutonian Plains", "Plüton Ovaları");
        lang.add("biome.spacebreaker.pluto_crater", "Plutonian Crater", "Plüton Krateri");
        lang.add("biome.spacebreaker.pluto_ice_mountains", "Water Ice Mountains", "Su Buzu Dağları");
        lang.add("biome.spacebreaker.pluto_polar", "Methane Frost Pole", "Metan Kırağılı Kutup");
        lang.add("biome.spacebreaker.charon_plains", "Charon Plains", "Charon Ovaları");
        lang.add("biome.spacebreaker.charon_crater", "Charon Crater", "Charon Krateri");
        lang.add("biome.spacebreaker.charon_chasms", "Chasm Belt", "Kanyon Kuşağı");
        lang.add("biome.spacebreaker.charon_polar", "Red Polar Cap", "Kızıl Kutup Başlığı");
        tech(lang, "nitrogen_glaciology", "Nitrogen Glaciology",
                "At -230 °C nitrogen is a soft rock: it flows in glaciers, turns over in slow cells a few blocks deep and floats mountains of water ice. Read it and you can build on it.",
                "Azot Buzulbilimi",
                "-230 °C'de azot yumuşak bir kayadır: buzullar halinde akar, birkaç blok derinlikte yavaş hücreler halinde döner ve su buzundan dağları yüzdürür. Onu okuyan üzerine inşa edebilir.");
        tech(lang, "kuiper_belt_navigation", "Kuiper Belt Navigation",
                "Forty times farther from the Sun than Earth, light takes five hours to reach home: navigate on your own, by the stars and by Pluto hanging still over Charon.",
                "Kuiper Kuşağı Seyrüseferi",
                "Güneş'e Dünya'dan kırk kat uzak; ışık eve beş saatte ulaşır: kendi başına, yıldızlarla ve Charon'un üstünde kıpırdamadan duran Plüton'la yön bul.");
    }

    static void tech(Lang lang, String id, String en, String enDesc, String tr, String trDesc) {
        lang.add("tech.spacebreaker." + id, en, tr);
        lang.add("tech.spacebreaker." + id + ".desc", enDesc, trDesc);
    }

    // ------------------------------------------------------------------ terrain

    public static TerrainProfile plutoProfile() {
        var m = new SmallBodyTerrain.Materials(s(PLUTONIAN_FROST), s(PLUTONIAN_ICE.base()), s(PLUTONIAN_ICE.base()), s(METHANE_FROST),
                Optional.of(s(METHANE_FROST)), Optional.of(s(NITROGEN_ICE)));
        // blocky mountains of water ice: three along the west shore of the basin, more scattered over the highlands
        double heartZ = -HEART_LATITUDE / 90.0 * SmallBodyTerrain.poleDistance(1188.3);
        var mountains = new Landforms.Volcanoes(1700, 0.25, 30, 60, 0.85, 52, 0.0, 6,
                List.of(new Landforms.Anchor(HEART_X - HEART_RADIUS - 40, heartZ - 170, 48, 50),
                        new Landforms.Anchor(HEART_X - HEART_RADIUS - 70, heartZ + 60, 40, 44),
                        new Landforms.Anchor(HEART_X - HEART_RADIUS + 10, heartZ + 260, 34, 36)),
                s(PLUTONIAN_ICE.base()), Optional.of(s(METHANE_FROST)));
        // the dark red tholin lowlands
        var lf = new Landforms(Optional.empty(), Optional.empty(), Optional.of(mountains), Optional.empty(), Optional.empty(), Optional.empty(),
                List.of(new Landforms.SoilPatch(s(THOLIN_REGOLITH), 520, 0.6, "any")),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        return SmallBodyTerrain.of(Spacebreaker.id("pluto"), 1188.3).baseHeight(74).roughness(0.6).craters(0.5).age(0.45)
                .iceLatitude(72).materials(m).landforms(lf).build();
    }

    /** The heart's western lobe: a shallow, flat-floored basin filled with nitrogen ice. */
    public static BasinFeature.Config heart() {
        return new BasinFeature.Config(HEART_X, HEART_LATITUDE, HEART_RADIUS, 12, 5, 0, 0.01, 0, 0.08, 0, s(PLUTONIAN_ICE.base()),
                Optional.of(s(NITROGEN_ICE)), Optional.empty());
    }

    /** Convection cells over the basin floor, dark methane-stained ice gathered in the troughs between them. */
    public static CellsFeature.Config convectionCells() {
        // flat, gently domed cells (a block or two) and shallow troughs of methane-stained ice
        return new CellsFeature.Config("pillows", 46, 2, 2.5, Optional.of(s(STAINED_NITROGEN_ICE)), Optional.of(s(NITROGEN_ICE)), HEART_X, HEART_LATITUDE,
                HEART_RADIUS * 0.92, -90, 90,
                Optional.of(HEART_LEVEL));
    }

    public static TerrainProfile charonProfile() {
        var m = new SmallBodyTerrain.Materials(s(CHARON_REGOLITH), s(CHARON_ICE), s(CHARON_ICE), s(CHARON_REGOLITH), Optional.of(s(RED_POLAR_FROST)),
                Optional.of(s(CHARON_ICE)));
        // a belt of chasms across the equator, where the young moon's ice shell split as its ocean froze
        var rift = new Landforms.Rift(4, 0, 9000, 130, 46, 110, 3, Optional.of(s(CHARON_REGOLITH)));
        // the dark red polar stain: methane leaking from Pluto, frozen out at the cold pole and cooked by sunlight
        var cap = new Landforms.IceCap(64, 3, 66, 0, s(RED_POLAR_FROST), Optional.of(s(RED_POLAR_FROST)), Optional.empty());
        var lf = new Landforms(Optional.empty(), Optional.of(rift), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(cap), List.of(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        return SmallBodyTerrain.of(Spacebreaker.id("charon"), 606.0).baseHeight(74).roughness(0.9).craters(0.9).age(0.7)
                .iceLatitude(80).materials(m).landforms(lf).build();
    }

    // ------------------------------------------------------------------ datagen

    static void datapack(SbDatapack dp) {
        dp.add(TerrainProfile.REGISTRY, ctx -> {
            ctx.register(terrain("pluto"), plutoProfile());
            ctx.register(terrain("charon"), charonProfile());
        });
        dp.add(Registries.CONFIGURED_FEATURE, PlutoData::configured);
        dp.add(Registries.PLACED_FEATURE, PlutoData::placed);
        dp.add(Registries.BIOME, PlutoData::biomes);
    }

    private static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(cf("pluto_heart"), new ConfiguredFeature<>(OuterWorlds.BASIN.get(), heart()));
        ctx.register(cf("pluto_cells"), new ConfiguredFeature<>(OuterWorlds.CELLS.get(), convectionCells()));
        ctx.register(cf("pluto_tholin"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(new TagMatchTest(PLUTO_STONE),
                s(PLUTONIAN_THOLIN_ORE), 9)));
        ctx.register(cf("pluto_block"), OuterWorlds.boulder(0.6F, 1.6F, 0.5F, s(PLUTONIAN_ICE.base()), 3, s(METHANE_FROST), 1));
        ctx.register(cf("charon_ammonia"), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(new TagMatchTest(CHARON_STONE),
                s(AMMONIA_HYDRATE_ORE), 8)));
        ctx.register(cf("charon_block"), OuterWorlds.boulder(0.6F, 1.8F, 0.5F, s(CHARON_ICE), 1));
    }

    private static void placed(BootstrapContext<PlacedFeature> ctx) {
        var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        for (String whole : new String[]{"pluto_heart", "pluto_cells"}) {
            PlacementUtils.register(ctx, pf(whole), cf.getOrThrow(cf(whole)), BiomeFilter.biome());
        }
        PlacementUtils.register(ctx, pf("pluto_tholin"), cf.getOrThrow(cf("pluto_tholin")), CountPlacement.of(9), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-40), VerticalAnchor.absolute(100)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("pluto_block"), cf.getOrThrow(cf("pluto_block")), RarityFilter.onAverageOnceEvery(5),
                InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("charon_ammonia"), cf.getOrThrow(cf("charon_ammonia")), CountPlacement.of(8), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-40), VerticalAnchor.absolute(100)), BiomeFilter.biome());
        PlacementUtils.register(ctx, pf("charon_block"), cf.getOrThrow(cf("charon_block")), RarityFilter.onAverageOnceEvery(4),
                InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
    }

    private static void biomes(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> pf = ctx.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
        var raw = GenerationStep.Decoration.RAW_GENERATION;
        var ores = GenerationStep.Decoration.UNDERGROUND_ORES;
        var local = GenerationStep.Decoration.LOCAL_MODIFICATIONS;
        for (ResourceKey<Biome> key : PLUTO_BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -2.0F, OuterWorlds.Look.airless(0xC9B7A2),
                    raw, "pluto_heart", ores, "pluto_tholin", local, "pluto_cells", local, "pluto_block"));
        }
        for (ResourceKey<Biome> key : CHARON_BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -2.0F, OuterWorlds.Look.airless(0x8E8C8A),
                    ores, "charon_ammonia", local, "charon_block"));
        }
    }
}
