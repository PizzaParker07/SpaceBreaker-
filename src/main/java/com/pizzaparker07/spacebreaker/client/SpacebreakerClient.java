package com.pizzaparker07.spacebreaker.client;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.model.EvaHelmetModel;
import com.pizzaparker07.spacebreaker.client.render.HelmetVisorLayer;
import com.pizzaparker07.spacebreaker.client.render.SolarPanelRenderer;
import com.pizzaparker07.spacebreaker.registry.ModBlockEntities;
import com.pizzaparker07.spacebreaker.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
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
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class SpacebreakerClient {
    private static EvaHelmetModel helmetModel;

    private SpacebreakerClient() {
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SOLAR_PANEL.get(), SolarPanelRenderer::new);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(EvaHelmetModel.LAYER, EvaHelmetModel::createLayer);
        event.registerLayerDefinition(EvaHelmetModel.VISOR_LAYER, EvaHelmetModel::createVisorLayer);
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void addLayers(EntityRenderersEvent.AddLayers event) {
        helmetModel = null; // re-bake after resource reloads
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new HelmetVisorLayer<>(renderer, event.getEntityModels()));
            }
        }
        if (event.getRenderer(EntityType.ARMOR_STAND) instanceof LivingEntityRenderer stand) {
            stand.addLayer(new HelmetVisorLayer(stand, event.getEntityModels()));
        }
        for (EntityType<?> type : new EntityType<?>[]{EntityType.ZOMBIE, EntityType.HUSK, EntityType.SKELETON, EntityType.STRAY, EntityType.DROWNED, EntityType.PIGLIN}) {
            if (event.getRenderer(type) instanceof LivingEntityRenderer living && living.getModel() instanceof HumanoidModel) {
                living.addLayer(new HelmetVisorLayer(living, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SolarPanelRenderer.HEAD_MODEL);
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (helmetModel == null) {
                    helmetModel = new EvaHelmetModel(Minecraft.getInstance().getEntityModels().bakeLayer(EvaHelmetModel.LAYER));
                }
                return helmetModel;
            }
        }, ModItems.EVA_HELMET.get());
    }
}
