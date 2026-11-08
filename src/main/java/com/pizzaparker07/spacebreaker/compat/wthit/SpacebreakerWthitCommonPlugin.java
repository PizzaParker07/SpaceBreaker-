package com.pizzaparker07.spacebreaker.compat.wthit;

import com.pizzaparker07.spacebreaker.compat.info.BlockInfoLines;
import com.pizzaparker07.spacebreaker.content.suit.block.GasMachineBlockEntity;
import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.IWailaCommonPlugin;

/**
 * WTHIT, server side: the values {@link BlockInfoLines} needs that the client does not have (the oxygen piped into a
 * gas machine). Declared in {@code waila_plugins.json}; loaded by WTHIT only, when installed.
 */
public final class SpacebreakerWthitCommonPlugin implements IWailaCommonPlugin {
    @Override
    public void register(ICommonRegistrar registrar) {
        registrar.blockData(new IDataProvider<GasMachineBlockEntity>() {
            @Override
            public void appendData(IDataWriter data, IServerAccessor<GasMachineBlockEntity> accessor, IPluginConfig config) {
                BlockInfoLines.serverData(accessor.getTarget(), data.raw());
            }
        }, GasMachineBlockEntity.class);
    }
}
