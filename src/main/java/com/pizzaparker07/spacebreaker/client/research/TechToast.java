package com.pizzaparker07.spacebreaker.client.research;

import com.pizzaparker07.spacebreaker.content.research.Research;
import com.pizzaparker07.spacebreaker.content.research.ResearchClientData;
import com.pizzaparker07.spacebreaker.content.research.ResearchTree;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** "Technology learned!" toast, in the style of the vanilla advancement toast. */
public class TechToast implements Toast {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");
    private final ResourceLocation tech;
    private final ItemStack icon;

    public TechToast(ResourceLocation tech) {
        this.tech = tech;
        this.icon = ResearchScreen.iconFor(tech);
    }

    public static void show(List<ResourceLocation> learned) {
        for (ResourceLocation t : learned) {
            Minecraft.getInstance().getToasts().addToast(new TechToast(t));
        }
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toasts, long time) {
        g.blitSprite(BACKGROUND, 0, 0, width(), height());
        String category = ResearchClientData.categoryOf(tech, null);
        int color = category == null ? 0xFFFF00 : ResearchTree.categoryColor(category);
        var font = toasts.getMinecraft().font;
        g.drawString(font, Component.translatable("toast.spacebreaker.tech_learned"), 30, 7, 0xFF000000 | color, false);
        g.drawString(font, Component.translatable(TechNode.nameKey(tech)), 30, 18, 0xFFFFFFFF, false);
        g.renderFakeItem(icon.isEmpty() ? Research.disk(tech) : icon, 8, 8);
        return time >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }
}
