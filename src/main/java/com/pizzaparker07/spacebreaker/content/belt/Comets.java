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
 * Comets, each a small package on the shared {@link TinyWorld} base. 67P/Churyumov-Gerasimenko: a dark, two-lobed
 * nucleus a few kilometres long, its ice hidden under a crust of dust darker than charcoal, with jets of gas and dust
 * rising from the sunlit ground (visual only). Its ice is water ice: a refuel stop far out on its orbit.
 */
public final class Comets {
    public static final TinyWorld CHURYUMOV = new TinyWorld(BeltModule.KIT, "churyumov_gerasimenko", "comet_dust", MapColor.COLOR_BLACK,
            "comet_ice", MapColor.DEEPSLATE, true);
    /** Where a jet rises when the sun is up. */
    public static final DeferredBlock<CometJetBlock> JET = BeltModule.KIT.block("comet_jet", CometJetBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.8F).sound(SoundType.GRAVEL));

    private Comets() {
    }

    static void load() {
    }

    public static TerrainProfile churyumovProfile() {
        return CHURYUMOV.profile(1.65, 22, Optional.empty(), Landforms.NONE);
    }

    static void translations(Lang lang) {
        CHURYUMOV.translations(lang, "67P/Churyumov-Gerasimenko", "67P/Çuryumov-Gerasimenko", "Comet Dust", "Kuyruklu Yıldız Tozu",
                "Comet Ice", "Kuyruklu Yıldız Buzu");
        lang.block(JET, "Comet Jet", "Kuyruklu Yıldız Jeti");
    }

    static void loot(SbBlockLoot loot) {
        CHURYUMOV.loot(loot);
        loot.self(JET.get());
    }

    static void tags(SbTags tags) {
        CHURYUMOV.tags(tags, NearEarthAsteroids.GROUND);
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, JET.get());
    }

    static void terrain(BootstrapContext<TerrainProfile> ctx) {
        ctx.register(CHURYUMOV.terrainKey(), churyumovProfile());
    }

    static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        CHURYUMOV.configured(ctx, s(CHURYUMOV.soil), 0.8F, 2.4F);
        ctx.register(cf("comet_jets"), new ConfiguredFeature<>(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(JET.get()))));
    }

    static void placed(BootstrapContext<PlacedFeature> ctx) {
        CHURYUMOV.placed(ctx, 2);
        // a jet every few chunks, set into the surface
        PlacementUtils.register(ctx, pf("comet_jets"), ctx.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(cf("comet_jets")),
                RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
    }

    static void biomes(BootstrapContext<Biome> ctx, HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        CHURYUMOV.biomes(ctx, pf, carvers, 0x2B2927, GenerationStep.Decoration.SURFACE_STRUCTURES, "comet_jets");
    }
}
