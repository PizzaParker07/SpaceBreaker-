package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** A long-lost lander, far out on the plains: placed sparsely, so the way to it is a rover trip. */
public class LanderWreckStructure extends Structure {
    public static final MapCodec<LanderWreckStructure> CODEC = simpleCodec(LanderWreckStructure::new);

    public LanderWreckStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMinBlockX();
        int z = ctx.chunkPos().getMinBlockZ();
        int y = ctx.chunkGenerator().getFirstOccupiedHeight(x + LanderWreckPiece.SIZE / 2, z + LanderWreckPiece.SIZE / 2,
                Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        long seed = ctx.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), b -> b.addPiece(new LanderWreckPiece(seed, x, y, z))));
    }

    @Override
    public StructureType<?> type() {
        return MarsStructures.LANDER.get();
    }
}
