package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.assembler.AssemblerSlots;
import com.pizzaparker07.spacebreaker.content.rocketry.assembler.RocketAssemblerMenu;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketGeometry;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketLayout;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketMath;
import com.pizzaparker07.spacebreaker.content.rocketry.item.Fmt;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketParts;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RocketAssemblerScreen extends AbstractContainerScreen<RocketAssemblerMenu> {
    private static final ResourceLocation BG = Spacebreaker.id("textures/gui/rocket_assembler.png");
    public static final int PREVIEW_X = 182, PREVIEW_Y = 18, PREVIEW_W = 110, PREVIEW_H = 104;
    public static final int STATS_X = 184, STATS_Y = 128;

    private Button assemble;
    private AssemblerSlots.Build build;
    private RocketLayout layout;

    public RocketAssemblerScreen(RocketAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300;
        imageHeight = 222;
        inventoryLabelY = RocketAssemblerMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        assemble = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.assembler.assemble"),
                        b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RocketAssemblerMenu.BUTTON_ASSEMBLE))
                .bounds(leftPos + 82, topPos + RocketAssemblerMenu.OUTPUT_Y, 60, 18).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        build = AssemblerSlots.build(menu::partIn, minecraft.level.registryAccess());
        layout = build.design().isEmpty() ? null : RocketLayout.of(build.design(), RocketParts.lookup(minecraft.level.registryAccess()));
        assemble.active = build.valid() && menu.partIn(AssemblerSlots.OUTPUT).isEmpty();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight, 512, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        int lx = 10;
        g.drawString(font, "P", lx, RocketAssemblerMenu.PAYLOAD_Y + 5, 0x606060, false);
        for (int s = 0; s < AssemblerSlots.STAGES; s++) {
            int y = RocketAssemblerMenu.STAGE_Y0 + (AssemblerSlots.STAGES - 1 - s) * RocketAssemblerMenu.ROW_GAP;
            g.drawString(font, String.valueOf(s + 1), lx, y + 5, 0x606060, false);
        }
        drawStats(g);
    }

    private void drawStats(GuiGraphics g) {
        int x = STATS_X, y = STATS_Y;
        if (build == null) {
            return;
        }
        if (layout == null || layout.stages().isEmpty()) {
            g.drawWordWrap(font, Component.translatable("gui.spacebreaker.assembler.empty"), x, y, 106, 0x8A939C);
            return;
        }
        double[] fuel = layout.fullFuel();
        double g0 = Environment.gravity(minecraft.level);
        double dv = RocketMath.totalDeltaV(layout, 0, fuel, RocketMath.ASCENT_ATMOSPHERE);
        double twr = RocketMath.twr(layout, 0, fuel, g0);
        g.drawString(font, Component.translatable("gui.spacebreaker.assembler.dv", Fmt.ms(dv)), x, y, 0xE0E6EC, false);
        g.drawString(font, Component.translatable("gui.spacebreaker.assembler.twr", Fmt.twr(twr)), x, y + 10, twr >= 1.05 ? 0xE0E6EC : 0xFF7A6A, false);
        g.drawString(font, Component.translatable("gui.spacebreaker.assembler.mass", Fmt.kg(RocketMath.mass(layout, 0, fuel))), x, y + 20, 0xE0E6EC, false);
        int ly = y + 33;
        if (!build.problems().isEmpty()) {
            g.drawWordWrap(font, Component.translatable("message.spacebreaker.assembler." + build.problems().get(0)), x, ly, 106, 0xFF7A6A);
            return;
        }
        String[] keys = {"orbit", "moon_orbit", "landing", "lunar_ascent", "home"};
        List<MissionPlanner.Milestone> ms = MissionPlanner.milestones(layout, SbConfig.realism());
        for (int i = 0; i < ms.size() && i < keys.length; i++) {
            MissionPlanner.Milestone m = ms.get(i);
            String mark = m.reached() ? "\u2714 " : "\u2716 ";
            int col = m.reached() ? 0x8FD88A : 0x8A939C;
            g.drawString(font, mark + Component.translatable("gui.spacebreaker.milestone." + keys[i]).getString(), x, ly, col, false);
            ly += 10;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderPreview(g, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (layout != null && isHovering(PREVIEW_X, PREVIEW_Y, PREVIEW_W, PREVIEW_H, mouseX, mouseY)) {
            g.renderComponentTooltip(font, stageLines(), mouseX, mouseY);
        }
    }

    private List<Component> stageLines() {
        List<Component> out = new ArrayList<>();
        double[] fuel = layout.fullFuel();
        double g0 = Environment.gravity(minecraft.level);
        for (int i = 0; i < layout.stages().size(); i++) {
            RocketLayout.Stage s = layout.stages().get(i);
            double dv = RocketMath.stageDeltaV(layout, i, fuel, i == 0 ? RocketMath.ASCENT_ATMOSPHERE : 0);
            double twr = RocketMath.twr(layout, i, fuel, g0);
            out.add(Component.translatable("gui.spacebreaker.assembler.stage_line", i + 1, Fmt.ms(dv), Fmt.twr(twr),
                    Fmt.kg(s.propellantKg()), String.format(Locale.ROOT, "%.0f", RocketMath.burnTime(s, s.propellantKg(), 0))));
        }
        out.add(Component.translatable("gui.spacebreaker.assembler.payload", Fmt.kg(layout.payload().massKg()), layout.payload().seats()));
        return out;
    }

    private void renderPreview(GuiGraphics g, float partialTick) {
        if (layout == null) {
            return;
        }
        RocketGeometry geo = RocketGeometry.ofActive(layout, 0);
        double heightBlocks = Math.max(1, geo.height() / 16.0);
        float scale = (float) Math.min(28, (PREVIEW_H - 14) / heightBlocks);
        float time = minecraft.level.getGameTime() + partialTick;
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(leftPos + PREVIEW_X + PREVIEW_W / 2F, topPos + PREVIEW_Y + PREVIEW_H - 6, 120);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(-12));
        pose.mulPose(Axis.YP.rotationDegrees(time * 1.2F));
        g.enableScissor(leftPos + PREVIEW_X, topPos + PREVIEW_Y, leftPos + PREVIEW_X + PREVIEW_W, topPos + PREVIEW_Y + PREVIEW_H);
        Lighting.setupForEntityInInventory();
        RocketStackRenderer.render(layout, geo, pose, g.bufferSource(), 0xF000F0, 0F);
        g.flush();
        Lighting.setupFor3DItems();
        g.disableScissor();
        pose.popPose();
        String h = String.format(Locale.ROOT, "%.1f m", heightBlocks);
        g.drawString(font, h, leftPos + PREVIEW_X + 3, topPos + PREVIEW_Y + 3, 0x8FB0D0, false);
        float pulse = Mth.sin(time * 0.1F) * 0.5F + 0.5F;
        if (!build.valid()) {
            g.drawString(font, "!", leftPos + PREVIEW_X + PREVIEW_W - 8, topPos + PREVIEW_Y + 3, pulse > 0.5F ? 0xFF7A6A : 0xA04A40, false);
        }
    }
}
