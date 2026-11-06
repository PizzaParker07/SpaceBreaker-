package com.pizzaparker07.spacebreaker.client.vehicles;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.vehicles.RoverEntity;
import com.pizzaparker07.spacebreaker.content.vehicles.VehiclesModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Locale;

/** Rover renderer, model layer and the driver's battery / speed readout above the hotbar. */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class VehiclesClient {
    private VehiclesClient() {
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RoverModel.LAYER, RoverModel::createLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VehiclesModule.ROVER.get(), RoverRenderer::new);
    }

    @SubscribeEvent
    static void gui(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Spacebreaker.id("rover_hud"), (g, delta) -> hud(g));
    }

    private static void hud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !(mc.player.getVehicle() instanceof RoverEntity rover)) {
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();
        int x = w / 2 + 96;
        int y = h - 22;
        float f = rover.energyFraction();
        int barW = 60;
        g.fill(x - 1, y - 1, x + barW + 1, y + 6, 0xC0101010);
        int color = f > 0.5F ? 0xFF58C048 : f > 0.2F ? 0xFFE0B030 : 0xFFD04030;
        g.fill(x, y, x + Math.round(barW * f), y + 5, color);
        double speed = Math.hypot(rover.getX() - rover.xo, rover.getZ() - rover.zo) * 20.0;
        Component line = rover.energy() <= 0 ? Component.translatable("hud.spacebreaker.rover.empty")
                : Component.translatable("hud.spacebreaker.rover.battery", Mth.floor(f * 100))
                .append("  ").append(Component.translatable("hud.spacebreaker.rover.speed", String.format(Locale.ROOT, "%.1f", speed)));
        g.drawString(mc.font, line, x, y - 11, 0xFFFFFFFF, true);
        int ix = x + barW + 4;
        if (rover.hasSolar()) {
            g.drawString(mc.font, Component.translatable("hud.spacebreaker.rover.solar"), ix, y - 2, 0xFF8FB8F0, true);
            ix += mc.font.width(Component.translatable("hud.spacebreaker.rover.solar")) + 4;
        }
        if (rover.hasRtg()) {
            g.drawString(mc.font, Component.translatable("hud.spacebreaker.rover.rtg"), ix, y - 2, rover.hasPellet() ? 0xFFF0C040 : 0xFF808080, true);
        }
    }
}
