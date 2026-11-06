package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Places an ancient buried riverbed cave, entered through a sinkhole. */
public class RiverbedCaveStructure extends Structure {
    public static final MapCodec<RiverbedCaveStructure> CODEC = simpleCodec(RiverbedCaveStructure::new);

    public RiverbedCaveStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMiddleBlockX();
        int z = ctx.chunkPos().getMiddleBlockZ();
        int top = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        long seed = ctx.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x, top, z), b -> b.addPiece(new RiverbedCavePiece(seed, x, top, z))));
    }

    @Override
    public StructureType<?> type() {
        return MarsStructures.RIVERBED.get();
    }
}
