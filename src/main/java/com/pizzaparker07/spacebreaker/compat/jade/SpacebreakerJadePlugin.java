package com.pizzaparker07.spacebreaker.compat.jade;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.info.BlockInfoLines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** Jade: extra lines in the block info box (see {@link BlockInfoLines}). Loaded by Jade only, when installed. */
@WailaPlugin
public final class SpacebreakerJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        // values the client does not have (the oxygen piped into a gas machine), sent only for those machines
        registration.registerBlockDataProvider(Lines.INSTANCE,
                com.pizzaparker07.spacebreaker.content.suit.block.GasMachineBlockEntity.class);
        // their pipe input reports no fluid ("Empty 32B"); the real amount is our piped-oxygen line
        registration.registerFluidStorage(NoGasMachineFluid.INSTANCE,
                com.pizzaparker07.spacebreaker.content.suit.block.GasMachineBlockEntity.class);
    }

    /** Hides Jade's fluid view for gas machines: an empty group list wins over the generic capability view. */
    enum NoGasMachineFluid implements snownee.jade.api.view.IServerExtensionProvider<net.minecraft.nbt.CompoundTag> {
        INSTANCE;

        private static final ResourceLocation UID = Spacebreaker.id("gas_machine_fluid");

        @Override
        public java.util.List<snownee.jade.api.view.ViewGroup<net.minecraft.nbt.CompoundTag>> getGroups(snownee.jade.api.Accessor<?> accessor) {
            return java.util.List.of();
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public int getDefaultPriority() {
            return -1000; // ahead of the generic capability view
        }
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Lines.INSTANCE, Block.class);
    }

    enum Lines implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = Spacebreaker.id("info_lines");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() != null) {
                BlockInfoLines.lines(accessor.getBlockEntity(), accessor.getServerData()).forEach(tooltip::add);
            }
        }

        @Override
        public void appendServerData(net.minecraft.nbt.CompoundTag data, BlockAccessor accessor) {
            BlockInfoLines.serverData(accessor.getBlockEntity(), data);
        }

        @Override
        public boolean shouldRequestData(BlockAccessor accessor) {
            return accessor.getBlockEntity() != null && BlockInfoLines.wantsServerData(accessor.getBlockEntity());
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
