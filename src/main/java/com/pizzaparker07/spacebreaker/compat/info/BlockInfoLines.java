package com.pizzaparker07.spacebreaker.compat.info;

import com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity;
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
        if (be instanceof SolarPanelBlockEntity panel) {
            float dust = panel.dust();
            int output = Math.round(SolarPanelBlockEntity.dustFactor(dust) * 100F);
            return List.of(Component.translatable(KEY_SOLAR_DUST, Math.round(dust * 100F), output));
        }
        return List.of();
    }
}
