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

    /** Headlights as moving lights through the shared light helper (dynamic lights mod, or soft pools). */
    @SubscribeEvent
    static void clientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        com.pizzaparker07.spacebreaker.client.light.LightBeams.register(RoverLights::collect);
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

    private static final net.minecraft.resources.ResourceLocation ICONS = Spacebreaker.id("textures/gui/rover_hud_icons.png");
    /** The suit HUD's icon sheet: its oxygen icon marks the cabin air, so both panels read the same. */
    private static final net.minecraft.resources.ResourceLocation SUIT_ICONS = Spacebreaker.id("textures/gui/hud_icons.png");
    private static final int BAR_W = 41, ROW_H = 11, PAD = 3;

    /**
     * Rover panel in the suit HUD's style (icon, bar, short value on a translucent backing), beside the hotbar on the
     * main-hand side (centre + 91 + 6, mirrored for left-handed play) while the suit panel keeps the off-hand side. Battery bar, speed only while moving, the
     * power sources as small icons (the solar icon turns rust when the panel is dusty, the RTG greys out without a
     * pellet), cabin air as a second bar on pressurised rovers.
     */
    private static void hud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !(mc.player.getVehicle() instanceof RoverEntity rover)) {
            return;
        }
        float f = rover.energyFraction();
        double speed = Math.hypot(rover.getX() - rover.xo, rover.getZ() - rover.zo) * 20.0;
        String speedText = speed >= 0.3 ? String.format(Locale.ROOT, "%.1f m/s", speed) : "";
        boolean blink = (mc.player.tickCount / 5) % 2 == 0;
        int battColor = rover.energy() <= 0 ? (blink ? 0xFFD04030 : 0xFF601810) : f > 0.5F ? 0xFF58C048 : f > 0.2F ? 0xFFE0B030 : 0xFFD04030;
        int sources = (rover.hasSolar() ? 1 : 0) + (rover.hasRtg() ? 1 : 0);
        boolean cabin = rover.roverType().cabin();
        // the percentage beside the bar: always in the detailed HUD, otherwise once the battery runs low
        int pct = RoverEntity.shownPercent(f, com.pizzaparker07.spacebreaker.client.hud.HudConfig.detailed());
        String pctText = pct < 0 ? "" : net.minecraft.network.chat.Component.translatable("hud.spacebreaker.rover.percent", pct).getString();
        int pctColor = rover.energy() <= 0 ? battColor : f < RoverEntity.LOW_BATTERY ? 0xFFE06048 : 0xFFE8E8E8;
        int width = 11 + BAR_W + (pctText.isEmpty() ? 0 : 3 + mc.font.width(pctText))
                + (speedText.isEmpty() ? 0 : 3 + mc.font.width(speedText)) + (sources > 0 ? 3 + sources * 10 - 1 : 0);
        int rows = cabin ? 2 : 1;
        int height = rows * ROW_H - 2;
        int pw = width + PAD * 2, ph = height + PAD * 2;
        boolean rightHanded = mc.options.mainHand().get() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        // agreed with the suit HUD: it keeps the off-hand side past the offhand slot, the rover takes the main-hand side
        // vanilla's attack indicator in "hotbar" mode sits right beside the hotbar on the main-hand side: step past it
        int gap = mc.options.attackIndicator().get() == net.minecraft.client.AttackIndicatorStatus.HOTBAR ? 6 + 18 + 2 : 6;
        int ax = rightHanded ? g.guiWidth() / 2 + 91 + gap : g.guiWidth() / 2 - 91 - gap - pw;
        int ay = g.guiHeight() - 2 - ph;
        if (ax < 2 || ax + pw > g.guiWidth() - 2) {
            ax = g.guiWidth() - pw - 4;
            ay = 4;
        }
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.fill(ax, ay, ax + pw, ay + ph, 0x73000000);
        int x = ax + PAD, y = ay + PAD;
        if (cabin) {
            // cabin air on top, like the suit's oxygen row
            float o2 = rover.cabinOxygen() / (float) RoverEntity.CABIN_O2_CAPACITY;
            int c = !rover.cabinPressurized() ? (blink ? 0xFFD04030 : 0xFF601810) : o2 > 0.2F ? 0xFF8FD0F0 : 0xFFE0B030;
            g.blit(SUIT_ICONS, x, y, 0, 0, 9, 9, 54, 9);
            bar(g, x + 11, y + 2, rover.cabinPressurized() ? o2 : 0F, c);
            y += ROW_H;
        }
        if (rover.energy() <= 0) {
            // an empty bar has nothing to colour: the battery icon itself blinks, red empty cell and dimmed
            if (!blink) {
                g.setColor(0.5F, 0.5F, 0.5F, 1F);
            }
            g.blit(ICONS, x, y, 27, 0, 9, 9, 36, 9);
            g.setColor(1F, 1F, 1F, 1F);
        } else {
            g.blit(ICONS, x, y, 0, 0, 9, 9, 36, 9);
        }
        bar(g, x + 11, y + 2, f, battColor);
        int ix = x + 11 + BAR_W + 3;
        if (!pctText.isEmpty()) {
            g.drawString(mc.font, pctText, ix, y + 1, pctColor, false);
            ix += mc.font.width(pctText) + 3;
        }
        if (!speedText.isEmpty()) {
            g.drawString(mc.font, speedText, ix, y + 1, 0xFFE8E8E8, false);
            ix += mc.font.width(speedText) + 3;
        }
        if (rover.hasSolar()) {
            if (rover.dust() >= 0.05F) {
                g.setColor(0.85F, 0.55F, 0.35F, 1F);
            }
            g.blit(ICONS, ix, y, 9, 0, 9, 9, 36, 9);
            g.setColor(1F, 1F, 1F, 1F);
            ix += 10;
        }
        if (rover.hasRtg()) {
            if (!rover.hasPellet()) {
                g.setColor(0.45F, 0.45F, 0.45F, 1F);
            }
            g.blit(ICONS, ix, y, 18, 0, 9, 9, 36, 9);
            g.setColor(1F, 1F, 1F, 1F);
        }
    }

    private static void bar(GuiGraphics g, int x, int y, float fill, int color) {
        g.fill(x, y, x + BAR_W, y + 5, 0xBF101014);
        g.fill(x + 1, y + 1, x + BAR_W - 1, y + 4, 0xBF2A2C33);
        int w = Math.round((BAR_W - 2) * Mth.clamp(fill, 0F, 1F));
        if (w > 0) {
            g.fill(x + 1, y + 1, x + 1 + w, y + 4, color);
        }
    }
}
