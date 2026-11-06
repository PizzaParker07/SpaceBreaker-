package com.pizzaparker07.spacebreaker.content.moon.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Places an abandoned survey outpost on level-ish ground. */
public class SurveyOutpostStructure extends Structure {
    public static final MapCodec<SurveyOutpostStructure> CODEC = simpleCodec(SurveyOutpostStructure::new);

    public SurveyOutpostStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMinBlockX();
        int z = ctx.chunkPos().getMinBlockZ();
        int half = OutpostPiece.SIZE / 2;
        int sum = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int[] o : new int[][]{{0, 0}, {-half, -half}, {half, -half}, {-half, half}, {half, half}}) {
            int h = ctx.chunkGenerator().getFirstOccupiedHeight(x + o[0], z + o[1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            sum += h;
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        if (max - min > 9) {
            return Optional.empty(); // too rough: crater walls, steep hills
        }
        int y = Math.round(sum / 5F) + 1;
        long seed = ctx.random().nextLong();
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random());
        BlockPos pos = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(pos, b -> b.addPiece(new OutpostPiece(seed, x - half, y, z - half, facing))));
    }

    @Override
    public StructureType<?> type() {
        return MoonStructures.SURVEY_OUTPOST.get();
    }
}
