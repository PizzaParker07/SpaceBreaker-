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

/** Client registration for the Industry area (fluids, machine renderers and screens). */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class IndustryClient {
    private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ResourceLocation OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");

    private IndustryClient() {
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
