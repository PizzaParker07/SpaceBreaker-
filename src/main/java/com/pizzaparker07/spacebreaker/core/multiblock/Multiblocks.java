package com.pizzaparker07.spacebreaker.core.multiblock;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Multiblock structures: the data-pack registry ({@code data/<ns>/spacebreaker/multiblock/*.json}, synced to
 * clients) and a validator that any machine, the Hologram Projector or the guide can use.
 *
 * <pre>{@code
 * MultiblockDefinition def = Multiblocks.get(level.registryAccess(), Spacebreaker.id("launch_site")).orElseThrow();
 * Multiblocks.Validation v = Multiblocks.validate(level, def, controllerPos, facing);
 * if (v.complete()) { ... } else { v.problems().get(0).pos() ... }
 * }</pre>
 */
public final class Multiblocks {
    public static final ResourceKey<Registry<MultiblockDefinition>> REGISTRY = ResourceKey.createRegistryKey(Spacebreaker.id("multiblock"));

    /** A position that is not built as the structure requires. */
    public record Problem(BlockPos pos, BlockSpec expected, BlockState actual, boolean missing) {
    }

    public record Validation(Direction facing, List<Problem> problems, int total) {
        public boolean complete() {
            return problems.isEmpty();
        }

        public int built() {
            return total - problems.size();
        }
    }

    private Multiblocks() {
    }

    /** Mod-bus listener; registered by the module that owns the projector (BlueprintModule). */
    public static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY, MultiblockDefinition.CODEC, MultiblockDefinition.CODEC);
    }

    public static Optional<Registry<MultiblockDefinition>> registry(RegistryAccess access) {
        return access.registry(REGISTRY);
    }

    public static Optional<MultiblockDefinition> get(RegistryAccess access, ResourceLocation id) {
        return registry(access).flatMap(r -> r.getOptional(id));
    }

    /** All definitions, sorted by id. */
    public static List<Map.Entry<ResourceLocation, MultiblockDefinition>> all(RegistryAccess access) {
        List<Map.Entry<ResourceLocation, MultiblockDefinition>> out = new ArrayList<>();
        registry(access).ifPresent(r -> r.entrySet().forEach(e -> out.add(Map.entry(e.getKey().location(), e.getValue()))));
        out.sort(Map.Entry.comparingByKey());
        return out;
    }

    /** Checks the structure whose controller is at {@code controllerPos}, facing {@code facing}. */
    public static Validation validate(Level level, MultiblockDefinition def, BlockPos controllerPos, Direction facing) {
        Rotation rot = MultiblockDefinition.rotation(facing);
        List<Problem> problems = new ArrayList<>();
        List<MultiblockDefinition.Placement> all = def.placements(controllerPos, facing);
        for (MultiblockDefinition.Placement p : all) {
            BlockState actual = level.getBlockState(p.pos());
            if (!p.spec().test(actual, rot)) {
                problems.add(new Problem(p.pos(), p.spec(), actual, actual.isAir() || actual.canBeReplaced()));
            }
        }
        return new Validation(facing, problems, all.size());
    }

    /** Tries the four horizontal facings and returns the closest match (fewest problems). */
    public static Validation validateAnyFacing(Level level, MultiblockDefinition def, BlockPos controllerPos) {
        Validation best = null;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            Validation v = validate(level, def, controllerPos, d);
            if (best == null || v.problems().size() < best.problems().size()) {
                best = v;
            }
        }
        return best;
    }

    /** Definitions whose controller accepts this block. */
    public static List<Map.Entry<ResourceLocation, MultiblockDefinition>> controlledBy(RegistryAccess access, BlockState state) {
        return all(access).stream().filter(e -> e.getValue().controllerSpec().test(state, Rotation.NONE)).toList();
    }

    /** Places the structure exactly as defined (tests, creative tools, showcases). */
    public static void place(Level level, MultiblockDefinition def, BlockPos controllerPos, Direction facing) {
        Rotation rot = MultiblockDefinition.rotation(facing);
        for (MultiblockDefinition.Placement p : def.placements(controllerPos, facing)) {
            level.setBlock(p.pos(), p.spec().display(rot), 3);
        }
    }
}
