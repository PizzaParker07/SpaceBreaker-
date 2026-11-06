package com.pizzaparker07.spacebreaker.content.suit;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

/**
 * The life-support backpack of an EVA torso holds up to {@link #SLOTS} oxygen tanks and optionally a
 * Maneuvering Unit. Both are stored as item components on the torso stack, so they travel with it
 * (dropping, storing in chests, multiplayer sync all work for free).
 */
public final class SuitTanks {
    public static final int SLOTS = 2;
    /** Most slots a backpack can have (with the extra oxygen upgrade). */
    public static final int MAX_SLOTS = 3;

    /** Tank slots of this torso: two, three with the extra oxygen upgrade. */
    public static int slots(ItemStack torso) {
        return SuitUpgrade.of(torso) == SuitUpgrade.EXTRA_OXYGEN ? MAX_SLOTS : SLOTS;
    }

    private SuitTanks() {
    }

    public static NonNullList<ItemStack> tanks(ItemStack torso) {
        NonNullList<ItemStack> list = NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);
        torso.getOrDefault(SuitModule.SUIT_TANKS.get(), ItemContainerContents.EMPTY).copyInto(list);
        return list;
    }

    public static void setTanks(ItemStack torso, List<ItemStack> tanks) {
        torso.set(SuitModule.SUIT_TANKS.get(), ItemContainerContents.fromItems(tanks));
    }

    public static boolean canHold(ItemStack torso) {
        return torso.getItem() instanceof SpaceSuitItem suit && suit.getType() == SpaceSuitItem.Type.CHESTPLATE;
    }

    /** Inserts a tank into a free slot. Returns false when both slots are taken. */
    public static boolean insert(ItemStack torso, ItemStack tank) {
        NonNullList<ItemStack> list = tanks(torso);
        for (int i = 0; i < slots(torso); i++) {
            if (list.get(i).isEmpty()) {
                list.set(i, tank.copyWithCount(1));
                setTanks(torso, list);
                return true;
            }
        }
        return false;
    }

    /** Removes the last inserted tank (or EMPTY). */
    public static ItemStack removeLast(ItemStack torso) {
        NonNullList<ItemStack> list = tanks(torso);
        for (int i = MAX_SLOTS - 1; i >= 0; i--) {
            if (!list.get(i).isEmpty()) {
                ItemStack out = list.get(i);
                list.set(i, ItemStack.EMPTY);
                setTanks(torso, list);
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Swaps in a fuller tank for the emptiest one; returns the removed tank or EMPTY. */
    public static ItemStack swapEmptiest(ItemStack torso, ItemStack tank) {
        NonNullList<ItemStack> list = tanks(torso);
        int idx = -1;
        int least = Integer.MAX_VALUE;
        for (int i = 0; i < slots(torso); i++) {
            int o = OxygenTankItem.getOxygen(list.get(i));
            if (o < least) {
                least = o;
                idx = i;
            }
        }
        if (idx < 0 || least >= OxygenTankItem.getOxygen(tank)) {
            return ItemStack.EMPTY;
        }
        ItemStack out = list.get(idx);
        list.set(idx, tank.copyWithCount(1));
        setTanks(torso, list);
        return out;
    }

    public static int oxygen(ItemStack torso) {
        int sum = 0;
        for (ItemStack t : tanks(torso)) {
            sum += OxygenTankItem.getOxygen(t);
        }
        return sum;
    }

    public static int capacity(ItemStack torso) {
        int sum = 0;
        for (ItemStack t : tanks(torso)) {
            if (t.getItem() instanceof OxygenTankItem item) {
                sum += item.getCapacity();
            }
        }
        return sum;
    }

    /** Takes up to {@code amount} breathing ticks from the tanks (fullest first). Returns what was taken. */
    public static int drain(ItemStack torso, int amount) {
        NonNullList<ItemStack> list = tanks(torso);
        int taken = 0;
        while (taken < amount) {
            int best = -1;
            for (int i = 0; i < MAX_SLOTS; i++) {
                if (OxygenTankItem.getOxygen(list.get(i)) > 0 && (best < 0 || OxygenTankItem.getOxygen(list.get(i)) > OxygenTankItem.getOxygen(list.get(best)))) {
                    best = i;
                }
            }
            if (best < 0) {
                break;
            }
            ItemStack t = list.get(best).copy();
            int have = OxygenTankItem.getOxygen(t);
            int take = Math.min(have, amount - taken);
            t.set(SuitModule.OXYGEN.get(), have - take);
            list.set(best, t);
            taken += take;
        }
        if (taken > 0) {
            setTanks(torso, list);
        }
        return taken;
    }

    // ------------------------------------------------------------------ maneuvering unit

    public static ItemStack maneuveringUnit(ItemStack torso) {
        NonNullList<ItemStack> list = NonNullList.withSize(1, ItemStack.EMPTY);
        torso.getOrDefault(SuitModule.SUIT_MMU.get(), ItemContainerContents.EMPTY).copyInto(list);
        return list.get(0);
    }

    public static void setManeuveringUnit(ItemStack torso, ItemStack unit) {
        if (unit.isEmpty()) {
            torso.remove(SuitModule.SUIT_MMU.get());
        } else {
            torso.set(SuitModule.SUIT_MMU.get(), ItemContainerContents.fromItems(List.of(unit.copyWithCount(1))));
        }
    }

    public static boolean hasManeuveringUnit(ItemStack torso) {
        return !maneuveringUnit(torso).isEmpty();
    }

    /** The torso the entity is wearing, or EMPTY. */
    public static ItemStack wornTorso(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return canHold(chest) ? chest : ItemStack.EMPTY;
    }

    static void appendTooltip(ItemStack torso, List<Component> tooltip) {
        List<ItemStack> list = tanks(torso);
        for (int i = 0; i < slots(torso); i++) {
            ItemStack t = list.get(i);
            if (t.getItem() instanceof OxygenTankItem item) {
                tooltip.add(Component.translatable("tooltip.spacebreaker.suit.tank", i + 1, t.getHoverName(),
                        OxygenTankItem.time(OxygenTankItem.getOxygen(t)), OxygenTankItem.time(item.getCapacity())).withStyle(ChatFormatting.AQUA));
            } else {
                tooltip.add(Component.translatable("tooltip.spacebreaker.suit.tank_empty", i + 1).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        ItemStack mmu = maneuveringUnit(torso);
        if (!mmu.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.spacebreaker.suit.mmu_installed", ManeuveringUnitItem.fuelPercent(mmu)).withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("tooltip.spacebreaker.suit.tank_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
