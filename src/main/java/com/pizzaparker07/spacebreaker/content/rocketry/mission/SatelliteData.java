package com.pizzaparker07.spacebreaker.content.rocketry.mission;

import com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Every satellite and probe the players have sent, kept with the world (overworld data storage), and the
 * samples and data probes have sent home for players who were offline.
 */
public class SatelliteData extends SavedData {
    private static final String NAME = "spacebreaker_satellites";

    /** One satellite in orbit or probe on a surface. */
    public record Craft(ResourceLocation part, ResourceLocation body, boolean landed, @Nullable UUID owner, long time) {
    }

    private final List<Craft> crafts = new ArrayList<>();
    private final Map<UUID, List<ItemStack>> pending = new HashMap<>();

    public static SatelliteData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(SatelliteData::new, SatelliteData::load), NAME);
    }

    private static SatelliteData load(CompoundTag tag, HolderLookup.Provider registries) {
        SatelliteData d = new SatelliteData();
        for (Tag t : tag.getList("crafts", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            ResourceLocation part = ResourceLocation.tryParse(c.getString("part"));
            ResourceLocation body = ResourceLocation.tryParse(c.getString("body"));
            if (part != null && body != null) {
                d.crafts.add(new Craft(part, body, c.getBoolean("landed"), c.hasUUID("owner") ? c.getUUID("owner") : null, c.getLong("time")));
            }
        }
        CompoundTag p = tag.getCompound("pending");
        for (String key : p.getAllKeys()) {
            List<ItemStack> items = new ArrayList<>();
            for (Tag t : p.getList(key, Tag.TAG_COMPOUND)) {
                ItemStack.parse(registries, t).ifPresent(items::add);
            }
            d.pending.put(UUID.fromString(key), items);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Craft c : crafts) {
            CompoundTag t = new CompoundTag();
            t.putString("part", c.part().toString());
            t.putString("body", c.body().toString());
            t.putBoolean("landed", c.landed());
            if (c.owner() != null) {
                t.putUUID("owner", c.owner());
            }
            t.putLong("time", c.time());
            list.add(t);
        }
        tag.put("crafts", list);
        CompoundTag p = new CompoundTag();
        pending.forEach((uuid, items) -> {
            ListTag l = new ListTag();
            for (ItemStack s : items) {
                if (!s.isEmpty()) {
                    l.add(s.save(registries));
                }
            }
            p.put(uuid.toString(), l);
        });
        tag.put("pending", p);
        return tag;
    }

    public List<Craft> crafts() {
        return List.copyOf(crafts);
    }

    public void add(Craft craft) {
        crafts.add(craft);
        setDirty();
    }

    /** Planet systems with a communications relay in orbit (Earth's system is always covered by ground stations). */
    public Set<ResourceLocation> relaySystems() {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        out.add(com.pizzaparker07.spacebreaker.content.rocketry.nav.Location.EARTH);
        for (Craft c : crafts) {
            if (c.part().getPath().equals("comm_satellite")) {
                out.add(Locations.parentPlanet(c.body()).orElse(c.body()));
            }
        }
        return out;
    }

    /** Worlds mapped by an observation satellite or visited by a probe. */
    public Set<ResourceLocation> observed() {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        for (Craft c : crafts) {
            if (c.part().getPath().equals("observation_satellite") || c.landed()) {
                out.add(c.body());
            }
        }
        return out;
    }

    /** Gives a player what a probe or satellite sent home: now if online, otherwise on their next login. */
    public void deliver(MinecraftServer server, @Nullable UUID owner, ItemStack stack) {
        if (owner == null || stack.isEmpty()) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player != null) {
            if (!player.getInventory().add(stack.copy())) {
                player.drop(stack.copy(), false);
            }
            return;
        }
        pending.computeIfAbsent(owner, k -> new ArrayList<>()).add(stack.copy());
        setDirty();
    }

    /** Hands over what arrived while the player was away. */
    public void flush(ServerPlayer player) {
        List<ItemStack> items = pending.remove(player.getUUID());
        if (items == null) {
            return;
        }
        for (ItemStack s : items) {
            if (!player.getInventory().add(s)) {
                player.drop(s, false);
            }
        }
        setDirty();
    }
}
