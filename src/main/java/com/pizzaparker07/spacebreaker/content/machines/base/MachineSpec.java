package com.pizzaparker07.spacebreaker.content.machines.base;

import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.function.Supplier;

/** Everything a generic processing machine needs: its recipe kind, screen layout, energy buffer and tanks. */
public record MachineSpec(Supplier<? extends BlockEntityType<?>> type, Supplier<ProcessingRecipe.Kind> recipe, MachineLayout layout,
                          int energyCapacity, int maxReceive, List<TankSpec> tanks) {
}
