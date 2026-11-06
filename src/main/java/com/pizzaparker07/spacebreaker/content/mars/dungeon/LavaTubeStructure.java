package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A natural Martian lava tube: the drained channel of an old lava flow, a few blocks under the surface of the
 * volcanic plains. The path follows the ground at a steady depth; collapsed skylights let you in.
 */
public class LavaTubeStructure extends Structure {
    public static final MapCodec<LavaTubeStructure> CODEC = simpleCodec(LavaTubeStructure::new);
    /** Roof thickness over the tube's ceiling, blocks. */
    public static final int ROOF = 7;

    public LavaTubeStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMiddleBlockX();
        int z = ctx.chunkPos().getMiddleBlockZ();
        long seed = ctx.random().nextLong();
        List<int[]> nodes = walk(seed, x, z, (px, pz) -> ctx.chunkGenerator().getFirstOccupiedHeight(px, pz, Heightmap.Types.WORLD_SURFACE_WG,
                ctx.heightAccessor(), ctx.randomState()));
        int top = nodes.get(0)[3];
        return Optional.of(new GenerationStub(new BlockPos(x, top, z), b -> b.addPiece(new LavaTubePiece(seed, nodes))));
    }

    /** Surface height lookup. */
    public interface Ground {
        int height(int x, int z);
    }

    static final int STEPS = 48;

    /** Nodes (x, y of the tube's centre, z, surface y) along a gently winding flow path. */
    public static List<int[]> walk(long seed, int x, int z, Ground ground) {
        RandomSource r = RandomSource.create(seed);
        List<int[]> path = new ArrayList<>();
        double a = r.nextDouble() * Mth.TWO_PI;
        double px = x;
        double pz = z;
        double turn = 0;
        double y = Double.NaN;
        for (int i = 0; i < STEPS; i++) {
            int gx = (int) Math.round(px);
            int gz = (int) Math.round(pz);
            int surface = ground.height(gx, gz);
            double want = surface - ROOF - LavaTubePiece.HALF_HEIGHT;
            // lava flowed smoothly: the tube follows the ground but never jumps
            y = Double.isNaN(y) ? want : Mth.clamp(want, y - 0.6, y + 0.6);
            path.add(new int[]{gx, (int) Math.round(y), gz, surface});
            turn = turn * 0.85 + (r.nextDouble() - 0.5) * 0.3;
            a += turn;
            px += Math.cos(a) * 3.0;
            pz += Math.sin(a) * 3.0;
        }
        return path;
    }

    @Override
    public StructureType<?> type() {
        return MarsStructures.LAVA_TUBE.get();
    }
}
