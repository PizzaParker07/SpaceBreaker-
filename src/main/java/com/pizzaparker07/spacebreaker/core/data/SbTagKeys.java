package com.pizzaparker07.spacebreaker.core.data;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/** Helpers for common ("c:") and mod tags. */
public final class SbTagKeys {
    public static TagKey<Block> commonBlock(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    public static TagKey<Item> commonItem(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    public static TagKey<Block> modBlock(String path) {
        return TagKey.create(Registries.BLOCK, Spacebreaker.id(path));
    }

    public static TagKey<Item> modItem(String path) {
        return TagKey.create(Registries.ITEM, Spacebreaker.id(path));
    }

    public static TagKey<Fluid> commonFluid(String path) {
        return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private SbTagKeys() {
    }
}
