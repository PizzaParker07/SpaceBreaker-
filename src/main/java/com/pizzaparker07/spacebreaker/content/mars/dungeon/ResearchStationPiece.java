package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * An abandoned automated Mars research station: a pressurised hab with the command deck, a greenhouse of dead
 * plants under cracked glass, an ISRU plant (Sabatier reactor, tanks, pipes) that once made methane and oxygen from
 * the air and ice, a rover garage overrun by rust beetles, a core-drill rig, a solar field and a dish antenna.
 * Half buried by dust storms; its old survey drones still patrol. Built block by block, layout fixed by the seed.
 */
public class ResearchStationPiece extends StructurePiece {
    static final int SIZE = 33;
    static final int HEIGHT = 16;
    private static final int F = 4;
    private final long seed;

    public ResearchStationPiece(long seed, int x, int y, int z, Direction facing) {
        super(MarsStructures.STATION_PIECE.get(), 0, new BoundingBox(x, y - F, z, x + SIZE - 1, y + HEIGHT, z + SIZE - 1));
        this.seed = seed;
        setOrientation(facing);
    }

    public ResearchStationPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(MarsStructures.STATION_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        BlockState pad = s(MarsModule.POLISHED_MARTIAN_BASALT.base().get());
        BlockState foundation = s(MarsModule.MARTIAN_BASALT.base().get());
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = F + 1; y <= HEIGHT + 3; y++) {
                    placeBlock(level, s(Blocks.AIR), x, y, z, box);
                }
                boolean edge = x == 0 || z == 0 || x == SIZE - 1 || z == SIZE - 1;
                placeBlock(level, edge ? s(MarsModule.MARTIAN_REGOLITH.get()) : pad, x, F, z, box);
                fillColumnDown(level, foundation, x, F - 1, z, box);
            }
        }
        BlockState wall = s(Blocks.WHITE_CONCRETE);
        BlockState frame = s(MarsModule.MARTIAN_BASALT_BRICKS.base().get());
        // hab (centre), greenhouse (north), ISRU plant (east), garage (west), airlock (south)
        module(level, box, 11, 11, 21, 21, 6, wall, frame, true);
        greenhouse(level, box, 11, 1, 21, 9);
        module(level, box, 23, 12, 31, 20, 6, wall, frame, true);
        module(level, box, 1, 12, 9, 20, 5, s(Blocks.LIGHT_GRAY_CONCRETE), frame, false);
        module(level, box, 14, 23, 18, 27, 4, wall, frame, false);
        tunnelZ(level, box, 16, 9, 11, frame);
        tunnelX(level, box, 16, 21, 23, frame);
        tunnelX(level, box, 16, 9, 11, frame);
        tunnelZ(level, box, 16, 21, 23, frame);
        for (int[] d : new int[][]{{16, 9}, {16, 11}, {21, 16}, {23, 16}, {9, 16}, {11, 16}, {16, 21}, {16, 23}}) {
            placeBlock(level, s(Blocks.AIR), d[0], F + 1, d[1], box);
            placeBlock(level, s(Blocks.AIR), d[0], F + 2, d[1], box);
        }
        airlock(level, box);
        hab(level, box, r);
        greenhouseInside(level, box, r);
        isru(level, box, r);
        garage(level, box, r);
        exterior(level, box, r);
        decay(level, box, r);
    }

    /** A module: basalt-brick frame, white walls with an orange band and windows, roof shielded with regolith bags. */
    private void module(WorldGenLevel level, BoundingBox box, int x0, int z0, int x1, int z1, int h, BlockState wall, BlockState frame,
                        boolean band) {
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
                        boolean window = y == F + 3 && ((ex ? z : x) % 3 == 1);
                        st = window ? glass : band && y == F + 1 ? s(Blocks.ORANGE_TERRACOTTA) : wall;
                    } else {
                        st = s(Blocks.AIR);
                    }
                    placeBlock(level, st, x, y, z, box);
                }
                placeBlock(level, ex || ez ? frame : s(Blocks.LIGHT_GRAY_CONCRETE), x, F + h, z, box);
                if (!ex && !ez) {
                    placeBlock(level, s(MarsModule.MARTIAN_REGOLITH.get()), x, F + h + 1, z, box);
                }
            }
        }
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), (x0 + x1) / 2, F + h - 1, (z0 + z1) / 2, box);
    }

    private void greenhouse(WorldGenLevel level, BoundingBox box, int x0, int z0, int x1, int z1) {
        BlockState glass = s(Blocks.GLASS);
        BlockState rib = s(Blocks.IRON_BARS);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean ex = x == x0 || x == x1;
                boolean ez = z == z0 || z == z1;
                placeBlock(level, ex || ez ? s(MarsModule.MARTIAN_BASALT_BRICKS.base().get()) : s(Blocks.SMOOTH_STONE), x, F, z, box);
                // a barrel vault: walls rise 3, then the glass roof steps in
                for (int y = F + 1; y <= F + 5; y++) {
                    int inset = Math.max(0, y - (F + 3));
                    boolean shell = (x == x0 + inset || x == x1 - inset) && y >= F + 3 || (ex && y < F + 3) || (ez && x >= x0 + inset && x <= x1 - inset);
                    boolean roof = y == F + 5 && x > x0 + 1 && x < x1 - 1;
                    if (shell || roof) {
                        placeBlock(level, (z % 3 == 0 && !ez) ? rib : glass, x, y, z, box);
                    }
                }
            }
        }
    }

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

    private void airlock(WorldGenLevel level, BoundingBox box) {
        door(level, box, 16, 27, Direction.SOUTH);
        door(level, box, 16, 23, Direction.SOUTH);
        // buttons hang on the airlock walls; aimed from world positions so they attach in every orientation
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.placeToward(level, this, s(Blocks.STONE_BUTTON),
                new BlockPos(17, F + 2, 28), new BlockPos(17, F + 2, 27), true, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.placeToward(level, this, s(Blocks.STONE_BUTTON),
                new BlockPos(15, F + 2, 26), new BlockPos(15, F + 2, 27), true, box);
        placeBlock(level, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE), 16, F + 1, 22, box);
        placeBlock(level, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE), 16, F + 1, 24, box);
        for (int x = 15; x <= 17; x++) {
            for (int z = 24; z <= 26; z++) {
                placeBlock(level, (x + z) % 2 == 0 ? s(Blocks.YELLOW_TERRACOTTA) : s(Blocks.BLACK_TERRACOTTA), x, F, z, box);
            }
        }
    }

    private void door(WorldGenLevel level, BoundingBox box, int x, int z, Direction facing) {
        BlockState lower = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, lower, x, F + 1, z, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), x, F + 2, z, box);
    }

    private void hab(WorldGenLevel level, BoundingBox box, RandomSource r) {
        // command deck: a ring of consoles facing a survey table with a model of the rift
        placeBlock(level, s(Blocks.CARTOGRAPHY_TABLE), 16, F + 1, 16, box);
        for (int x = 13; x <= 19; x++) {
            if (x < 15 || x > 17) {
                placeBlock(level, x % 2 == 0 ? s(Blocks.OBSERVER) : s(Blocks.CRAFTER), x, F + 1, 12, box);
            }
        }
        placeBlock(level, s(Blocks.LECTERN), 14, F + 1, 13, box);
        // bunks along the west wall
        for (int z = 13; z <= 19; z += 3) {
            placeBlock(level, s(Blocks.WHITE_WOOL), 12, F + 1, z, box);
            placeBlock(level, s(Blocks.WHITE_WOOL), 12, F + 1, z + 1, box);
        }
        container(level, box, r, 20, F + 1, 20, MarsLoot.STATION_COMMAND, true);
        placeBlock(level, s(Blocks.REDSTONE_LAMP), 20, F + 1, 12, box);
    }

    private void greenhouseInside(WorldGenLevel level, BoundingBox box, RandomSource r) {
        // dead crops in dry planters: the life support failed long ago
        for (int x = 13; x <= 19; x++) {
            for (int z : new int[]{3, 4, 6, 7}) {
                placeBlock(level, s(Blocks.COARSE_DIRT), x, F, z, box);
                if (r.nextInt(3) != 0) {
                    placeBlock(level, s(Blocks.DEAD_BUSH), x, F + 1, z, box);
                }
            }
        }
        placeBlock(level, s(Blocks.COMPOSTER), 12, F + 1, 2, box);
        placeBlock(level, s(Blocks.CAULDRON), 20, F + 1, 2, box);
        container(level, box, r, 12, F + 1, 8, MarsLoot.STATION_LAB, true);
        container(level, box, r, 20, F + 1, 8, MarsLoot.STATION_LAB, true);
    }

    private void isru(WorldGenLevel level, BoundingBox box, RandomSource r) {
        // Sabatier reactor (blast furnace), electrolysis cells (cauldrons), tanks and the pipe rack
        placeBlock(level, s(Blocks.BLAST_FURNACE), 27, F + 1, 13, box);
        placeBlock(level, s(Blocks.CAULDRON), 25, F + 1, 13, box);
        placeBlock(level, s(Blocks.CAULDRON), 29, F + 1, 13, box);
        for (int y = F + 1; y <= F + 3; y++) {
            placeBlock(level, s(Blocks.IRON_BLOCK), 30, y, 18, box);
            placeBlock(level, s(Blocks.WAXED_COPPER_BLOCK), 30, y, 19, box);
            placeBlock(level, s(Blocks.IRON_BLOCK), 24, y, 19, box);
        }
        for (int x = 24; x <= 30; x++) {
            placeBlock(level, s(Blocks.CHAIN), x, F + 4, 16, box);
        }
        placeBlock(level, s(MarsModule.MARTIAN_ICE.get()), 25, F + 1, 18, box);
        placeBlock(level, s(MarsModule.DRY_ICE.get()), 26, F + 1, 19, box);
        container(level, box, r, 28, F + 1, 19, MarsLoot.STATION_ISRU, true);
        container(level, box, r, 24, F + 1, 15, MarsLoot.STATION_STORAGE, false);
        container(level, box, r, 24, F + 2, 15, MarsLoot.STATION_STORAGE, false);
    }

    private void garage(WorldGenLevel level, BoundingBox box, RandomSource r) {
        // the wreck of a six-wheeled rover; rust beetles have moved in
        for (int x = 3; x <= 7; x++) {
            placeBlock(level, s(Blocks.LIGHT_GRAY_CONCRETE), x, F + 2, 15, box);
            placeBlock(level, s(Blocks.LIGHT_GRAY_CONCRETE), x, F + 2, 16, box);
            placeBlock(level, s(Blocks.LIGHT_GRAY_CONCRETE), x, F + 2, 17, box);
        }
        for (int x : new int[]{3, 5, 7}) {
            placeBlock(level, s(Blocks.BLACK_CONCRETE), x, F + 1, 14, box);
            placeBlock(level, s(Blocks.BLACK_CONCRETE), x, F + 1, 18, box);
        }
        placeBlock(level, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP), 4, F + 3, 16, box);
        placeBlock(level, s(MachinesModule.SOLAR_PANEL.get()), 6, F + 3, 16, box);
        placeBlock(level, s(Blocks.SPAWNER), 2, F + 1, 19, box);
        BlockPos sp = getWorldPos(2, F + 1, 19);
        if (box.isInside(sp) && level.getBlockEntity(sp) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(MarsEntities.RUST_BEETLE.get(), RandomSource.create(seed + 7));
        }
        container(level, box, r, 8, F + 1, 13, MarsLoot.STATION_STORAGE, false);
        // trap: the station's perimeter defence still has power. A plate in the garage tunnel fires a dispenser hidden in
        // the tunnel wall.
        placeBlock(level, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE), 10, F + 1, 16, box);
        BlockPos disp = getWorldPos(10, F + 1, 15);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.placeToward(level, this, s(Blocks.DISPENSER),
                new BlockPos(10, F + 1, 15), new BlockPos(10, F + 1, 16), false, box);
        if (box.isInside(disp)) {
            DungeonBits.load(level, disp, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW, 12));
        }
        guard(level, box, com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities.SURVEY_DRONE.get(), 5, F + 3, 13);
        guard(level, box, com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities.SURVEY_DRONE.get(), 16, F + 3, 18);
    }

    private void exterior(WorldGenLevel level, BoundingBox box, RandomSource r) {
        // solar field in the south-east
        for (int x = 22; x <= 30; x += 2) {
            for (int z = 23; z <= 30; z += 2) {
                if (r.nextInt(5) != 0) {
                    placeBlock(level, s(MachinesModule.SOLAR_PANEL.get()), x, F + 1, z, box);
                }
            }
        }
        // core-drill rig in the south-west: a lattice tower over a cased hole
        for (int y = F + 1; y <= F + 10; y++) {
            placeBlock(level, s(Blocks.IRON_BARS), 4, y, 26, box);
            placeBlock(level, s(Blocks.IRON_BARS), 6, y, 26, box);
            placeBlock(level, s(Blocks.IRON_BARS), 4, y, 28, box);
            placeBlock(level, s(Blocks.IRON_BARS), 6, y, 28, box);
        }
        placeBlock(level, s(Blocks.ANVIL), 5, F + 10, 27, box);
        placeBlock(level, s(Blocks.CHAIN), 5, F + 9, 27, box);
        placeBlock(level, s(Blocks.CHAIN), 5, F + 8, 27, box);
        for (int y = F; y >= 0; y--) {
            placeBlock(level, y == F ? s(Blocks.IRON_BLOCK) : s(MarsModule.MARTIAN_CLAY.get()), 5, y, 27, box);
        }
        placeBlock(level, s(Blocks.HOPPER), 5, F + 1, 27, box);
        // dish antenna on the hab roof
        int ry = F + 8;
        placeBlock(level, s(Blocks.IRON_BLOCK), 16, ry, 16, box);
        placeBlock(level, s(Blocks.DAYLIGHT_DETECTOR), 16, ry + 1, 16, box);
        for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            placeBlock(level, s(Blocks.WHITE_CONCRETE), 16 + d[0], ry + 1, 16 + d[1], box);
        }
        placeBlock(level, s(Blocks.LIGHTNING_ROD), 16, ry + 2, 16, box);
    }

    /** Dust storms and decades: drifts against the walls, broken glass, holes in the panels. */
    private void decay(WorldGenLevel level, BoundingBox box, RandomSource r) {
        for (int i = 0; i < 80; i++) {
            int x = r.nextInt(SIZE);
            int z = r.nextInt(SIZE);
            int y = F + 1 + r.nextInt(6);
            BlockState here = getBlock(level, x, y, z, box);
            if (here.is(Blocks.GLASS) || here.is(Blocks.LIGHT_GRAY_STAINED_GLASS) || (here.is(Blocks.WHITE_CONCRETE) && r.nextInt(4) == 0)) {
                placeBlock(level, s(Blocks.AIR), x, y, z, box);
            }
        }
        for (int i = 0; i < 70; i++) {
            int x = r.nextInt(SIZE);
            int z = r.nextInt(SIZE);
            if (getBlock(level, x, F + 1, z, box).isAir() && !getBlock(level, x, F, z, box).isAir()) {
                placeBlock(level, r.nextInt(4) == 0 ? s(MarsModule.MARTIAN_SAND.get()) : s(MarsModule.MARTIAN_REGOLITH.get()), x, F + 1, z, box);
            }
        }
    }

    private void container(WorldGenLevel level, BoundingBox box, RandomSource layout, int x, int y, int z, ResourceKey<LootTable> loot, boolean chest) {
        RandomSource r = RandomSource.create(seed ^ (x * 341873128712L + y * 132897987541L + z));
        if (chest) {
            createChest(level, box, r, x, y, z, loot);
            return;
        }
        BlockPos pos = getWorldPos(x, y, z);
        if (box.isInside(pos)) {
            placeBlock(level, s(Blocks.BARREL), x, y, z, box);
            if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
                barrel.setLootTable(loot, r.nextLong());
            }
        }
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
