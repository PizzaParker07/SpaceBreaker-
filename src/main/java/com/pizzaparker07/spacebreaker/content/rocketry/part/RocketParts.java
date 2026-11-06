package com.pizzaparker07.spacebreaker.content.rocketry.part;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import java.util.Optional;
import java.util.function.Function;

/** Data-pack registry of rocket parts (synced to clients for the assembler preview and tooltips). */
public final class RocketParts {
    public static final ResourceKey<Registry<RocketPart>> REGISTRY = ResourceKey.createRegistryKey(Spacebreaker.id("rocket_part"));

    public static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY, RocketPart.CODEC, RocketPart.CODEC);
    }

    public static Optional<RocketPart> get(RegistryAccess access, ResourceLocation id) {
        return access.registry(REGISTRY).flatMap(r -> r.getOptional(id));
    }

    /** Lookup function for the maths code (null when unknown). */
    public static Function<ResourceLocation, RocketPart> lookup(RegistryAccess access) {
        Optional<Registry<RocketPart>> reg = access.registry(REGISTRY);
        return id -> reg.flatMap(r -> r.getOptional(id)).orElse(null);
    }

    public static ResourceLocation idOf(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public static Optional<RocketPart> forStack(RegistryAccess access, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return get(access, idOf(stack.getItem()));
    }

    private RocketParts() {
    }
}
