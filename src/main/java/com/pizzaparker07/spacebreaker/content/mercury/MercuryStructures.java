package com.pizzaparker07.spacebreaker.content.mercury;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.MarsData;
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

/** Mercury's dungeon: the abandoned polar ice mine. */
public final class MercuryStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<StructureType<?>, StructureType<IceMineStructure>> ICE_MINE =
            TYPES.register("mercury_ice_mine", () -> () -> IceMineStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ICE_MINE_PIECE =
            PIECES.register("mercury_ice_mine", () -> IceMinePiece::new);
    public static final ResourceKey<Structure> ICE_MINE_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("mercury_ice_mine"));
    public static final ResourceKey<StructureSet> ICE_MINE_SET = ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("mercury_ice_mines"));

    private MercuryStructures() {
    }

    static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
        MarsData.EXTRA_STRUCTURES.add(ctx -> structures(ctx));
        MarsData.EXTRA_SETS.add(ctx -> ctx.register(ICE_MINE_SET, new StructureSet(ctx.lookup(Registries.STRUCTURE).getOrThrow(ICE_MINE_KEY),
                new RandomSpreadStructurePlacement(8, 3, RandomSpreadType.LINEAR, 0x1CE0E0))));
    }

    static void structures(BootstrapContext<Structure> ctx) {
        var b = ctx.lookup(Registries.BIOME);
        ctx.register(ICE_MINE_KEY, new IceMineStructure(new Structure.StructureSettings(HolderSet.direct(b.getOrThrow(MercuryData.POLAR)),
                Map.of(), GenerationStep.Decoration.UNDERGROUND_STRUCTURES, TerrainAdjustment.NONE)));
    }
}
