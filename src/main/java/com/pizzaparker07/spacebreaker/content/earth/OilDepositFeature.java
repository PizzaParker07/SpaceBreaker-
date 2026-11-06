package com.pizzaparker07.spacebreaker.content.earth;

import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A crude-oil reservoir: a lens-shaped pocket of oil trapped in rock, with a small seep of tar-stained
 * ground and a puddle of oil on the surface right above it, so prospectors can find it.
 */
public class OilDepositFeature extends Feature<NoneFeatureConfiguration> {
    public OilDepositFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        int top = Math.min(48, surface - 14);
        if (top < -40) {
            return false;
        }
        int cy = top - random.nextInt(Math.max(1, Math.min(40, top + 48)));
        int rx = 3 + random.nextInt(3);
        int rz = 3 + random.nextInt(3);
        int ry = 2 + random.nextInt(2);
        BlockState oil = FluidsModule.CRUDE_OIL.block.get().defaultBlockState();
        int placed = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                for (int dy = -ry; dy <= ry; dy++) {
                    double d = (dx * dx) / (double) (rx * rx) + (dz * dz) / (double) (rz * rz) + (dy * dy) / (double) (ry * ry);
                    if (d > 1.0) {
                        continue;
                    }
                    p.set(origin.getX() + dx, cy + dy, origin.getZ() + dz);
                    if (isRock(level.getBlockState(p)) && sealed(level, p)) {
                        level.setBlock(p, oil, Block.UPDATE_CLIENTS);
                        placed++;
                    }
                }
            }
        }
        if (placed == 0) {
            return false;
        }
        placeSeep(level, random, origin.getX(), origin.getZ(), oil);
        return true;
    }

    private static boolean isRock(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    /** Oil only replaces rock that has no open air next to it, so it never spills into caves. */
    private static boolean sealed(WorldGenLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            if (n.isAir() || (!n.getFluidState().isEmpty() && !n.is(FluidsModule.CRUDE_OIL.block.get()))) {
                return false;
            }
        }
        return true;
    }

    private static void placeSeep(WorldGenLevel level, RandomSource random, int x, int z, BlockState oil) {
        BlockState seep = EarthModule.OIL_SEEP.get().defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 2 + random.nextInt(2)) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x + dx, z + dz) - 1;
                BlockPos pos = new BlockPos(x + dx, y, z + dz);
                BlockState ground = level.getBlockState(pos);
                if (ground.is(BlockTags.DIRT) || ground.is(BlockTags.SAND) || ground.is(BlockTags.BASE_STONE_OVERWORLD)
                        || ground.is(net.minecraft.world.level.block.Blocks.GRAVEL)) {
                    if (!level.getFluidState(pos.above()).isEmpty()) {
                        continue;
                    }
                    level.setBlock(pos, seep, Block.UPDATE_CLIENTS);
                }
            }
        }
        // a small puddle in the middle, only if it is walled in by solid ground
        int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
        BlockPos centre = new BlockPos(x, y, z);
        if (!level.getBlockState(centre).is(EarthModule.OIL_SEEP.get())) {
            return;
        }
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
            if (!level.getBlockState(centre.relative(d)).isSolidRender(level, centre.relative(d))) {
                return;
            }
        }
        level.setBlock(centre, oil, Block.UPDATE_CLIENTS);
    }
}
