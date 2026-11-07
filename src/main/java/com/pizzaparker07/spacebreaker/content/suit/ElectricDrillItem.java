package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Electric Drill: a battery hand drill for prospecting. With charge it digs stone and soil much faster than any
 * pick or shovel (iron mining level) and spends {@link #FE_PER_BLOCK} per block instead of wearing out; flat, it
 * is barely better than a fist. Holds {@link #CAPACITY} FE; charge it in the Suit Maintenance Station or any
 * FE item charger (standard item energy capability).
 */
public class ElectricDrillItem extends DiggerItem {
    public static final int CAPACITY = 100_000;
    public static final int FE_PER_BLOCK = 100;
    public static final int MAX_TRANSFER = 1000;
    public static final float SPEED = 14F;
    /** Everything a pickaxe or a shovel mines. */
    public static final TagKey<Block> MINEABLE = TagKey.create(Registries.BLOCK, Spacebreaker.id("mineable/drill"));

    public ElectricDrillItem(Properties properties) {
        super(TitaniumTools.TITANIUM, MINEABLE, properties.stacksTo(1)
                .attributes(DiggerItem.createAttributes(TitaniumTools.TITANIUM, 1.0F, -2.6F))
                // the battery runs down, not the drill: unbreakable (tooltip line hidden)
                .component(net.minecraft.core.component.DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(false)));
    }

    public static int energy(ItemStack stack) {
        return stack.getOrDefault(SuitModule.DRILL_ENERGY.get(), 0);
    }

    public static void setEnergy(ItemStack stack, int fe) {
        stack.set(SuitModule.DRILL_ENERGY.get(), Mth.clamp(fe, 0, CAPACITY));
    }

    public static boolean charged(ItemStack stack) {
        return energy(stack) >= FE_PER_BLOCK;
    }

    /** Adds up to {@code fe}; returns what was taken. */
    public static int charge(ItemStack stack, int fe) {
        int add = Math.max(0, Math.min(fe, CAPACITY - energy(stack)));
        setEnergy(stack, energy(stack) + add);
        return add;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!state.is(MINEABLE)) {
            return 1.0F;
        }
        return charged(stack) ? SPEED : 1.5F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return charged(stack) && super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F && !(miner instanceof Player p && p.isCreative())) {
            setEnergy(stack, energy(stack) - FE_PER_BLOCK);
        }
        return true; // no durability: the battery is what runs down
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // hitting with it costs nothing
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * energy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x36B1C0;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.electric_drill.energy", energy(stack), CAPACITY)
                .withStyle(charged(stack) ? ChatFormatting.AQUA : ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.spacebreaker.electric_drill").withStyle(ChatFormatting.GRAY));
    }
}
