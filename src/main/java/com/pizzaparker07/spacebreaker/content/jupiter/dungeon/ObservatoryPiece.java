package com.pizzaparker07.spacebreaker.content.jupiter.dungeon;

import com.pizzaparker07.spacebreaker.content.dungeons.Dungeons;
import com.pizzaparker07.spacebreaker.content.jupiter.JupiterModule;
import com.pizzaparker07.spacebreaker.content.jupiter.entity.JupiterEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * An abandoned volcano observatory on Io, raised on stilts above the warm sulfur plain: a hull-panel building with
 * radiator fins down both sides and an airlock door at the top of its steps, a slit dome on the roof with the telescope
 * tube pointing out through the slit, a control room (power monitor, seismometers) and an archive inside, and a hatch
 * with a steel ladder down to the seismometer vault in the rock. The side that faced the plume is scorched and drifted
 * with sulfur; the last log says why they left. Sulfur crabs have moved into the warm ruins.
 */
public class ObservatoryPiece extends StructurePiece {
    public static final int SIZE = 17;
    static final ResourceKey<LootTable> LOGS = Dungeons.lootTable("io", "observatory_logs");
    static final ResourceKey<LootTable> VAULT = Dungeons.lootTable("io", "observatory_vault");
    private final long seed;
    static final int F = 5;

    public ObservatoryPiece(long seed, int x, int y, int z, Direction facing) {
        super(JupiterStructures.OBSERVATORY_PIECE.get(), 0, makeBoundingBox(x, y - F, z, facing, SIZE, F + 12, SIZE));
        this.seed = seed;
        setOrientation(facing);
    }

    public ObservatoryPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(JupiterStructures.OBSERVATORY_PIECE.get(), tag);
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

    /** Local y of the platform deck (people stand on F + 3). */
    public static final int DECK = F + 2;
    /** Centre of the building and dome. */
    public static final int C = 8;

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        BlockState basalt = s(JupiterModule.IONIAN_BASALT.base().get());
        BlockState air = s(Blocks.AIR);
        BlockState rockWall = s(Blocks.POLISHED_BLACKSTONE_BRICKS);
        BlockState deck = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.base().get());
        BlockState deckStairs = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.stairs().get());
        BlockState strut = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.wall().get());
        BlockState panel = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.base().get());
        BlockState hull = s(com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule.STATION_HULL.get());
        BlockState window = s(com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule.STATION_WINDOW.get());
        BlockState hazard = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.HAZARD_STRIPE.base().get());
        BlockState grate = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE.get());
        BlockState cable = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.CABLE_DUCT.get());
        BlockState led = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get());
        BlockState scorch = s(Blocks.BLACKSTONE);
        // footing, clearing and a sulfur pad
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = 0; y < F; y++) {
                    placeBlock(level, basalt, x, y, z, box);
                }
                for (int y = F; y < F + 12; y++) {
                    placeBlock(level, air, x, y, z, box);
                }
                placeBlock(level, s(JupiterModule.SULFUR_SAND.get()), x, F - 1, z, box);
            }
        }
        // the platform on stilts, two blocks above the plain, a hazard-striped edge
        for (int x = 3; x <= 13; x++) {
            for (int z = 3; z <= 13; z++) {
                boolean edge = x == 3 || x == 13 || z == 3 || z == 13;
                placeBlock(level, edge ? hazard : deck, x, DECK, z, box);
                if ((x == 3 || x == 13 || x == C) && (z == 3 || z == 13 || z == C)) {
                    for (int y = F; y < DECK; y++) {
                        placeBlock(level, strut, x, y, z, box);
                    }
                }
            }
        }
        // steps up to the door on the south side
        for (int i = 0; i < 3; i++) {
            com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                    deckStairs.setValue(StairBlock.FACING, Direction.NORTH), C, F + i, 16 - i, box);
        }
        // the building: hull walls, a deck roof, slit windows, radiator fins down the east and west sides
        for (int x = 4; x <= 12; x++) {
            for (int z = 4; z <= 12; z++) {
                boolean edge = x == 4 || x == 12 || z == 4 || z == 12;
                for (int y = DECK + 1; y <= DECK + 4; y++) {
                    if (edge) {
                        boolean slit = y == DECK + 2 && ((x == 4 || x == 12) && z % 2 == 0 || (z == 4 || z == 12) && x % 2 == 0 && x != C);
                        boolean corner = (x == 4 || x == 12) && (z == 4 || z == 12);
                        placeBlock(level, corner ? panel : slit ? window : hull, x, y, z, box);
                    }
                }
                placeBlock(level, deck, x, DECK + 5, z, box);
            }
        }
        for (int z = 5; z <= 11; z += 2) {
            for (int y = DECK + 1; y <= DECK + 4; y++) {
                placeBlock(level, grate, 3, y, z, box);
                placeBlock(level, grate, 13, y, z, box);
            }
        }
        BlockState lower = com.pizzaparker07.spacebreaker.content.suit.SuitModule.AIRLOCK_DOOR.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.FACING, Direction.SOUTH)
                .setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF, DoubleBlockHalf.LOWER);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, lower, C, DECK + 1, 12, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                lower.setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF, DoubleBlockHalf.UPPER), C, DECK + 2, 12, box);
        // the slit dome on the roof, the telescope tube pointing out through the slit to the north
        int base = DECK + 5;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = 1; dy <= 4; dy++) {
                    double d = Math.sqrt(dx * dx + dz * dz + dy * dy * 1.3);
                    if (d >= 3.5 && d < 4.5) {
                        boolean slit = dx == 0 && dz <= 0;
                        placeBlock(level, slit ? air : (dy == 1 ? hazard : panel), C + dx, base + dy, C + dz, box);
                    }
                }
            }
        }
        placeBlock(level, cable, C, base + 1, C, box);
        for (int i = 0; i < 4; i++) {
            placeBlock(level, strut, C, base + 2 + i / 2, C - 1 - i, box);
        }
        placeBlock(level, window, C, base + 4, C - 5, box);
        // inside, south half: the control room (power monitor between cable runs, seismometers); north half: the archive
        int floor = DECK + 1;
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.POWER_MONITOR.get())
                        .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH), 6, floor, 11, box);
        placeBlock(level, cable, 5, floor, 11, box);
        placeBlock(level, cable, 7, floor, 11, box);
        placeBlock(level, s(Blocks.CALIBRATED_SCULK_SENSOR), 11, floor, 10, box);
        placeBlock(level, s(Blocks.SCULK_SENSOR), 11, floor, 11, box);
        placeBlock(level, s(Blocks.CARTOGRAPHY_TABLE), 5, floor, 5, box);
        placeBlock(level, s(Blocks.CHISELED_BOOKSHELF), 6, floor, 5, box);
        placeBlock(level, s(Blocks.CHISELED_BOOKSHELF), 7, floor, 5, box);
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, 5, floor, 6, Direction.EAST, LOGS, seed + 1, box);
        BlockPos sign = com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                s(Blocks.BIRCH_SIGN).setValue(net.minecraft.world.level.block.StandingSignBlock.ROTATION, 8), 7, floor, 7, box);
        if (sign != null && box.isInside(sign)) {
            com.pizzaparker07.spacebreaker.content.mars.dungeon.DungeonBits.signText(level, sign, "io_observatory_log");
        }
        // the telescope pier rising through the room to the mount in the dome, two chairs at the console
        for (int y = floor; y <= DECK + 5; y++) {
            placeBlock(level, y == DECK + 5 ? cable : strut, C, y, C, box);
        }
        for (int x : new int[]{5, 7}) {
            com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                    deckStairs.setValue(StairBlock.FACING, Direction.NORTH), x, floor, 9, box);
        }
        placeBlock(level, s(Blocks.REDSTONE_LAMP), 9, floor, 11, box);
        placeBlock(level, cable, 10, floor, 11, box);
        placeBlock(level, led, C, DECK + 5, C + 2, box);
        placeBlock(level, led, C, DECK + 5, C - 2, box);
        // the hatch and steel ladder down through the platform and the rock to the seismometer vault
        int lx = 10;
        int lz = 5;
        for (int y = 1; y < DECK; y++) {
            placeBlock(level, hull, lx, y, lz - 1, box);
            com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                    com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.STEEL_LADDER.get().defaultBlockState()
                            .setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH), lx, y, lz, box);
        }
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, s(Blocks.IRON_TRAPDOOR), lx, DECK, lz, box);
        for (int x = 9; x <= 13; x++) {
            for (int z = 3; z <= 6; z++) {
                for (int y = 1; y <= 3; y++) {
                    if (x == lx && z == lz || x == lx && z == lz - 1) {
                        continue;
                    }
                    placeBlock(level, x == 9 || x == 13 || z == 3 || z == 6 || y == 3 ? rockWall : air, x, y, z, box);
                }
                placeBlock(level, s(Blocks.POLISHED_BASALT), x, 0, z, box);
            }
        }
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, 12, 1, 4, Direction.WEST, VAULT, seed + 2, box);
        placeBlock(level, s(Blocks.SEA_LANTERN), 12, 2, 5, box);
        placeBlock(level, s(Blocks.CALIBRATED_SCULK_SENSOR), 11, 1, 4, box);
        // the plume-facing south side scorched, sulfur and frost drifted onto the deck and against the stilts
        for (int x = 3; x <= 13; x++) {
            if (r.nextInt(2) == 0) {
                placeBlock(level, scorch, x, DECK + 1 + r.nextInt(2), 12, box);
            }
        }
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                boolean onDeckEdge = (z == 13 || x == 3 || x == 13) && x >= 3 && x <= 13 && z >= 3 && z <= 13 && !(x == C && z == 13);
                if (onDeckEdge && r.nextInt(4) == 0) {
                    placeBlock(level, s(r.nextBoolean() ? JupiterModule.RED_SULFUR.get() : JupiterModule.SO2_FROST.get()), x, DECK + 1, z, box);
                }
                boolean plain = x < 3 || x > 13 || z < 3 || z > 13;
                if (plain && r.nextInt(6) == 0) {
                    placeBlock(level, s(r.nextBoolean() ? JupiterModule.RED_SULFUR.get() : JupiterModule.SO2_FROST.get()), x, F, z, box);
                }
            }
        }
        for (int[] g : new int[][]{{8, 15}, {1, 8}}) {
            BlockPos pos = getWorldPos(g[0], F, g[1]);
            if (box.isInside(pos)) {
                Mob m = JupiterEntities.SULFUR_CRAB.get().create(level.getLevel());
                if (m != null) {
                    m.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, r.nextFloat() * 360F, 0F);
                    m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
                    m.setPersistenceRequired();
                    level.addFreshEntityWithPassengers(m);
                }
            }
        }
    }
}
