package com.pizzaparker07.spacebreaker.content.blueprint;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.multiblock.BlockSpec;
import com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition;
import com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The projector's "build" action: places what is missing from a projection out of the player's inventory, layer by
 * layer, one block every few ticks, with the block's own place sound and a puff of particles. Creative players need
 * no materials. Positions holding a wrong block are left alone and only counted. Every placement goes through the
 * block place event (protection mods can refuse it) and the block's own placement hook, so a controller assembles or
 * links its structure just as when a player sets it down. One job per player; it stops when the player leaves the
 * dimension or walks away.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID)
public final class BlueprintBuilder {
    /** Ticks between two placed blocks. */
    public static final int INTERVAL = 3;
    /** The builder stops when the player is farther than this from the projection (blocks). */
    public static final double MAX_DISTANCE = 48;

    /** What a finished (or stopped) job did. */
    public record Result(ResourceLocation blueprint, int placed, Map<Item, Integer> missing, int blocked, boolean complete, boolean stopped) {
    }

    private static final class Job {
        final ServerPlayer player;
        final ServerLevel level;
        final ResourceLocation id;
        final MultiblockDefinition def;
        final BlockPos anchor;
        final Direction facing;
        final List<MultiblockDefinition.Placement> queue;
        int next;
        int cooldown;
        int placed;
        int blocked;
        final Map<Item, Integer> missing = new LinkedHashMap<>();

        Job(ServerPlayer player, ResourceLocation id, MultiblockDefinition def, BlockPos anchor, Direction facing) {
            this.player = player;
            this.level = player.serverLevel();
            this.id = id;
            this.def = def;
            this.anchor = anchor;
            this.facing = facing;
            List<MultiblockDefinition.Placement> all = new ArrayList<>();
            for (MultiblockDefinition.Placement p : def.placements(anchor, facing)) {
                // a self-assembling structure needs only its controller; air positions are not built
                if (!p.spec().isAir() && (!def.selfAssembling() || def.isController(p))) {
                    all.add(p);
                }
            }
            // bottom layer first, and within a layer in the definition's own order
            all.sort((a, b) -> Integer.compare(a.layer(), b.layer()));
            this.queue = all;
        }
    }

    private static final Map<UUID, Job> JOBS = new HashMap<>();
    private static final Map<UUID, Result> LAST = new HashMap<>();

    private BlueprintBuilder() {
    }

    /** Starts (or restarts) building the projection on the player's projector. Returns false when there is none. */
    public static boolean start(ServerPlayer player, Hologram h) {
        if (h == null || h.anchor().isEmpty()) {
            return false;
        }
        var def = Multiblocks.get(player.registryAccess(), h.blueprint());
        if (def.isEmpty()) {
            return false;
        }
        LAST.remove(player.getUUID());
        JOBS.put(player.getUUID(), new Job(player, h.blueprint(), def.get(), h.anchor().get(), h.facing()));
        player.displayClientMessage(Component.translatable("message.spacebreaker.hologram.build_start", BlueprintModule.name(h.blueprint())), true);
        return true;
    }

    public static boolean building(ServerPlayer player) {
        return JOBS.containsKey(player.getUUID());
    }

    /** What the player's last job did, or null (tests, and the summary message). */
    public static Result lastResult(ServerPlayer player) {
        return LAST.get(player.getUUID());
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (JOBS.isEmpty()) {
            return;
        }
        var it = JOBS.values().iterator();
        while (it.hasNext()) {
            Job job = it.next();
            Boolean done = step(job);
            if (done != null) {
                it.remove();
                finish(job, !done);
            }
        }
    }

    @SubscribeEvent
    static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        JOBS.clear();
        LAST.clear();
    }

    /** One tick of a job: null while it goes on, true when finished, false when it had to stop. */
    private static Boolean step(Job job) {
        ServerPlayer p = job.player;
        if (p.isRemoved() || p.serverLevel() != job.level || p.blockPosition().distSqr(job.anchor) > MAX_DISTANCE * MAX_DISTANCE) {
            return false;
        }
        if (job.cooldown-- > 0) {
            return null;
        }
        Rotation rot = MultiblockDefinition.rotation(job.facing);
        while (job.next < job.queue.size()) {
            MultiblockDefinition.Placement pl = job.queue.get(job.next++);
            BlockState actual = job.level.getBlockState(pl.pos());
            if (pl.spec().test(actual, rot)) {
                continue;                                         // already right
            }
            if (!actual.isAir() && !actual.canBeReplaced()) {
                job.blocked++;                                    // a wrong block: leave it, only warn
                continue;
            }
            if (place(job, pl, rot)) {
                job.cooldown = INTERVAL - 1;
                return null;                                      // one block per step
            }
        }
        return true;
    }

    /** Places one position from the inventory (creative: for free). Returns true when a block was set. */
    private static boolean place(Job job, MultiblockDefinition.Placement pl, Rotation rot) {
        ServerPlayer player = job.player;
        boolean creative = player.getAbilities().instabuild;
        ItemStack source = ItemStack.EMPTY;
        BlockState state = null;
        for (Map.Entry<Item, BlockState> option : options(pl.spec(), rot).entrySet()) {
            ItemStack found = creative ? new ItemStack(option.getKey()) : find(player, option.getKey());
            if (!found.isEmpty()) {
                source = found;
                state = option.getValue();
                break;
            }
        }
        if (state == null) {
            job.missing.merge(pl.spec().item(), 1, Integer::sum);
            return false;
        }
        ServerLevel level = job.level;
        BlockPos pos = pl.pos();
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, state, 3)) {
            return false;
        }
        if (EventHooks.onBlockPlace(player, snapshot, Direction.UP)) {
            snapshot.restore();                                   // refused (protected area, ...): put it back
            job.blocked++;
            return false;
        }
        ItemStack placedFrom = source.copyWithCount(1);
        if (!creative) {
            source.shrink(1);
        }
        state.getBlock().setPlacedBy(level, pos, level.getBlockState(pos), player, placedFrom);
        var sound = state.getSoundType(level, pos, player);
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1F) / 2F, sound.getPitch() * 0.8F);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.01);
        job.placed++;
        return true;
    }

    /** Items that can fill a position and the state each one places (any_of options, tag members, or the block). */
    static Map<Item, BlockState> options(BlockSpec spec, Rotation rot) {
        Map<Item, BlockState> out = new LinkedHashMap<>();
        if (!spec.anyOf().isEmpty()) {
            for (BlockSpec option : spec.anyOf()) {
                options(option, rot).forEach(out::putIfAbsent);
            }
            return out;
        }
        if (spec.tag().isPresent()) {
            BuiltInRegistries.BLOCK.getTag(spec.tag().get()).ifPresent(set -> set.forEach(h -> {
                Item item = h.value().asItem();
                if (item != Items.AIR) {
                    out.putIfAbsent(item, h.value().defaultBlockState());
                }
            }));
            return out;
        }
        BlockState state = spec.display(rot);
        Item item = state.getBlock().asItem();
        if (item != Items.AIR) {
            out.put(item, state);
        }
        return out;
    }

    /** A stack of {@code item} in the player's inventory (hotbar and main inventory), or EMPTY. */
    private static ItemStack find(ServerPlayer player, Item item) {
        for (ItemStack s : player.getInventory().items) {
            if (!s.isEmpty() && s.is(item) && s.getItem() instanceof BlockItem) {
                return s;
            }
        }
        for (ItemStack s : player.getInventory().offhand) {
            if (!s.isEmpty() && s.is(item)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void finish(Job job, boolean stopped) {
        boolean complete = Multiblocks.validate(job.level, job.def, job.anchor, job.facing).complete();
        Result r = new Result(job.id, job.placed, Map.copyOf(job.missing), job.blocked, complete, stopped);
        LAST.put(job.player.getUUID(), r);
        Component name = BlueprintModule.name(job.id);
        ServerPlayer p = job.player;
        if (stopped) {
            p.displayClientMessage(Component.translatable("message.spacebreaker.hologram.build_stopped", job.placed), false);
            return;
        }
        if (complete) {
            p.displayClientMessage(Component.translatable("message.spacebreaker.hologram.build_done", name), true);
            job.level.playSound(null, job.anchor, net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.5F, 1.4F);
            return;
        }
        if (!job.missing.isEmpty()) {
            p.displayClientMessage(Component.translatable("message.spacebreaker.hologram.build_missing", job.placed, missingList(job.missing)), false);
        }
        if (job.blocked > 0) {
            p.displayClientMessage(Component.translatable("message.spacebreaker.hologram.build_blocked", job.blocked), false);
        }
    }

    /** "3× Launch Pad Plating, 1× Fuel Umbilical". */
    public static MutableComponent missingList(Map<Item, Integer> missing) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (Map.Entry<Item, Integer> e : missing.entrySet()) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append(e.getValue() + "× ").append(new ItemStack(e.getKey()).getHoverName());
        }
        return out;
    }
}
