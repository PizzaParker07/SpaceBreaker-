package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.worldgen.Crater;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Exactly one anchored lander per world, on the floor of the named crater Stickney: every chunk is a candidate, and
 * only the one holding the landing spot (a little off the crater's centre) builds it.
 */
public class StickneyLanderStructure extends Structure {
    public static final MapCodec<StickneyLanderStructure> CODEC = simpleCodec(StickneyLanderStructure::new);
    /** Offset of the landing spot from the crater's centre, in blocks. */
    public static final int OFFSET_X = -24;
    public static final int OFFSET_Z = 18;

    public StickneyLanderStructure(StructureSettings settings) {
        super(settings);
    }

    /** The lander's corner (piece origin, x/z) in a world whose generator has Stickney, else empty. */
    public static Optional<BlockPos> site(net.minecraft.world.level.chunk.ChunkGenerator generator) {
        if (!(generator instanceof PlanetChunkGenerator planet)) {
            return Optional.empty();
        }
        Optional<Crater> stickney = planet.terrain().namedCrater("stickney");
        return stickney.map(c -> new BlockPos(Mth.floor(c.x()) + OFFSET_X - StickneyLanderPiece.SIZE / 2, 0,
                Mth.floor(c.z()) + OFFSET_Z - StickneyLanderPiece.SIZE / 2));
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        Optional<BlockPos> site = site(ctx.chunkGenerator());
        if (site.isEmpty() || !new ChunkPos(site.get()).equals(ctx.chunkPos())) {
            return Optional.empty();
        }
        int x = site.get().getX();
        int z = site.get().getZ();
        int y = ctx.chunkGenerator().getFirstOccupiedHeight(x + StickneyLanderPiece.SIZE / 2, z + StickneyLanderPiece.SIZE / 2,
                Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        long seed = ctx.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), b -> b.addPiece(new StickneyLanderPiece(seed, x, y, z))));
    }

    @Override
    public StructureType<?> type() {
        return MarsStructures.STICKNEY.get();
    }
}
