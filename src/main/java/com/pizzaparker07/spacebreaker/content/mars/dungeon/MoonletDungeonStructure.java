package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Places a small moonlet dungeon (probe wreck or ice cache) on the surface. */
public class MoonletDungeonStructure extends Structure {
    public static final MapCodec<MoonletDungeonStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.BOOL.fieldOf("cache").forGetter(s -> s.cache)
    ).apply(i, MoonletDungeonStructure::new));
    private final boolean cache;

    public MoonletDungeonStructure(StructureSettings settings, boolean cache) {
        super(settings);
        this.cache = cache;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMinBlockX();
        int z = ctx.chunkPos().getMinBlockZ();
        int y = ctx.chunkGenerator().getFirstOccupiedHeight(x + 7, z + 7, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState()) - 1;
        long seed = ctx.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), b -> b.addPiece(new MoonletDungeonPiece(seed, cache, x, y, z))));
    }

    @Override
    public StructureType<?> type() {
        return MarsStructures.MOONLET.get();
    }
}
