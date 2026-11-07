package com.pizzaparker07.spacebreaker.content.venus;

import com.pizzaparker07.spacebreaker.content.venus.entity.DescentProbe;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Venus Descent Probe: a small heat-hardened lander. Nobody can walk on the 464 °C, 92-bar surface, so samples come up
 * only this way: release it from the cloud deck (from an aerostat base, say) and it parachutes down and radios home.
 */
public class DescentProbeItem extends Item {
    public DescentProbeItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.spacebreaker.descent_probe.desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (level.dimension() != VenusModule.VENUS || player.getY() < VenusClimate.CLOUD_BASE - 20) {
            player.displayClientMessage(Component.translatable("message.spacebreaker.descent_probe.where"), true);
            return InteractionResultHolder.fail(stack);
        }
        level.addFreshEntity(DescentProbe.release(level, player.position().add(player.getLookAngle().scale(1.5)).add(0, 0.5, 0), player.getUUID()));
        level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_LAUNCH, SoundSource.PLAYERS, 1F, 0.8F);
        player.displayClientMessage(Component.translatable("message.spacebreaker.descent_probe.released"), true);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }
}
