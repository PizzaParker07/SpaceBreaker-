package com.pizzaparker07.spacebreaker.content.mercury;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Places the polar ice mine under the floor of a shadowed crater (only where the terrain says the floor is shadowed). */
public class IceMineStructure extends Structure {
    public static final MapCodec<IceMineStructure> CODEC = simpleCodec(IceMineStructure::new);

    public IceMineStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMinBlockX();
        int z = ctx.chunkPos().getMinBlockZ();
        if (ctx.chunkGenerator() instanceof com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator gen) {
            var t = gen.terrain();
            if (!t.column(x + 12, z + 20).shadowed(t.profile())) {
                return Optional.empty();
            }
        }
        int y = ctx.chunkGenerator().getFirstOccupiedHeight(x + 12, z + 20, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState()) - 1;
        long seed = ctx.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), b -> b.addPiece(new IceMinePiece(seed, x, y, z))));
    }

    @Override
    public StructureType<?> type() {
        return MercuryStructures.ICE_MINE.get();
    }
}
