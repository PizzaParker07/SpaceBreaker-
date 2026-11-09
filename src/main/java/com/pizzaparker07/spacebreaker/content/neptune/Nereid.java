package com.pizzaparker07.spacebreaker.content.neptune;

import com.pizzaparker07.spacebreaker.content.belt.TinyWorld;
import com.pizzaparker07.spacebreaker.content.worldgen.Landforms;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import java.util.Optional;

import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.s;

/**
 * Nereid, Neptune's outermost large moon, probably a captured body: a light-grey icy world on the most stretched orbit
 * of any moon, swinging from 1.4 to 9.7 million km out, so Neptune is only a small blue disc in its sky. Clean water ice:
 * a refuel world.
 */
public final class Nereid {
    public static final TinyWorld NEREID = new TinyWorld(NeptuneModule.KIT, "nereid", "nereid_frost", MapColor.COLOR_LIGHT_GRAY, "nereid_ice",
            MapColor.TERRACOTTA_WHITE, true, SoundType.GLASS);

    private Nereid() {
    }

    static void load() {
    }

    public static TerrainProfile profile() {
        return NEREID.profile(170.0, 22, Optional.empty(), Landforms.NONE);
    }

    static void translations(Lang lang) {
        NEREID.translations(lang, "Nereid", "Nereid", "Nereid Frost", "Nereid Kırağısı", "Nereid Ice", "Nereid Buzu");
    }

    static void loot(SbBlockLoot loot) {
        NEREID.loot(loot);
    }

    static void tags(SbTags tags) {
        NEREID.tags(tags, NeptuneModule.TRITON_GROUND);
    }

    static void terrain(BootstrapContext<TerrainProfile> ctx) {
        ctx.register(NEREID.terrainKey(), profile());
    }

    static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        NEREID.configured(ctx, s(NEREID.soil));
    }

    static void placed(BootstrapContext<PlacedFeature> ctx) {
        NEREID.placed(ctx);
    }

    static void biomes(BootstrapContext<Biome> ctx, HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        NEREID.biomes(ctx, pf, carvers, 0xA6A39E);
    }
}
