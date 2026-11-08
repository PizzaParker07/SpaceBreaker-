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

import java.util.ArrayList;
import java.util.List;

/**
 * "Technology learned!" toast, in the style of the vanilla advancement toast. Technologies learned together share one
 * toast ("3 technologies learned!") that steps through their names, instead of a column of toasts down the screen.
 */
public class TechToast implements Toast {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");
    /** How long each name shows when several share the toast, ms. */
    private static final long STEP = 1500L;
    private final List<ResourceLocation> techs = new ArrayList<>();
    private long shownFor = 5000L;
    private boolean restart;

    public TechToast(List<ResourceLocation> techs) {
        this.techs.addAll(techs);
        this.shownFor = Math.max(5000L, techs.size() * STEP + 1000L);
    }

    /** Shows the newly learned technologies: added to a toast already on screen, or as one new toast. */
    public static void show(List<ResourceLocation> learned) {
        if (learned.isEmpty()) {
            return;
        }
        ToastComponent toasts = Minecraft.getInstance().getToasts();
        TechToast open = toasts.getToast(TechToast.class, NO_TOKEN);
        if (open != null) {
            open.add(learned);
        } else {
            toasts.addToast(new TechToast(learned));
        }
    }

    private void add(List<ResourceLocation> more) {
        for (ResourceLocation t : more) {
            if (!techs.contains(t)) {
                techs.add(t);
            }
        }
        shownFor = Math.max(5000L, techs.size() * STEP + 1000L);
        restart = true;
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toasts, long time) {
        if (restart) {
            restart = false;
            shownFor += time;
        }
        g.blitSprite(BACKGROUND, 0, 0, width(), height());
        ResourceLocation tech = techs.size() == 1 ? techs.get(0) : techs.get((int) ((time / STEP) % techs.size()));
        String category = ResearchClientData.categoryOf(tech, null);
        int color = category == null ? 0xFFFF00 : ResearchTree.categoryColor(category);
        var font = toasts.getMinecraft().font;
        Component head = techs.size() == 1 ? Component.translatable("toast.spacebreaker.tech_learned")
                : Component.translatable("toast.spacebreaker.tech_learned_many", techs.size());
        g.drawString(font, head, 30, 7, 0xFF000000 | color, false);
        g.drawString(font, Component.translatable(TechNode.nameKey(tech)), 30, 18, 0xFFFFFFFF, false);
        ItemStack icon = ResearchScreen.iconFor(tech);
        g.renderFakeItem(icon.isEmpty() ? Research.disk(tech) : icon, 8, 8);
        return time >= shownFor ? Visibility.HIDE : Visibility.SHOW;
    }
}
