package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.MarsData;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;

/** Dungeons of Mars and its moons: structure and piece types, structures and structure sets (datagen). */
public final class MarsStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Spacebreaker.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<ResearchStationStructure>> STATION =
            TYPES.register("mars_research_station", () -> () -> ResearchStationStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<RiverbedCaveStructure>> RIVERBED =
            TYPES.register("mars_riverbed_cave", () -> () -> RiverbedCaveStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<MoonletDungeonStructure>> MOONLET =
            TYPES.register("moonlet_dungeon", () -> () -> MoonletDungeonStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> MOONLET_PIECE =
            PIECES.register("moonlet_dungeon", () -> MoonletDungeonPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> STATION_PIECE =
            PIECES.register("mars_research_station", () -> ResearchStationPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> RIVERBED_PIECE =
            PIECES.register("mars_riverbed_cave", () -> RiverbedCavePiece::new);

    public static final ResourceKey<Structure> STATION_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("mars_research_station"));
    public static final ResourceKey<Structure> RIVERBED_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("mars_riverbed_cave"));
    public static final ResourceKey<Structure> PHOBOS_PROBE_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("phobos_probe_wreck"));
    public static final ResourceKey<Structure> DEIMOS_CACHE_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("deimos_ice_cache"));
    public static final ResourceKey<StructureSet> PHOBOS_PROBE_SET = ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("phobos_probe_wrecks"));
    public static final ResourceKey<StructureSet> DEIMOS_CACHE_SET = ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("deimos_ice_caches"));
    public static final ResourceKey<StructureSet> STATION_SET = ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("mars_research_stations"));
    public static final ResourceKey<StructureSet> RIVERBED_SET = ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("mars_riverbed_caves"));

    private MarsStructures() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
        MarsData.EXTRA_STRUCTURES.add(MarsStructures::structures);
        MarsData.EXTRA_SETS.add(MarsStructures::sets);
    }

    static void structures(BootstrapContext<Structure> ctx) {
        HolderGetter<Biome> b = ctx.lookup(Registries.BIOME);
        ctx.register(STATION_KEY, new ResearchStationStructure(new Structure.StructureSettings(
                HolderSet.direct(b.getOrThrow(MarsData.PLAINS), b.getOrThrow(MarsData.HIGHLANDS), b.getOrThrow(MarsData.VALLEY)),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE)));
        ctx.register(RIVERBED_KEY, new RiverbedCaveStructure(new Structure.StructureSettings(
                HolderSet.direct(b.getOrThrow(MarsData.VALLEY), b.getOrThrow(MarsData.HIGHLANDS), b.getOrThrow(MarsData.PLAINS)),
                Map.of(), GenerationStep.Decoration.UNDERGROUND_STRUCTURES, TerrainAdjustment.NONE)));
        ctx.register(PHOBOS_PROBE_KEY, new MoonletDungeonStructure(new Structure.StructureSettings(
                HolderSet.direct(b.getOrThrow(MarsData.PHOBOS_SURFACE), b.getOrThrow(MarsData.PHOBOS_CRATER)),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE), false));
        ctx.register(DEIMOS_CACHE_KEY, new MoonletDungeonStructure(new Structure.StructureSettings(
                HolderSet.direct(b.getOrThrow(MarsData.DEIMOS_SURFACE)),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE), true));
    }

    static void sets(BootstrapContext<StructureSet> ctx) {
        HolderGetter<Structure> s = ctx.lookup(Registries.STRUCTURE);
        ctx.register(STATION_SET, new StructureSet(s.getOrThrow(STATION_KEY), new RandomSpreadStructurePlacement(36, 14, RandomSpreadType.LINEAR, 0x3A45A1)));
        ctx.register(PHOBOS_PROBE_SET, new StructureSet(s.getOrThrow(PHOBOS_PROBE_KEY), new RandomSpreadStructurePlacement(14, 6, RandomSpreadType.LINEAR, 0x9B0B05)));
        ctx.register(DEIMOS_CACHE_SET, new StructureSet(s.getOrThrow(DEIMOS_CACHE_KEY), new RandomSpreadStructurePlacement(12, 5, RandomSpreadType.LINEAR, 0xDE1A05)));
        ctx.register(RIVERBED_SET, new StructureSet(s.getOrThrow(RIVERBED_KEY), new RandomSpreadStructurePlacement(28, 10, RandomSpreadType.LINEAR, 0x41BEDD)));
    }
}
