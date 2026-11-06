package com.pizzaparker07.spacebreaker.content.jupiter.dungeon;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.jupiter.JupiterData;
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

/** Dungeons of the Galilean moons: the buried probe on Europa and the volcano observatory on Io. */
public final class JupiterStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<StructureType<?>, StructureType<Probe>> PROBE = TYPES.register("europa_probe", () -> () -> Probe.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PROBE_PIECE = PIECES.register("europa_probe", () -> ProbePiece::new);
    public static final DeferredHolder<StructureType<?>, StructureType<Observatory>> OBSERVATORY =
            TYPES.register("io_observatory", () -> () -> Observatory.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> OBSERVATORY_PIECE =
            PIECES.register("io_observatory", () -> ObservatoryPiece::new);
    public static final ResourceKey<Structure> PROBE_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("europa_probe"));
    public static final ResourceKey<Structure> OBSERVATORY_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("io_observatory"));

    private JupiterStructures() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
    }

    public static void structures(BootstrapContext<Structure> ctx) {
        var b = ctx.lookup(Registries.BIOME);
        ctx.register(PROBE_KEY, new Probe(new Structure.StructureSettings(HolderSet.direct(b.getOrThrow(JupiterData.EUROPA_PLAINS)),
                Map.of(), GenerationStep.Decoration.UNDERGROUND_STRUCTURES, TerrainAdjustment.NONE)));
        ctx.register(OBSERVATORY_KEY, new Observatory(new Structure.StructureSettings(HolderSet.direct(b.getOrThrow(JupiterData.IO_PLAINS)),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE)));
    }

    public static void sets(BootstrapContext<StructureSet> ctx) {
        var s = ctx.lookup(Registries.STRUCTURE);
        ctx.register(ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("europa_probes")), new StructureSet(s.getOrThrow(PROBE_KEY),
                new RandomSpreadStructurePlacement(11, 4, RandomSpreadType.LINEAR, 0xE0A0B5)));
        ctx.register(ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("io_observatories")), new StructureSet(s.getOrThrow(OBSERVATORY_KEY),
                new RandomSpreadStructurePlacement(12, 5, RandomSpreadType.LINEAR, 0x10B5E7)));
    }

    /** Surface height averaged at five points; empty if the ground is too uneven. */
    static Optional<Integer> level(Structure.GenerationContext ctx, int x, int z, int half, int maxStep) {
        int sum = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int[] o : new int[][]{{0, 0}, {-half, -half}, {half, -half}, {-half, half}, {half, half}}) {
            int h = ctx.chunkGenerator().getFirstOccupiedHeight(x + o[0], z + o[1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            sum += h;
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        return max - min > maxStep ? Optional.empty() : Optional.of(Math.round(sum / 5F) - 1);
    }

    public static class Probe extends Structure {
        public static final MapCodec<Probe> CODEC = simpleCodec(Probe::new);

        public Probe(StructureSettings settings) {
            super(settings);
        }

        @Override
        protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
            int x = ctx.chunkPos().getMinBlockX();
            int z = ctx.chunkPos().getMinBlockZ();
            int half = ProbePiece.SIZE / 2;
            return level(ctx, x, z, half, 10).map(y -> {
                long seed = ctx.random().nextLong();
                Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random());
                return new GenerationStub(new BlockPos(x, y, z), b -> b.addPiece(new ProbePiece(seed, x - half, y, z - half, facing)));
            });
        }

        @Override
        public StructureType<?> type() {
            return PROBE.get();
        }
    }

    public static class Observatory extends Structure {
        public static final MapCodec<Observatory> CODEC = simpleCodec(Observatory::new);

        public Observatory(StructureSettings settings) {
            super(settings);
        }

        @Override
        protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
            int x = ctx.chunkPos().getMinBlockX();
            int z = ctx.chunkPos().getMinBlockZ();
            int half = ObservatoryPiece.SIZE / 2;
            return level(ctx, x, z, half, 6).map(y -> {
                long seed = ctx.random().nextLong();
                Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random());
                return new GenerationStub(new BlockPos(x, y + 1, z), b -> b.addPiece(new ObservatoryPiece(seed, x - half, y + 1, z - half, facing)));
            });
        }

        @Override
        public StructureType<?> type() {
            return OBSERVATORY.get();
        }
    }
}
