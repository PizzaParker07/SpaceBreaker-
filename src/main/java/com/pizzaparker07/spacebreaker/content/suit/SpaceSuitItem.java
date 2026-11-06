package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A piece of the EVA pressure suit. The suit is only sealed (and only protects against vacuum, cold,
 * heat and radiation) when all four pieces are worn. Rendered with dedicated 3D models (client.suit).
 */
public class SpaceSuitItem extends ArmorItem {
    private final SuitTier tier;
    private final ResourceLocation texture;
    private final ResourceLocation accentTexture;

    public SpaceSuitItem(Holder<ArmorMaterial> material, Type type, SuitTier tier, String textureName, Properties properties) {
        super(material, type, properties);
        this.tier = tier;
        this.texture = Spacebreaker.id("textures/models/armor/" + textureName + ".png");
        this.accentTexture = Spacebreaker.id("textures/models/armor/" + textureName + "_accent.png");
    }

    public SuitTier tier() {
        return tier;
    }

    public static boolean isSunVisorDown(ItemStack stack) {
        return stack.getOrDefault(SuitModule.SUN_VISOR.get(), false);
    }

    /** True when the entity wears a complete EVA suit (helmet, torso, legs and boots). */
    public static boolean isSealed(LivingEntity entity) {
        return SuitProtection.rating(entity) != null;
    }

    public static boolean wearsHelmet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof SpaceSuitItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getType() == Type.HELMET && player.isShiftKeyDown()) {
            stack.set(SuitModule.SUN_VISOR.get(), !isSunVisorDown(stack));
            level.playSound(player, player.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    /** Click a tank or Maneuvering Unit onto the torso to install it; right-click with an empty cursor to take one out. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (!SuitTanks.canHold(stack) || action != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return false;
        }
        if (other.getItem() instanceof OxygenTankItem) {
            if (SuitTanks.insert(stack, other)) {
                other.shrink(1);
                player.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.8F, 1.2F);
            }
            return true;
        }
        if (other.getItem() instanceof ManeuveringUnitItem && !SuitTanks.hasManeuveringUnit(stack)) {
            SuitTanks.setManeuveringUnit(stack, other);
            other.shrink(1);
            player.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.8F, 0.9F);
            return true;
        }
        if (other.isEmpty()) {
            ItemStack out = SuitTanks.removeLast(stack);
            if (out.isEmpty()) {
                out = SuitTanks.maneuveringUnit(stack);
                SuitTanks.setManeuveringUnit(stack, ItemStack.EMPTY);
            }
            if (!out.isEmpty()) {
                access.set(out);
                player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 0.8F, 1.0F);
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return layer.dyeable() ? accentTexture : texture;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (getType() == Type.HELMET) {
            tooltip.add(Component.translatable(isSunVisorDown(stack) ? "tooltip.spacebreaker.sun_visor.down" : "tooltip.spacebreaker.sun_visor.up")
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.spacebreaker.sun_visor.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (getType() == Type.CHESTPLATE) {
            SuitTanks.appendTooltip(stack, tooltip);
        }
        tooltip.add(Component.translatable("tooltip.spacebreaker.suit.range", tier.minTempC(), tier.maxTempC()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.spacebreaker.suit.shielding", tier.shieldingPercent()).withStyle(ChatFormatting.GRAY));
        SuitUpgrade upgrade = SuitUpgrade.of(stack);
        if (upgrade != null) {
            tooltip.add(Component.translatable("tooltip.spacebreaker.suit.upgrade", Component.translatable(upgrade.descriptionKey()))
                    .withStyle(ChatFormatting.AQUA));
        }
        if (tier.tech() != null) {
            tooltip.add(Component.translatable("tooltip.spacebreaker.suit.needs_tech",
                    Component.translatable(com.pizzaparker07.spacebreaker.core.tech.TechNode.nameKey(tier.tech()))).withStyle(ChatFormatting.DARK_PURPLE));
        }
        tooltip.add(Component.translatable("tooltip.spacebreaker.suit.set").withStyle(ChatFormatting.DARK_GRAY));
    }
}
