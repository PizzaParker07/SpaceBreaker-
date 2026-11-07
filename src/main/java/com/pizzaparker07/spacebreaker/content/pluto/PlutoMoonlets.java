package com.pizzaparker07.spacebreaker.content.pluto;

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
import net.minecraft.world.level.material.MapColor;

import java.util.List;
import java.util.Optional;

import static com.pizzaparker07.spacebreaker.content.belt.OuterWorlds.s;

/**
 * Nix and Hydra, two of Pluto's small outer moons: lumpy shards of water ice a few tens of kilometres long, spinning
 * out of step with everything, where a jump nearly reaches orbit. Nix is grey ice with a large reddish stain (tholin
 * swept up from around Pluto); Hydra is the brightest thing in Pluto's system, almost pure, clean water ice.
 */
public final class PlutoMoonlets {
    public static final TinyWorld NIX = new TinyWorld(PlutoModule.KIT, "nix", "nix_frost", MapColor.COLOR_LIGHT_GRAY, "nix_ice",
            MapColor.TERRACOTTA_LIGHT_GRAY, true);
    public static final TinyWorld HYDRA = new TinyWorld(PlutoModule.KIT, "hydra", "hydra_frost", MapColor.SNOW, "hydra_ice",
            MapColor.QUARTZ, true);

    private PlutoMoonlets() {
    }

    /** Loads the block fields before the kit registers. */
    static void load() {
    }

    public static TerrainProfile nixProfile() {
        // the reddish stain: Pluto's tholin dust settled on one side
        var lf = new Landforms(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                List.of(new Landforms.SoilPatch(s(PlutoModule.THOLIN_REGOLITH), 260, 0.7, "any")), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
        return NIX.profile(18.6, 26, Optional.empty(), lf);
    }

    public static TerrainProfile hydraProfile() {
        return HYDRA.profile(19.2, 30, Optional.empty(), Landforms.NONE);
    }

    static void translations(Lang lang) {
        NIX.translations(lang, "Nix", "Nix", "Nix Frost", "Nix Kırağısı", "Nix Ice", "Nix Buzu");
        HYDRA.translations(lang, "Hydra", "Hydra", "Hydra Frost", "Hydra Kırağısı", "Hydra Ice", "Hydra Buzu");
    }

    static void loot(SbBlockLoot loot) {
        NIX.loot(loot);
        HYDRA.loot(loot);
    }

    static void tags(SbTags tags) {
        NIX.tags(tags, PlutoModule.PLUTONIAN_GROUND);
        HYDRA.tags(tags, PlutoModule.PLUTONIAN_GROUND);
    }

    static void terrain(BootstrapContext<TerrainProfile> ctx) {
        ctx.register(NIX.terrainKey(), nixProfile());
        ctx.register(HYDRA.terrainKey(), hydraProfile());
    }

    static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        NIX.configured(ctx, s(PlutoModule.THOLIN_REGOLITH));
        HYDRA.configured(ctx, s(HYDRA.soil));
    }

    static void placed(BootstrapContext<PlacedFeature> ctx) {
        NIX.placed(ctx);
        HYDRA.placed(ctx);
    }

    static void biomes(BootstrapContext<Biome> ctx, HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        NIX.biomes(ctx, pf, carvers, 0x9A9690);
        HYDRA.biomes(ctx, pf, carvers, 0xD8DCDF);
    }

    /** The moonlets as worlds of the outer system (spawns, landers). */
    public static List<TinyWorld> all() {
        return List.of(NIX, HYDRA);
    }

}
