package com.pizzaparker07.spacebreaker.content.suit.block;

import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fills the sealed room around it with breathable air from an oxygen tank. The room is found by a
 * bounded, tick-spread flood fill ({@link SealedRoom}); while the room is sealed and the tank has oxygen,
 * every air block of it counts as breathable for {@code Environment.canBreathe}.
 * Oxygen use: 1 breathing tick per tick plus one per 40 blocks of volume.
 */
public class OxygenDistributorBlockEntity extends GasMachineBlockEntity {
    public static final int MAX_VOLUME = 4096;
    /** Full re-check interval even without block changes (catches changes the events miss). */
    static final int RECHECK_TICKS = 200;

    private final SealedRoom room;
    private boolean dirty = true;
    private boolean registered;
    private int sinceCheck;
    private SealedRoom.State lastState = SealedRoom.State.SCANNING;

    public OxygenDistributorBlockEntity(BlockPos pos, BlockState state) {
        super(SuitModule.OXYGEN_DISTRIBUTOR_BE.get(), pos, state, 0);
        this.room = new SealedRoom(pos, MAX_VOLUME);
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return stack.getItem() instanceof OxygenTankItem;
    }

    public SealedRoom room() {
        return room;
    }

    /** True while the room is sealed and supplied: its air blocks are breathable. */
    public boolean active() {
        return room.sealed() && OxygenTankItem.getOxygen(getStack()) > 0 && !isRemoved();
    }

    public void markDirty() {
        dirty = true;
    }

    /** Oxygen used per tick by a room of this size. */
    public static int usePerTick(int volume, long gameTime) {
        int perSecond = 20 + volume / 2; // 1/tick + 1 per 40 blocks
        return (int) (perSecond / 20 + (gameTime % 20 < perSecond % 20 ? 1 : 0));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OxygenDistributorBlockEntity be) {
        if (!be.registered) {
            SealedRooms.add((ServerLevel) level, be);
            be.registered = true;
        }
        if (++be.sinceCheck >= RECHECK_TICKS) {
            be.dirty = true;
        }
        if (be.dirty && !be.room.isScanning()) {
            be.dirty = false;
            be.sinceCheck = 0;
            be.room.restart();
        }
        if (be.room.tick(level)) {
            SealedRoom.State now = be.room.state();
            if (now != be.lastState && now == SealedRoom.State.SEALED) {
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.4F, 1.6F);
            }
            be.lastState = now;
        }
        ItemStack tank = be.getStack();
        if (be.room.sealed() && OxygenTankItem.getOxygen(tank) > 0) {
            int use = usePerTick(be.room.volume(), level.getGameTime());
            ItemStack copy = tank.copy();
            copy.set(SuitModule.OXYGEN.get(), Math.max(0, OxygenTankItem.getOxygen(copy) - use));
            be.items.setStackInSlot(0, copy);
        }
        boolean lit = be.active();
        if (state.getValue(GasMachineBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(GasMachineBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean removeOnUse(Player player) {
        return player.isShiftKeyDown();
    }

    @Override
    public void describe(Player player) {
        player.displayClientMessage(status(), true);
    }

    public Component status() {
        ItemStack tank = getStack();
        return switch (room.state()) {
            case SCANNING -> Component.translatable("message.spacebreaker.distributor.scanning").withStyle(ChatFormatting.GRAY);
            case LEAKING -> room.leak() != null
                    ? Component.translatable("message.spacebreaker.distributor.leak_at", room.leak().getX(), room.leak().getY(), room.leak().getZ())
                    .withStyle(ChatFormatting.RED)
                    : Component.translatable("message.spacebreaker.distributor.leak").withStyle(ChatFormatting.RED);
            case SEALED -> tank.isEmpty() || OxygenTankItem.getOxygen(tank) == 0
                    ? Component.translatable("message.spacebreaker.distributor.no_oxygen", room.volume()).withStyle(ChatFormatting.GOLD)
                    : Component.translatable("message.spacebreaker.distributor.sealed", room.volume(),
                    OxygenTankItem.time(OxygenTankItem.getOxygen(tank) / Math.max(1, usePerTick(room.volume(), 0)))).withStyle(ChatFormatting.AQUA);
        };
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel sl) {
            SealedRooms.remove(sl, this);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level instanceof ServerLevel sl) {
            SealedRooms.remove(sl, this);
        }
        registered = false;
    }
}
