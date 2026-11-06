package com.pizzaparker07.spacebreaker.content.rocketry.pad;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The launch facility multiblock:
 * <pre>
 *   P P P P P        P = launch pad plating (16, the outer ring of a 5x5)
 *   P T T T P        T = flame trench grate (3x3); the rocket stands on the centre one
 *   P T T T P
 *   P T T T P        a service tower (6+ blocks high) stands just outside the ring, with the
 *   P P P P P        crew access arm and fuel umbilical on it, facing the pad
 * </pre>
 * The pad and trench are required to launch; tower, arm and umbilical are reported in the checklist.
 * <p>
 * The structure is defined in {@code data/spacebreaker/spacebreaker/multiblock/launch_site.json}, which the
 * Hologram Projector shows. The required pad and trench are read from the controller layer of that file; the
 * tower and its attachments stay flexible here (any side of the ring), the file only shows the suggested spot.
 */
public final class LaunchPad {
    public static final int MIN_TOWER = 6;
    public static final net.minecraft.resources.ResourceLocation SITE = com.pizzaparker07.spacebreaker.Spacebreaker.id("launch_site");

    /** Result of checking one trench centre. */
    public record Result(BlockPos center, List<BlockPos> missingPad, List<BlockPos> missingTrench, @Nullable BlockPos towerBase,
                         int towerHeight, List<BlockPos> arms, List<BlockPos> umbilicals) {
        public boolean formed() {
            return missingPad.isEmpty() && missingTrench.isEmpty();
        }

        public boolean hasTower() {
            return towerBase != null && towerHeight >= MIN_TOWER;
        }

        /** Where a rocket stands: on top of the centre grate. */
        public Vec3 rocketPos() {
            return Vec3.atBottomCenterOf(center.above());
        }
    }

    private LaunchPad() {
    }

    public static boolean isPad(BlockState s) {
        return s.is(RocketryModule.LAUNCH_PAD.get());
    }

    public static boolean isTrench(BlockState s) {
        return s.is(RocketryModule.FLAME_TRENCH.get());
    }

    /** Checks the facility around a trench centre (it does not need to be complete). */
    public static Result check(Level level, BlockPos center) {
        List<BlockPos> missingPad = new ArrayList<>();
        List<BlockPos> missingTrench = new ArrayList<>();
        var def = com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks.get(level.registryAccess(), SITE);
        if (def.isPresent()) {
            var placements = def.get().placements(center, Direction.NORTH);
            int base = placements.stream().filter(p -> p.pos().equals(center)).mapToInt(p -> p.layer()).findFirst().orElse(0);
            for (var p : placements) {
                BlockState shown = p.spec().display(net.minecraft.world.level.block.Rotation.NONE);
                // only the pad and trench of the controller layer are required (the tower base sits there too)
                if (p.layer() != base || !(isPad(shown) || isTrench(shown))) {
                    continue;
                }
                if (!p.spec().test(level.getBlockState(p.pos()), net.minecraft.world.level.block.Rotation.NONE)) {
                    (isTrench(shown) ? missingTrench : missingPad).add(p.pos());
                }
            }
        } else {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = center.offset(dx, 0, dz);
                    boolean inner = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                    BlockState s = level.getBlockState(p);
                    if (inner && !isTrench(s)) {
                        missingTrench.add(p);
                    } else if (!inner && !isPad(s)) {
                        missingPad.add(p);
                    }
                }
            }
        }
        // tower: the tallest column of tower blocks on the ring just outside the pad
        BlockPos bestBase = null;
        int bestHeight = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 3) {
                    continue;
                }
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos base = center.offset(dx, dy, dz);
                    int h = 0;
                    while (h < 64 && level.getBlockState(base.above(h)).is(RocketryModule.SERVICE_TOWER.get())) {
                        h++;
                    }
                    if (h > bestHeight) {
                        bestHeight = h;
                        bestBase = base;
                    }
                }
            }
        }
        List<BlockPos> arms = new ArrayList<>();
        List<BlockPos> umbilicals = new ArrayList<>();
        if (bestBase != null) {
            // attachments anywhere on the tower side of the pad, up to the top of the tower
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) < 2) {
                        continue;
                    }
                    for (int dy = 1; dy <= bestHeight + 1; dy++) {
                        p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        BlockState s = level.getBlockState(p);
                        if (s.is(RocketryModule.CREW_ARM.get())) {
                            arms.add(p.immutable());
                        } else if (s.is(RocketryModule.FUEL_UMBILICAL.get())) {
                            umbilicals.add(p.immutable());
                        }
                    }
                }
            }
        }
        return new Result(center.immutable(), missingPad, missingTrench, bestBase, bestHeight, arms, umbilicals);
    }

    /** The trench centre whose 3x3 contains {@code pos}, if pos is a trench block; best guess otherwise. */
    @Nullable
    public static BlockPos centerNear(Level level, BlockPos pos) {
        BlockPos best = null;
        int bestScore = -1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos c = pos.offset(dx, 0, dz);
                if (!isTrench(level.getBlockState(c))) {
                    continue;
                }
                int score = 0;
                for (int ix = -1; ix <= 1; ix++) {
                    for (int iz = -1; iz <= 1; iz++) {
                        if (isTrench(level.getBlockState(c.offset(ix, 0, iz)))) {
                            score++;
                        }
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = c;
                }
            }
        }
        return best;
    }

    /** Searches a box around {@code from} for a complete pad (consoles and umbilicals use this). */
    @Nullable
    public static Result findNear(Level level, BlockPos from, int radius) {
        Result best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -6; dy <= 2; dy++) {
                    p.set(from.getX() + dx, from.getY() + dy, from.getZ() + dz);
                    if (!isTrench(level.getBlockState(p))) {
                        continue;
                    }
                    if (!isTrench(level.getBlockState(p.east())) || !isTrench(level.getBlockState(p.west()))
                            || !isTrench(level.getBlockState(p.north())) || !isTrench(level.getBlockState(p.south()))) {
                        continue;
                    }
                    Result r = check(level, p.immutable());
                    double d = p.distSqr(from);
                    if (r.formed() && d < bestDist) {
                        best = r;
                        bestDist = d;
                    }
                }
            }
        }
        return best;
    }

    /** The formed pad a rocket at {@code pos} is standing on, or null. */
    @Nullable
    public static Result padUnder(Level level, Vec3 pos) {
        BlockPos below = BlockPos.containing(pos.x, pos.y - 0.5, pos.z);
        if (!isTrench(level.getBlockState(below))) {
            return null;
        }
        Result r = check(level, below);
        if (!r.formed() || Math.abs(pos.x - (below.getX() + 0.5)) > 0.6 || Math.abs(pos.z - (below.getZ() + 0.5)) > 0.6) {
            return null;
        }
        return r;
    }

    /** The rocket standing on the pad, if any. */
    @Nullable
    public static RocketEntity rocketOn(Level level, BlockPos center) {
        AABB box = new AABB(center.above()).inflate(0.5, 0.5, 0.5);
        List<RocketEntity> found = level.getEntitiesOfClass(RocketEntity.class, box);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Liftoff effects on the facility (arms already swing away during the countdown). */
    public static void onLiftoff(Level level, Result pad) {
        // nothing to change in the world: the trench and tower are effects-only
    }
}
