package com.pizzaparker07.spacebreaker.content.research;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** The Spacebreaker field guide: opens the illustrated, searchable guide (client/guide). */
public class GuideBookItem extends Item {
    /** Set by the client module; does nothing on a dedicated server. */
    public static Runnable opener = () -> {
    };

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            opener.run();
        } else if (player instanceof ServerPlayer sp) {
            sp.awardStat(Stats.ITEM_USED.get(this));
            Milestones.award(sp, Milestones.OPEN_GUIDE);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.guide_book").withStyle(ChatFormatting.GRAY));
    }
}
