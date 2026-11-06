package com.pizzaparker07.spacebreaker.datagen;

import com.pizzaparker07.spacebreaker.registry.ModBlocks;
import com.pizzaparker07.spacebreaker.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Set;

public class ModLootTables extends BlockLootSubProvider {
    public ModLootTables(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        add(ModBlocks.BAUXITE_ORE.get(), b -> createOreDrop(b, ModItems.RAW_BAUXITE.get()));
        add(ModBlocks.DEEPSLATE_BAUXITE_ORE.get(), b -> createOreDrop(b, ModItems.RAW_BAUXITE.get()));
        dropSelf(ModBlocks.RAW_BAUXITE_BLOCK.get());
        dropSelf(ModBlocks.LUNAR_REGOLITH.get());
        dropSelf(ModBlocks.MARE_BASALT.get());
        dropSelf(ModBlocks.ILMENITE_ORE.get()); // processed later in the ISRU chain
        dropSelf(ModBlocks.SOLAR_PANEL.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(DeferredHolder::get).map(Block.class::cast).toList();
    }
}
