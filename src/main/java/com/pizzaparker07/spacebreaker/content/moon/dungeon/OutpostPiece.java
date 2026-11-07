package com.pizzaparker07.spacebreaker.content.moon.dungeon;

import com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.resources.ResourceKey;

/**
 * An abandoned automated lunar survey outpost: a central hub with a lab, a storage module and a drone bay
 * around it, an airlock to the outside, a solar field, a landing pad and a radio mast. Half-buried in dust,
 * windows cracked, still guarded by its survey drones. Built block by block (no template files), layout fixed
 * per structure seed so every chunk agrees.
 */
public class OutpostPiece extends StructurePiece {
    static final int SIZE = 31;
    static final int HEIGHT = 14;
    private final long seed;

    public OutpostPiece(long seed, int x, int y, int z, Direction facing) {
        super(MoonStructures.OUTPOST_PIECE.get(), 0, new BoundingBox(x, y - 4, z, x + SIZE - 1, y + HEIGHT, z + SIZE - 1));
        this.seed = seed;
        setOrientation(facing);
    }

    public OutpostPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(MoonStructures.OUTPOST_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    /** World position of a local block (local +x = east, +z = south before orientation). */
    public BlockPos localToWorld(int x, int y, int z) {
        return PieceOrientation.worldPos(this, x, y, z);
    }

    /** World direction of a local direction, from how this piece maps local coordinates. */
    public Direction worldDirection(Direction local) {
        return PieceOrientation.direction(this, local);
    }

    /**
     * Places a block written in piece space through the shared dungeon helper: turned to match the piece's coordinate
     * mapping and, like world generation, without neighbour shape updates.
     */
    @Override
    protected void placeBlock(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox box) {
        BlockPos pos = PieceOrientation.place(level, this, state, x, y, z, box);
        if (pos != null && (state.is(Blocks.IRON_BARS) || state.getBlock() instanceof net.minecraft.world.level.block.WallBlock)) {
            level.getChunk(pos).markPosForPostprocessing(pos);
        }
    }

    // local y: 0..3 foundation, 4 = floor level of the pad
    private static final int F = 4;

    private BlockState s(net.minecraft.world.level.block.Block b) {
        return b.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        BlockState pad = s(MoonModule.SINTERED_REGOLITH.get());
        BlockState foundation = s(MoonModule.ANORTHOSITE.base().get());
        // clear the site and lay a sintered pad on a solid foundation
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = F + 1; y <= HEIGHT + 3; y++) {
                    placeBlock(level, s(Blocks.AIR), x, y, z, box);
                }
                boolean edge = x == 0 || z == 0 || x == SIZE - 1 || z == SIZE - 1;
                placeBlock(level, edge ? s(MoonModule.LUNAR_REGOLITH.get()) : pad, x, F, z, box);
                fillColumnDown(level, foundation, x, F - 1, z, box);
            }
        }
        BlockState wall = s(Blocks.WHITE_CONCRETE);
        BlockState frame = s(Blocks.LIGHT_GRAY_CONCRETE);
        // room variety: the east module is either a storage hall or a collapsed greenhouse
        boolean greenhouse = (seed & 1) == 1;
        // hub, lab (north), storage (east), drone bay (west), airlock (south)
        module(level, box, r, 10, 10, 20, 20, 6, wall, frame);
        module(level, box, r, 11, 1, 19, 8, 5, wall, frame);
        module(level, box, r, 22, 11, 29, 19, 5, wall, frame);
        module(level, box, r, 1, 11, 8, 19, 5, wall, frame);
        module(level, box, r, 13, 22, 17, 26, 4, wall, frame);
        module(level, box, r, 1, 1, 8, 8, 5, wall, frame);   // crew quarters (north-west)
        tunnelX(level, box, 5, 8, 11, frame);
        vault(level, box, frame);                             // sealed vault (north-east)
        tunnelX(level, box, 5, 19, 22, frame);
        // short pressurised tunnels between modules
        tunnelZ(level, box, 15, 8, 10, frame);
        tunnelX(level, box, 15, 20, 22, frame);
        tunnelX(level, box, 15, 8, 10, frame);
        tunnelZ(level, box, 15, 20, 22, frame);
        // doorways through the walls
        for (int[] d : new int[][]{{15, 8}, {15, 10}, {20, 15}, {22, 15}, {8, 15}, {10, 15}, {15, 20}, {15, 22}, {8, 5}, {11, 5}, {19, 5}}) {
            placeBlock(level, s(Blocks.AIR), d[0], F + 1, d[1], box);
            placeBlock(level, s(Blocks.AIR), d[0], F + 2, d[1], box);
        }
        airlockDoors(level, box);
        furnishHub(level, box, r);
        furnishLab(level, box, r);
        if (greenhouse) {
            furnishGreenhouse(level, box);
        } else {
            furnishStorage(level, box, r);
        }
        furnishQuarters(level, box);
        vaultPuzzle(level, box);
        furnishDroneBay(level, box, r);
        exterior(level, box, r);
        decay(level, box, r);
    }

    /** A box module: frame edges, concrete walls with windows, light-grey roof under a regolith shield. */
    private void module(WorldGenLevel level, BoundingBox box, RandomSource r, int x0, int z0, int x1, int z1, int h,
                        BlockState wall, BlockState frame) {
        BlockState floor = s(Blocks.SMOOTH_STONE);
        BlockState glass = s(Blocks.LIGHT_GRAY_STAINED_GLASS);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean ex = x == x0 || x == x1;
                boolean ez = z == z0 || z == z1;
                placeBlock(level, ex || ez ? frame : floor, x, F, z, box);
                for (int y = F + 1; y < F + h; y++) {
                    BlockState st;
                    if (ex && ez) {
                        st = frame;
                    } else if (ex || ez) {
                        boolean window = y == F + 2 && ((ex ? z : x) % 3 == 0);
                        st = window ? glass : wall;
                    } else {
                        st = s(Blocks.AIR);
                    }
                    placeBlock(level, st, x, y, z, box);
                }
                placeBlock(level, ex || ez ? frame : s(Blocks.LIGHT_GRAY_CONCRETE), x, F + h, z, box);
                // regolith shielding piled on the roof (radiation protection)
                if (!ex && !ez) {
                    placeBlock(level, s(MoonModule.LUNAR_REGOLITH.get()), x, F + h + 1, z, box);
                }
            }
        }
        // one hanging lamp in the middle
        int cx = (x0 + x1) / 2;
        int cz = (z0 + z1) / 2;
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), cx, F + h - 1, cz, box);
    }

    /** A pressurised tunnel along z between two module walls (exclusive), one block wide inside. */
    private void tunnelZ(WorldGenLevel level, BoundingBox box, int cx, int z0, int z1, BlockState frame) {
        for (int z = z0 + 1; z < z1; z++) {
            for (int dx = -1; dx <= 1; dx++) {
                placeBlock(level, frame, cx + dx, F, z, box);
                placeBlock(level, frame, cx + dx, F + 3, z, box);
                for (int y = F + 1; y <= F + 2; y++) {
                    placeBlock(level, dx == 0 ? s(Blocks.AIR) : frame, cx + dx, y, z, box);
                }
            }
        }
    }

    /** The same along x. */
    private void tunnelX(WorldGenLevel level, BoundingBox box, int cz, int x0, int x1, BlockState frame) {
        for (int x = x0 + 1; x < x1; x++) {
            for (int dz = -1; dz <= 1; dz++) {
                placeBlock(level, frame, x, F, cz + dz, box);
                placeBlock(level, frame, x, F + 3, cz + dz, box);
                for (int y = F + 1; y <= F + 2; y++) {
                    placeBlock(level, dz == 0 ? s(Blocks.AIR) : frame, x, y, cz + dz, box);
                }
            }
        }
    }

    private void airlockDoors(WorldGenLevel level, BoundingBox box) {
        // outer door in the south wall of the airlock, inner door toward the hub
        door(level, box, 15, 26, Direction.SOUTH);
        door(level, box, 15, 22, Direction.SOUTH);
        placeBlock(level, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.SOUTH), 16, F + 2, 27, box);
        placeBlock(level, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.NORTH), 14, F + 2, 25, box);
        // inner door opens from pressure plates on both sides
        placeBlock(level, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE), 15, F + 1, 21, box);
        placeBlock(level, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE), 15, F + 1, 23, box);
        // pressure warning stripes on the airlock floor
        for (int x = 14; x <= 16; x++) {
            for (int z = 23; z <= 25; z++) {
                placeBlock(level, (x + z) % 2 == 0 ? s(Blocks.YELLOW_TERRACOTTA) : s(Blocks.BLACK_TERRACOTTA), x, F, z, box);
            }
        }
    }

    private void door(WorldGenLevel level, BoundingBox box, int x, int z, Direction facing) {
        BlockState lower = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        placeBlock(level, lower, x, F + 1, z, box);
        placeBlock(level, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), x, F + 2, z, box);
    }

    private void furnishHub(WorldGenLevel level, BoundingBox box, RandomSource r) {
        // survey console ring around a map table
        placeBlock(level, s(Blocks.CARTOGRAPHY_TABLE), 15, F + 1, 15, box);
        placeBlock(level, s(Blocks.LECTERN), 13, F + 1, 12, box);
        for (int x = 12; x <= 18; x++) {
            if (x < 14 || x > 16) {
                placeBlock(level, x % 2 == 0 ? s(Blocks.OBSERVER) : s(Blocks.CRAFTER), x, F + 1, 11, box);
            }
        }
        placeBlock(level, Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), 15, F + 1, 13, box);
        container(level, box, r, 19, F + 1, 19, MoonLoot.OUTPOST_COMMAND, true);
        placeBlock(level, s(Blocks.REDSTONE_LAMP), 11, F + 1, 19, box);
    }

    private void furnishLab(WorldGenLevel level, BoundingBox box, RandomSource r) {
        for (int x = 12; x <= 18; x++) {
            placeBlock(level, s(Blocks.SMOOTH_STONE_SLAB), x, F + 1, 2, box);
        }
        placeBlock(level, s(Blocks.BREWING_STAND), 13, F + 2, 2, box);
        placeBlock(level, s(Blocks.CAULDRON), 17, F + 1, 3, box);
        placeBlock(level, s(MoonModule.LUNAR_ICE.get()), 12, F + 1, 7, box);
        placeBlock(level, s(MoonModule.KREEP_ORE.get()), 18, F + 1, 7, box);
        container(level, box, r, 12, F + 1, 4, MoonLoot.OUTPOST_LAB, true);
        container(level, box, r, 18, F + 1, 4, MoonLoot.OUTPOST_LAB, true);
    }

    private void furnishStorage(WorldGenLevel level, BoundingBox box, RandomSource r) {
        for (int z = 12; z <= 18; z += 2) {
            container(level, box, r, 28, F + 1, z, MoonLoot.OUTPOST_STORAGE, false);
            if (r.nextBoolean()) {
                container(level, box, r, 28, F + 2, z, MoonLoot.OUTPOST_STORAGE, false);
            }
        }
        container(level, box, r, 23, F + 1, 18, MoonLoot.OUTPOST_STORAGE, true);
    }

    private void furnishDroneBay(WorldGenLevel level, BoundingBox box, RandomSource r) {
        // charging pads and the drone dispenser (spawner) in a cage
        placeBlock(level, s(Blocks.IRON_BLOCK), 4, F, 15, box);
        placeBlock(level, s(Blocks.SPAWNER), 4, F + 1, 15, box);
        BlockPos sp = getWorldPos(4, F + 1, 15);
        if (box.isInside(sp) && level.getBlockEntity(sp) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(MoonEntities.SURVEY_DRONE.get(), RandomSource.create(seed + 7));
        }
        for (int[] c : new int[][]{{3, 14}, {5, 14}, {3, 16}, {5, 16}}) {
            for (int y = F + 1; y <= F + 3; y++) {
                placeBlock(level, s(Blocks.IRON_BARS), c[0], y, c[1], box);
            }
        }
        placeBlock(level, s(Blocks.IRON_BLOCK), 3, F, 12, box);
        placeBlock(level, s(Blocks.IRON_BLOCK), 3, F, 18, box);
        guard(level, box, MoonEntities.SURVEY_DRONE.get(), 3, F + 2, 12);
        guard(level, box, MoonEntities.SURVEY_DRONE.get(), 15, F + 3, 16);
    }

    private void exterior(WorldGenLevel level, BoundingBox box, RandomSource r) {
        // solar field
        for (int x = 22; x <= 29; x += 2) {
            for (int z = 22; z <= 29; z += 2) {
                if (r.nextInt(6) != 0) {
                    placeBlock(level, MachinesModule.SOLAR_PANEL.get().defaultBlockState(), x, F + 1, z, box);
                }
            }
        }
        // landing pad: polished disc with a yellow ring
        for (int x = 1; x <= 9; x++) {
            for (int z = 21; z <= 29; z++) {
                double d = Math.hypot(x - 5, z - 25);
                if (d <= 4.6) {
                    placeBlock(level, d > 3.6 && d <= 4.6 ? s(Blocks.YELLOW_TERRACOTTA) : s(MoonModule.POLISHED_ANORTHOSITE.base().get()), x, F, z, box);
                }
            }
        }
        // radio mast
        for (int y = F + 1; y <= F + 9; y++) {
            placeBlock(level, y == F + 9 ? s(Blocks.LIGHTNING_ROD) : s(Blocks.IRON_BARS), 20, y, 27, box);
        }
        placeBlock(level, s(Blocks.IRON_BLOCK), 20, F, 27, box);
        placeBlock(level, s(Blocks.DAYLIGHT_DETECTOR), 21, F + 1, 27, box);
    }

    // ------------------------------------------------------------------ extra rooms, trap and puzzle

    /** Crew quarters: three bunks, lockers, a table; the crew left in a hurry. */
    private void furnishQuarters(WorldGenLevel level, BoundingBox box) {
        for (int z : new int[]{2, 4, 6}) {
            bed(level, box, 2, z);
        }
        container(level, box, null, 7, F + 1, 2, MoonLoot.OUTPOST_QUARTERS, false);
        container(level, box, null, 7, F + 1, 3, MoonLoot.OUTPOST_QUARTERS, false);
        placeBlock(level, s(Blocks.SMOOTH_STONE_SLAB), 6, F + 1, 6, box);
        placeBlock(level, Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 5, F + 1, 6, box);
        placeBlock(level, s(Blocks.FLOWER_POT), 6, F + 2, 6, box);
    }

    private void bed(WorldGenLevel level, BoundingBox box, int x, int z) {
        BlockState foot = Blocks.WHITE_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.EAST)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT);
        placeBlock(level, foot, x, F + 1, z, box);
        placeBlock(level, foot.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD),
                x + 1, F + 1, z, box);
    }

    /** A greenhouse that lost its air: glass roof, dry hydroponic trays, dead plants, a seed store. */
    private void furnishGreenhouse(WorldGenLevel level, BoundingBox box) {
        for (int x = 23; x <= 28; x++) {
            for (int z = 12; z <= 18; z++) {
                placeBlock(level, s(Blocks.LIGHT_GRAY_STAINED_GLASS), x, F + 5, z, box); // glass roof
                placeBlock(level, s(Blocks.AIR), x, F + 6, z, box);                      // no regolith shield on glass
                if ((z == 13 || z == 17) && x >= 23 && x <= 27) {
                    placeBlock(level, s(Blocks.COARSE_DIRT), x, F, z, box);
                    placeBlock(level, x % 2 == 0 ? s(Blocks.POTTED_DEAD_BUSH) : s(Blocks.FLOWER_POT), x, F + 1, z, box);
                }
            }
        }
        placeBlock(level, s(Blocks.COMPOSTER), 28, F + 1, 12, box);
        placeBlock(level, s(Blocks.REDSTONE_LAMP), 28, F + 4, 15, box);
        container(level, box, null, 28, F + 1, 18, MoonLoot.OUTPOST_GREENHOUSE, false);
        container(level, box, null, 23, F + 1, 15, MoonLoot.OUTPOST_GREENHOUSE, true);
    }

    /** The vault: reinforced walls, no windows, an iron door that only opens with power. */
    private void vault(WorldGenLevel level, BoundingBox box, BlockState frame) {
        BlockState armour = s(MoonModule.ANORTHOSITE_BRICKS.base().get());
        for (int x = 22; x <= 28; x++) {
            for (int z = 2; z <= 8; z++) {
                boolean edge = x == 22 || x == 28 || z == 2 || z == 8;
                placeBlock(level, armour, x, F, z, box);
                for (int y = F + 1; y <= F + 3; y++) {
                    placeBlock(level, edge ? armour : s(Blocks.AIR), x, y, z, box);
                }
                placeBlock(level, armour, x, F + 4, z, box);
            }
        }
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 25, F + 3, 5, box);
        container(level, box, null, 27, F + 1, 4, MoonLoot.OUTPOST_VAULT, true);
        container(level, box, null, 27, F + 1, 6, MoonLoot.OUTPOST_VAULT, true);
    }

    /**
     * Puzzle and trap: the vault door lost its power. A power cell (block of redstone, kept in the command chest)
     * fits the socket above the door. The approach tunnel is guarded by an arrow trap on a pressure plate.
     */
    private void vaultPuzzle(WorldGenLevel level, BoundingBox box) {
        door(level, box, 22, 5, Direction.EAST);
        placeBlock(level, s(Blocks.AIR), 22, F + 3, 5, box); // empty socket above the door
        placeBlock(level, s(Blocks.YELLOW_TERRACOTTA), 22, F + 3, 4, box);
        placeBlock(level, s(Blocks.YELLOW_TERRACOTTA), 22, F + 3, 6, box);
        placeBlock(level, s(Blocks.REDSTONE_LAMP), 22, F + 4, 5, box);
        // warning sign on the tunnel wall
        BlockPos signPos = getWorldPos(21, F + 2, 5);
        BlockState sign = Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.WallSignBlock.FACING, Direction.SOUTH);
        if (box.isInside(signPos)) {
            placeBlock(level, sign, 21, F + 2, 5, box);
            if (level.getBlockEntity(signPos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity be) {
                net.minecraft.world.level.block.entity.SignText text = new net.minecraft.world.level.block.entity.SignText();
                for (int i = 0; i < 4; i++) {
                    text = text.setMessage(i, net.minecraft.network.chat.Component.translatable("sign.spacebreaker.outpost_vault." + i));
                }
                be.setText(text, true);
            }
        }
        // arrow trap: a dispenser hidden in the tunnel wall, fired by the pressure plate in front of the door
        placeBlock(level, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE), 21, F + 1, 5, box);
        BlockPos disp = getWorldPos(21, F + 1, 4);
        placeBlock(level, Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, Direction.SOUTH),
                21, F + 1, 4, box);
        if (box.isInside(disp) && level.getBlockEntity(disp) instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity d) {
            d.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW, 16));
        }
    }

    /** Time and impacts: holes, dust drifts, broken windows. Fixed by the seed, like the layout. */
    private void decay(WorldGenLevel level, BoundingBox box, RandomSource r) {
        for (int i = 0; i < 70; i++) {
            int x = r.nextInt(SIZE);
            int z = r.nextInt(SIZE);
            int y = F + 1 + r.nextInt(5);
            BlockState here = getBlock(level, x, y, z, box);
            if (here.is(Blocks.LIGHT_GRAY_STAINED_GLASS) || (here.is(Blocks.WHITE_CONCRETE) && r.nextInt(3) == 0)) {
                placeBlock(level, s(Blocks.AIR), x, y, z, box);
            }
        }
        for (int i = 0; i < 40; i++) {
            int x = r.nextInt(SIZE);
            int z = r.nextInt(SIZE);
            if (getBlock(level, x, F + 1, z, box).isAir() && !getBlock(level, x, F, z, box).isAir()) {
                placeBlock(level, s(MoonModule.LUNAR_REGOLITH.get()), x, F + 1, z, box);
            }
        }
    }

    private void container(WorldGenLevel level, BoundingBox box, @javax.annotation.Nullable RandomSource layout, int x, int y, int z,
                           ResourceKey<LootTable> loot, boolean chest) {
        // loot seeds come from the position, so the shared layout random is consumed the same way in every chunk.
        // Every store is the shared supply locker (it was a chest or a barrel), doors turned toward the middle.
        RandomSource r = RandomSource.create(seed ^ (x * 341873128712L + y * 132897987541L + z));
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, x, y, z, IceDrillPiece.inward(x, z, SIZE), loot,
                r.nextLong(), box);
    }

    private void guard(WorldGenLevel level, BoundingBox box, EntityType<? extends Mob> type, int x, int y, int z) {
        BlockPos pos = getWorldPos(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        Mob mob = type.create(level.getLevel());
        if (mob == null) {
            return;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }
}
