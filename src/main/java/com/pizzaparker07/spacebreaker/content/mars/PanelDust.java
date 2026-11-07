package com.pizzaparker07.spacebreaker.content.mars;

import com.pizzaparker07.spacebreaker.core.env.DustLoad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Cleaning dusty solar panels by hand: right-click one with a brush. */
public final class PanelDust {
    private PanelDust() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, PanelDust::onUse);
    }

    private static void onUse(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.BRUSH)) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (!(be instanceof DustLoad.Dusty dusty) || dusty.dust() <= 0.01F) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getLevel() instanceof ServerLevel level) {
            brush(level, event.getPos(), dusty);
            be.setChanged();
            stack.hurtAndBreak(1, event.getEntity(), event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            event.getEntity().displayClientMessage(Component.translatable("message.spacebreaker.panel_brushed"), true);
        }
    }

    /** Wipes one panel and shows the dust coming off. */
    public static void brush(ServerLevel level, BlockPos pos, DustLoad.Dusty dusty) {
        dusty.setDust(0F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, MarsModule.MARTIAN_REGOLITH.get().defaultBlockState()),
                pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 14, 0.4, 0.05, 0.4, 0.02);
        level.playSound(null, pos, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 0.8F, 1.1F);
    }
}
