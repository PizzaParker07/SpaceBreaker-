package com.pizzaparker07.spacebreaker.content.saturn.dungeon;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.saturn.SaturnData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
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
import java.util.Optional;

/** Titan's dungeon: the abandoned rotorcraft hangar. */
public final class SaturnStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<StructureType<?>, StructureType<Hangar>> HANGAR = TYPES.register("titan_hangar", () -> () -> Hangar.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> HANGAR_PIECE = PIECES.register("titan_hangar", () -> HangarPiece::new);
    public static final ResourceKey<Structure> HANGAR_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("titan_hangar"));

    private SaturnStructures() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
    }

    public static void structures(BootstrapContext<Structure> ctx) {
        var b = ctx.lookup(Registries.BIOME);
        ctx.register(HANGAR_KEY, new Hangar(new Structure.StructureSettings(HolderSet.direct(b.getOrThrow(SaturnData.TITAN_PLAINS)),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE)));
    }

    public static void sets(BootstrapContext<StructureSet> ctx) {
        ctx.register(ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("titan_hangars")), new StructureSet(
                ctx.lookup(Registries.STRUCTURE).getOrThrow(HANGAR_KEY), new RandomSpreadStructurePlacement(12, 5, RandomSpreadType.LINEAR, 0x717A4)));
    }

    public static class Hangar extends Structure {
        public static final MapCodec<Hangar> CODEC = simpleCodec(Hangar::new);

        public Hangar(StructureSettings settings) {
            super(settings);
        }

        @Override
        protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
            int x = ctx.chunkPos().getMinBlockX();
            int z = ctx.chunkPos().getMinBlockZ();
            int sum = 0;
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int[] o : new int[][]{{8, 12}, {0, 0}, {16, 0}, {0, 24}, {16, 24}}) {
                int h = ctx.chunkGenerator().getFirstOccupiedHeight(x + o[0], z + o[1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
                sum += h;
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
            if (max - min > 6) {
                return Optional.empty();
            }
            int y = Math.round(sum / 5F);
            long seed = ctx.random().nextLong();
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random());
            return Optional.of(new GenerationStub(new BlockPos(x, y, z), b -> b.addPiece(new HangarPiece(seed, x, y, z, facing))));
        }

        @Override
        public StructureType<?> type() {
            return HANGAR.get();
        }
    }
}
