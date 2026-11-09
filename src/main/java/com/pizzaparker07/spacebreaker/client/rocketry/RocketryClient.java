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
    /** Shows or hides the full flight panel (by default only destination, time and fuel are shown). */
    public static final KeyMapping FLIGHT_DETAILS = new KeyMapping("key.spacebreaker.flight_details", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, KEY_CATEGORY);

    private RocketryClient() {
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RocketryModule.ROCKET_ENTITY.get(), RocketRenderer::new);
        event.registerEntityRenderer(RocketryModule.SPENT_STAGE.get(), SpentStageRenderer::new);
        event.registerEntityRenderer(RocketryModule.SATELLITE_ENTITY.get(), SatelliteRenderer::new);
        event.registerBlockEntityRenderer(RocketryModule.CREW_ARM_BE.get(), CrewArmRenderer::new);
        event.registerBlockEntityRenderer(RocketryModule.WARNING_LIGHT_BE.get(), WarningLightRenderer::new);
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
        event.register(RocketryModule.STATION_CORE_MENU.get(), StationCoreScreen::new);
    }

    @SubscribeEvent
    static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        com.pizzaparker07.spacebreaker.content.rocketry.ship.ShipyardConsoleBlock.openScreen = pos -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null && mc.level.getBlockEntity(pos) instanceof com.pizzaparker07.spacebreaker.content.rocketry.ship.ShipyardConsoleBlockEntity be) {
                mc.setScreen(new ShipyardScreen(pos, be.design()));
            }
        };
        com.pizzaparker07.spacebreaker.content.rocketry.net.RocketToastPayload.onClient = RocketToast::show;
        // high up in a rocket a shader pack's own sky (and clouds) give way to ours, so the climb ends in the orbit sky
        // (only once our ball covers the ground beyond the view, or the sky is nearly black: before that the pack's own
        // sky and haze fill the far ground, while ours would leave it as dark space)
        com.pizzaparker07.spacebreaker.compat.client.ShaderSkyPass.spaceView = () -> {
            float k = SpaceView.factor();
            return k > 0.6F || SpaceView.ball(k) > 0.5F;
        };
        com.pizzaparker07.spacebreaker.content.rocketry.item.Fmt.localDots = () -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            return mc != null && mc.getLanguageManager() != null && mc.getLanguageManager().getSelected().startsWith("tr");
        };
    }

    @SubscribeEvent
    static void particles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(com.pizzaparker07.spacebreaker.content.rocketry.RocketryParticles.STEAM.get(), SteamParticle.Provider::new);
    }

    @SubscribeEvent
    static void keys(RegisterKeyMappingsEvent event) {
        event.register(LAUNCH);
        event.register(STAR_MAP);
        event.register(FLIGHT_DETAILS);
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
