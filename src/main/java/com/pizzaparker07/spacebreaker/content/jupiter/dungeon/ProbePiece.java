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
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * An ice-drilling camp on Europa whose cryobot melted down through the shell. On the surface: a four-legged drill
 * derrick over the bore, its winch and floodlight on the crown and a railed grate platform round the collar; beside it
 * the crew's habitat, a hull cylinder with portholes and an airlock door, holding the sample lab and the last log; a
 * power mast with solar panels feeding the derrick. At the bottom of the shaft (a steel ladder bolted to its wall) the
 * cryobot broke into a cave in the ice where a pocket of salty water never froze - and something in that water glows.
 */
public class ProbePiece extends StructurePiece {
    public static final int SIZE = 21;
    /** Depth of the cave floor below the surface. */
    public static final int DEPTH = 24;
    static final ResourceKey<LootTable> INSTRUMENTS = Dungeons.lootTable("europa", "probe_instruments");
    static final ResourceKey<LootTable> SAMPLES = Dungeons.lootTable("europa", "probe_samples");
    private final long seed;

    public ProbePiece(long seed, int x, int y, int z, Direction facing) {
        super(JupiterStructures.PROBE_PIECE.get(), 0, makeBoundingBox(x, y - DEPTH, z, facing, SIZE, DEPTH + 10, SIZE));
        this.seed = seed;
        setOrientation(facing);
    }

    public ProbePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(JupiterStructures.PROBE_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** Local y of the surface. */
    static final int F = DEPTH;
    /** Local y of the cave floor. */
    static final int CAVE = 2;
    static final int SHAFT_X = 10;
    static final int SHAFT_Z = 10;

    /** World position of a local position (for showcases and tests). */
    public BlockPos world(int x, int y, int z) {
        return getWorldPos(x, y, z);
    }

    /** World position of the cave centre (for showcases and tests). */
    public BlockPos caveCentre() {
        return getWorldPos(10, CAVE + 1, 10);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        BlockState ice = s(JupiterModule.EUROPAN_ICE.get());
        BlockState stained = s(JupiterModule.STAINED_ICE.get());
        BlockState brine = s(JupiterModule.REFROZEN_BRINE.get());
        // the cave: a flattened ellipsoid melted out of the shell, its floor a frozen brine crust
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = 0; y <= CAVE + 8; y++) {
                    double e = Math.pow((x - 10) / 9.0, 2) + Math.pow((z - 10) / 8.0, 2) + Math.pow((y - CAVE - 2.5) / 4.5, 2);
                    if (e < 1 && y > CAVE) {
                        placeBlock(level, s(Blocks.AIR), x, y, z, box);
                    } else if (e < 1.35) {
                        placeBlock(level, y <= CAVE ? brine : (r.nextInt(5) == 0 ? stained : ice), x, y, z, box);
                    }
                }
            }
        }
        // the brine pool, a little lower, lit from below by the serpents' glow
        for (int x = 3; x <= 9; x++) {
            for (int z = 8; z <= 15; z++) {
                double e = Math.pow((x - 6) / 3.4, 2) + Math.pow((z - 11.5) / 3.8, 2);
                if (e < 1) {
                    placeBlock(level, s(Blocks.WATER), x, CAVE, z, box);
                    placeBlock(level, s(Blocks.WATER), x, CAVE - 1, z, box);
                    placeBlock(level, e < 0.3 ? s(Blocks.SEA_LANTERN) : brine, x, CAVE - 2, z, box);
                } else if (e < 1.4) {
                    placeBlock(level, brine, x, CAVE - 1, z, box);
                }
            }
        }
        // icicles hanging from the roof
        for (int[] c : new int[][]{{13, 6}, {15, 12}, {8, 5}, {12, 15}, {5, 7}}) {
            int top = CAVE + 6;
            int len = 1 + r.nextInt(3);
            for (int i = 0; i < len; i++) {
                placeBlock(level, ice, c[0], top - i, c[1], box);
            }
        }
        // the cryobot: a copper cylinder lying where it broke through, drill tip into the pool, tether up the shaft
        for (int y = CAVE + 1; y <= CAVE + 3; y++) {
            placeBlock(level, s(Blocks.WAXED_CUT_COPPER), SHAFT_X, y, SHAFT_Z, box);
        }
        placeBlock(level, s(Blocks.WAXED_COPPER_BULB), SHAFT_X, CAVE + 4, SHAFT_Z, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.WEST), SHAFT_X - 1, CAVE + 1, SHAFT_Z, box);
        placeBlock(level, s(Blocks.WAXED_CUT_COPPER_SLAB), SHAFT_X + 1, CAVE + 1, SHAFT_Z, box);
        // shaft: melted straight down, walls refrozen and stained, a ladder bolted in later and the tether beside it
        for (int y = CAVE + 5; y <= F; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean wall = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                    if (wall) {
                        placeBlock(level, r.nextInt(3) == 0 ? brine : stained, SHAFT_X + dx, y, SHAFT_Z + dz, box);
                    } else {
                        placeBlock(level, s(Blocks.AIR), SHAFT_X, y, SHAFT_Z, box);
                    }
                }
            }
            placeBlock(level, s(Blocks.CHAIN), SHAFT_X, y, SHAFT_Z, box);
        }
        for (int y = CAVE + 1; y <= F; y++) {
            placeBlock(level, s(Blocks.AIR), SHAFT_X, y, SHAFT_Z + 1, box);
            com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                    com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.STEEL_LADDER.get().defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH),
                    SHAFT_X, y, SHAFT_Z + 1, box);
            placeBlock(level, stained, SHAFT_X, y, SHAFT_Z + 2, box);
        }
        // instrument racks by the cave wall
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, 15, CAVE + 1, 8, Direction.WEST, INSTRUMENTS, seed + 1, box);
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, 15, CAVE + 1, 12, Direction.WEST, SAMPLES, seed + 2, box);
        placeBlock(level, s(Blocks.IRON_BLOCK), 15, CAVE + 1, 10, box);
        placeBlock(level, s(Blocks.DAYLIGHT_DETECTOR), 15, CAVE + 2, 10, box);
        placeBlock(level, s(Blocks.BREWING_STAND), 14, CAVE + 1, 10, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 13, CAVE + 5, 10, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 9, CAVE + 5, 12, box);
        surface(level, box, r);
        for (int i = 0; i < 3; i++) {
            BlockPos pos = getWorldPos(5 + i, CAVE - 1, 10 + i);
            if (box.isInside(pos)) {
                Mob m = JupiterEntities.GLOW_SERPENT.get().create(level.getLevel());
                if (m != null) {
                    m.moveTo(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, r.nextFloat() * 360F, 0F);
                    m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
                    m.setPersistenceRequired();
                    level.addFreshEntityWithPassengers(m);
                }
            }
        }
    }

    /** Habitat cylinder on the surface: its axis along x at (y, z) = (HAB_Y, HAB_Z), from HAB_X0 to HAB_X1 (door end). */
    static final int HAB_X0 = 2;
    public static final int HAB_X1 = 7;
    static final int HAB_Y = F + 2;
    public static final int HAB_Z = 16;

    /** World position of a local position on the surface (for showcases and tests). */
    public BlockPos surfacePos(int x, int dy, int z) {
        return getWorldPos(x, F + dy, z);
    }

    private static boolean round(int a, int b) {
        return Math.abs(a) <= 2 && Math.abs(b) <= 2 && !(Math.abs(a) == 2 && Math.abs(b) == 2);
    }

    /** The drilling camp on the surface: derrick, collar platform, habitat with the lab, power mast. */
    private void surface(WorldGenLevel level, BoundingBox box, RandomSource r) {
        BlockState frost = s(JupiterModule.EUROPAN_FROST.get());
        BlockState air = s(Blocks.AIR);
        BlockState strut = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.wall().get());
        BlockState deck = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.base().get());
        BlockState hazard = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.HAZARD_STRIPE.base().get());
        BlockState grate = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE_PLATFORM.get());
        BlockState rail = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.RAILING.get());
        BlockState hull = s(com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule.STATION_HULL.get());
        BlockState window = s(com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule.STATION_WINDOW.get());
        BlockState cable = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.CABLE_DUCT.get());
        BlockState solar = s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.SOLAR_PANEL.get());
        BlockState led = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get());
        // a cleared, frosted pad with open sky above it
        for (int x = 1; x <= 19; x++) {
            for (int z = 1; z <= 19; z++) {
                for (int y = F + 1; y <= F + 9; y++) {
                    placeBlock(level, air, x, y, z, box);
                }
                if (Math.hypot(x - SHAFT_X, z - SHAFT_Z) > 1.6) {
                    placeBlock(level, frost, x, F, z, box);
                }
            }
        }
        // shaft collar of refrozen, stained ice, and a railed grate platform round the bore
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                double d = Math.hypot(dx, dz);
                if (d > 1.6 && d < 2.9) {
                    placeBlock(level, s(JupiterModule.STAINED_ICE.get()), SHAFT_X + dx, F, SHAFT_Z + dz, box);
                    placeBlock(level, grate, SHAFT_X + dx, F + 1, SHAFT_Z + dz, box);
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if ((Math.abs(dx) == 2 || Math.abs(dz) == 2) && !(dz == 2 && dx == 0)) {
                    placeBlock(level, rail, SHAFT_X + dx, F + 2, SHAFT_Z + dz, box);
                }
            }
        }
        // the derrick: four straight legs at the platform corners up to a deck crown with the winch at its centre
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            for (int h = 0; h <= 6; h++) {
                placeBlock(level, strut, SHAFT_X + c[0], F + 1 + h, SHAFT_Z + c[1], box);
            }
            placeBlock(level, deck, SHAFT_X + c[0], F, SHAFT_Z + c[1], box);
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                BlockState b = dx == 0 && dz == 0 ? s(Blocks.GRINDSTONE) : edge ? deck : (dx == 0 || dz == 0 ? hazard : air);
                placeBlock(level, b, SHAFT_X + dx, F + 8, SHAFT_Z + dz, box);
            }
        }
        placeBlock(level, s(Blocks.CHAIN), SHAFT_X, F + 7, SHAFT_Z, box);
        placeBlock(level, s(Blocks.CHAIN), SHAFT_X, F + 6, SHAFT_Z, box);
        placeBlock(level, s(Blocks.CHAIN), SHAFT_X, F + 5, SHAFT_Z, box);
        placeBlock(level, s(Blocks.CHAIN), SHAFT_X, F + 4, SHAFT_Z, box);
        placeBlock(level, s(Blocks.CHAIN), SHAFT_X, F + 3, SHAFT_Z, box);
        placeBlock(level, s(Blocks.CHAIN), SHAFT_X, F + 2, SHAFT_Z, box);
        placeBlock(level, s(Blocks.CHAIN), SHAFT_X, F + 1, SHAFT_Z, box);
        placeBlock(level, led, SHAFT_X, F + 9, SHAFT_Z, box);
        // the power mast: a pole of railing with solar panels either side and a cable run to the derrick
        for (int y = F + 1; y <= F + 5; y++) {
            placeBlock(level, rail, 16, y, 4, box);
        }
        for (int x = 14; x <= 18; x++) {
            if (x != 16) {
                placeBlock(level, solar, x, F + 5, 4, box);
                placeBlock(level, solar, x, F + 5, 5, box);
            }
        }
        placeBlock(level, deck, 16, F + 5, 4, box);
        for (int x = 12; x <= 16; x++) {
            placeBlock(level, cable, x, F, 5, box);
        }
        // the habitat: a hull cylinder lying along x with portholes, its airlock door facing the derrick
        for (int x = HAB_X0; x <= HAB_X1; x++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (!round(dy, dz)) {
                        continue;
                    }
                    boolean shell = !round(dy - 1, dz) || !round(dy + 1, dz) || !round(dy, dz - 1) || !round(dy, dz + 1) || x == HAB_X0 || x == HAB_X1;
                    BlockState b = shell ? hull : air;
                    if (shell && dy == 0 && Math.abs(dz) == 2 && (x == 4 || x == 5)) {
                        b = window;
                    }
                    if (shell && dy == 2 && dz == 0 && x == 5) {
                        b = led;
                    }
                    placeBlock(level, b, x, HAB_Y + dy, HAB_Z + dz, box);
                }
            }
            // feet under the hull
            if (x == HAB_X0 + 1 || x == HAB_X1 - 1) {
                placeBlock(level, deck, x, F, HAB_Z - 2, box);
                placeBlock(level, deck, x, F, HAB_Z + 2, box);
            }
        }
        BlockState lower = com.pizzaparker07.spacebreaker.content.suit.SuitModule.AIRLOCK_DOOR.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.FACING, Direction.EAST)
                .setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, lower, HAB_X1, HAB_Y - 1, HAB_Z, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                lower.setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), HAB_X1, HAB_Y, HAB_Z, box);
        // inside: the sample lab against the back wall, cable runs, the crew's last log
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                s(com.pizzaparker07.spacebreaker.content.research.ResearchModule.RESEARCH_LAB.get())
                        .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST), HAB_X0 + 1, HAB_Y - 1, HAB_Z, box);
        placeBlock(level, cable, HAB_X0 + 1, HAB_Y, HAB_Z - 1, box);
        placeBlock(level, cable, HAB_X0 + 1, HAB_Y, HAB_Z + 1, box);
        placeBlock(level, s(Blocks.BREWING_STAND), HAB_X0 + 1, HAB_Y - 1, HAB_Z - 1, box);
        BlockPos sign = com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                s(Blocks.BIRCH_SIGN).setValue(net.minecraft.world.level.block.StandingSignBlock.ROTATION, 12), HAB_X0 + 1, HAB_Y - 1, HAB_Z + 1, box);
        if (sign != null && box.isInside(sign)) {
            com.pizzaparker07.spacebreaker.content.mars.dungeon.DungeonBits.signText(level, sign, "europa_probe_log");
        }
    }
}
