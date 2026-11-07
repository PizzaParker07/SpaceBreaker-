package com.pizzaparker07.spacebreaker.compat.jade;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.info.BlockInfoLines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** Jade: extra lines in the block info box (see {@link BlockInfoLines}). Loaded by Jade only, when installed. */
@WailaPlugin
public final class SpacebreakerJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Lines.INSTANCE, Block.class);
    }

    enum Lines implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID = Spacebreaker.id("info_lines");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() != null) {
                BlockInfoLines.lines(accessor.getBlockEntity()).forEach(tooltip::add);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
