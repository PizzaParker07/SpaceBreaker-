package com.pizzaparker07.spacebreaker.compat.client;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;

/** Client hooks of the renderer compatibility layer. */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class CompatClient {
    private CompatClient() {
    }

    @SubscribeEvent
    static void stage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            HazePass.captureFog();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            ShaderSkyPass.afterLevel(event.getModelViewMatrix(), event.getProjectionMatrix(), partialTick);
            HazePass.afterLevel(event.getModelViewMatrix(), event.getProjectionMatrix(), partialTick);
        }
    }

    @EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        static void shaders(RegisterShadersEvent event) throws java.io.IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), Spacebreaker.id("compat_haze"), DefaultVertexFormat.POSITION),
                    HazePass::setShader);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), Spacebreaker.id("compat_sky_composite"), DefaultVertexFormat.POSITION),
                    ShaderSkyPass::setCompositeShader);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), Spacebreaker.id("compat_sky_glass"), DefaultVertexFormat.POSITION),
                    ShaderSkyPass::setGlassShader);
        }
    }

    @SubscribeEvent
    static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ShaderSkyPass.release();
        HazePass.release();
        SpaceSky.reset();
    }
}
