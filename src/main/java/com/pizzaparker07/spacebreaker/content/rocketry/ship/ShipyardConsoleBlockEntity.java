package com.pizzaparker07.spacebreaker.content.rocketry.ship;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The shipyard console: keeps the spacecraft design worked out on it (synced to clients for the screen). */
public class ShipyardConsoleBlockEntity extends BlockEntity {
    private ShipDesign design = ShipDesign.EMPTY;

    public ShipyardConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(RocketryModule.SHIPYARD_CONSOLE_BE.get(), pos, state);
    }

    public ShipDesign design() {
        return design;
    }

    public void setDesign(ShipDesign design) {
        this.design = design;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        design = tag.contains("design") ? ShipDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("design")).result().orElse(ShipDesign.EMPTY) : ShipDesign.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ShipDesign.CODEC.encodeStart(NbtOps.INSTANCE, design).result().ifPresent(t -> tag.put("design", t));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
