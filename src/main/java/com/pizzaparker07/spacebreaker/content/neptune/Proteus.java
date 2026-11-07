package com.pizzaparker07.spacebreaker.content.neptune;

import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.belt.TinyWorld;
import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.worldgen.Landforms;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;

import java.util.Optional;

import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.cf;
import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.s;

/**
 * Proteus, Neptune's second-largest moon: about as big as a body can be without pulling itself round, a lumpy box of
 * dark, soot-coated rock and ice, reflecting only a twentieth of the light. One side is scooped out by Pharos, a crater
 * so big it nearly broke the moon apart. Neptune fills a quarter of the sky. Its ice lies under a dark crust and is not
 * clean water ice: no refuelling here.
 */
public final class Proteus {
    public static final TinyWorld PROTEUS = new TinyWorld(NeptuneModule.KIT, "proteus", "proteus_regolith", MapColor.COLOR_BLACK,
            "proteus_crust", MapColor.DEEPSLATE, false);

    private Proteus() {
    }

    static void load() {
    }

    public static TerrainProfile profile() {
        return PROTEUS.profile(210.0, 34, Optional.empty(), Landforms.NONE);
    }

    /** Pharos: a vast, shallow basin west of the landing site, its far rim on the horizon. */
    public static BasinFeature.Config pharos() {
        return new BasinFeature.Config(-760, 8, 640, 30, 12, 0, 0.1, 3, 0.18, 1.2, s(PROTEUS.rock), Optional.of(s(PROTEUS.soil)), Optional.empty());
    }

    static void translations(Lang lang) {
        PROTEUS.translations(lang, "Proteus", "Proteus", "Proteus Regolith", "Proteus Regoliti", "Proteus Crust", "Proteus Kabuğu");
    }

    static void loot(SbBlockLoot loot) {
        PROTEUS.loot(loot);
    }

    static void tags(SbTags tags) {
        PROTEUS.tags(tags, NeptuneModule.TRITON_GROUND);
    }

    static void terrain(BootstrapContext<TerrainProfile> ctx) {
        ctx.register(PROTEUS.terrainKey(), profile());
    }

    static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        PROTEUS.configured(ctx, s(PROTEUS.soil));
        ctx.register(cf("proteus_pharos"), new ConfiguredFeature<>(OuterWorlds.BASIN.get(), pharos()));
    }

    static void placed(BootstrapContext<PlacedFeature> ctx) {
        PROTEUS.placed(ctx);
        OuterWorlds.placedWhole(ctx, "proteus_pharos");
    }

    static void biomes(BootstrapContext<Biome> ctx, HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        PROTEUS.biomes(ctx, pf, carvers, 0x3A3836, GenerationStep.Decoration.RAW_GENERATION, "proteus_pharos");
    }
}
