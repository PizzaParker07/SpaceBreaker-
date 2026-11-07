package com.pizzaparker07.spacebreaker.content.rocketry.item;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketLayout;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketMath;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketParts;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** An assembled rocket, ready to be stood up on a launch pad. Carries its design as a data component. */
public class RocketItem extends Item {
    public RocketItem(Properties properties) {
        super(properties);
    }

    public static RocketDesign design(ItemStack stack) {
        return stack.getOrDefault(RocketryModule.ROCKET_DESIGN.get(), RocketDesign.EMPTY);
    }

    public static ItemStack of(RocketDesign design) {
        ItemStack stack = new ItemStack(RocketryModule.ROCKET.get());
        stack.set(RocketryModule.ROCKET_DESIGN.get(), design);
        return stack;
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        net.minecraft.world.level.Level level = context.getLevel();
        RocketDesign design = design(context.getItemInHand());
        if (design.isEmpty()) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        net.minecraft.core.BlockPos center = com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchPad.centerNear(level, context.getClickedPos());
        com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchPad.Result pad = center == null ? null
                : com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchPad.check(level, center);
        net.minecraft.world.entity.player.Player player = context.getPlayer();
        if (pad == null || !pad.formed()) {
            if (player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.spacebreaker.rocket.needs_pad")
                        .withStyle(ChatFormatting.RED), true);
            }
            return net.minecraft.world.InteractionResult.FAIL;
        }
        if (com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchPad.rocketOn(level, pad.center()) != null) {
            if (player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.spacebreaker.rocket.pad_occupied"), true);
            }
            return net.minecraft.world.InteractionResult.FAIL;
        }
        com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity rocket =
                com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity.create(level, design, pad.rocketPos(), player == null ? 0F : player.getYRot());
        if (player != null) {
            rocket.setOwner(player.getUUID());
        }
        level.addFreshEntity(rocket);
        level.playSound(null, pad.center(), net.minecraft.sounds.SoundEvents.ANVIL_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 0.7F);
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        RocketDesign design = design(stack);
        if (design.isEmpty() || !(context.registries() instanceof RegistryAccess access)) {
            return;
        }
        RocketLayout layout = RocketLayout.of(design, RocketParts.lookup(access));
        double[] fuel = layout.fullFuel();
        ChatFormatting grey = ChatFormatting.GRAY;
        tooltip.add(Component.translatable("tooltip.spacebreaker.rocket.stages", layout.stages().size()).withStyle(grey));
        tooltip.add(Component.translatable("tooltip.spacebreaker.rocket.delta_v",
                Fmt.ms(RocketMath.totalDeltaV(layout, 0, fuel, RocketMath.ASCENT_ATMOSPHERE))).withStyle(grey));
        tooltip.add(Component.translatable("tooltip.spacebreaker.rocket.mass", Fmt.kg(RocketMath.mass(layout, 0, fuel))).withStyle(grey));
        tooltip.add(Component.translatable("tooltip.spacebreaker.rocket.height", String.format(java.util.Locale.ROOT, "%.1f", layout.heightBlocks(0))).withStyle(grey));
        tooltip.add(Component.translatable("tooltip.spacebreaker.rocket.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
