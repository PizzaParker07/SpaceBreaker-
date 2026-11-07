package com.pizzaparker07.spacebreaker.content.moon.dungeon;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Places an old ice-drilling station on a fairly flat floor of a permanently shadowed polar crater (a cold trap). */
public class IceDrillStructure extends Structure {
    public static final MapCodec<IceDrillStructure> CODEC = simpleCodec(IceDrillStructure::new);

    public IceDrillStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        if (!(ctx.chunkGenerator() instanceof PlanetChunkGenerator gen)) {
            return Optional.empty();
        }
        int half = IceDrillPiece.SIZE / 2;
        int cx = ctx.chunkPos().getMiddleBlockX();
        int cz = ctx.chunkPos().getMiddleBlockZ();
        if (gen.coldTrapFloor(cx, cz) == Integer.MIN_VALUE) {
            return Optional.empty();
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int shadowed = 0;
        for (int[] o : new int[][]{{0, 0}, {-half, -half}, {half, -half}, {-half, half}, {half, half}}) {
            if (gen.coldTrapFloor(cx + o[0], cz + o[1]) != Integer.MIN_VALUE) {
                shadowed++;
            }
            int h = gen.getFirstOccupiedHeight(cx + o[0], cz + o[1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        if (shadowed < 3 || max - min > 7) { // mostly in the shadow, on a fairly flat floor
            return Optional.empty();
        }
        int floorY = min;
        long seed = ctx.random().nextLong();
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random());
        return Optional.of(new GenerationStub(new BlockPos(cx, floorY, cz),
                b -> b.addPiece(new IceDrillPiece(seed, cx - half, floorY, cz - half, facing))));
    }

    @Override
    public StructureType<?> type() {
        return MoonStructures.ICE_DRILL.get();
    }
}
