package com.pizzaparker07.spacebreaker.core.tech;

import com.mojang.serialization.Codec;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.minecraft.core.RegistryAccess;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Research tree core: the data-pack registry of {@link TechNode}s and the per-player set of known technologies.
 * Research screens, tech disks from dungeons, network sync and the Research Lab live in {@code content/research};
 * other areas check requirements through {@link TechRequirement}.
 * <p>Every change made through {@link #learn} / {@link #forget} posts a {@link TechChangedEvent} on the NeoForge
 * event bus (server side), which the research module uses to sync clients and award advancements.
 */
public final class Tech {
    public static final ResourceKey<Registry<TechNode>> REGISTRY = ResourceKey.createRegistryKey(Spacebreaker.id("tech"));

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Spacebreaker.MOD_ID);

    private static final Codec<Set<ResourceLocation>> SET_CODEC = ResourceLocation.CODEC.listOf()
            .xmap(HashSet::new, ArrayList::new);

    public static final Supplier<AttachmentType<Set<ResourceLocation>>> KNOWN = ATTACHMENTS.register("known_tech",
            () -> AttachmentType.<Set<ResourceLocation>>builder(() -> new HashSet<>()).serialize(SET_CODEC).copyOnDeath().build());

    public static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY, TechNode.CODEC, TechNode.CODEC);
    }

    public static boolean knows(Player player, ResourceLocation tech) {
        return player.getData(KNOWN).contains(tech);
    }

    /** @return true if the technology was newly learned */
    public static boolean learn(Player player, ResourceLocation tech) {
        Set<ResourceLocation> known = new HashSet<>(player.getData(KNOWN));
        boolean added = known.add(tech);
        if (added) {
            player.setData(KNOWN, known);
            if (!player.level().isClientSide) {
                NeoForge.EVENT_BUS.post(new TechChangedEvent(player, Set.of(tech), Set.of()));
            }
        }
        return added;
    }

    /** @return true if the player knew the technology (operator/debug use) */
    public static boolean forget(Player player, ResourceLocation tech) {
        Set<ResourceLocation> known = new HashSet<>(player.getData(KNOWN));
        boolean removed = known.remove(tech);
        if (removed) {
            player.setData(KNOWN, known);
            if (!player.level().isClientSide) {
                NeoForge.EVENT_BUS.post(new TechChangedEvent(player, Set.of(), Set.of(tech)));
            }
        }
        return removed;
    }

    /** Replaces the whole known set without posting events (used by client sync and tests). */
    public static void setKnownSilently(Player player, Set<ResourceLocation> known) {
        player.setData(KNOWN, new HashSet<>(known));
    }

    public static Optional<TechNode> get(RegistryAccess access, ResourceLocation id) {
        return access.registry(REGISTRY).flatMap(r -> r.getOptional(id));
    }

    /** All technologies currently loaded (data packs included). */
    public static Map<ResourceLocation, TechNode> all(RegistryAccess access) {
        java.util.LinkedHashMap<ResourceLocation, TechNode> out = new java.util.LinkedHashMap<>();
        access.registry(REGISTRY).ifPresent(r -> r.entrySet().stream()
                .sorted(java.util.Comparator.comparing(e -> e.getKey().location()))
                .forEach(e -> out.put(e.getKey().location(), e.getValue())));
        return out;
    }

    /** True if every prerequisite of {@code tech} is known (the tech itself may or may not be). */
    public static boolean prerequisitesMet(Player player, TechNode tech) {
        Set<ResourceLocation> known = player.getData(KNOWN);
        return known.containsAll(tech.requires());
    }

    public static Set<ResourceLocation> known(Player player) {
        return Set.copyOf(player.getData(KNOWN));
    }

    private Tech() {
    }
}
