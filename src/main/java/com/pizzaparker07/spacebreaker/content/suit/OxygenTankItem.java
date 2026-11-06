package com.pizzaparker07.spacebreaker.content.suit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Pressurised oxygen bottle for the EVA suit. Contents are measured in breathing ticks. Tanks are slotted
 * into the life-support backpack of the EVA torso (use the tank while wearing the torso, or click it onto
 * the torso in the inventory) and refilled at an Oxygen Filler.
 */
public class OxygenTankItem extends Item {
    /** Five minutes of breathing. */
    public static final int SMALL_CAPACITY = 20 * 60 * 5;
    /** Fifteen minutes of breathing. */
    public static final int LARGE_CAPACITY = 20 * 60 * 15;

    private final int capacity;

    public OxygenTankItem(Properties properties, int capacity) {
        super(properties.component(SuitModule.OXYGEN.get(), 0));
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public static int getOxygen(ItemStack stack) {
        return stack.getOrDefault(SuitModule.OXYGEN.get(), 0);
    }

    public static int capacityOf(ItemStack stack) {
        return stack.getItem() instanceof OxygenTankItem t ? t.capacity : 0;
    }

    public static ItemStack filled(OxygenTankItem item) {
        ItemStack stack = new ItemStack(item);
        stack.set(SuitModule.OXYGEN.get(), item.capacity);
        return stack;
    }

    /** Adds oxygen, returns the amount accepted. */
    public static int fill(ItemStack stack, int amount) {
        int cap = capacityOf(stack);
        int have = getOxygen(stack);
        int add = Math.max(0, Math.min(amount, cap - have));
        if (add > 0) {
            stack.set(SuitModule.OXYGEN.get(), have + add);
        }
        return add;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack torso = SuitTanks.wornTorso(player);
        if (torso.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            if (SuitTanks.insert(torso, stack)) {
                stack.shrink(1);
            } else {
                ItemStack old = SuitTanks.swapEmptiest(torso, stack);
                if (old.isEmpty()) {
                    return InteractionResultHolder.fail(stack);
                }
                stack.shrink(1);
                if (stack.isEmpty()) {
                    return InteractionResultHolder.success(old);
                }
                if (!player.getInventory().add(old)) {
                    player.drop(old, false);
                }
            }
            level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.8F, 1.2F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * getOxygen(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = (float) getOxygen(stack) / capacity;
        return f > 0.25F ? 0x36B1C0 : Mth.hsvToRgb(0.02F, 0.8F, 1F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int ticks = getOxygen(stack);
        tooltip.add(Component.translatable("tooltip.spacebreaker.oxygen", time(ticks), time(capacity)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.spacebreaker.oxygen.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static String time(int ticks) {
        int seconds = ticks / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
