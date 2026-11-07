package com.pizzaparker07.spacebreaker.compat.wthit;

import com.pizzaparker07.spacebreaker.compat.info.BlockInfoLines;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaClientPlugin;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * WTHIT: extra lines in the block info box (see {@link BlockInfoLines}). Declared in {@code waila_plugins.json};
 * loaded by WTHIT only, when installed.
 */
public final class SpacebreakerWthitPlugin implements IWailaClientPlugin {
    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(new IBlockComponentProvider() {
            @Override
            public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
                BlockEntity be = accessor.getBlockEntity();
                if (be != null) {
                    BlockInfoLines.lines(be).forEach(tooltip::addLine);
                }
            }
        }, Block.class);
    }
}
