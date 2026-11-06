package com.pizzaparker07.spacebreaker.core.data;

import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

import java.util.function.Consumer;

/**
 * Data-pack registry sink handed to modules during datagen (configured/placed features, biome modifiers, ...).
 * Several modules may contribute to the same registry; their bootstraps run one after another.
 */
public interface SbDatapack {
    <T> void add(ResourceKey<? extends Registry<T>> registry, Consumer<BootstrapContext<T>> bootstrap);
}
