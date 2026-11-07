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
    public static final DeferredHolder<StructureType<?>, StructureType<LanderStructure>> LANDER =
            TYPES.register("supply_lander", () -> () -> LanderStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> LANDER_PIECE =
            PIECES.register("supply_lander", () -> LanderPiece::new);
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
        for (LanderPiece.Variant v : LanderPiece.Variant.values()) {
            ctx.register(landerKey(v), new LanderStructure(new Structure.StructureSettings(
                    HolderSet.direct(landerBiomes(v).stream().map(b::getOrThrow).toList()),
                    Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE), v));
        }
    }

    public static void sets(BootstrapContext<StructureSet> ctx) {
        ctx.register(OUTPOST_SET, new StructureSet(ctx.lookup(Registries.STRUCTURE).getOrThrow(OUTPOST_KEY),
                new RandomSpreadStructurePlacement(10, 4, RandomSpreadType.LINEAR, 0xCE2E5)));
        int salt = 0x1A4D;
        for (LanderPiece.Variant v : LanderPiece.Variant.values()) {
            ctx.register(ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id(v.id() + "_landers")), new StructureSet(
                    ctx.lookup(Registries.STRUCTURE).getOrThrow(landerKey(v)), new RandomSpreadStructurePlacement(11, 4, RandomSpreadType.LINEAR, salt++)));
        }
    }

    public static ResourceKey<Structure> landerKey(LanderPiece.Variant v) {
        return ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id(v.id() + "_lander"));
    }

    /** Biomes a lander can lie in on each world. */
    static java.util.List<ResourceKey<net.minecraft.world.level.biome.Biome>> landerBiomes(LanderPiece.Variant v) {
        return switch (v) {
            case VESTA -> java.util.List.of(BeltData.VESTA_HIGHLANDS, BeltData.VESTA_CRATER);
            case GANYMEDE -> java.util.List.of(com.pizzaparker07.spacebreaker.content.jupiter.JupiterData.GANYMEDE_DARK,
                    com.pizzaparker07.spacebreaker.content.jupiter.JupiterData.GANYMEDE_GROOVED);
            case CALLISTO -> java.util.List.of(com.pizzaparker07.spacebreaker.content.jupiter.JupiterData.CALLISTO_PLAINS);
            case ENCELADUS -> java.util.List.of(com.pizzaparker07.spacebreaker.content.saturn.SaturnData.ENCELADUS_PLAINS,
                    com.pizzaparker07.spacebreaker.content.saturn.SaturnData.ENCELADUS_POLAR);
            case MIRANDA -> java.util.List.of(com.pizzaparker07.spacebreaker.content.uranus.UranusData.MIRANDA_PLAINS);
            case ARIEL -> java.util.List.of(com.pizzaparker07.spacebreaker.content.uranus.UranusData.ARIEL_PLAINS);
            case TITANIA -> java.util.List.of(com.pizzaparker07.spacebreaker.content.uranus.UranusData.TITANIA_UPLANDS);
            case UMBRIEL -> java.util.List.of(com.pizzaparker07.spacebreaker.content.uranus.Umbriel.PLAINS);
            case OBERON -> java.util.List.of(com.pizzaparker07.spacebreaker.content.uranus.Oberon.PLAINS);
            case NIX -> java.util.List.of(com.pizzaparker07.spacebreaker.content.pluto.PlutoMoonlets.NIX.plains);
            case HYDRA -> java.util.List.of(com.pizzaparker07.spacebreaker.content.pluto.PlutoMoonlets.HYDRA.plains);
            case PROTEUS -> java.util.List.of(com.pizzaparker07.spacebreaker.content.neptune.Proteus.PROTEUS.plains);
            case TRITON -> java.util.List.of(com.pizzaparker07.spacebreaker.content.neptune.NeptuneData.TRITON_CANTALOUPE,
                    com.pizzaparker07.spacebreaker.content.neptune.NeptuneData.TRITON_POLAR);
            case PLUTO -> java.util.List.of(com.pizzaparker07.spacebreaker.content.pluto.PlutoData.PLUTO_PLAINS);
            case CHARON -> java.util.List.of(com.pizzaparker07.spacebreaker.content.pluto.PlutoData.CHARON_PLAINS,
                    com.pizzaparker07.spacebreaker.content.pluto.PlutoData.CHARON_POLAR);
        };
    }
}
