package com.pizzaparker07.spacebreaker.content.venus.dungeon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.MarsData;
import com.pizzaparker07.spacebreaker.content.venus.VenusData;
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

/** The Venus dungeon: the derelict research aerostat. */
public final class VenusStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<StructureType<?>, StructureType<AerostatStructure>> AEROSTAT =
            TYPES.register("venus_aerostat", () -> () -> AerostatStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> AEROSTAT_PIECE =
            PIECES.register("venus_aerostat", () -> AerostatPiece::new);
    public static final ResourceKey<Structure> AEROSTAT_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("venus_aerostat"));
    public static final ResourceKey<StructureSet> AEROSTAT_SET = ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("venus_aerostats"));

    private VenusStructures() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
        MarsData.EXTRA_STRUCTURES.add(VenusStructures::structures);
        MarsData.EXTRA_SETS.add(VenusStructures::sets);
    }

    static void structures(BootstrapContext<Structure> ctx) {
        HolderGetter<Biome> b = ctx.lookup(Registries.BIOME);
        ctx.register(AEROSTAT_KEY, new AerostatStructure(new Structure.StructureSettings(
                HolderSet.direct(VenusData.BIOMES.stream().map(b::getOrThrow).toList()),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE)));
    }

    static void sets(BootstrapContext<StructureSet> ctx) {
        ctx.register(AEROSTAT_SET, new StructureSet(ctx.lookup(Registries.STRUCTURE).getOrThrow(AEROSTAT_KEY),
                new RandomSpreadStructurePlacement(32, 12, RandomSpreadType.LINEAR, 0x7E9A05)));
    }
}
