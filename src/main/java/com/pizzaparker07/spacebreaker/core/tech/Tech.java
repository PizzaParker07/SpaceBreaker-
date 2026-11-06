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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Research tree core: the data-pack registry of {@link TechNode}s and the per-player set of known technologies.
 * (Research screens, data disks from dungeons, network sync and recipe gating are built on top of this.)
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
        }
        return added;
    }

    public static Set<ResourceLocation> known(Player player) {
        return Set.copyOf(player.getData(KNOWN));
    }

    private Tech() {
    }
}
