package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Places Deimos' relay spire with its ring of hopping stacks on the dusty plains. */
public class HopSpireStructure extends Structure {
    public static final MapCodec<HopSpireStructure> CODEC = simpleCodec(HopSpireStructure::new);

    public HopSpireStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMinBlockX();
        int z = ctx.chunkPos().getMinBlockZ();
        int y = ctx.chunkGenerator().getFirstOccupiedHeight(x + HopSpirePiece.SIZE / 2, z + HopSpirePiece.SIZE / 2,
                Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        long seed = ctx.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), b -> b.addPiece(new HopSpirePiece(seed, x, y, z))));
    }

    @Override
    public StructureType<?> type() {
        return MarsStructures.HOP_SPIRE.get();
    }
}
