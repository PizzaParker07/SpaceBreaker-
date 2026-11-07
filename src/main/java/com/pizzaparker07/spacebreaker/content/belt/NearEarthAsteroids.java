package com.pizzaparker07.spacebreaker.content.belt;

import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.RidgeFeature;
import com.pizzaparker07.spacebreaker.content.worldgen.Landforms;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;

import java.util.List;
import java.util.Optional;

import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.cf;
import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.s;

/**
 * Four near-Earth asteroids visited by spacecraft, each a small package on the shared {@link TinyWorld} base, and the
 * cheapest worlds to reach beyond the Moon:
 * <ul>
 *   <li>Bennu: a rubble pile, darker than coal, strewn with huge boulders, with a low ridge round its equator;</li>
 *   <li>Ryugu: a spinning top of dark carbon-rich rubble with a sharp equatorial ridge;</li>
 *   <li>Eros: a long stony (S-type) asteroid, heavily cratered, with the great saddle Himeros on one side;</li>
 *   <li>Itokawa: a peanut of two lumps, rough and bouldery except for the smooth gravel plain of the Muses Sea in the neck.</li>
 * </ul>
 * None has water ice; they are worlds for rock and metal, not refuelling.
 */
public final class NearEarthAsteroids {
    public static final TagKey<Block> GROUND = SbTagKeys.modBlock("near_earth_asteroid_ground");
    public static final TinyWorld BENNU = new TinyWorld(BeltModule.KIT, "bennu", "bennu_regolith", MapColor.COLOR_BLACK, "bennu_boulder",
            MapColor.DEEPSLATE, false);
    public static final TinyWorld RYUGU = new TinyWorld(BeltModule.KIT, "ryugu", "ryugu_regolith", MapColor.TERRACOTTA_BLACK, "ryugu_rock",
            MapColor.COLOR_BLACK, false);
    public static final TinyWorld EROS = new TinyWorld(BeltModule.KIT, "eros", "eros_regolith", MapColor.TERRACOTTA_BROWN, "eros_chondrite",
            MapColor.TERRACOTTA_LIGHT_GRAY, false);
    public static final TinyWorld ITOKAWA = new TinyWorld(BeltModule.KIT, "itokawa", "itokawa_gravel", MapColor.TERRACOTTA_LIGHT_GRAY,
            "itokawa_rock", MapColor.STONE, false);

    private NearEarthAsteroids() {
    }

    static void load() {
    }

    public static List<TinyWorld> all() {
        return List.of(BENNU, RYUGU, EROS, ITOKAWA);
    }

    // ------------------------------------------------------------------ terrain

    public static TerrainProfile bennuProfile() {
        return BENNU.profile(0.2452, 14, Optional.empty(), Landforms.NONE);
    }

    public static TerrainProfile ryuguProfile() {
        return RYUGU.profile(0.448, 12, Optional.empty(), Landforms.NONE);
    }

    public static TerrainProfile erosProfile() {
        return EROS.profile(8.42, 24, Optional.empty(), Landforms.NONE);
    }

    public static TerrainProfile itokawaProfile() {
        return ITOKAWA.profile(0.165, 10, Optional.empty(), Landforms.NONE);
    }

    /** Bennu's low equatorial bulge. */
    public static RidgeFeature.Config bennuRidge() {
        return new RidgeFeature.Config(0, 8, 18, s(BENNU.rock));
    }

    /** Ryugu's sharp equatorial ridge: the rim of the spinning top. */
    public static RidgeFeature.Config ryuguRidge() {
        return new RidgeFeature.Config(0, 18, 12, s(RYUGU.rock));
    }

    /** Himeros, the great saddle on Eros' side, east of the landing site. */
    public static BasinFeature.Config himeros() {
        return new BasinFeature.Config(260, 4, 200, 22, 6, 0, 0.1, 2, 0.3, 0, s(EROS.rock), Optional.of(s(EROS.soil)), Optional.empty());
    }

    /** The Muses Sea: the smooth, flat gravel plain in Itokawa's neck, right at the landing site. */
    public static BasinFeature.Config musesSea() {
        return new BasinFeature.Config(40, 0, 90, 6, 2, 0, 0.1, 1, 0.35, 0, s(ITOKAWA.rock), Optional.of(s(ITOKAWA.soil)), Optional.empty());
    }

    // ------------------------------------------------------------------ data

    static void translations(Lang lang) {
        BENNU.translations(lang, "Bennu", "Bennu", "Bennu Regolith", "Bennu Regoliti", "Bennu Boulder Rock", "Bennu Kaya Bloğu");
        RYUGU.translations(lang, "Ryugu", "Ryugu", "Ryugu Regolith", "Ryugu Regoliti", "Ryugu Rock", "Ryugu Kayası");
        EROS.translations(lang, "Eros", "Eros", "Eros Regolith", "Eros Regoliti", "Eros Chondrite", "Eros Kondriti");
        ITOKAWA.translations(lang, "Itokawa", "Itokawa", "Itokawa Gravel", "Itokawa Çakılı", "Itokawa Rock", "Itokawa Kayası");
    }

    static void loot(SbBlockLoot loot) {
        all().forEach(w -> w.loot(loot));
    }

    static void tags(SbTags tags) {
        all().forEach(w -> w.tags(tags, GROUND));
    }

    static void terrain(BootstrapContext<TerrainProfile> ctx) {
        ctx.register(BENNU.terrainKey(), bennuProfile());
        ctx.register(RYUGU.terrainKey(), ryuguProfile());
        ctx.register(EROS.terrainKey(), erosProfile());
        ctx.register(ITOKAWA.terrainKey(), itokawaProfile());
    }

    static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        // Bennu and Itokawa: big boulders everywhere; Ryugu: dark boulders; Eros: fewer, smaller blocks
        BENNU.configured(ctx, s(BENNU.soil), 1.2F, 3.8F);
        RYUGU.configured(ctx, s(RYUGU.soil), 0.8F, 2.6F);
        EROS.configured(ctx, s(EROS.soil));
        ITOKAWA.configured(ctx, s(ITOKAWA.soil), 0.8F, 2.8F);
        ctx.register(cf("bennu_ridge"), new ConfiguredFeature<>(OuterWorlds.RIDGE.get(), bennuRidge()));
        ctx.register(cf("ryugu_ridge"), new ConfiguredFeature<>(OuterWorlds.RIDGE.get(), ryuguRidge()));
        ctx.register(cf("eros_himeros"), new ConfiguredFeature<>(OuterWorlds.BASIN.get(), himeros()));
        ctx.register(cf("itokawa_muses_sea"), new ConfiguredFeature<>(OuterWorlds.BASIN.get(), musesSea()));
    }

    static void placed(BootstrapContext<PlacedFeature> ctx) {
        BENNU.placed(ctx, 1);
        RYUGU.placed(ctx, 2);
        EROS.placed(ctx, 4);
        ITOKAWA.placed(ctx, 1);
        OuterWorlds.placedWhole(ctx, "bennu_ridge", "ryugu_ridge", "eros_himeros", "itokawa_muses_sea");
    }

    static void biomes(BootstrapContext<Biome> ctx, HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        var raw = GenerationStep.Decoration.RAW_GENERATION;
        BENNU.biomes(ctx, pf, carvers, 0x2A2826, raw, "bennu_ridge");
        RYUGU.biomes(ctx, pf, carvers, 0x2E2B29, raw, "ryugu_ridge");
        EROS.biomes(ctx, pf, carvers, 0x8C7A62, raw, "eros_himeros");
        ITOKAWA.biomes(ctx, pf, carvers, 0x8E8A80, raw, "itokawa_muses_sea");
    }
}
