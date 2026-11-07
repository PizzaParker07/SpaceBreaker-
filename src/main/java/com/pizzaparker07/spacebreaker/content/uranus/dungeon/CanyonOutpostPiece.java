package com.pizzaparker07.spacebreaker.content.uranus.dungeon;

import com.pizzaparker07.spacebreaker.content.dungeons.Dungeons;
import com.pizzaparker07.spacebreaker.content.uranus.UranusModule;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * An abandoned survey outpost on the rim of one of Titania's great canyons: a low stone hut with a long window facing the
 * canyon, a balcony that juts out over the edge on steel struts, a survey light at its corner and a winch whose cable
 * still hangs down toward the canyon floor. Inside, the mapping bench and the store; regolith has drifted in through the
 * back door since the crew left.
 *
 * <p>Piece space: local +z points toward the canyon (the piece's facing). The hut stands on the rim at z 1..9, the balcony
 * reaches z 10..15 and the winch arm z 16.
 */
public class CanyonOutpostPiece extends StructurePiece {
    public static final int W = 13;
    public static final int D = 17;
    /** Footing depth below the floor. */
    static final int F = 2;
    /** How far below the floor the struts and the cable may reach. */
    static final int DROP = 24;
    static final int H = 10;
    /** Middle of the hut (x) and of its doors. */
    public static final int MID = 6;
    static final ResourceKey<LootTable> LAB = Dungeons.lootTable("titania", "outpost_lab");
    static final ResourceKey<LootTable> STORE = Dungeons.lootTable("titania", "outpost_store");
    private final long seed;

    public CanyonOutpostPiece(long seed, int x, int y, int z, Direction facing) {
        super(UranusStructures.OUTPOST_PIECE.get(), 0, makeBoundingBox(x, y - F - DROP, z, facing, W, F + DROP + H, D));
        this.seed = seed;
        setOrientation(facing);
    }

    public CanyonOutpostPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(UranusStructures.OUTPOST_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** Local y of the floor's top surface (the hut stands on y {@code FLOOR - 1}). */
    public static final int FLOOR = F + DROP;

    /** World position of a local position (for showcases and tests). */
    public BlockPos world(int x, int y, int z) {
        return getWorldPos(x, y, z);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        final int y0 = FLOOR;
        BlockState ice = s(UranusModule.TITANIA_ICE.base().get());
        BlockState dust = s(UranusModule.TITANIA_REGOLITH.get());
        BlockState floor = s(Blocks.SMOOTH_STONE);
        BlockState wall = s(Blocks.SMOOTH_STONE);
        BlockState steel = s(Blocks.IRON_BLOCK);
        BlockState pane = s(Blocks.LIGHT_BLUE_STAINED_GLASS);
        BlockState air = s(Blocks.AIR);
        // the hut: footing of ice, a stone floor, air above
        for (int x = 0; x < W; x++) {
            for (int z = 0; z <= 9; z++) {
                for (int y = y0 - F; y < y0 - 1; y++) {
                    placeBlock(level, ice, x, y, z, box);
                }
                placeBlock(level, x >= 1 && x <= 11 && z >= 1 ? floor : dust, x, y0 - 1, z, box);
                for (int y = y0; y < y0 + H; y++) {
                    placeBlock(level, air, x, y, z, box);
                }
            }
        }
        // walls and roof: stone between steel corner posts
        for (int x = 1; x <= 11; x++) {
            for (int z = 1; z <= 9; z++) {
                boolean edgeX = x == 1 || x == 11;
                boolean edgeZ = z == 1 || z == 9;
                if (edgeX || edgeZ) {
                    for (int y = y0; y <= y0 + 3; y++) {
                        placeBlock(level, edgeX && edgeZ ? steel : wall, x, y, z, box);
                    }
                }
                placeBlock(level, wall, x, y0 + 4, z, box);
            }
        }
        // the long window toward the canyon, a pane or two cracked out
        for (int x = 2; x <= 10; x++) {
            for (int y = y0 + 1; y <= y0 + 2; y++) {
                placeBlock(level, r.nextInt(7) == 0 ? air : pane, x, y, 9, box);
            }
        }
        // doors: the entrance from the uplands at the back, the way out onto the balcony at the front
        for (int y = y0; y <= y0 + 1; y++) {
            placeBlock(level, air, MID, y, 1, box);
            placeBlock(level, air, MID, y, 9, box);
        }
        // inside: a titanium deck floor between the stone sills, a cable run along both long walls under the roof
        BlockState deck = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.base().get());
        BlockState cable = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.CABLE_DUCT.get());
        BlockState led = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get());
        for (int x = 2; x <= 10; x++) {
            for (int z = 2; z <= 8; z++) {
                placeBlock(level, deck, x, y0 - 1, z, box);
            }
        }
        for (int z = 2; z <= 8; z++) {
            placeBlock(level, cable, 2, y0 + 3, z, box);
            placeBlock(level, cable, 10, y0 + 3, z, box);
        }
        // light panels set into the roof
        for (int[] l : new int[][]{{4, 4}, {8, 4}, {MID, 7}}) {
            placeBlock(level, led, l[0], y0 + 4, l[1], box);
        }
        // the mapping bench (west): the table, the sample stand, the survey shelf, the log
        placeBlock(level, s(Blocks.CARTOGRAPHY_TABLE), 2, y0, 4, box);
        placeBlock(level, s(Blocks.BREWING_STAND), 2, y0, 5, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, s(Blocks.CHISELED_BOOKSHELF)
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST), 2, y0 + 1, 4, box);
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, 2, y0, 3, Direction.EAST, LAB, seed + 1, box);
        BlockPos sign = com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                s(Blocks.BIRCH_WALL_SIGN).setValue(net.minecraft.world.level.block.WallSignBlock.FACING, Direction.EAST), 2, y0 + 1, 6, box);
        if (sign != null && box.isInside(sign)) {
            com.pizzaparker07.spacebreaker.content.mars.dungeon.DungeonBits.signText(level, sign, "titania_outpost_log");
        }
        // the power board at the back wall, its cable running up to the roof
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.POWER_MONITOR.get())
                        .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH), 4, y0, 2, box);
        for (int y = y0 + 1; y <= y0 + 3; y++) {
            placeBlock(level, cable, 4, y, 2, box);
        }
        // two chairs at the long window, turned to the canyon, and a little chart table between them
        BlockState chair = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.stairs().get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.SOUTH);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, chair, 4, y0, 7, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, chair, 8, y0, 7, box);
        for (int x = 5; x <= 7; x++) {
            placeBlock(level, s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.slab().get())
                    .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP), x, y0, 8, box);
        }
        // the store (east): crates, the chest, the bench, a bunk against the wall
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.BLOCK.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlock.FACING, Direction.WEST), 10, y0, 4, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.BLOCK.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlock.FACING, Direction.WEST), 10, y0, 5, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.BLOCK.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlock.FACING, Direction.WEST), 10, y0 + 1, 4, box);
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, 10, y0, 3, Direction.WEST, STORE, seed + 2, box);
        placeBlock(level, s(Blocks.CRAFTING_TABLE), 10, y0, 7, box);
        placeBlock(level, s(Blocks.LIGHT_GRAY_WOOL), 9, y0, 2, box);
        placeBlock(level, s(Blocks.LIGHT_GRAY_WOOL), 10, y0, 2, box);
        placeBlock(level, s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.slab().get()), 9, y0 + 2, 2, box);
        placeBlock(level, s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.slab().get()), 10, y0 + 2, 2, box);
        // regolith drifted in through the back door
        for (int x = MID - 2; x <= MID + 2; x++) {
            for (int z = 2; z <= 5; z++) {
                if (r.nextInt(z) == 0) {
                    placeBlock(level, dust, x, y0 - 1, z, box);
                }
            }
        }
        // the antenna mast on the roof
        for (int y = y0 + 5; y <= y0 + 8; y++) {
            placeBlock(level, s(Blocks.IRON_BARS), 9, y, 3, box);
        }
        placeBlock(level, s(Blocks.WAXED_COPPER_BULB), 9, y0 + 9, 3, box);
        // the balcony over the edge: a stone deck on steel struts, a railing, the survey light
        for (int x = 4; x <= 8; x++) {
            for (int z = 10; z <= 15; z++) {
                placeBlock(level, floor, x, y0 - 1, z, box);
                for (int y = y0; y < y0 + 4; y++) {
                    placeBlock(level, air, x, y, z, box);
                }
                boolean rail = x == 4 || x == 8 || z == 15;
                if (rail && !(x == MID && z == 15)) {
                    placeBlock(level, s(Blocks.IRON_BARS), x, y0, z, box);
                }
            }
        }
        for (int[] c : new int[][]{{4, 11}, {8, 11}, {4, 15}, {8, 15}}) {
            strut(level, box, c[0], y0 - 2, c[1]);
        }
        placeBlock(level, s(Blocks.IRON_BARS), 8, y0 + 1, 15, box);
        placeBlock(level, s(Blocks.WAXED_COPPER_BULB), 8, y0 + 2, 15, box);
        // the winch: a post, an arm out over the edge, the cable hanging down toward the floor of the canyon
        placeBlock(level, steel, MID, y0, 15, box);
        placeBlock(level, steel, MID, y0 + 1, 15, box);
        placeBlock(level, s(Blocks.IRON_BARS), MID, y0 + 2, 15, box);
        placeBlock(level, s(Blocks.IRON_BARS), MID, y0 + 2, 16, box);
        for (int y = y0 + 1; y > y0 - DROP + 2; y--) {
            BlockPos p = getWorldPos(MID, y, 16);
            if (box.isInside(p) && !level.getBlockState(p).isAir()) {
                break;
            }
            placeBlock(level, s(Blocks.CHAIN), MID, y, 16, box);
        }
    }

    /** A steel strut from the deck down to the ground (or as far as the piece reaches). */
    private void strut(WorldGenLevel level, BoundingBox box, int x, int top, int z) {
        for (int y = top; y >= 0; y--) {
            BlockPos p = getWorldPos(x, y, z);
            if (box.isInside(p) && !level.getBlockState(p).isAir()) {
                return;
            }
            placeBlock(level, s(Blocks.IRON_BARS), x, y, z, box);
        }
    }
}
