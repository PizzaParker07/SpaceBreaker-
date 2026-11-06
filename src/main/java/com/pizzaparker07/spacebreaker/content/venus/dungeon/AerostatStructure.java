package com.pizzaparker07.spacebreaker.content.venus.dungeon;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.venus.VenusClimate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Places a derelict aerostat floating in the middle of the cloud deck. */
public class AerostatStructure extends Structure {
    public static final MapCodec<AerostatStructure> CODEC = simpleCodec(AerostatStructure::new);

    public AerostatStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMinBlockX();
        int z = ctx.chunkPos().getMinBlockZ();
        int y = VenusClimate.CLOUD_TOP - 9 + ctx.random().nextInt(4); // gondola in the cloud tops, envelope above them
        long seed = ctx.random().nextLong();
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random());
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), b -> b.addPiece(new AerostatPiece(seed, x, y, z, facing))));
    }

    @Override
    public StructureType<?> type() {
        return VenusStructures.AEROSTAT.get();
    }
}
