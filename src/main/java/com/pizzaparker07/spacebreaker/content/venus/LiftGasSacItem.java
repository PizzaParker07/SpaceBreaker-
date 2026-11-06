package com.pizzaparker07.spacebreaker.content.venus;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Lift gas sac from a Cloud Jelly: a bladder of light gas. Squeeze it and hold on: it lets you sink slowly for half a
 * minute, which is what you want when a walkway gives way above the clouds of Venus.
 */
public class LiftGasSacItem extends Item {
    public static final int DURATION = 600;

    public LiftGasSacItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, java.util.List<net.minecraft.network.chat.Component> lines,
                                net.minecraft.world.item.TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("item.spacebreaker.lift_gas_sac.desc")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, DURATION, 0, false, true));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.PLAYERS, 0.6F, 1.6F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
