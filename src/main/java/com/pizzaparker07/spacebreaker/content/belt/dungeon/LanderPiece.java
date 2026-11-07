package com.pizzaparker07.spacebreaker.content.belt.dungeon;

import com.pizzaparker07.spacebreaker.content.belt.BeltModule;
import com.pizzaparker07.spacebreaker.content.dungeons.Dungeons;
import com.pizzaparker07.spacebreaker.content.jupiter.JupiterModule;
import com.pizzaparker07.spacebreaker.content.saturn.SaturnModule;
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
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Locale;

/**
 * A supply lander that came down hard: a skid trench ploughed through the local soil and thrown up in a ridge, the
 * lander on its side at the end of it with one leg folded under, a solar wing torn off and lying flat, scattered debris -
 * and the survey team's cargo hold, buried beside it with a hatch and a ladder. One piece, dressed in each world's own
 * ground: Vesta, Ganymede, Callisto, Enceladus, Miranda, Triton, Pluto and Charon each have their own cargo and technology.
 */
public class LanderPiece extends StructurePiece {
    /** The worlds a lander can lie on, with the ground it digs into. */
    public enum Variant {
        VESTA, GANYMEDE, CALLISTO, ENCELADUS, MIRANDA, TRITON, PLUTO, CHARON;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        BlockState soil() {
            return switch (this) {
                case VESTA -> BeltModule.VESTAN_REGOLITH.get().defaultBlockState();
                case GANYMEDE -> JupiterModule.GANYMEDE_REGOLITH.get().defaultBlockState();
                case CALLISTO -> JupiterModule.CALLISTO_REGOLITH.get().defaultBlockState();
                case ENCELADUS -> SaturnModule.ENCELADUS_SNOW.get().defaultBlockState();
                case MIRANDA -> com.pizzaparker07.spacebreaker.content.uranus.UranusModule.MIRANDAN_REGOLITH.get().defaultBlockState();
                case TRITON -> com.pizzaparker07.spacebreaker.content.neptune.NeptuneModule.TRITON_FROST.get().defaultBlockState();
                case PLUTO -> com.pizzaparker07.spacebreaker.content.pluto.PlutoModule.PLUTONIAN_FROST.get().defaultBlockState();
                case CHARON -> com.pizzaparker07.spacebreaker.content.pluto.PlutoModule.CHARON_REGOLITH.get().defaultBlockState();
            };
        }

        BlockState rock() {
            return switch (this) {
                case VESTA -> BeltModule.EUCRITE.base().get().defaultBlockState();
                case GANYMEDE -> JupiterModule.DARK_ICE_CRUST.get().defaultBlockState();
                case CALLISTO -> JupiterModule.CALLISTAN_ROCK.base().get().defaultBlockState();
                case ENCELADUS -> SaturnModule.ENCELADUS_ICE.get().defaultBlockState();
                case MIRANDA -> com.pizzaparker07.spacebreaker.content.uranus.UranusModule.MIRANDAN_ICE.base().get().defaultBlockState();
                case TRITON -> com.pizzaparker07.spacebreaker.content.neptune.NeptuneModule.TRITON_ICE.base().get().defaultBlockState();
                case PLUTO -> com.pizzaparker07.spacebreaker.content.pluto.PlutoModule.PLUTONIAN_ICE.base().get().defaultBlockState();
                case CHARON -> com.pizzaparker07.spacebreaker.content.pluto.PlutoModule.CHARON_ICE.get().defaultBlockState();
            };
        }

        public ResourceKey<LootTable> cargo() {
            return Dungeons.lootTable(id(), "lander_cargo");
        }

        public ResourceKey<LootTable> cabin() {
            return Dungeons.lootTable(id(), "lander_cabin");
        }
    }

    public static final int W = 13;
    public static final int L = 27;
    static final int F = 5;
    private final long seed;
    private final Variant variant;

    public LanderPiece(long seed, Variant variant, int x, int y, int z, Direction facing) {
        super(BeltStructures.LANDER_PIECE.get(), 0, makeBoundingBox(x, y - F, z, facing, W, F + 9, L));
        this.seed = seed;
        this.variant = variant;
        setOrientation(facing);
    }

    public LanderPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(BeltStructures.LANDER_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
        this.variant = Variant.valueOf(tag.getString("Variant"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
        tag.putString("Variant", variant.name());
    }

    public Variant variant() {
        return variant;
    }

    /** World position of a local position (for showcases and tests). */
    public BlockPos world(int x, int y, int z) {
        return getWorldPos(x, y, z);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        BlockState soil = variant.soil();
        BlockState rock = variant.rock();
        // level ground under the site
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < L; z++) {
                for (int y = 0; y < F; y++) {
                    placeBlock(level, y == F - 1 ? soil : rock, x, y, z, box);
                }
                for (int y = F; y < F + 9; y++) {
                    placeBlock(level, s(Blocks.AIR), x, y, z, box);
                }
            }
        }
        // the skid trench, one to two blocks deep, with the soil heaped along both sides
        for (int z = 1; z <= 15; z++) {
            int half = 1 + (z > 8 ? 1 : 0);
            for (int x = 6 - half; x <= 6 + half; x++) {
                placeBlock(level, s(Blocks.AIR), x, F - 1, z, box);
                if (z > 6) {
                    placeBlock(level, s(Blocks.AIR), x, F - 2, z, box);
                    placeBlock(level, soil, x, F - 3, z, box);
                } else {
                    placeBlock(level, soil, x, F - 2, z, box);
                }
            }
            for (int side : new int[]{-1, 1}) {
                int x = 6 + side * (half + 1);
                placeBlock(level, soil, x, F, z, box);
                if (z > 8 && r.nextInt(3) > 0) {
                    placeBlock(level, soil, x + side, F, z, box);
                }
            }
        }
        // the lander on its side at the end of the trench: a 3x3x4 body, heat shield, two legs, the broken third
        int bz = 16;
        for (int x = 5; x <= 7; x++) {
            for (int y = F - 1; y <= F + 1; y++) {
                for (int z = bz; z <= bz + 3; z++) {
                    boolean edge = x == 5 || x == 7 || y == F - 1 || y == F + 1;
                    placeBlock(level, edge ? s(Blocks.WHITE_CONCRETE) : s(Blocks.AIR), x, y, z, box);
                }
            }
            placeBlock(level, s(Blocks.WAXED_CUT_COPPER), x, F, bz - 1, box);
        }
        placeBlock(level, s(Blocks.WAXED_CUT_COPPER), 5, F - 1, bz - 1, box);
        placeBlock(level, s(Blocks.WAXED_CUT_COPPER), 7, F + 1, bz - 1, box);
        placeBlock(level, s(Blocks.GOLD_BLOCK), 6, F + 1, bz + 1, box);
        placeBlock(level, s(Blocks.IRON_BLOCK), 6, F + 1, bz + 3, box);
        placeBlock(level, s(Blocks.GLASS_PANE), 5, F, bz + 2, box);
        placeBlock(level, s(Blocks.AIR), 7, F, bz + 2, box);
        createChest(level, box, RandomSource.create(seed + 1), 6, F, bz + 2, variant.cabin());
        for (int[] leg : new int[][]{{4, F + 1, bz}, {8, F + 1, bz + 3}}) {
            placeBlock(level, s(Blocks.IRON_BARS), leg[0], leg[1], leg[2], box);
            placeBlock(level, s(Blocks.IRON_BARS), leg[0] + (leg[0] < 6 ? -1 : 1), leg[1] + 1, leg[2], box);
        }
        placeBlock(level, s(Blocks.CHAIN), 8, F, bz + 1, box);
        // the torn solar wing lying flat beside the trench
        for (int x = 9; x <= 11; x++) {
            for (int z = 9; z <= 13; z++) {
                placeBlock(level, s(Blocks.DAYLIGHT_DETECTOR), x, F, z, box);
            }
        }
        placeBlock(level, s(Blocks.IRON_BARS), 9, F, 14, box);
        // debris strewn back along the skid
        for (int i = 0; i < 6; i++) {
            int x = 2 + r.nextInt(9);
            int z = 2 + r.nextInt(14);
            BlockState debris = switch (r.nextInt(4)) {
                case 0 -> s(Blocks.IRON_BARS);
                case 1 -> Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
                case 2 -> s(Blocks.CHAIN);
                default -> s(Blocks.LIGHTNING_ROD);
            };
            placeBlock(level, debris, x, F, z, box);
        }
        // the buried cargo hold beside the lander: a hatch, a ladder, a small room
        for (int x = 1; x <= 4; x++) {
            for (int z = 20; z <= 25; z++) {
                for (int y = 0; y <= 3; y++) {
                    boolean shell = x == 1 || x == 4 || z == 20 || z == 25 || y == 0 || y == 3;
                    placeBlock(level, shell ? s(Blocks.LIGHT_GRAY_CONCRETE) : s(Blocks.AIR), x, y, z, box);
                }
            }
        }
        for (int y = 1; y <= F - 1; y++) {
            com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), 2, y, 21, box);
        }
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, s(Blocks.IRON_TRAPDOOR), 2, F - 1, 21, box);
        placeBlock(level, s(Blocks.IRON_BLOCK), 3, F, 21, box);
        createChest(level, box, RandomSource.create(seed + 2), 3, 1, 24, variant.cargo());
        placeBlock(level, s(Blocks.BARREL), 2, 1, 24, box);
        placeBlock(level, s(Blocks.BARREL), 3, 1, 23, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 3, 2, 22, box);
    }
}
