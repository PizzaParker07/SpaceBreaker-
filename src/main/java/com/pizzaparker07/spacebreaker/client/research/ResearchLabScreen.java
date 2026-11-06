package com.pizzaparker07.spacebreaker.client.research;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.research.lab.ResearchLabBlockEntity;
import com.pizzaparker07.spacebreaker.content.research.lab.ResearchLabMenu;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/** Vanilla-style container screen of the Research Lab. */
public class ResearchLabScreen extends AbstractContainerScreen<ResearchLabMenu> {
    private static final ResourceLocation BG = Spacebreaker.id("textures/gui/container/research_lab.png");
    private static final ResourceLocation ARROW = Spacebreaker.id("container/research_lab/progress");
    private static final ResourceLocation ENERGY = Spacebreaker.id("container/research_lab/energy");
    private static final ResourceLocation SCREEN_ON = Spacebreaker.id("container/research_lab/screen_on");
    private static final ResourceLocation ICON_TREE = Spacebreaker.id("container/research_lab/icon_tree");
    private static final ResourceLocation ICON_TEAM_ON = Spacebreaker.id("container/research_lab/icon_team_on");
    private static final ResourceLocation ICON_TEAM_OFF = Spacebreaker.id("container/research_lab/icon_team_off");
    /** Energy bar geometry (inside the background frame). */
    private static final int EN_X = 153, EN_Y = 18, EN_W = 14, EN_H = 50;
    private Button shareButton;

    public ResearchLabScreen(ResearchLabMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new IconButton(leftPos + 7, topPos + 17, ICON_TREE, b -> minecraft.setScreen(new ResearchScreen(this)),
                Component.translatable("gui.spacebreaker.lab.tree")));
        shareButton = addRenderableWidget(new IconButton(leftPos + 7, topPos + 37, ICON_TEAM_OFF, b ->
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ResearchLabMenu.BUTTON_TOGGLE_SHARE),
                Component.translatable("gui.spacebreaker.lab.share.off")));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean on = menu.sharing();
        ((IconButton) shareButton).icon = on ? ICON_TEAM_ON : ICON_TEAM_OFF;
        shareButton.setTooltip(Tooltip.create(Component.translatable(on ? "gui.spacebreaker.lab.share.on" : "gui.spacebreaker.lab.share.off")
                .append("\n").append(Component.translatable("gui.spacebreaker.lab.share.tip").withStyle(ChatFormatting.GRAY))));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int total = menu.total();
        if (total > 0) {
            int w = Mth.ceil(24F * menu.progress() / total);
            g.blitSprite(ARROW, 24, 16, 0, 0, leftPos + 80, topPos + 35, w, 16);
        }
        int stored = menu.energy();
        int h = Mth.ceil((float) EN_H * stored / ResearchLabBlockEntity.CAPACITY);
        if (h > 0) {
            g.blitSprite(ENERGY, EN_W, EN_H, 0, EN_H - h, leftPos + EN_X, topPos + EN_Y + EN_H - h, EN_W, h);
        }
        if (menu.status() == ResearchLabBlockEntity.WORKING) {
            g.blitSprite(SCREEN_ON, leftPos + 79, topPos + 18, 26, 12);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        Component status = statusLine();
        int w = font.width(status);
        int maxW = 146 - 30;
        if (w > maxW) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(status, maxW);
            int y = 56;
            for (var line : lines.subList(0, Math.min(2, lines.size()))) {
                g.drawString(font, line, 30 + (maxW - font.width(line)) / 2, y, 0x404040, false);
                y += 9;
            }
        } else {
            g.drawString(font, status, 30 + (maxW - w) / 2, 60, 0x404040, false);
        }
    }

    private Component statusLine() {
        ResearchLabBlockEntity lab = menu.lab();
        int status = menu.status();
        ResourceLocation tech = lab == null ? null : (lab.target() != null ? lab.target() : lab.lastResult());
        Component techName = tech == null ? Component.literal("?") : Component.translatable(TechNode.nameKey(tech));
        return switch (status) {
            case ResearchLabBlockEntity.WORKING, ResearchLabBlockEntity.NO_POWER -> lab != null && lab.target() != null
                    ? Component.translatable("gui.spacebreaker.lab.analysing", techName)
                    : Component.translatable("gui.spacebreaker.lab.status." + status);
            case ResearchLabBlockEntity.DONE, ResearchLabBlockEntity.ALREADY_KNOWN ->
                    Component.translatable("gui.spacebreaker.lab.status." + status, techName);
            default -> Component.translatable("gui.spacebreaker.lab.status." + status);
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= EN_X - 1 && rx < EN_X + EN_W + 1 && ry >= EN_Y - 1 && ry < EN_Y + EN_H + 1) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.spacebreaker.lab.energy", menu.energy(), ResearchLabBlockEntity.CAPACITY));
            if (menu.draw() > 0) {
                lines.add(Component.translatable("gui.spacebreaker.lab.draw", menu.draw()).withStyle(ChatFormatting.GRAY));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (rx >= 79 && rx < 105 && ry >= 34 && ry < 52 && menu.total() > 0) {
            int left = Math.max(0, menu.total() - menu.progress()) / 20;
            List<Component> lines = new ArrayList<>();
            lines.add(statusLine());
            lines.add(Component.translatable("gui.spacebreaker.lab.time", left).withStyle(ChatFormatting.GRAY));
            ResearchLabBlockEntity lab = menu.lab();
            if (lab != null && !lab.researcherName().isEmpty()) {
                lines.add(Component.translatable("gui.spacebreaker.lab.researcher", lab.researcherName()).withStyle(ChatFormatting.GRAY));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    /** 18×18 vanilla-style button with a 16×16 icon sprite. */
    static final class IconButton extends Button {
        ResourceLocation icon;

        IconButton(int x, int y, ResourceLocation icon, OnPress onPress, Component tooltip) {
            super(x, y, 18, 18, Component.empty(), onPress, DEFAULT_NARRATION);
            this.icon = icon;
            setTooltip(Tooltip.create(tooltip));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            g.blitSprite(icon, getX() + 1, getY() + 1, 16, 16);
        }
    }
}
