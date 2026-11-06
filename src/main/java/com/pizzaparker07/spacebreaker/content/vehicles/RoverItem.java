package com.pizzaparker07.spacebreaker.content.vehicles;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

/** Places a rover on the clicked surface, facing where the player looks. */
public class RoverItem extends Item {
    public RoverItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        RoverEntity rover = VehiclesModule.ROVER.get().create(level);
        if (rover == null) {
            return InteractionResult.FAIL;
        }
        rover.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ctx.getRotation(), 0);
        if (!level.noCollision(rover, rover.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        level.addFreshEntity(rover);
        level.gameEvent(ctx.getPlayer(), GameEvent.ENTITY_PLACE, pos);
        ctx.getItemInHand().consume(1, ctx.getPlayer());
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.rover").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.spacebreaker.rover.charge").withStyle(ChatFormatting.DARK_GRAY));
    }
}
