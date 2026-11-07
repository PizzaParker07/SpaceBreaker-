package com.pizzaparker07.spacebreaker.content.rocketry.station;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Station construction kit: in empty space (nothing to build against, as in orbit) it unfolds a 5 × 5 hull
 * floor a few blocks in front of the player, the first piece of a station to build on.
 */
public class StationKitItem extends Item {
    public static final int SIZE = 5;

    public StationKitItem(Properties properties) {
        super(properties);
    }

    /** Places the floor centred {@code ahead} blocks in front of the eyes; false if anything is in the way. */
    public static boolean unfold(Level level, BlockPos centre) {
        int h = SIZE / 2;
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-h, 0, -h), centre.offset(h, 0, h))) {
            if (!level.getBlockState(p).canBeReplaced()) {
                return false;
            }
        }
        BlockState hull = RocketryModule.STATION_HULL.get().defaultBlockState();
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-h, 0, -h), centre.offset(h, 0, h))) {
            level.setBlock(p, hull, 3);
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Direction facing = player.getDirection();
        BlockPos centre = BlockPos.containing(player.getEyePosition()).relative(facing, 4).below(2);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!unfold(level, centre)) {
            player.displayClientMessage(Component.translatable("message.spacebreaker.station_kit.blocked"), true);
            return InteractionResultHolder.fail(stack);
        }
        level.playSound(null, centre, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1F, 0.8F);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.station_kit").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
