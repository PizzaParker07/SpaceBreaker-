package com.pizzaparker07.spacebreaker.content.rocketry.item;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * Never stranded: away from Earth, this calls down an emergency return capsule (or turns your rocket's
 * capsule around) and brings you home under a parachute, whatever fuel you have left.
 */
public class EmergencyBeaconItem extends Item {
    public static final RocketDesign RETURN_CAPSULE = new RocketDesign(List.of(
            Spacebreaker.id("heat_shield"), Spacebreaker.id("crew_capsule"), Spacebreaker.id("parachute")));

    public EmergencyBeaconItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        Location here = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.at(level.dimension(), null).orElse(null);
        if (player.getVehicle() instanceof RocketEntity rocket) {
            if (rocket.state() == FlightState.REENTRY || rocket.state() == FlightState.PARACHUTE || Location.EARTH_SURFACE.equals(here)) {
                player.displayClientMessage(Component.translatable("message.spacebreaker.beacon.not_needed"), true);
                return InteractionResultHolder.fail(stack);
            }
            rocket.emergencyReturn();
        } else {
            if (here == null || Location.EARTH_SURFACE.equals(here)) {
                player.displayClientMessage(Component.translatable("message.spacebreaker.beacon.not_needed"), true);
                return InteractionResultHolder.fail(stack);
            }
            RocketEntity capsule = RocketEntity.create(level, RETURN_CAPSULE, player.position(), player.getYRot());
            level.addFreshEntity(capsule);
            player.startRiding(capsule, true);
            capsule.emergencyReturn();
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1F, 1.2F);
        player.displayClientMessage(Component.translatable("message.spacebreaker.beacon.activated").withStyle(ChatFormatting.GOLD), false);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (player instanceof ServerPlayer sp) {
            sp.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.emergency_beacon").withStyle(ChatFormatting.GRAY));
    }
}
