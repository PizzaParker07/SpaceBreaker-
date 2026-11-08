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

/**
 * Container screen of the Research Lab, laid out like the industry machines: energy on the far left, input, arrow and
 * output on one centre line, the buttons in the title row. Only the title is written on the screen; the numbers and
 * the status sit in tooltips, and a problem shows as a small red mark beside the arrow.
 */
public class ResearchLabScreen extends AbstractContainerScreen<ResearchLabMenu> {
    private static final ResourceLocation BG = Spacebreaker.id("textures/gui/container/research_lab.png");
    private static final ResourceLocation ARROW = Spacebreaker.id("container/research_lab/progress");
    private static final ResourceLocation ENERGY = Spacebreaker.id("container/research_lab/energy");
    private static final ResourceLocation SCREEN_ON = Spacebreaker.id("container/research_lab/screen_on");
    private static final ResourceLocation ICON_TREE = Spacebreaker.id("container/research_lab/icon_tree");
    private static final ResourceLocation ICON_TEAM_ON = Spacebreaker.id("container/research_lab/icon_team_on");
    private static final ResourceLocation ICON_TEAM_OFF = Spacebreaker.id("container/research_lab/icon_team_off");
    /** Energy bar geometry (inside the background frame). */
    private static final int EN_X = 8, EN_Y = 18, EN_W = 14, EN_H = 50;
    /** Progress arrow and the small status display beside the output. */
    private static final int ARROW_X = 73, ARROW_Y = 35, SCREEN_X = 132, SCREEN_Y = 37;
    /** The problem mark: just above the arrow's tip. */
    private static final int MARK_X = 92, MARK_Y = 24;
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
        addRenderableWidget(new IconButton(leftPos + 131, topPos + 3, ICON_TREE, b -> minecraft.setScreen(new ResearchScreen(this)),
                Component.translatable("gui.spacebreaker.lab.tree")));
        shareButton = addRenderableWidget(new IconButton(leftPos + 151, topPos + 3, ICON_TEAM_OFF, b ->
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
            g.blitSprite(ARROW, 24, 16, 0, 0, leftPos + ARROW_X, topPos + ARROW_Y, w, 16);
        }
        int stored = menu.energy();
        int h = Mth.ceil((float) EN_H * stored / ResearchLabBlockEntity.CAPACITY);
        if (h > 0) {
            g.blitSprite(ENERGY, EN_W, EN_H, 0, EN_H - h, leftPos + EN_X, topPos + EN_Y + EN_H - h, EN_W, h);
        }
        if (menu.status() == ResearchLabBlockEntity.WORKING) {
            g.blitSprite(SCREEN_ON, leftPos + SCREEN_X, topPos + SCREEN_Y, 26, 12);
        }
    }

    /** Statuses that need the player: shown as a small red mark with the reason in its tooltip. */
    private boolean problem() {
        return switch (menu.status()) {
            case ResearchLabBlockEntity.NO_POWER, ResearchLabBlockEntity.NOTHING_NEW, ResearchLabBlockEntity.OUTPUT_FULL,
                 ResearchLabBlockEntity.UNKNOWN_TECH, ResearchLabBlockEntity.NO_RESEARCHER, ResearchLabBlockEntity.MISSING_PREREQS -> true;
            default -> false;
        };
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // only the title: no "Inventory" caption and no status sentence
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        if (problem()) {
            g.fill(MARK_X, MARK_Y, MARK_X + 7, MARK_Y + 9, 0xFFB02020);
            g.drawString(font, "!", MARK_X + 2, MARK_Y + 1, 0xFFFFFFFF, false);
        } else if (menu.status() == ResearchLabBlockEntity.DONE) {
            g.drawString(font, "\u2714", MARK_X, MARK_Y + 1, 0xFF2E8B3A, false);
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
        } else if (rx >= MARK_X - 1 && rx < MARK_X + 8 && ry >= MARK_Y - 1 && ry < MARK_Y + 10 && (problem() || menu.status() == ResearchLabBlockEntity.DONE)) {
            g.renderTooltip(font, statusLine(), mouseX, mouseY);
        } else if (rx >= ARROW_X - 1 && rx < ARROW_X + 25 && ry >= ARROW_Y - 1 && ry < ARROW_Y + 17) {
            int left = Math.max(0, menu.total() - menu.progress()) / 20;
            List<Component> lines = new ArrayList<>();
            lines.add(statusLine());
            if (menu.total() > 0) {
                lines.add(Component.translatable("gui.spacebreaker.lab.time", left).withStyle(ChatFormatting.GRAY));
            }
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
