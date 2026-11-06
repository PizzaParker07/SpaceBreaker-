package com.pizzaparker07.spacebreaker.content.item;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A piece of the EVA pressure suit. Rendered with a dedicated 3D model (see client.model.EvaHelmetModel). */
public class SpaceSuitItem extends ArmorItem {
    public static final ResourceLocation HELMET_TEXTURE = Spacebreaker.id("textures/models/armor/eva_helmet.png");

    public SpaceSuitItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public static boolean isSunVisorDown(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SUN_VISOR.get(), false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getType() == Type.HELMET && player.isShiftKeyDown()) {
            stack.set(ModDataComponents.SUN_VISOR.get(), !isSunVisorDown(stack));
            level.playSound(player, player.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return getType() == Type.HELMET ? HELMET_TEXTURE : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (getType() == Type.HELMET) {
            tooltip.add(Component.translatable(isSunVisorDown(stack) ? "tooltip.spacebreaker.sun_visor.down" : "tooltip.spacebreaker.sun_visor.up")
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.spacebreaker.sun_visor.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
