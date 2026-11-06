package com.pizzaparker07.spacebreaker.content.saturn.dungeon;

import com.pizzaparker07.spacebreaker.content.dungeons.Dungeons;
import com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities;
import com.pizzaparker07.spacebreaker.content.saturn.SaturnModule;
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
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * An abandoned rotorcraft hangar on Titan's organic plains: a long vaulted hall of ice-brick and steel, its doors open
 * to the orange haze, with the expedition's eight-rotor flyer still parked on its charging pad, a workshop, a sample lab
 * and a fuel store of methane drawn from a nearby lake. Two of the old survey drones still keep watch.
 */
public class HangarPiece extends StructurePiece {
    public static final int W = 17;
    public static final int L = 25;
    static final int F = 4;
    static final ResourceKey<LootTable> WORKSHOP = Dungeons.lootTable("titan", "hangar_workshop");
    static final ResourceKey<LootTable> LAB = Dungeons.lootTable("titan", "hangar_lab");
    private final long seed;

    public HangarPiece(long seed, int x, int y, int z, Direction facing) {
        super(SaturnStructures.HANGAR_PIECE.get(), 0, makeBoundingBox(x, y - F, z, facing, W, F + 13, L));
        this.seed = seed;
        setOrientation(facing);
    }

    public HangarPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(SaturnStructures.HANGAR_PIECE.get(), tag);
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
        BlockState ice = s(SaturnModule.TITAN_ICE.base().get());
        BlockState mud = s(SaturnModule.THOLIN_MUD.get());
        BlockState wall = s(Blocks.LIGHT_GRAY_CONCRETE);
        BlockState rib = s(Blocks.IRON_BLOCK);
        // footing
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < L; z++) {
                for (int y = 0; y < F; y++) {
                    placeBlock(level, ice, x, y, z, box);
                }
                for (int y = F; y < F + 13; y++) {
                    placeBlock(level, s(Blocks.AIR), x, y, z, box);
                }
                placeBlock(level, mud, x, F - 1, z, box);
            }
        }
        // a vaulted hall: semicircular cross-section, radius 7, along z from 2 to 20, steel ribs every fourth block
        int cx = 8;
        for (int z = 2; z <= 20; z++) {
            for (int x = 1; x <= 15; x++) {
                for (int y = F; y <= F + 8; y++) {
                    double d = Math.hypot(x - cx, (y - F) * 1.15);
                    boolean shell = d >= 6.2 && d < 7.3;
                    if (shell) {
                        boolean ribHere = z % 4 == 0;
                        boolean window = !ribHere && y == F + 4 && (z % 4 == 2);
                        placeBlock(level, ribHere ? rib : (window ? s(Blocks.TINTED_GLASS) : wall), x, y, z, box);
                    } else if (d < 6.2) {
                        placeBlock(level, s(Blocks.AIR), x, y, z, box);
                    }
                }
                if (Math.abs(x - cx) <= 6) {
                    placeBlock(level, s(Blocks.SMOOTH_STONE), x, F - 1, z, box);
                }
            }
        }
        // back wall closed, front left open to the plains
        for (int x = 1; x <= 15; x++) {
            for (int y = F; y <= F + 8; y++) {
                if (Math.hypot(x - cx, (y - F) * 1.15) < 7.3) {
                    placeBlock(level, wall, x, y, 20, box);
                }
            }
        }
        // charging pad and the parked rotorcraft: a flat body, four arms, eight rotors folded as trapdoors
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = 8; z <= 14; z++) {
                boolean edge = x == cx - 3 || x == cx + 3 || z == 8 || z == 14;
                placeBlock(level, edge ? s(Blocks.YELLOW_TERRACOTTA) : s(Blocks.POLISHED_ANDESITE), x, F - 1, z, box);
            }
        }
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = 10; z <= 12; z++) {
                placeBlock(level, s(Blocks.WAXED_CUT_COPPER), x, F + 1, z, box);
            }
        }
        placeBlock(level, s(Blocks.WAXED_COPPER_BULB), cx, F + 2, 11, box);
        for (int[] a : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            placeBlock(level, s(Blocks.IRON_BARS), cx + a[0], F, 11 + a[1], box);
            BlockState rotor = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP);
            placeBlock(level, rotor, cx + a[0], F + 1, 11 + a[1], box);
            placeBlock(level, rotor, cx + a[0], F + 2, 11 + a[1], box);
        }
        // workshop (west wall) and lab (east wall)
        placeBlock(level, s(Blocks.SMITHING_TABLE), 3, F, 17, box);
        placeBlock(level, s(Blocks.ANVIL), 3, F, 16, box);
        createChest(level, box, RandomSource.create(seed + 1), 3, F, 18, WORKSHOP);
        placeBlock(level, s(Blocks.BREWING_STAND), 13, F, 17, box);
        placeBlock(level, s(Blocks.CARTOGRAPHY_TABLE), 13, F, 16, box);
        createChest(level, box, RandomSource.create(seed + 2), 13, F, 18, LAB);
        // methane fuel store: cryogenic tanks against the back wall
        for (int x = 6; x <= 10; x += 2) {
            placeBlock(level, s(Blocks.WHITE_CONCRETE), x, F, 19, box);
            placeBlock(level, s(Blocks.WHITE_CONCRETE), x, F + 1, 19, box);
            placeBlock(level, s(Blocks.LIGHT_BLUE_CONCRETE), x, F + 2, 19, box);
        }
        for (int z = 5; z <= 17; z += 6) {
            placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), cx, F + 7, z, box);
        }
        // tholin dust blown in through the open front
        for (int x = 2; x <= 14; x++) {
            for (int z = 2; z <= 5; z++) {
                if (r.nextInt(z) == 0) {
                    placeBlock(level, s(SaturnModule.THOLIN_SAND.get()), x, F - 1, z, box);
                }
            }
        }
        for (int[] g : new int[][]{{5, 8}, {11, 14}}) {
            BlockPos pos = getWorldPos(g[0], F + 3, g[1]);
            if (box.isInside(pos)) {
                Mob m = MoonEntities.SURVEY_DRONE.get().create(level.getLevel());
                if (m != null) {
                    m.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
                    m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
                    m.setPersistenceRequired();
                    level.addFreshEntityWithPassengers(m);
                }
            }
        }
    }
}
