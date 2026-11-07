package com.pizzaparker07.spacebreaker.content.neptune.dungeon;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.neptune.NeptuneData;
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

/** Triton's second dungeon (beside the crashed lander): the geyser watch station on the southern cap. */
public final class NeptuneStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<StructureType<?>, StructureType<Station>> STATION =
            TYPES.register("triton_geyser_station", () -> () -> Station.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> STATION_PIECE =
            PIECES.register("triton_geyser_station", () -> GeyserStationPiece::new);
    public static final ResourceKey<Structure> STATION_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("triton_geyser_station"));

    private NeptuneStructures() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
    }

    public static void structures(BootstrapContext<Structure> ctx) {
        var b = ctx.lookup(Registries.BIOME);
        ctx.register(STATION_KEY, new Station(new Structure.StructureSettings(HolderSet.direct(b.getOrThrow(NeptuneData.TRITON_POLAR)),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE)));
    }

    public static void sets(BootstrapContext<StructureSet> ctx) {
        ctx.register(ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("triton_geyser_stations")), new StructureSet(
                ctx.lookup(Registries.STRUCTURE).getOrThrow(STATION_KEY), new RandomSpreadStructurePlacement(9, 3, RandomSpreadType.LINEAR, 0x7E15E)));
    }

    public static class Station extends Structure {
        public static final MapCodec<Station> CODEC = simpleCodec(Station::new);

        public Station(StructureSettings settings) {
            super(settings);
        }

        @Override
        protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
            int x = ctx.chunkPos().getMinBlockX();
            int z = ctx.chunkPos().getMinBlockZ();
            int sum = 0;
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int[] o : new int[][]{{7, 7}, {0, 0}, {14, 0}, {0, 14}, {14, 14}}) {
                int h = ctx.chunkGenerator().getFirstOccupiedHeight(x + o[0], z + o[1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
                sum += h;
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
            if (max - min > 5) {
                return Optional.empty();
            }
            int y = Math.round(sum / 5F);
            long seed = ctx.random().nextLong();
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random());
            return Optional.of(new GenerationStub(new BlockPos(x, y, z), b -> b.addPiece(new GeyserStationPiece(seed, x, y, z, facing))));
        }

        @Override
        public StructureType<?> type() {
            return STATION.get();
        }
    }
}
