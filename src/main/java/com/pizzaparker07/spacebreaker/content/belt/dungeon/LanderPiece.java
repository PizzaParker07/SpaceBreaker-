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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Locale;

/**
 * A two-stage supply lander that came down hard and lies on its side at the end of the trench it ploughed: an octagonal
 * descent stage in aluminium panels with a gold foil band and its burnt-out engine pointing back along the skid, two
 * legs sticking up and the third snapped off in the trench; the white ascent cabin in front with portholes and an open
 * airlock door, two seats, a dead console and the crew's last message on a sign. One solar wing still hangs off the
 * descent stage, the other lies torn in the dust with the dish antenna; the cargo pods burst open along the trench. One
 * piece, dressed in each world's own ground: Vesta, Ganymede, Callisto, Enceladus, Miranda, Ariel, Titania, Umbriel,
 * Oberon, Triton, Proteus, Nereid, Nix, Hydra, Bennu, Ryugu, Eros, Itokawa, 67P, Encke, Halley, Tempel 1, Pluto and
 * Charon each have their own cargo and technology.
 */
public class LanderPiece extends StructurePiece {
    /** The worlds a lander can lie on, with the ground it digs into. */
    public enum Variant {
        VESTA, GANYMEDE, CALLISTO, ENCELADUS, MIRANDA, TRITON, PLUTO, CHARON,
        // new worlds go last: the order sets each world's structure-set salt
        ARIEL, TITANIA, UMBRIEL, OBERON, NIX, HYDRA, PROTEUS, NEREID, BENNU, RYUGU, EROS, ITOKAWA, CHURYUMOV_GERASIMENKO, ENCKE, HALLEY, TEMPEL_1;

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
                case ARIEL -> com.pizzaparker07.spacebreaker.content.uranus.UranusModule.ARIEL_REGOLITH.get().defaultBlockState();
                case TITANIA -> com.pizzaparker07.spacebreaker.content.uranus.UranusModule.TITANIA_REGOLITH.get().defaultBlockState();
                case UMBRIEL -> com.pizzaparker07.spacebreaker.content.uranus.Umbriel.REGOLITH.get().defaultBlockState();
                case OBERON -> com.pizzaparker07.spacebreaker.content.uranus.Oberon.REGOLITH.get().defaultBlockState();
                case NIX -> com.pizzaparker07.spacebreaker.content.pluto.PlutoMoonlets.NIX.soil.get().defaultBlockState();
                case HYDRA -> com.pizzaparker07.spacebreaker.content.pluto.PlutoMoonlets.HYDRA.soil.get().defaultBlockState();
                case PROTEUS -> com.pizzaparker07.spacebreaker.content.neptune.Proteus.PROTEUS.soil.get().defaultBlockState();
                case NEREID -> com.pizzaparker07.spacebreaker.content.neptune.Nereid.NEREID.soil.get().defaultBlockState();
                case BENNU -> com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.BENNU.soil.get().defaultBlockState();
                case RYUGU -> com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.RYUGU.soil.get().defaultBlockState();
                case EROS -> com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.EROS.soil.get().defaultBlockState();
                case ITOKAWA -> com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.ITOKAWA.soil.get().defaultBlockState();
                case CHURYUMOV_GERASIMENKO -> com.pizzaparker07.spacebreaker.content.belt.Comets.CHURYUMOV.soil.get().defaultBlockState();
                case ENCKE -> com.pizzaparker07.spacebreaker.content.belt.Comets.ENCKE.soil.get().defaultBlockState();
                case HALLEY -> com.pizzaparker07.spacebreaker.content.belt.Comets.HALLEY.soil.get().defaultBlockState();
                case TEMPEL_1 -> com.pizzaparker07.spacebreaker.content.belt.Comets.TEMPEL.soil.get().defaultBlockState();
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
                case ARIEL -> com.pizzaparker07.spacebreaker.content.uranus.UranusModule.ARIEL_ICE.base().get().defaultBlockState();
                case TITANIA -> com.pizzaparker07.spacebreaker.content.uranus.UranusModule.TITANIA_ICE.base().get().defaultBlockState();
                case UMBRIEL -> com.pizzaparker07.spacebreaker.content.uranus.Umbriel.ICE.base().get().defaultBlockState();
                case OBERON -> com.pizzaparker07.spacebreaker.content.uranus.Oberon.ICE.base().get().defaultBlockState();
                case NIX -> com.pizzaparker07.spacebreaker.content.pluto.PlutoMoonlets.NIX.rock.get().defaultBlockState();
                case HYDRA -> com.pizzaparker07.spacebreaker.content.pluto.PlutoMoonlets.HYDRA.rock.get().defaultBlockState();
                case PROTEUS -> com.pizzaparker07.spacebreaker.content.neptune.Proteus.PROTEUS.rock.get().defaultBlockState();
                case NEREID -> com.pizzaparker07.spacebreaker.content.neptune.Nereid.NEREID.rock.get().defaultBlockState();
                case BENNU -> com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.BENNU.rock.get().defaultBlockState();
                case RYUGU -> com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.RYUGU.rock.get().defaultBlockState();
                case EROS -> com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.EROS.rock.get().defaultBlockState();
                case ITOKAWA -> com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.ITOKAWA.rock.get().defaultBlockState();
                case CHURYUMOV_GERASIMENKO -> com.pizzaparker07.spacebreaker.content.belt.Comets.CHURYUMOV.rock.get().defaultBlockState();
                case ENCKE -> com.pizzaparker07.spacebreaker.content.belt.Comets.ENCKE.rock.get().defaultBlockState();
                case HALLEY -> com.pizzaparker07.spacebreaker.content.belt.Comets.HALLEY.rock.get().defaultBlockState();
                case TEMPEL_1 -> com.pizzaparker07.spacebreaker.content.belt.Comets.TEMPEL.rock.get().defaultBlockState();
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

    /** Which last message the crew left: dusty rubble, ice, or a comet. */
    public String logKey() {
        return switch (variant) {
            case CHURYUMOV_GERASIMENKO, ENCKE, HALLEY, TEMPEL_1 -> "lander_log_comet";
            case VESTA, BENNU, RYUGU, EROS, ITOKAWA, PROTEUS, NIX, HYDRA, NEREID -> "lander_log_dust";
            default -> "lander_log_ice";
        };
    }

    /** Cells of the descent stage's octagonal cross-section (radius 3 around the axis). */
    static boolean octagon(int dx, int dy) {
        return Math.abs(dx) <= 3 && Math.abs(dy) <= 3 && Math.abs(dx) + Math.abs(dy) <= 4;
    }

    /** Cells of the ascent cabin's round cross-section (radius 2). */
    static boolean round(int dx, int dy) {
        return Math.abs(dx) <= 2 && Math.abs(dy) <= 2 && !(Math.abs(dx) == 2 && Math.abs(dy) == 2);
    }

    /** Axis of the lander (x, y) and the extents of its parts along z. */
    public static final int AX = 6;
    static final int AY = F + 2;
    public static final int DESCENT0 = 15;
    public static final int DESCENT1 = 19;
    public static final int CABIN0 = 20;
    public static final int CABIN1 = 24;

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        BlockState soil = variant.soil();
        BlockState rock = variant.rock();
        BlockState air = s(Blocks.AIR);
        BlockState panel = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.base().get());
        BlockState foil = s(com.pizzaparker07.spacebreaker.content.belt.LanderBlocks.GOLD_FOIL_CLADDING.get());
        BlockState hull = s(com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule.STATION_HULL.get());
        BlockState window = s(com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule.STATION_WINDOW.get());
        BlockState rail = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.RAILING.get());
        BlockState deck = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.base().get());
        BlockState deckSlab = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.slab().get());
        BlockState hazard = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.HAZARD_STRIPE.base().get());
        BlockState duct = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PIPE_DUCT.get());
        BlockState cable = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.CABLE_DUCT.get());
        BlockState grate = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE_PLATFORM.get());
        BlockState solar = s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.SOLAR_PANEL.get());
        // level ground under the site, open sky above it
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < L; z++) {
                for (int y = 0; y < F; y++) {
                    placeBlock(level, y == F - 1 ? soil : rock, x, y, z, box);
                }
                for (int y = F; y < F + 9; y++) {
                    placeBlock(level, air, x, y, z, box);
                }
            }
        }
        // the skid trench, one to two blocks deep, soil heaped along both sides; scorched where the engine first hit
        for (int z = 1; z <= 13; z++) {
            int half = 1 + (z > 6 ? 1 : 0);
            for (int x = AX - half; x <= AX + half; x++) {
                placeBlock(level, air, x, F - 1, z, box);
                boolean deep = z > 5;
                if (deep) {
                    placeBlock(level, air, x, F - 2, z, box);
                }
                BlockState floor = z <= 3 && r.nextInt(3) > 0 ? s(Blocks.BLACKSTONE) : soil;
                placeBlock(level, floor, x, deep ? F - 3 : F - 2, z, box);
            }
            for (int side : new int[]{-1, 1}) {
                int x = AX + side * (half + 1);
                placeBlock(level, soil, x, F, z, box);
                if (z > 6 && r.nextInt(3) > 0) {
                    placeBlock(level, soil, x + side, F, z, box);
                }
            }
        }
        // the descent stage on its side: aluminium shell, a gold foil band, bulkheads at both ends, tanks inside
        for (int z = DESCENT0; z <= DESCENT1; z++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    if (!octagon(dx, dy)) {
                        continue;
                    }
                    boolean shell = !octagon(dx - 1, dy) || !octagon(dx + 1, dy) || !octagon(dx, dy - 1) || !octagon(dx, dy + 1);
                    boolean bulkhead = z == DESCENT0 || z == DESCENT1;
                    BlockState b;
                    if (shell) {
                        b = z == DESCENT0 + 1 || z == DESCENT0 + 2 ? foil : panel;
                    } else if (bulkhead) {
                        b = panel;
                    } else {
                        b = dx == 0 || dy == 0 ? duct : air;
                    }
                    placeBlock(level, b, AX + dx, AY + dy, z, box);
                }
            }
        }
        // the burnt-out engine, pointing back along the skid, and a hazard ring round its mount
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                com.pizzaparker07.spacebreaker.content.belt.LanderBlocks.SPENT_ENGINE.get().defaultBlockState()
                        .setValue(com.pizzaparker07.spacebreaker.content.belt.block.SpentEngineBlock.FACING, Direction.NORTH), AX, AY, DESCENT0 - 1, box);
        for (int[] o : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            placeBlock(level, hazard, AX + o[0], AY + o[1], DESCENT0, box);
        }
        // two legs sticking up and out, the third snapped off and lying in the trench
        for (int side : new int[]{-1, 1}) {
            placeBlock(level, rail, AX + side * 3, AY + 2, DESCENT0 + 1, box);
            placeBlock(level, rail, AX + side * 4, AY + 3, DESCENT0, box);
            placeBlock(level, rail, AX + side * 5, AY + 4, DESCENT0, box);
            placeBlock(level, deckSlab, AX + side * 5, AY + 5, DESCENT0, box);
        }
        for (int z = 8; z <= 10; z++) {
            placeBlock(level, rail, AX + 1, F - 2, z, box);
        }
        placeBlock(level, deckSlab, AX + 1, F - 2, 7, box);
        // one solar wing still on its boom off the descent stage
        placeBlock(level, rail, AX + 4, AY, DESCENT0 + 2, box);
        for (int x = AX + 5; x <= AX + 6; x++) {
            for (int z = DESCENT0 + 1; z <= DESCENT0 + 3; z++) {
                placeBlock(level, solar, x, AY, z, box);
            }
        }
        // the ascent cabin: a white hull cylinder with portholes, the airlock door at its nose, a step up to it
        for (int z = CABIN0; z <= CABIN1; z++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (!round(dx, dy)) {
                        continue;
                    }
                    boolean shell = !round(dx - 1, dy) || !round(dx + 1, dy) || !round(dx, dy - 1) || !round(dx, dy + 1) || z == CABIN0
                            || z == CABIN1;
                    BlockState b = shell ? hull : air;
                    if (shell && dy == 0 && Math.abs(dx) == 2 && z == CABIN0 + 2) {
                        b = window;
                    }
                    if (shell && dy == 2 && dx == 0 && z == CABIN0 + 2) {
                        b = window;
                    }
                    placeBlock(level, b, AX + dx, AY + dy, z, box);
                }
            }
        }
        BlockState lower = com.pizzaparker07.spacebreaker.content.suit.SuitModule.AIRLOCK_DOOR.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.FACING, Direction.SOUTH)
                .setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, lower, AX, AY - 1, CABIN1, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                lower.setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), AX, AY, CABIN1, box);
        placeBlock(level, deck, AX, F, CABIN1 + 1, box);
        // inside: two seats facing a dead console, the cabin locker and the crew's last message
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.stairs().get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.SOUTH), AX - 1, AY - 1, CABIN0 + 2, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.stairs().get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.SOUTH), AX + 1, AY - 1, CABIN0 + 2, box);
        placeBlock(level, cable, AX - 1, AY - 1, CABIN0 + 1, box);
        placeBlock(level, cable, AX + 1, AY - 1, CABIN0 + 1, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.POWER_MONITOR.get())
                        .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH), AX, AY - 1, CABIN0 + 1, box);
        placeBlock(level, s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get()), AX, AY + 1, CABIN0 + 2, box);
        createChest(level, box, RandomSource.create(seed + 1), AX - 1, AY - 1, CABIN1 - 1, variant.cabin());
        BlockPos sign = com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                s(Blocks.BIRCH_SIGN).setValue(net.minecraft.world.level.block.StandingSignBlock.ROTATION, 8), AX + 1, AY - 1, CABIN1 - 1, box);
        if (sign != null && box.isInside(sign)) {
            com.pizzaparker07.spacebreaker.content.mars.dungeon.DungeonBits.signText(level, sign, logKey());
        }
        // the other solar wing, torn off and lying in the dust beside the trench, with the dish antenna
        for (int x = 9; x <= 11; x++) {
            for (int z = 3; z <= 5; z++) {
                if (!(x == 11 && z == 5)) {
                    placeBlock(level, solar, x, F, z, box);
                }
            }
        }
        placeBlock(level, rail, 9, F, 6, box);
        for (int x = 1; x <= 3; x++) {
            for (int z = 8; z <= 10; z++) {
                placeBlock(level, grate, x, F, z, box);
            }
        }
        placeBlock(level, s(Blocks.LIGHTNING_ROD), 2, F + 1, 9, box);
        // the cargo pods burst open along the trench: the cargo in one, spilled packing in the other
        placeBlock(level, panel, 1, F, 14, box);
        placeBlock(level, hazard, 2, F, 14, box);
        placeBlock(level, panel, 1, F + 1, 14, box);
        placeBlock(level, panel, 1, F, 13, box);
        createChest(level, box, RandomSource.create(seed + 2), 2, F, 13, variant.cargo());
        placeBlock(level, panel, 11, F, 21, box);
        placeBlock(level, hazard, 11, F, 22, box);
        placeBlock(level, s(Blocks.BARREL), 10, F, 22, box);
        placeBlock(level, panel, 11, F + 1, 22, box);
        // debris strewn back along the skid
        for (int i = 0; i < 7; i++) {
            int x = 1 + r.nextInt(11);
            int z = 1 + r.nextInt(12);
            if (Math.abs(x - AX) <= 2) {
                continue;
            }
            BlockState debris = switch (r.nextInt(4)) {
                case 0 -> rail;
                case 1 -> grate;
                case 2 -> panel;
                default -> s(Blocks.CHAIN);
            };
            placeBlock(level, debris, x, F, z, box);
        }
    }
}
