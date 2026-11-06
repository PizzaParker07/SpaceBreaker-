package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.platform.InputConstants;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Client registrations of the rocketry module (mod bus). */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class RocketryClient {
    public static final String KEY_CATEGORY = "key.categories.spacebreaker.spaceflight";
    public static final KeyMapping LAUNCH = new KeyMapping("key.spacebreaker.launch", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KEY_CATEGORY);
    public static final KeyMapping STAR_MAP = new KeyMapping("key.spacebreaker.star_map", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, KEY_CATEGORY);

    private RocketryClient() {
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RocketryModule.ROCKET_ENTITY.get(), RocketRenderer::new);
        event.registerEntityRenderer(RocketryModule.SPENT_STAGE.get(), SpentStageRenderer::new);
        event.registerBlockEntityRenderer(RocketryModule.CREW_ARM_BE.get(), CrewArmRenderer::new);
    }

    @SubscribeEvent
    static void models(ModelEvent.RegisterAdditional event) {
        event.register(RocketRenderer.CANOPY);
        event.register(CrewArmRenderer.WALKWAY);
    }

    @SubscribeEvent
    static void screens(RegisterMenuScreensEvent event) {
        event.register(RocketryModule.ASSEMBLER_MENU.get(), RocketAssemblerScreen::new);
        event.register(RocketryModule.LAUNCH_CONSOLE_MENU.get(), LaunchConsoleScreen::new);
    }

    @SubscribeEvent
    static void keys(RegisterKeyMappingsEvent event) {
        event.register(LAUNCH);
        event.register(STAR_MAP);
    }

    @SubscribeEvent
    static void gui(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CROSSHAIR, Spacebreaker.id("rocket_overlay"), RocketHud::renderOverlay);
        event.registerAbove(VanillaGuiLayers.HOTBAR, Spacebreaker.id("rocket_hud"), RocketHud::renderHud);
    }

    @SubscribeEvent
    static void skies(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Spacebreaker.id("orbit"), new OrbitSkyEffects());
    }
}
