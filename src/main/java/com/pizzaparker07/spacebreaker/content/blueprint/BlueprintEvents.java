package com.pizzaparker07.spacebreaker.content.blueprint;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition;
import com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;

/**
 * Sneak + use a multiblock's controller with an empty hand: if the structure is incomplete, a projector in
 * the inventory starts projecting exactly what is missing (aligned to the controller); if it is complete,
 * the player is told so.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID)
public final class BlueprintEvents {
    private BlueprintEvents() {
    }

    @SubscribeEvent
    static void onUse(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        var defs = Multiblocks.controlledBy(event.getLevel().registryAccess(), state);
        if (defs.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide) {
            return;
        }
        // the definition this controller matches best
        Map.Entry<ResourceLocation, MultiblockDefinition> best = null;
        Multiblocks.Validation bestV = null;
        for (var e : defs) {
            Multiblocks.Validation v = Multiblocks.validateAnyFacing(event.getLevel(), e.getValue(), event.getPos());
            if (bestV == null || v.problems().size() < bestV.problems().size()) {
                best = e;
                bestV = v;
            }
        }
        Component name = BlueprintModule.name(best.getKey());
        if (bestV.complete()) {
            player.displayClientMessage(Component.translatable("message.spacebreaker.hologram.complete", name), true);
            return;
        }
        ItemStack projector = ItemStack.EMPTY;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(BlueprintModule.HOLOGRAM_PROJECTOR.get())) {
                projector = s;
                break;
            }
        }
        if (projector.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.spacebreaker.hologram.need_projector"), true);
            return;
        }
        projector.set(BlueprintModule.HOLOGRAM.get(), Hologram.of(best.getKey()).withAnchor(event.getPos(), bestV.facing()));
        player.displayClientMessage(Component.translatable("message.spacebreaker.hologram.opened", name), true);
    }
}
