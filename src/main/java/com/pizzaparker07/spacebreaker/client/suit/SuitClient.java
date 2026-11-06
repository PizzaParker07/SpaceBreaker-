package com.pizzaparker07.spacebreaker.client.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.SuitTanks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitTier;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.HashMap;
import java.util.Map;

/** Client registration for the EVA suit: armour models, overlay layer. */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class SuitClient {
    private static final Map<String, EvaSuitModel> MODELS = new HashMap<>();

    private SuitClient() {
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SuitModule.AIRLOCK_DOOR_BE.get(), AirlockDoorRenderer::new);
        event.registerBlockEntityRenderer(SuitModule.OXYGEN_FILLER_BE.get(), OxygenFillerRenderer::new);
    }

    @SubscribeEvent
    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(AirlockDoorRenderer.LEFT_LOWER);
        event.register(AirlockDoorRenderer.RIGHT_LOWER);
        event.register(AirlockDoorRenderer.LEFT_UPPER);
        event.register(AirlockDoorRenderer.RIGHT_UPPER);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(EvaSuitModel.HELMET, EvaSuitModel::createHelmet);
        event.registerLayerDefinition(EvaSuitModel.TORSO, EvaSuitModel::createTorso);
        event.registerLayerDefinition(EvaSuitModel.LEGS, EvaSuitModel::createLegs);
        event.registerLayerDefinition(EvaSuitModel.BOOTS, EvaSuitModel::createBoots);
        event.registerLayerDefinition(EvaSuitModel.OVERLAY, EvaSuitModel::createOverlay);
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void addLayers(EntityRenderersEvent.AddLayers event) {
        MODELS.clear(); // re-bake after resource reloads
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new SuitOverlayLayer<>(renderer, event.getEntityModels()));
            }
        }
        for (EntityType<?> type : new EntityType<?>[]{EntityType.ARMOR_STAND, EntityType.ZOMBIE, EntityType.HUSK, EntityType.SKELETON,
                EntityType.STRAY, EntityType.DROWNED, EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.WITHER_SKELETON}) {
            if (event.getRenderer(type) instanceof LivingEntityRenderer living && living.getModel() instanceof HumanoidModel) {
                living.addLayer(new SuitOverlayLayer(living, event.getEntityModels()));
            }
        }
    }

    private static EvaSuitModel model(ModelLayerLocation layer, String tier) {
        return MODELS.computeIfAbsent(layer + "/" + tier, k -> new EvaSuitModel(Minecraft.getInstance().getEntityModels().bakeLayer(layer), tier));
    }

    /** Default colour of the dyeable accent parts (bearing rings, patches) per tier. */
    public static int defaultAccent(SuitTier tier) {
        if (tier == SuitTier.THERMAL) {
            return 0xE0B04A;  // gold anodised
        }
        if (tier == SuitTier.HEATED) {
            return 0x3A3F46;  // dark steel
        }
        if (tier == SuitTier.RADIATION) {
            return 0xE6B820;  // hazard yellow
        }
        return 0x68A0D8;      // blue anodised
    }

    public static int accentColor(ItemStack stack) {
        SuitTier tier = stack.getItem() instanceof SpaceSuitItem suit ? suit.tier() : SuitTier.MK1;
        return FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, defaultAccent(tier)));
    }

    private static IClientItemExtensions armour(ModelLayerLocation layer) {
        return new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                String tier = stack.getItem() instanceof SpaceSuitItem suit ? suit.tier().id() : "eva";
                EvaSuitModel model = model(layer, tier);
                model.showManeuveringUnit(SuitTanks.hasManeuveringUnit(stack));
                if (slot == EquipmentSlot.CHEST) {
                    model.animatePack(entity, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
                }
                return model;
            }

            @Override
            public int getDefaultDyeColor(ItemStack stack) {
                return accentColor(stack);
            }
        };
    }

    @SubscribeEvent
    static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 1 ? accentColor(stack) : -1,
                SuitModule.ALL_PIECES.stream().map(DeferredItem::get).toArray(net.minecraft.world.level.ItemLike[]::new));
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        for (DeferredItem<SpaceSuitItem> piece : SuitModule.ALL_PIECES) {
            SpaceSuitItem item = piece.get();
            ModelLayerLocation layer = switch (item.getType()) {
                case HELMET -> EvaSuitModel.HELMET;
                case CHESTPLATE -> EvaSuitModel.TORSO;
                case LEGGINGS -> EvaSuitModel.LEGS;
                default -> EvaSuitModel.BOOTS;
            };
            event.registerItem(armour(layer), item);
        }
    }
}
