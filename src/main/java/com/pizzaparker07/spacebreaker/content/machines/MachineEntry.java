package com.pizzaparker07.spacebreaker.content.machines;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** A machine's block, item and block-entity type registered together. */
public record MachineEntry<B extends Block, T extends BlockEntity>(String id, DeferredBlock<B> block, DeferredItem<BlockItem> item,
                                                                   DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> type) {
}
