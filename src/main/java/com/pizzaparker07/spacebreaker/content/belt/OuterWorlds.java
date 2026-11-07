package com.pizzaparker07.spacebreaker.content.belt;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.CraterFloorFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.LakeFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.LineaeFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.VentFeature;
import com.pizzaparker07.spacebreaker.content.worldgen.BoulderFeature;
import com.pizzaparker07.spacebreaker.content.worldgen.WorldgenModule;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Shared code of the outer-system worlds: surface features that shape the ground beyond what the planet engine's
 * terrain profile does (salt faculae on crater floors, a giant polar impact basin, cracked ice with stained lineae,
 * liquid lakes, vents and geysers), and helpers for biomes and features used by every world's datagen.
 */
public final class OuterWorlds implements SbModule {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<Feature<?>, CraterFloorFeature> CRATER_FLOOR = FEATURES.register("crater_floor_deposit", CraterFloorFeature::new);
    public static final DeferredHolder<Feature<?>, BasinFeature> BASIN = FEATURES.register("impact_basin", BasinFeature::new);
    public static final DeferredHolder<Feature<?>, LineaeFeature> LINEAE = FEATURES.register("lineae", LineaeFeature::new);
    public static final DeferredHolder<Feature<?>, LakeFeature> LAKES = FEATURES.register("surface_lakes", LakeFeature::new);
    public static final DeferredHolder<Feature<?>, VentFeature> VENTS = FEATURES.register("surface_vents", VentFeature::new);
    public static final DeferredHolder<Feature<?>, com.pizzaparker07.spacebreaker.content.belt.world.LayerFeature> LAYER =
            FEATURES.register("buried_layer", com.pizzaparker07.spacebreaker.content.belt.world.LayerFeature::new);
    public static final DeferredHolder<Feature<?>, com.pizzaparker07.spacebreaker.content.belt.world.CellsFeature> CELLS =
            FEATURES.register("polygon_cells", com.pizzaparker07.spacebreaker.content.belt.world.CellsFeature::new);
    public static final DeferredHolder<Feature<?>, com.pizzaparker07.spacebreaker.content.belt.world.CoronaFeature> CORONAE =
            FEATURES.register("coronae", com.pizzaparker07.spacebreaker.content.belt.world.CoronaFeature::new);
    public static final DeferredHolder<Feature<?>, com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature> GRABENS =
            FEATURES.register("grabens", com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature::new);
    public static final DeferredHolder<Feature<?>, com.pizzaparker07.spacebreaker.content.belt.world.RidgeFeature> RIDGE =
            FEATURES.register("latitude_ridge", com.pizzaparker07.spacebreaker.content.belt.world.RidgeFeature::new);
    public static final DeferredHolder<Feature<?>, com.pizzaparker07.spacebreaker.content.belt.world.RingFeature> MARKS =
            FEATURES.register("ground_marks", com.pizzaparker07.spacebreaker.content.belt.world.RingFeature::new);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Spacebreaker.MOD_ID);
    /** Vent blocks of every outer world (added by the region modules before registration). */
    public static final List<Supplier<? extends Block>> VENT_BLOCKS = new ArrayList<>();
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<com.pizzaparker07.spacebreaker.content.belt.block.VentBlockEntity>> VENT =
            BLOCK_ENTITIES.register("vent", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    com.pizzaparker07.spacebreaker.content.belt.block.VentBlockEntity::new,
                    VENT_BLOCKS.stream().map(Supplier::get).toArray(Block[]::new)).build(null));

    /** A creature spawn entry for one biome, collected before datagen builds the biomes. */
    public record Spawn(ResourceKey<Biome> biome, Supplier<? extends EntityType<?>> type, MobCategory category, int weight, int min, int max) {
    }

    public static final List<Spawn> SPAWNS = new ArrayList<>();

    @Override
    public String id() {
        return "outer_worlds";
    }

    @Override
    public void register(IEventBus modBus) {
        FEATURES.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    // ------------------------------------------------------------------ keys and helpers

    public static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, Spacebreaker.id(name));
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> cf(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Spacebreaker.id(name));
    }

    public static ResourceKey<PlacedFeature> pf(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Spacebreaker.id(name));
    }

    public static BlockState s(DeferredBlock<? extends Block> b) {
        return b.get().defaultBlockState();
    }

    /** Boulders of mixed rocks: pairs of (BlockState, weight). */
    public static ConfiguredFeature<?, ?> boulder(float min, float max, float buried, Object... weighted) {
        SimpleWeightedRandomList.Builder<BlockState> list = SimpleWeightedRandomList.builder();
        for (int i = 0; i < weighted.length; i += 2) {
            list.add((BlockState) weighted[i], (Integer) weighted[i + 1]);
        }
        return new ConfiguredFeature<>(WorldgenModule.BOULDER.get(), new BoulderFeature.Config(new WeightedStateProvider(list),
                UniformFloat.of(min, max), buried));
    }

    /** An ore vein of the given size in a world's bedrock (configured feature {@code name}). */
    public static void ore(BootstrapContext<ConfiguredFeature<?, ?>> ctx, String name, TagKey<Block> stone, BlockState ore, int size) {
        ctx.register(cf(name), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(new TagMatchTest(stone), ore, size)));
    }

    /** Places ore veins {@code count} times per chunk between y -40 and 100. */
    public static void placedOre(BootstrapContext<PlacedFeature> ctx, String name, int count) {
        PlacementUtils.register(ctx, pf(name), ctx.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(cf(name)), CountPlacement.of(count),
                InSquarePlacement.spread(), HeightRangePlacement.uniform(VerticalAnchor.absolute(-40), VerticalAnchor.absolute(100)), BiomeFilter.biome());
    }

    /** Places boulders on the surface, on average once every {@code rarity} chunks. */
    public static void placedBoulder(BootstrapContext<PlacedFeature> ctx, String name, int rarity) {
        PlacementUtils.register(ctx, pf(name), ctx.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(cf(name)), RarityFilter.onAverageOnceEvery(rarity),
                InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
    }

    /** Places whole-world features (they shape every chunk themselves) once per chunk. */
    public static void placedWhole(BootstrapContext<PlacedFeature> ctx, String... names) {
        var cfs = ctx.lookup(Registries.CONFIGURED_FEATURE);
        for (String name : names) {
            PlacementUtils.register(ctx, pf(name), cfs.getOrThrow(cf(name)), BiomeFilter.biome());
        }
    }

    /** Creature spawns registered for a biome. */
    public static MobSpawnSettings spawnsFor(ResourceKey<Biome> key) {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder().creatureGenerationProbability(0.04F);
        for (Spawn s : SPAWNS) {
            if (s.biome() == key) {
                spawns.addSpawn(s.category(), new MobSpawnSettings.SpawnerData(s.type().get(), s.weight(), s.min(), s.max()));
            }
        }
        return spawns.build();
    }

    /** Sky and fog colours of a biome plus optional ambient particles. */
    public record Look(int sky, int fog, int grass, AmbientParticleSettings particles) {
        public static Look airless(int grass) {
            return new Look(0x000000, 0x000000, grass, null);
        }
    }

    /**
     * A biome of an outer-system world. Placed features are listed per generation step in the order given.
     *
     * @param steps pairs of (GenerationStep.Decoration, placed feature name)
     */
    public static Biome biome(HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers, ResourceKey<Biome> key,
                              float temperature, Look look, Object... steps) {
        BiomeGenerationSettings.Builder gen = new BiomeGenerationSettings.Builder(pf, carvers);
        for (int i = 0; i < steps.length; i += 2) {
            gen.addFeature((GenerationStep.Decoration) steps[i], pf.getOrThrow(pf((String) steps[i + 1])));
        }
        BiomeSpecialEffects.Builder fx = new BiomeSpecialEffects.Builder()
                .fogColor(look.fog()).skyColor(look.sky()).waterColor(0x3F76E4).waterFogColor(0x050533)
                .grassColorOverride(look.grass()).foliageColorOverride(look.grass());
        if (look.particles() != null) {
            fx.ambientParticle(look.particles());
        }
        var music = music(key, look);
        if (music != null) {
            fx.backgroundMusic(new net.minecraft.sounds.Music(music, 3600, 12000, false));
        }
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false).temperature(temperature).downfall(0F)
                .specialEffects(fx.build())
                .mobSpawnSettings(spawnsFor(key))
                .generationSettings(gen.build())
                .build();
    }

    /**
     * Ambient music of a biome: Titan's own track under its haze, the Jovian track on Jupiter's moons, the belt's
     * track on Ceres and Vesta, the ice giants' track from Uranus out to Pluto, and the Moon's low hum on the rest (Enceladus).
     */
    static net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> music(ResourceKey<Biome> key, Look look) {
        String world = key.location().getPath().split("_")[0];
        return switch (world) {
            case "titan" -> com.pizzaparker07.spacebreaker.content.audio.SbSounds.MUSIC_TITAN;
            case "io", "europa", "ganymede", "callisto" -> com.pizzaparker07.spacebreaker.content.audio.SbSounds.MUSIC_JOVIAN;
            case "ceres", "vesta", "bennu", "ryugu", "eros", "itokawa", "churyumov", "encke", "halley", "tempel", "hale" -> com.pizzaparker07.spacebreaker.content.audio.SbSounds.MUSIC_BELT;
            case "miranda", "ariel", "titania", "umbriel", "oberon", "triton", "proteus", "nereid", "pluto", "charon", "nix", "hydra" -> com.pizzaparker07.spacebreaker.content.audio.SbSounds.MUSIC_ICE_GIANTS;
            default -> look.sky() == 0 && look.fog() == 0 ? com.pizzaparker07.spacebreaker.content.audio.SbSounds.MUSIC_MOON : null;
        };
    }

    /** Runs several bootstraps in order (used to combine one world's entries). */
    @SafeVarargs
    public static <T> Consumer<T> all(Consumer<T>... parts) {
        return t -> {
            for (Consumer<T> p : parts) {
                p.accept(t);
            }
        };
    }
}
