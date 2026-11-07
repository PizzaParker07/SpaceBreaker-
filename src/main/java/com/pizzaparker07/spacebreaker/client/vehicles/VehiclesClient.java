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
        event.registerLayerDefinition(RoverModel.SPRINT_LAYER, RoverModel::createSprintLayer);
        event.registerLayerDefinition(RoverModel.PRESSURIZED_LAYER, RoverModel::createPressurizedLayer);
        event.registerLayerDefinition(RoverModel.AMPHIBIOUS_LAYER, RoverModel::createAmphibiousLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VehiclesModule.ROVER.get(), RoverRenderer::new);
        event.registerEntityRenderer(VehiclesModule.SPRINT_ROVER.get(), RoverRenderer::sprint);
        event.registerEntityRenderer(VehiclesModule.PRESSURIZED_ROVER.get(), RoverRenderer::pressurized);
        event.registerEntityRenderer(VehiclesModule.AMPHIBIOUS_ROVER.get(), RoverRenderer::amphibious);
    }

    private static final java.util.Map<Integer, RoverSound> SOUNDS = new java.util.HashMap<>();

    /** Starts a motor sound for every running rover nearby (each keeps itself up to date and stops on its own). */
    @SubscribeEvent
    static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            SOUNDS.clear();
            return;
        }
        SOUNDS.values().removeIf(RoverSound::isStopped);
        for (RoverEntity rover : mc.level.getEntitiesOfClass(RoverEntity.class, mc.player.getBoundingBox().inflate(32))) {
            double speed = Math.hypot(rover.getX() - rover.xo, rover.getZ() - rover.zo);
            if (!SOUNDS.containsKey(rover.getId()) && RoverSound.wanted(rover, speed)) {
                RoverSound s = new RoverSound(rover);
                SOUNDS.put(rover.getId(), s);
                mc.getSoundManager().play(s);
            }
        }
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
            // a dusty roof panel reads in rust instead of blue, with how much of it is covered
            int dust = Math.round(rover.dust() * 100);
            Component solar = dust >= 5 ? Component.translatable("hud.spacebreaker.rover.solar_dusty", dust)
                    : Component.translatable("hud.spacebreaker.rover.solar");
            g.drawString(mc.font, solar, ix, y - 2, dust >= 5 ? 0xFFC8854A : 0xFF8FB8F0, true);
            ix += mc.font.width(solar) + 4;
        }
        if (rover.hasRtg()) {
            g.drawString(mc.font, Component.translatable("hud.spacebreaker.rover.rtg"), ix, y - 2, rover.hasPellet() ? 0xFFF0C040 : 0xFF808080, true);
        }
        if (rover.roverType().cabin()) {
            // cabin air above the battery line: blue while pressurised, red when the tank is empty
            int o2 = Math.round(rover.cabinOxygen() * 100F / RoverEntity.CABIN_O2_CAPACITY);
            Component cabin = rover.cabinPressurized() ? Component.translatable("hud.spacebreaker.rover.cabin", o2)
                    : Component.translatable("hud.spacebreaker.rover.cabin_empty");
            g.drawString(mc.font, cabin, x, y - 22, rover.cabinPressurized() ? (o2 > 20 ? 0xFF8FD0F0 : 0xFFE0B030) : 0xFFD04030, true);
        }
    }
}
