package com.pizzaparker07.spacebreaker.content.neptune.dungeon;

import com.pizzaparker07.spacebreaker.content.dungeons.Dungeons;
import com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation;
import com.pizzaparker07.spacebreaker.content.neptune.NeptuneModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A geyser watch station on Triton's southern cap: a low glass dome on steel ribs built right over a nitrogen geyser,
 * the vent ringed by a collar of bars inside, a tall instrument mast rising through the dome's crown, a plume sampling
 * bench and a store. The door faces the open cap; pink frost has drifted in over the floor since the crew left. The
 * plume beetles that live off the geyser's fallout guard it (dungeon definition).
 */
public class GeyserStationPiece extends StructurePiece {
    public static final int W = 15;
    /** Footing depth below the floor. */
    static final int F = 3;
    /** Dome radius. */
    static final int R = 6;
    public static final int C = 7;
    static final ResourceKey<LootTable> LAB = Dungeons.lootTable("triton", "station_lab");
    static final ResourceKey<LootTable> STORE = Dungeons.lootTable("triton", "station_store");
    private final long seed;

    public GeyserStationPiece(long seed, int x, int y, int z, Direction facing) {
        super(NeptuneStructures.STATION_PIECE.get(), 0, makeBoundingBox(x, y - F, z, facing, W, F + 14, W));
        this.seed = seed;
        setOrientation(facing);
    }

    public GeyserStationPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(NeptuneStructures.STATION_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** World position of a local position (for showcases and tests). */
    public BlockPos world(int x, int y, int z) {
        return getWorldPos(x, y, z);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        BlockState ice = s(NeptuneModule.TRITON_ICE.base().get());
        BlockState frost = s(NeptuneModule.PINK_NITROGEN_FROST.get());
        BlockState floor = s(Blocks.SMOOTH_STONE);
        BlockState rib = s(Blocks.IRON_BLOCK);
        BlockState glass = s(Blocks.LIGHT_BLUE_STAINED_GLASS);
        // footing of ice, air above, the cap's frost around the dome
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < W; z++) {
                for (int y = 0; y < F; y++) {
                    placeBlock(level, ice, x, y, z, box);
                }
                for (int y = F; y < F + 14; y++) {
                    placeBlock(level, s(Blocks.AIR), x, y, z, box);
                }
                double d = Math.hypot(x - C, z - C);
                placeBlock(level, d <= R ? floor : frost, x, F - 1, z, box);
            }
        }
        // the floor inside: titanium deck plates, a hazard ring kept clear round the vent, grates under the collar so the
        // vent can breathe, a grate walkway in from the door, pipes sunk in the floor out to the bench and the store,
        // and four light panels set flush at the diagonals
        BlockState deck = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.base().get());
        BlockState hazard = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.HAZARD_STRIPE.base().get());
        BlockState grate = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE.get());
        BlockState pipe = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PIPE_DUCT.get());
        BlockState led = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get());
        BlockState panel = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.base().get());
        for (int x = C - R; x <= C + R; x++) {
            for (int z = C - R; z <= C + R; z++) {
                double d = Math.hypot(x - C, z - C);
                if (d > R - 0.5) {
                    continue;
                }
                BlockState b = d > R - 1.5 ? panel : deck;
                if (d >= 1.9 && d < 2.9) {
                    b = hazard;
                } else if (d > 0.5 && d < 1.9) {
                    b = grate;
                } else if (x == C && z < C - 2) {
                    b = grate;
                } else if (z == C && Math.abs(x - C) >= 3 && Math.abs(x - C) <= 4) {
                    b = pipe;
                } else if (Math.abs(x - C) == 3 && Math.abs(z - C) == 3) {
                    b = led;
                }
                placeBlock(level, b, x, F - 1, z, box);
            }
        }
        // the dome: a hemisphere shell, steel ribs on the eight compass lines, glass between
        for (int x = C - R; x <= C + R; x++) {
            for (int z = C - R; z <= C + R; z++) {
                for (int y = F; y <= F + R; y++) {
                    double d = Math.sqrt((x - C) * (x - C) + (z - C) * (z - C) + (y - F) * (y - F) * 1.0);
                    if (d >= R - 0.5 && d < R + 0.5) {
                        boolean ribHere = x == C || z == C || Math.abs(x - C) == Math.abs(z - C) || y == F;
                        placeBlock(level, ribHere ? rib : glass, x, y, z, box);
                    }
                }
            }
        }
        // the door, facing the open cap (local north, z = C - R)
        for (int y = F; y <= F + 2; y++) {
            for (int x = C - 1; x <= C + 1; x++) {
                placeBlock(level, s(Blocks.AIR), x, y, C - R, box);
                placeBlock(level, s(Blocks.AIR), x, y, C - R + 1, box);
            }
        }
        // the geyser in the middle of the floor, a collar of bars around it
        placeBlock(level, s(NeptuneModule.NITROGEN_GEYSER.get()), C, F - 1, C, box);
        placeBlock(level, ice, C, F - 2, C, box);
        for (int[] o : new int[][]{{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}}) {
            placeBlock(level, s(Blocks.IRON_BARS), C + o[0], F, C + o[1], box);
        }
        // the instrument mast: up through the crown, a light at the top and a lightning rod
        for (int y = F + R; y <= F + R + 5; y++) {
            placeBlock(level, s(Blocks.IRON_BARS), C, y, C + 2, box);
        }
        placeBlock(level, s(Blocks.WAXED_COPPER_BULB), C, F + R + 6, C + 2, box);
        placeBlock(level, s(Blocks.LIGHTNING_ROD), C, F + R + 7, C + 2, box);
        // plume sampling bench (west) and the store (east)
        placeBlock(level, s(Blocks.BREWING_STAND), C - 4, F, C + 1, box);
        placeBlock(level, s(Blocks.CARTOGRAPHY_TABLE), C - 4, F, C + 2, box);
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, C - 4, F, C, Direction.EAST, LAB, seed + 1, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.BLOCK.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlock.FACING, Direction.WEST), C + 4, F, C + 2, box);
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, C + 4, F, C, Direction.WEST, STORE, seed + 2, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), C, F + R - 2, C - 2, box);
        // pink frost drifted in through the open door
        for (int x = C - 3; x <= C + 3; x++) {
            for (int z = C - R + 1; z <= C - 2; z++) {
                if (r.nextInt(z - (C - R)) == 0) {
                    placeBlock(level, frost, x, F - 1, z, box);
                }
            }
        }
    }
}
