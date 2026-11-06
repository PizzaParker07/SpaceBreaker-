package com.pizzaparker07.spacebreaker.content.moon.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.List;
import java.util.Optional;

/** Places an ancient lava tube under the maria, entered through a collapsed skylight. */
public class LavaTubeStructure extends Structure {
    public static final MapCodec<LavaTubeStructure> CODEC = simpleCodec(LavaTubeStructure::new);

    public LavaTubeStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMiddleBlockX();
        int z = ctx.chunkPos().getMiddleBlockZ();
        int top = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        long seed = ctx.random().nextLong();
        List<int[]> path = LavaTubePiece.walk(ctx.random(), x, z,
                (px, pz) -> ctx.chunkGenerator().getFirstOccupiedHeight(px, pz, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState()));
        return Optional.of(new GenerationStub(new BlockPos(x, top, z), b -> b.addPiece(new LavaTubePiece(seed, path, top))));
    }

    @Override
    public StructureType<?> type() {
        return MoonStructures.LAVA_TUBE.get();
    }
}
