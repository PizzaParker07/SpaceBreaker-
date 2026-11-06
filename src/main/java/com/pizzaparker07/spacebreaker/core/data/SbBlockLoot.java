package com.pizzaparker07.spacebreaker.core.data;

import com.pizzaparker07.spacebreaker.core.module.Modules;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Set;
import java.util.function.Function;

/** Block loot datagen; modules contribute through {@link SbModule#addBlockLoot}. */
public class SbBlockLoot extends BlockLootSubProvider {
    public SbBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (SbModule module : Modules.ALL) {
            module.addBlockLoot(this);
        }
    }

    public void self(Block block) {
        dropSelf(block);
    }

    public void ore(Block block, Item drop) {
        add(block, b -> createOreDrop(b, drop));
    }

    /** Slabs drop two items when double. */
    public void slab(Block block) {
        add(block, createSlabItemTable(block));
    }

    public void custom(Block block, Function<Block, LootTable.Builder> builder) {
        add(block, builder);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return BuiltInRegistries.BLOCK.stream()
                .filter(b -> BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals(com.pizzaparker07.spacebreaker.Spacebreaker.MOD_ID))
                .toList();
    }
}
