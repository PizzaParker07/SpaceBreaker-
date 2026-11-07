package com.pizzaparker07.spacebreaker.content.rocketry.station;

import com.pizzaparker07.spacebreaker.content.rocketry.item.RocketPartItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * A station module: a rocket payload (flown up and docked, it waits in the station core's console) and, in hand, the
 * kit that builds it. Used on a mouth's hatch it is attached there (the outline shows where it will go, green if
 * there is room); a core used in open space in orbit unfolds a new station in front of the player.
 */
public class StationModuleItem extends RocketPartItem {
    public StationModuleItem(Properties properties) {
        super(properties);
    }

    public Optional<StationModule> module() {
        return StationModule.byPart(BuiltInRegistries.ITEM.getKey(this));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos mouth = StationHatchBlock.mouthCenter(level, context.getClickedPos());
        Optional<StationModule> m = module();
        if (mouth == null || m.isEmpty()) {
            return super.useOn(context);
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        String owner = player == null ? "" : player.getGameProfile().getName();
        if (!m.get().attach((ServerLevel) level, mouth, owner)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.spacebreaker.station_module.blocked").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        level.playSound(null, mouth, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1F, 0.6F);
        level.playSound(null, mouth, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1F, 0.7F);
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (module().orElse(null) != StationModule.CORE) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        boolean orbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.at(level.dimension(), null)
                .map(com.pizzaparker07.spacebreaker.content.rocketry.nav.Location::orbit).orElse(
                        com.pizzaparker07.spacebreaker.content.rocketry.nav.Location.Orbits.SHARED.equals(level.dimension()));
        if (!orbit) {
            player.displayClientMessage(Component.translatable("message.spacebreaker.station_module.orbit_only"), true);
            return InteractionResultHolder.fail(stack);
        }
        BlockPos centre = BlockPos.containing(player.getEyePosition()).relative(player.getDirection(), 7).below(3);
        if (!StationModule.buildCore((ServerLevel) level, centre, player.getDirection(), player.getGameProfile().getName())) {
            player.displayClientMessage(Component.translatable("message.spacebreaker.station_module.blocked").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        level.playSound(null, centre, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1F, 0.7F);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.station_module." + module().map(StationModule::name).orElse("CORE")
                .toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.spacebreaker.station_module.use").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
