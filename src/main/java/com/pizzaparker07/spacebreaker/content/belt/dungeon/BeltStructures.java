package com.pizzaparker07.spacebreaker.content.belt.dungeon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.BeltData;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
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

/** The belt's dungeon: the abandoned ice mining outpost on Ceres. */
public final class BeltStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<StructureType<?>, StructureType<MiningOutpostStructure>> OUTPOST =
            TYPES.register("ceres_mining_outpost", () -> () -> MiningOutpostStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> OUTPOST_PIECE =
            PIECES.register("ceres_mining_outpost", () -> MiningOutpostPiece::new);
    public static final ResourceKey<Structure> OUTPOST_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("ceres_mining_outpost"));
    public static final ResourceKey<StructureSet> OUTPOST_SET = ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("ceres_mining_outposts"));

    private BeltStructures() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
    }

    public static void structures(BootstrapContext<Structure> ctx) {
        var b = ctx.lookup(Registries.BIOME);
        ctx.register(OUTPOST_KEY, new MiningOutpostStructure(new Structure.StructureSettings(
                HolderSet.direct(b.getOrThrow(BeltData.CERES_PLAINS), b.getOrThrow(BeltData.CERES_CRATER)),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE)));
    }

    public static void sets(BootstrapContext<StructureSet> ctx) {
        ctx.register(OUTPOST_SET, new StructureSet(ctx.lookup(Registries.STRUCTURE).getOrThrow(OUTPOST_KEY),
                new RandomSpreadStructurePlacement(10, 4, RandomSpreadType.LINEAR, 0xCE2E5)));
    }
}
