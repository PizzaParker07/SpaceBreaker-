package com.pizzaparker07.spacebreaker.content.blueprint;

import com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition;
import com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Hologram Projector: projects how a multiblock is built.
 * <ul>
 *   <li>use in the air: choose a structure (list screen);</li>
 *   <li>use on a block: place the projection there (controller position), turned the way you look;</li>
 *   <li>use on the projection (a block of it, or a spot inside it): build what is missing from the inventory
 *   ({@link BlueprintBuilder});</li>
 *   <li>sneak + use on a block: remove the projection; sneak + use in the air: next layer / all layers;</li>
 *   <li>sneak + mouse wheel: turn the projection.</li>
 * </ul>
 */
public class HologramProjectorItem extends Item {
    /** Set by the client to open the selection screen (keeps client classes out of common code). */
    public static Runnable openSelection = () -> {
    };

    public HologramProjectorItem(Properties properties) {
        super(properties);
    }

    public static Hologram hologram(ItemStack stack) {
        return stack.get(BlueprintModule.HOLOGRAM.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Hologram h = hologram(stack);
        if (player.isShiftKeyDown() && h != null) {
            if (!level.isClientSide) {
                int height = Multiblocks.get(level.registryAccess(), h.blueprint()).map(MultiblockDefinition::height).orElse(1);
                Hologram next = h.nextLayer(height);
                stack.set(BlueprintModule.HOLOGRAM.get(), next);
                player.displayClientMessage(next.layer() < 0 ? Component.translatable("message.spacebreaker.hologram.all_layers")
                        : Component.translatable("message.spacebreaker.hologram.layer", next.layer() + 1, height), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (level.isClientSide) {
            openSelection.run();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        Hologram h = hologram(stack);
        Player player = ctx.getPlayer();
        Level level = ctx.getLevel();
        if (h == null) {
            if (level.isClientSide) {
                openSelection.run();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            if (player != null && player.isShiftKeyDown()) {
                stack.set(BlueprintModule.HOLOGRAM.get(), h.cleared());
                player.displayClientMessage(Component.translatable("message.spacebreaker.hologram.cleared"), true);
            } else if (player instanceof net.minecraft.server.level.ServerPlayer sp && onProjection(level, h, ctx.getClickedPos(), ctx.getClickedPos().relative(ctx.getClickedFace()))) {
                BlueprintBuilder.start(sp, h);
            } else {
                var anchor = ctx.getClickedPos().relative(ctx.getClickedFace());
                stack.set(BlueprintModule.HOLOGRAM.get(), h.withAnchor(anchor, ctx.getHorizontalDirection()));
                level.playSound(null, anchor, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.3F, 1.8F);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Whether either position belongs to the placed projection. */
    static boolean onProjection(Level level, Hologram h, BlockPos clicked, BlockPos against) {
        if (h.anchor().isEmpty()) {
            return false;
        }
        var def = Multiblocks.get(level.registryAccess(), h.blueprint());
        if (def.isEmpty()) {
            return false;
        }
        for (MultiblockDefinition.Placement p : def.get().placements(h.anchor().get(), h.facing())) {
            if (p.pos().equals(clicked) || p.pos().equals(against)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Hologram h = hologram(stack);
        if (h != null) {
            tooltip.add(Component.translatable("tooltip.spacebreaker.hologram.blueprint",
                    Component.translatable("multiblock." + h.blueprint().getNamespace() + "." + h.blueprint().getPath())).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.spacebreaker.hologram.hint1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.spacebreaker.hologram.hint2").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.spacebreaker.hologram.hint3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
