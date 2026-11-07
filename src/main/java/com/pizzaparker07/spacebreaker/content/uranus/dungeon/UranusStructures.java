package com.pizzaparker07.spacebreaker.content.uranus.dungeon;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.world.Columns;
import com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature;
import com.pizzaparker07.spacebreaker.content.uranus.UranusData;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
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

/** Titania's second dungeon (beside the crashed lander): the survey outpost on the rim of a great canyon. */
public final class UranusStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<StructureType<?>, StructureType<Outpost>> OUTPOST =
            TYPES.register("titania_canyon_outpost", () -> () -> Outpost.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> OUTPOST_PIECE =
            PIECES.register("titania_canyon_outpost", () -> CanyonOutpostPiece::new);
    public static final ResourceKey<Structure> OUTPOST_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("titania_canyon_outpost"));
    /** How far from a canyon's floor edge the middle of the piece stands (blocks): the hut on the rim, the balcony over the wall. */
    public static final double RIM_MIN = 13;
    public static final double RIM_MAX = 21;

    private UranusStructures() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
    }

    public static void structures(BootstrapContext<Structure> ctx) {
        var b = ctx.lookup(Registries.BIOME);
        ctx.register(OUTPOST_KEY, new Outpost(new Structure.StructureSettings(HolderSet.direct(b.getOrThrow(UranusData.TITANIA_UPLANDS)),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE)));
    }

    public static void sets(BootstrapContext<StructureSet> ctx) {
        ctx.register(ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("titania_canyon_outposts")), new StructureSet(
                ctx.lookup(Registries.STRUCTURE).getOrThrow(OUTPOST_KEY), new RandomSpreadStructurePlacement(4, 1, RandomSpreadType.LINEAR, 0x71A0C)));
    }

    /**
     * Where an outpost stands for a piece whose corner is at (x, z): the side of a canyon it is on and the way it faces
     * (toward the canyon's middle), or empty if that spot is not on a rim.
     */
    public static Optional<Direction> rimFacing(PlanetTerrain t, int x, int z) {
        GrabenFeature.Config cfg = UranusData.canyons();
        double cx = x + CanyonOutpostPiece.W / 2.0;
        double cz = z + CanyonOutpostPiece.D / 2.0;
        for (GrabenFeature.Graben g : cfg.grabens()) {
            double[] uv = GrabenFeature.local(t, g, cx, cz);
            // keep away from the tapering ends
            if (Math.abs(uv[0]) > g.length() * 0.38) {
                continue;
            }
            double fromFloor = Math.abs(uv[1]) - g.width() / 2;
            if (fromFloor < RIM_MIN || fromFloor > RIM_MAX) {
                continue;
            }
            double a = Math.toRadians(g.angleDeg());
            // the v axis is (-sin a, cos a); the canyon's middle lies against the sign of v
            double sign = uv[1] > 0 ? -1 : 1;
            return Optional.of(Direction.getNearest(-Math.sin(a) * sign, 0, Math.cos(a) * sign));
        }
        return Optional.empty();
    }

    public static class Outpost extends Structure {
        public static final MapCodec<Outpost> CODEC = simpleCodec(Outpost::new);

        public Outpost(StructureSettings settings) {
            super(settings);
        }

        @Override
        protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
            PlanetTerrain t = Columns.terrain(ctx.chunkGenerator());
            if (t == null) {
                return Optional.empty();
            }
            int x = ctx.chunkPos().getMinBlockX();
            int z = ctx.chunkPos().getMinBlockZ();
            Optional<Direction> facing = rimFacing(t, x, z);
            if (facing.isEmpty()) {
                return Optional.empty();
            }
            int sum = 0;
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int[] o : new int[][]{{6, 8}, {0, 0}, {12, 0}, {0, 16}, {12, 16}}) {
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
            return Optional.of(new GenerationStub(new BlockPos(x, y, z), b -> b.addPiece(new CanyonOutpostPiece(seed, x, y, z, facing.get()))));
        }

        @Override
        public StructureType<?> type() {
            return OUTPOST.get();
        }
    }
}
