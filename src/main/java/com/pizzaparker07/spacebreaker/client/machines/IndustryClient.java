package com.pizzaparker07.spacebreaker.client.machines;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.fluids.SbFluid;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;

/** Client registration for the Industry area (fluids, machine renderers and screens). */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class IndustryClient {
    private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ResourceLocation OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");

    private IndustryClient() {
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MachinesModule.TANK_BE.get(), TankRenderer::new);
        event.registerBlockEntityRenderer(MachinesModule.SOLAR_ARRAY_BE.get(), SolarPanelRenderer::array);
        event.registerBlockEntityRenderer(MachinesModule.POWER_MONITOR_BE.get(), PowerMonitorRenderer::new);
        event.registerBlockEntityRenderer(MachinesModule.PRESS.type().get(), PressRenderer::new);
        event.registerBlockEntityRenderer(MachinesModule.REGOLITH_EXCAVATOR.type().get(), RegolithExcavatorRenderer::new);
        event.registerBlockEntityRenderer(MachinesModule.PUMPJACK.type().get(), PumpjackRenderer::new);
    }

    @SubscribeEvent
    static void models(ModelEvent.RegisterAdditional event) {
        event.register(PressRenderer.RAM);
        event.register(RegolithExcavatorRenderer.WHEEL);
        event.register(SolarPanelRenderer.ARRAY_HEAD_MODEL);
        event.register(PumpjackRenderer.BEAM);
        event.register(PumpjackRenderer.CRANK);
        event.register(PumpjackRenderer.ROD);
    }

    static {
        com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock.CLIENT_LOOP = MachineLoopSound::start;
    }

    @SubscribeEvent
    static void screens(RegisterMenuScreensEvent event) {
        event.register(MachinesModule.MACHINE_MENU.get(), MachineScreen::new);
    }

    @SubscribeEvent
    static void clientExtensions(RegisterClientExtensionsEvent event) {
        for (SbFluid fluid : FluidsModule.ALL) {
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return STILL;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return FLOW;
                }

                @Override
                public ResourceLocation getOverlayTexture() {
                    return OVERLAY;
                }

                @Override
                public int getTintColor() {
                    return fluid.tint;
                }
            }, fluid.type.get());
        }
    }
}
