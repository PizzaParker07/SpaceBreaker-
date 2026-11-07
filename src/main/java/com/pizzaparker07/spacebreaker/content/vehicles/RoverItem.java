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
    private final java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends RoverEntity>> type;

    public RoverItem(Properties properties) {
        this(properties, VehiclesModule.ROVER);
    }

    public RoverItem(Properties properties, java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends RoverEntity>> type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        RoverEntity rover = type.get().create(level);
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
        if (type == VehiclesModule.SPRINT_ROVER) {
            tooltip.add(Component.translatable("tooltip.spacebreaker.sprint_rover",
                    String.format(java.util.Locale.ROOT, "%.0f", RoverType.SPRINT.maxSpeed() * 20)).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.spacebreaker.rover.charge").withStyle(ChatFormatting.DARK_GRAY));
    }
}
