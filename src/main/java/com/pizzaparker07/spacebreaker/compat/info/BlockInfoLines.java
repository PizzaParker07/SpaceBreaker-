package com.pizzaparker07.spacebreaker.compat.info;

import com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.block.GasMachineBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Extra lines for block info overlays (Jade, WTHIT), shared by both plugins. Most of what is shown is already synced
 * to the client by the block entity; what is not (the oxygen piped into a gas machine) comes from {@link #serverData},
 * which the plugins call on the server and hand back to {@link #lines} on the client. Energy, item slots and fluid
 * handlers are shown by the overlays' own NeoForge capability views.
 */
public final class BlockInfoLines {
    public static final String KEY_SOLAR_DUST = "compat.spacebreaker.info.solar_dust";
    public static final String KEY_GAS_TANK = "compat.spacebreaker.info.gas_tank";
    public static final String KEY_GAS_HOLDING = "compat.spacebreaker.info.gas_holding";
    public static final String KEY_GAS_EMPTY = "compat.spacebreaker.info.gas_empty";
    public static final String KEY_GAS_PIPED = "compat.spacebreaker.info.gas_piped";
    /** Server data: breathing ticks of oxygen piped into a gas machine. */
    public static final String DATA_PIPED_OXYGEN = "spacebreaker_piped_oxygen";

    private BlockInfoLines() {
    }

    /** Whether {@link #serverData} has anything to send for this block entity. */
    public static boolean wantsServerData(BlockEntity be) {
        return be instanceof GasMachineBlockEntity;
    }

    /** Server side: adds the values the client does not have to {@code data}. */
    public static void serverData(BlockEntity be, CompoundTag data) {
        if (be instanceof GasMachineBlockEntity gas) {
            data.putInt(DATA_PIPED_OXYGEN, gas.storedOxygen());
        }
    }

    /** Lines for this block entity without server data. */
    public static List<Component> lines(BlockEntity be) {
        return lines(be, new CompoundTag());
    }

    /** Lines for this block entity, empty when there is nothing to add; {@code data} is what {@link #serverData} wrote. */
    public static List<Component> lines(BlockEntity be, CompoundTag data) {
        if (be instanceof SolarPanelBlockEntity panel) {
            float dust = panel.dust();
            int output = Math.round(SolarPanelBlockEntity.dustFactor(dust) * 100F);
            return List.of(Component.translatable(KEY_SOLAR_DUST, Math.round(dust * 100F), output));
        }
        if (be instanceof GasMachineBlockEntity gas) {
            List<Component> out = new ArrayList<>();
            ItemStack stack = gas.getStack();
            if (stack.isEmpty()) {
                out.add(Component.translatable(KEY_GAS_EMPTY));
            } else if (stack.getItem() instanceof OxygenTankItem) {
                int have = OxygenTankItem.getOxygen(stack), cap = OxygenTankItem.capacityOf(stack);
                int percent = cap <= 0 ? 0 : Math.round(have * 100F / cap);
                out.add(Component.translatable(KEY_GAS_TANK, OxygenTankItem.time(have), OxygenTankItem.time(cap), percent));
            } else {
                out.add(Component.translatable(KEY_GAS_HOLDING, stack.getHoverName()));
            }
            if (data.contains(DATA_PIPED_OXYGEN)) {
                int piped = data.getInt(DATA_PIPED_OXYGEN);
                if (piped > 0) {
                    out.add(Component.translatable(KEY_GAS_PIPED, OxygenTankItem.time(piped),
                            OxygenTankItem.time(GasMachineBlockEntity.OXYGEN_BUFFER)));
                }
            }
            return out;
        }
        return List.of();
    }
}
