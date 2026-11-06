package com.pizzaparker07.spacebreaker.content.belt.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Places the Ceres mining outpost on fairly level ground. */
public class MiningOutpostStructure extends Structure {
    public static final MapCodec<MiningOutpostStructure> CODEC = simpleCodec(MiningOutpostStructure::new);

    public MiningOutpostStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMinBlockX();
        int z = ctx.chunkPos().getMinBlockZ();
        int half = MiningOutpostPiece.SIZE / 2;
        int sum = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int[] o : new int[][]{{0, 0}, {-half, -half}, {half, -half}, {-half, half}, {half, half}}) {
            int h = ctx.chunkGenerator().getFirstOccupiedHeight(x + o[0], z + o[1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            sum += h;
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        if (max - min > 8) {
            return Optional.empty();
        }
        int y = Math.round(sum / 5F);
        long seed = ctx.random().nextLong();
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random());
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), b -> b.addPiece(new MiningOutpostPiece(seed, x - half, y, z - half, facing))));
    }

    @Override
    public StructureType<?> type() {
        return BeltStructures.OUTPOST.get();
    }
}
