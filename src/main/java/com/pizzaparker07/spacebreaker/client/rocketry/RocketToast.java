package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.net.RocketToastPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A short flight toast (orbit reached), in the style of the advancement toast; the details live in the guide. */
public class RocketToast implements Toast {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");
    private final Component title, line;

    public RocketToast(Component title, Component line) {
        this.title = title;
        this.line = line;
    }

    public static void show(RocketToastPayload p) {
        Minecraft.getInstance().getToasts().addToast(new RocketToast(p.title(), p.line()));
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toasts, long time) {
        g.blitSprite(BACKGROUND, 0, 0, width(), height());
        var font = toasts.getMinecraft().font;
        g.drawString(font, title, 30, 7, 0xFF8FC8FF, false);
        g.drawString(font, font.substrByWidth(line, width() - 36).getString(), 30, 18, 0xFFFFFFFF, false);
        g.renderFakeItem(new ItemStack(RocketryModule.CREW_CAPSULE.get()), 8, 8);
        return time >= 4000L ? Visibility.HIDE : Visibility.SHOW;
    }
}
