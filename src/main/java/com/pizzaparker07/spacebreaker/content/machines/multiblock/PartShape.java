package com.pizzaparker07.spacebreaker.content.machines.multiblock;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** What a multiblock part looks like and how it collides (shapes are drawn facing north). */
public enum PartShape implements StringRepresentable {
    PUMPJACK_WELL("pumpjack_well", Shapes.or(Block.box(0, 0, 0, 16, 3, 16), Block.box(6, 3, 6, 10, 10, 10))),
    PUMPJACK_REAR("pumpjack_rear", Shapes.or(Block.box(0, 0, 0, 16, 3, 16), Block.box(3, 3, 3, 13, 12, 13))),
    PUMPJACK_POST("pumpjack_post", Block.box(4, 0, 4, 12, 16, 12)),
    PUMPJACK_BEAM("pumpjack_beam", Block.box(5, 0, 0, 11, 8, 16)),
    PUMPJACK_HEAD("pumpjack_head", Block.box(5, 0, 0, 11, 12, 16)),
    REFINERY_COLUMN("refinery_column", Block.box(2, 0, 2, 14, 16, 14)),
    REFINERY_TOP("refinery_top", Block.box(2, 0, 2, 14, 12, 14));

    private final String name;
    public final VoxelShape shape;

    PartShape(String name, VoxelShape shape) {
        this.name = name;
        this.shape = shape;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
