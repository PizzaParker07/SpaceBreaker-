package com.pizzaparker07.spacebreaker.content.belt.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Places a crashed supply lander on fairly level ground; the variant picks the world's ground and cargo. */
public class LanderStructure extends Structure {
    public static final MapCodec<LanderStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.xmap(s -> LanderPiece.Variant.valueOf(s.toUpperCase(java.util.Locale.ROOT)), LanderPiece.Variant::id)
                    .fieldOf("variant").forGetter(s -> s.variant)
    ).apply(i, LanderStructure::new));

    private final LanderPiece.Variant variant;

    public LanderStructure(StructureSettings settings, LanderPiece.Variant variant) {
        super(settings);
        this.variant = variant;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMinBlockX();
        int z = ctx.chunkPos().getMinBlockZ();
        int sum = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int[] o : new int[][]{{6, 13}, {0, 0}, {12, 0}, {0, 26}, {12, 26}}) {
            int h = ctx.chunkGenerator().getFirstOccupiedHeight(x + o[0], z + o[1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            sum += h;
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        if (max - min > 7) {
            return Optional.empty();
        }
        int y = Math.round(sum / 5F);
        long seed = ctx.random().nextLong();
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random());
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), b -> b.addPiece(new LanderPiece(seed, variant, x, y, z, facing))));
    }

    @Override
    public StructureType<?> type() {
        return BeltStructures.LANDER.get();
    }
}
