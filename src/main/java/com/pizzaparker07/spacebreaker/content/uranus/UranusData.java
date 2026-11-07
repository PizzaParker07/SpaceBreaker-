package com.pizzaparker07.spacebreaker.content.uranus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.belt.SmallBodyTerrain;
import com.pizzaparker07.spacebreaker.content.belt.world.CoronaFeature;
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
import static com.pizzaparker07.spacebreaker.content.uranus.UranusModule.*;

/** Miranda: terrain profile, biomes and features (datagen). */
public final class UranusData {
    public static final ResourceKey<Biome> MIRANDA_PLAINS = OuterWorlds.biome("miranda_plains");
    public static final ResourceKey<Biome> MIRANDA_CRATER = OuterWorlds.biome("miranda_crater");
    public static final ResourceKey<Biome> MIRANDA_POLAR = OuterWorlds.biome("miranda_polar");
    public static final List<ResourceKey<Biome>> MIRANDA_BIOMES = List.of(MIRANDA_PLAINS, MIRANDA_CRATER, MIRANDA_POLAR);
    public static final ResourceKey<Biome> ARIEL_PLAINS = OuterWorlds.biome("ariel_plains");
    public static final ResourceKey<Biome> ARIEL_CRATER = OuterWorlds.biome("ariel_crater");
    public static final ResourceKey<Biome> ARIEL_POLAR = OuterWorlds.biome("ariel_polar");
    public static final List<ResourceKey<Biome>> ARIEL_BIOMES = List.of(ARIEL_PLAINS, ARIEL_CRATER, ARIEL_POLAR);
    public static final ResourceKey<Biome> TITANIA_UPLANDS = OuterWorlds.biome("titania_uplands");
    public static final ResourceKey<Biome> TITANIA_CRATER = OuterWorlds.biome("titania_crater");
    public static final ResourceKey<Biome> TITANIA_POLAR = OuterWorlds.biome("titania_polar");
    public static final List<ResourceKey<Biome>> TITANIA_BIOMES = List.of(TITANIA_UPLANDS, TITANIA_CRATER, TITANIA_POLAR);

    private UranusData() {
    }

    static ResourceKey<TerrainProfile> terrain(String name) {
        return ResourceKey.create(TerrainProfile.REGISTRY, Spacebreaker.id(name));
    }

    static void translations(Lang lang) {
        lang.add("dungeon.spacebreaker.titania_canyon_outpost", "Canyon Rim Outpost", "Kanyon Kenarı Karakolu");
        lang.add("structure.spacebreaker.titania_canyon_outpost", "Canyon Rim Outpost", "Kanyon Kenarı Karakolu");
        lang.add("biome.spacebreaker.miranda_plains", "Patchwork Plains", "Yamalı Ovalar");
        lang.add("biome.spacebreaker.miranda_crater", "Mirandan Crater", "Miranda Krateri");
        lang.add("biome.spacebreaker.miranda_polar", "Mirandan Pole", "Miranda Kutbu");
        lang.add("biome.spacebreaker.ariel_plains", "Arielian Plains", "Ariel Ovaları");
        lang.add("biome.spacebreaker.ariel_crater", "Bright-Rayed Crater", "Parlak Işınlı Krater");
        lang.add("biome.spacebreaker.ariel_polar", "Arielian Pole", "Ariel Kutbu");
        lang.add("tech.spacebreaker.ice_shell_tectonics", "Ice Shell Tectonics", "Buz Kabuğu Tektoniği");
        lang.add("tech.spacebreaker.ice_shell_tectonics.desc",
                "Ariel's crust was pulled apart until long strips dropped between parallel faults, then icy lava flooded their floors: read the faults and you know where the warm ice once rose.",
                "Ariel'in kabuğu çekilip gerildi, paralel faylar arasında uzun şeritler çöktü, sonra tabanlarını buzlu lav doldurdu: fayları okuyan, sıcak buzun bir zamanlar nereden yükseldiğini bilir.");
        lang.add("biome.spacebreaker.titania_uplands", "Titanian Uplands", "Titania Yaylaları");
        lang.add("biome.spacebreaker.titania_crater", "Titanian Crater", "Titania Krateri");
        lang.add("biome.spacebreaker.titania_polar", "Titanian Pole", "Titania Kutbu");
        lang.add("tech.spacebreaker.tectonic_mapping", "Tectonic Mapping", "Tektonik Haritalama");
        lang.add("tech.spacebreaker.tectonic_mapping.desc",
                "Titania's canyons run straight for hundreds of kilometres, the cracks of a moon that swelled as its insides froze: map them and you can read its whole history from the walls.",
                "Titania'nın kanyonları yüzlerce kilometre dümdüz uzanır; içi donarken şişen bir uydunun çatlaklarıdır: onları haritalayan, uydunun bütün geçmişini duvarlarından okur.");
        lang.add("tech.spacebreaker.vertical_exploration", "Vertical Exploration", "Dikey Keşif");
        lang.add("tech.spacebreaker.vertical_exploration.desc",
                "Rope anchors in ice, slow controlled descents and the patience to fall for minutes: how to get down Miranda's great cliff and back up again.",
                "Buza çakılan halat çapaları, yavaş ve denetimli inişler ve dakikalarca düşmeye yetecek sabır: Miranda'nın dev uçurumundan nasıl inilir ve yine nasıl çıkılır.");
    }

    // ------------------------------------------------------------------ terrain

    public static TerrainProfile mirandaProfile() {
        var m = new SmallBodyTerrain.Materials(s(MIRANDAN_REGOLITH), s(MIRANDAN_ICE.base()), s(MIRANDAN_ICE.base()), s(MIRANDAN_REGOLITH),
                Optional.of(s(MIRANDAN_REGOLITH)), Optional.of(s(BRIGHT_RIDGE_ICE)));
        // the great fault cliff is built into the terrain itself (planet engine rupes): no second pass over the chunk
        var lf = new com.pizzaparker07.spacebreaker.content.worldgen.Landforms(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), List.of(cliff()));
        return SmallBodyTerrain.of(Spacebreaker.id("miranda"), 235.8).baseHeight(76).roughness(1.0).craters(1.0).age(0.8)
                .iceLatitude(84).materials(m).landforms(lf).build();
    }

    /** The great fault cliff, a short walk from the landing site. */
    public static com.pizzaparker07.spacebreaker.content.worldgen.Landforms.Rupes cliff() {
        return new com.pizzaparker07.spacebreaker.content.worldgen.Landforms.Rupes(380, -8, 1000, 18, 96, 7, s(MIRANDAN_ICE.base()),
                Optional.of(s(DARK_BAND_ICE)));
    }

    /** Three racetrack coronae of bright and dark ridges; the cliff runs along the edge of the first. */
    public static CoronaFeature.Config coronae() {
        return new CoronaFeature.Config(List.of(
                new CoronaFeature.Corona(380, -18, 420, 240, 18),
                new CoronaFeature.Corona(-760, 28, 300, 260, -30),
                new CoronaFeature.Corona(1300, 30, 380, 210, 60)),
                16, 5, s(BRIGHT_RIDGE_ICE), s(DARK_BAND_ICE));
    }

    public static TerrainProfile arielProfile() {
        var m = new SmallBodyTerrain.Materials(s(ARIEL_REGOLITH), s(ARIEL_ICE.base()), s(ARIEL_ICE.base()), s(com.pizzaparker07.spacebreaker.content.mars.MarsModule.CO2_FROST),
                Optional.of(s(com.pizzaparker07.spacebreaker.content.mars.MarsModule.CO2_FROST)), Optional.of(s(ARIEL_ICE.base())));
        // young surface: few big craters, bright rays (CO2 frost), smooth plains
        return SmallBodyTerrain.of(Spacebreaker.id("ariel"), 578.9).baseHeight(76).roughness(0.6).craters(0.6).age(0.3)
                .iceLatitude(80).materials(m).build();
    }

    /** Ariel's chasmata: long flat-floored rift valleys, several crossing the plains near the landing site. */
    public static com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.Config chasmata() {
        return new com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.Config(List.of(
                new com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.Graben(300, -6, 2200, 48, 34, 12),
                new com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.Graben(-200, 10, 1800, 36, 28, -8),
                new com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.Graben(900, 22, 1400, 60, 40, 30),
                new com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.Graben(-900, -24, 1600, 40, 30, -20)),
                9, 4, s(GRABEN_FLOOR_ICE), s(ARIEL_ICE.base()));
    }

    public static TerrainProfile titaniaProfile() {
        var m = new SmallBodyTerrain.Materials(s(TITANIA_REGOLITH), s(TITANIA_ICE.base()), s(TITANIA_ICE.base()), s(TITANIA_REGOLITH),
                Optional.of(s(com.pizzaparker07.spacebreaker.content.mars.MarsModule.CO2_FROST)), Optional.of(s(TITANIA_ICE.base())));
        return SmallBodyTerrain.of(Spacebreaker.id("titania"), 788.9).baseHeight(78).roughness(0.9).craters(0.8).age(0.55)
                .iceLatitude(78).materials(m).build();
    }

    /** Titania's great canyons: much wider and deeper than Ariel's valleys, crossing the uplands near the landing site. */
    public static com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.Config canyons() {
        return new com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.Config(List.of(
                new com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.Graben(250, -4, 3200, 90, 64, 20),
                new com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.Graben(-600, 18, 2400, 70, 52, 35),
                new com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.Graben(1100, -28, 2000, 60, 48, 10)),
                14, 0, s(TITANIA_REGOLITH), s(CANYON_WALL_ICE));
    }

    /** The great old crater basin, with a central peak. */
    public static com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature.Config greatCrater() {
        return new com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature.Config(-900, -40, 420, 28, 12, 30, 0.14, 3, 0.18, 1.2,
                s(TITANIA_ICE.base()), Optional.of(s(TITANIA_REGOLITH)), Optional.of(s(CANYON_WALL_ICE)));
    }

    // ------------------------------------------------------------------ datagen

    static void datapack(SbDatapack dp) {
        dp.add(TerrainProfile.REGISTRY, ctx -> {
            ctx.register(terrain("miranda"), mirandaProfile());
            ctx.register(terrain("ariel"), arielProfile());
            ctx.register(terrain("titania"), titaniaProfile());
            ctx.register(terrain("umbriel"), Umbriel.profile());
            ctx.register(terrain("oberon"), Oberon.profile());
        });
        dp.add(Registries.CONFIGURED_FEATURE, UranusData::configured);
        dp.add(Registries.PLACED_FEATURE, UranusData::placed);
        dp.add(Registries.BIOME, UranusData::biomes);
        dp.add(Registries.STRUCTURE, com.pizzaparker07.spacebreaker.content.uranus.dungeon.UranusStructures::structures);
        dp.add(Registries.STRUCTURE_SET, com.pizzaparker07.spacebreaker.content.uranus.dungeon.UranusStructures::sets);
    }

    private static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(cf("miranda_coronae"), new ConfiguredFeature<>(OuterWorlds.CORONAE.get(), coronae()));
        OuterWorlds.ore(ctx, "miranda_carbon", MIRANDA_STONE, s(CARBON_ORE), 10);
        ctx.register(cf("ariel_chasmata"), new ConfiguredFeature<>(OuterWorlds.GRABENS.get(), chasmata()));
        OuterWorlds.ore(ctx, "ariel_ammonia", ARIEL_STONE, s(ARIEL_AMMONIA_ORE), 8);
        ctx.register(cf("ariel_block"), OuterWorlds.boulder(0.6F, 1.5F, 0.5F, s(ARIEL_ICE.base()), 3, s(com.pizzaparker07.spacebreaker.content.mars.MarsModule.CO2_FROST), 1));
        ctx.register(cf("titania_canyons"), new ConfiguredFeature<>(OuterWorlds.GRABENS.get(), canyons()));
        ctx.register(cf("titania_crater"), new ConfiguredFeature<>(OuterWorlds.BASIN.get(), greatCrater()));
        OuterWorlds.ore(ctx, "titania_carbon", TITANIA_STONE, s(TITANIA_CARBON_ORE), 10);
        ctx.register(cf("titania_block"), OuterWorlds.boulder(0.6F, 1.8F, 0.5F, s(TITANIA_ICE.base()), 3, s(CANYON_WALL_ICE), 1));
        ctx.register(cf("miranda_block"), OuterWorlds.boulder(0.6F, 1.8F, 0.5F, s(MIRANDAN_ICE.base()), 3, s(DARK_BAND_ICE), 1));
        Umbriel.configured(ctx);
        Oberon.configured(ctx);
    }

    private static void placed(BootstrapContext<PlacedFeature> ctx) {
        OuterWorlds.placedWhole(ctx, "miranda_coronae", "ariel_chasmata", "titania_canyons", "titania_crater");
        OuterWorlds.placedOre(ctx, "miranda_carbon", 10);
        OuterWorlds.placedOre(ctx, "ariel_ammonia", 8);
        OuterWorlds.placedOre(ctx, "titania_carbon", 10);
        OuterWorlds.placedBoulder(ctx, "ariel_block", 6);
        OuterWorlds.placedBoulder(ctx, "titania_block", 4);
        OuterWorlds.placedBoulder(ctx, "miranda_block", 4);
        Umbriel.placed(ctx);
        Oberon.placed(ctx);
    }

    private static void biomes(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> pf = ctx.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
        var raw = GenerationStep.Decoration.RAW_GENERATION;
        var ores = GenerationStep.Decoration.UNDERGROUND_ORES;
        var local = GenerationStep.Decoration.LOCAL_MODIFICATIONS;
        for (ResourceKey<Biome> key : MIRANDA_BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -2.0F, OuterWorlds.Look.airless(0x9A9A98),
                    raw, "miranda_coronae", ores, "miranda_carbon", local, "miranda_block"));
        }
        for (ResourceKey<Biome> key : ARIEL_BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -2.0F, OuterWorlds.Look.airless(0xB3AEA6),
                    raw, "ariel_chasmata", ores, "ariel_ammonia", local, "ariel_block"));
        }
        for (ResourceKey<Biome> key : TITANIA_BIOMES) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -2.0F, OuterWorlds.Look.airless(0x7D7871),
                    raw, "titania_crater", raw, "titania_canyons", ores, "titania_carbon", local, "titania_block"));
        }
        Umbriel.biomes(ctx, pf, carvers);
        Oberon.biomes(ctx, pf, carvers);
    }
}
