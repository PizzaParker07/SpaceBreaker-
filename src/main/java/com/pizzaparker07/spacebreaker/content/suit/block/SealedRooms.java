package com.pizzaparker07.spacebreaker.content.suit.block;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * All active oxygen distributors per dimension. Registered as an {@code Environment.OxygenSource}: a
 * position is breathable when it lies in the sealed, supplied room of any distributor. Block changes
 * near a room mark it for re-scanning.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID)
public final class SealedRooms {
    private static final Map<ResourceKey<Level>, List<OxygenDistributorBlockEntity>> BY_LEVEL = new ConcurrentHashMap<>();

    private SealedRooms() {
    }

    static void add(ServerLevel level, OxygenDistributorBlockEntity be) {
        List<OxygenDistributorBlockEntity> list = BY_LEVEL.computeIfAbsent(level.dimension(), k -> new CopyOnWriteArrayList<>());
        if (!list.contains(be)) {
            list.add(be);
        }
    }

    static void remove(ServerLevel level, OxygenDistributorBlockEntity be) {
        List<OxygenDistributorBlockEntity> list = BY_LEVEL.get(level.dimension());
        if (list != null) {
            list.remove(be);
        }
    }

    /** Environment.OxygenSource hook. */
    /** Positions of the distributors keeping a room pressurised in this level (the shelters to head back to). */
    public static List<BlockPos> activeDistributors(Level level) {
        List<OxygenDistributorBlockEntity> list = BY_LEVEL.get(level.dimension());
        if (list == null) {
            return List.of();
        }
        return list.stream().filter(be -> be.getLevel() == level && be.active()).map(OxygenDistributorBlockEntity::getBlockPos).toList();
    }

    public static boolean providesOxygen(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return false;
        }
        List<OxygenDistributorBlockEntity> list = BY_LEVEL.get(level.dimension());
        if (list == null) {
            return false;
        }
        for (OxygenDistributorBlockEntity be : list) {
            if (be.getLevel() == level && be.active() && be.room().contains(pos)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<OxygenDistributorBlockEntity> list = BY_LEVEL.get(level.dimension());
        if (list == null || list.isEmpty()) {
            return;
        }
        BlockPos pos = event.getPos();
        for (OxygenDistributorBlockEntity be : list) {
            if (be.room().near(pos) || (!be.room().sealed() && be.getBlockPos().distManhattan(pos) < 24)) {
                be.markDirty();
            }
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        BY_LEVEL.clear();
    }
}
