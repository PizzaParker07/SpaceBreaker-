package com.pizzaparker07.spacebreaker.client.research;

import com.mojang.blaze3d.platform.InputConstants;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.guide.GuideData;
import com.pizzaparker07.spacebreaker.client.guide.GuideScreen;
import com.pizzaparker07.spacebreaker.content.research.GuideBookItem;
import com.pizzaparker07.spacebreaker.content.research.ResearchModule;
import com.pizzaparker07.spacebreaker.content.research.SampleItem;
import com.pizzaparker07.spacebreaker.content.research.TechDiskItem;
import com.pizzaparker07.spacebreaker.content.research.net.ResearchNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** Client wiring of the research area: screens, keys, item tints, toasts and the guide opener. */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ResearchClient {
    public static final KeyMapping OPEN_RESEARCH = new KeyMapping("key.spacebreaker.research", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.spacebreaker");
    public static final KeyMapping OPEN_GUIDE = new KeyMapping("key.spacebreaker.guide", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.spacebreaker");

    private ResearchClient() {
    }

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        GuideBookItem.opener = () -> Minecraft.getInstance().setScreen(new GuideScreen());
        ResearchNet.onLearnedClient = learned -> Minecraft.getInstance().execute(() -> TechToast.show(learned));
        NeoForge.EVENT_BUS.addListener(ResearchClient::tick);
    }

    @SubscribeEvent
    static void reloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(GuideData.INSTANCE);
    }

    @SubscribeEvent
    static void screens(RegisterMenuScreensEvent event) {
        event.register(ResearchModule.RESEARCH_LAB_MENU.get(), ResearchLabScreen::new);
    }

    @SubscribeEvent
    static void keys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_RESEARCH);
        event.register(OPEN_GUIDE);
    }

    @SubscribeEvent
    static void colors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> layer == 1 ? TechDiskItem.labelColor(stack) : -1, ResearchModule.TECH_DISK.get());
        event.register((stack, layer) -> layer == 1 ? SampleItem.tint(stack) : -1,
                ResearchModule.FIELD_SAMPLE.get(), ResearchModule.EXPERIMENT_DATA.get());
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_RESEARCH.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                mc.setScreen(new ResearchScreen(null));
            }
        }
        while (OPEN_GUIDE.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                mc.setScreen(new GuideScreen());
            }
        }
    }
}
