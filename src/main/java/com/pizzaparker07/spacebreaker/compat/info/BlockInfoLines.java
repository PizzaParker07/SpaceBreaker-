package com.pizzaparker07.spacebreaker.compat.info;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * Extra lines for block info overlays (Jade, WTHIT), shared by both plugins. Client side only: everything shown here
 * is already synced to the client by the block entity, so no server data provider is needed.
 */
public final class BlockInfoLines {
    public static final String KEY_SOLAR_DUST = "compat.spacebreaker.info.solar_dust";

    private BlockInfoLines() {
    }

    /** Lines for this block entity, empty when there is nothing to add. */
    public static List<Component> lines(BlockEntity be) {
        return List.of();
    }
}
