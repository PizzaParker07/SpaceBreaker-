package com.pizzaparker07.spacebreaker.content.rocketry.pad;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Rotates voxel shapes authored facing north to any horizontal direction (cached). */
final class Shapes {
    private static final Map<VoxelShape, Map<Direction, VoxelShape>> CACHE = new ConcurrentHashMap<>();

    private Shapes() {
    }

    static VoxelShape rotate(VoxelShape north, Direction to) {
        return CACHE.computeIfAbsent(north, k -> new EnumMap<>(Direction.class)).computeIfAbsent(to, d -> {
            VoxelShape[] out = {net.minecraft.world.phys.shapes.Shapes.empty()};
            north.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
                double ax0, az0, ax1, az1;
                switch (d) {
                    case SOUTH -> { ax0 = 1 - x1; az0 = 1 - z1; ax1 = 1 - x0; az1 = 1 - z0; }
                    case EAST -> { ax0 = 1 - z1; az0 = x0; ax1 = 1 - z0; az1 = x1; }
                    case WEST -> { ax0 = z0; az0 = 1 - x1; ax1 = z1; az1 = 1 - x0; }
                    default -> { ax0 = x0; az0 = z0; ax1 = x1; az1 = z1; }
                }
                out[0] = net.minecraft.world.phys.shapes.Shapes.or(out[0], net.minecraft.world.phys.shapes.Shapes.box(ax0, y0, az0, ax1, y1, az1));
            });
            return out[0];
        });
    }
}
