package com.pizzaparker07.spacebreaker.compat.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.Compat;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.opengl.GL11;

/**
 * Block info overlays above our HUD layers. Vanilla stacks GUI layers 200 units apart in depth and draws them with the
 * depth test; Jade draws in {@link RenderGuiEvent.Post} and WTHIT at the end of {@code Gui.render} (right after that
 * event), both near depth 0, so the semi-transparent pixels of our visor frame, helmet panels and vehicle HUDs, drawn
 * in later layers, hid their info box. With either installed the GUI depth is cleared before they draw, so they always
 * end up on top. Nothing changes without them.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class InfoOverlayDepth {
    private static Boolean wanted;

    private InfoOverlayDepth() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void beforeOverlays(RenderGuiEvent.Post event) {
        if (wanted == null) {
            wanted = Compat.isInstalled("jade") || Compat.isInstalled("wthit");
        }
        if (wanted) {
            event.getGuiGraphics().flush();
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        }
    }
}
