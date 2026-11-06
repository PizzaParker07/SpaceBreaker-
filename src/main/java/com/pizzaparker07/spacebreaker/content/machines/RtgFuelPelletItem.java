package com.pizzaparker07.spacebreaker.content.machines;

import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

/**
 * Radioisotope heat source for the RTG. Its activity halves every {@link #HALF_LIFE} ticks from the moment it is
 * first handled (time stamp in a data component, so it keeps decaying in chests too). Carried unshielded, it gives a
 * small radiation dose that the suit's dose system picks up through {@link Environment#addRadiationSource}.
 */
public class RtgFuelPelletItem extends Item {
    /** Three in-game weeks: a pellet runs an outpost for many play sessions before it needs replacing. */
    public static final long HALF_LIFE = 24_000L * 21;
    /** Time stamps snap to quarter days so pellets made close together still stack. */
    private static final long STAMP_STEP = 6_000L;
    /** Dose from one fresh pellet in the inventory, mSv/day (about 5x the Moon's surface dose). */
    public static final double DOSE_PER_PELLET = 8.0;

    public RtgFuelPelletItem(Properties properties) {
        super(properties);
    }

    public static void stamp(ItemStack stack, Level level) {
        if (!stack.has(MachinesModule.DECAY_START.get())) {
            stack.set(MachinesModule.DECAY_START.get(), level.getGameTime() / STAMP_STEP * STAMP_STEP);
        }
    }

    /** Remaining activity, 1 = fresh. Unstamped pellets count as fresh. */
    public static double activity(ItemStack stack, long gameTime) {
        Long start = stack.get(MachinesModule.DECAY_START.get());
        return start == null ? 1.0 : activity(Math.max(0L, gameTime - start));
    }

    public static double activity(long age) {
        return Math.pow(0.5, (double) age / HALF_LIFE);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide) {
            stamp(stack, level);
        }
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        stamp(stack, level);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Level level = context.level();
        double a = level == null ? 1.0 : activity(stack, level.getGameTime());
        tooltip.add(Component.translatable("tooltip.spacebreaker.rtg_fuel_pellet.activity", Math.round(a * 100),
                Math.round(com.pizzaparker07.spacebreaker.content.machines.machine.RtgBlockEntity.OUTPUT * a)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.spacebreaker.rtg_fuel_pellet.half_life",
                String.format(Locale.ROOT, "%d", HALF_LIFE / 24_000L)).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.spacebreaker.rtg_fuel_pellet.radiation").withStyle(ChatFormatting.GOLD));
    }

    /** Dose from pellets carried by players whose head is at (or next to) this position. */
    public static double carriedDose(Level level, BlockPos pos) {
        double dose = 0;
        for (Player p : level.players()) {
            if (p.isSpectator() || BlockPos.containing(p.getEyePosition()).distManhattan(pos) > 2) {
                continue;
            }
            dose += inventoryDose(p, level.getGameTime());
        }
        return dose;
    }

    /** Dose rate from every pellet in a player's inventory (hands, armour slots and backpack rows), mSv/day. */
    public static double inventoryDose(Player player, long gameTime) {
        double dose = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof RtgFuelPelletItem) {
                dose += s.getCount() * DOSE_PER_PELLET * activity(s, gameTime);
            }
        }
        return dose;
    }
}
