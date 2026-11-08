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
    public static final int PREVIEW_X = 182, PREVIEW_Y = 18, PREVIEW_W = 110, PREVIEW_H = 150;
    public static final int STATS_X = 184, STATS_Y = 174, STATS_W = 106, STATS_H = 38;
    private static final int ROW_ICON_X = 8, ROW_ICON_W = 9;

    private Button assemble;
    private AssemblerSlots.Build build;
    private RocketLayout layout;

    public RocketAssemblerScreen(RocketAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300;
        imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        // parts -> assemble arrow -> output, on one line like a machine's progress arrow
        assemble = addRenderableWidget(Button.builder(Component.literal("\u25B6"),
                        b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RocketAssemblerMenu.BUTTON_ASSEMBLE))
                .bounds(leftPos + RocketAssemblerMenu.OUTPUT_X - 24, topPos + RocketAssemblerMenu.OUTPUT_Y, 20, 18)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.spacebreaker.assembler.assemble")))
                .build());
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

    /** Grey out parts in the inventory that the player has not researched yet. */
    private void greyLockedParts(GuiGraphics g) {
        for (var slot : menu.slots) {
            if (slot.index < AssemblerSlots.COUNT || !slot.hasItem()) {
                continue;
            }
            var stack = slot.getItem();
            if (com.pizzaparker07.spacebreaker.content.rocketry.part.RocketParts.forStack(minecraft.level.registryAccess(), stack).isPresent()
                    && !com.pizzaparker07.spacebreaker.content.rocketry.part.RocketTech.unlocked(minecraft.player, stack)) {
                g.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 300, 0xA08B8B8B);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        drawVerdict(g);
    }

    /** One line: the problem, or how far the rocket gets; the numbers are in the tooltip. */
    private void drawVerdict(GuiGraphics g) {
        if (build == null) {
            return;
        }
        if (layout == null || layout.stages().isEmpty()) {
            g.drawWordWrap(font, Component.translatable("gui.spacebreaker.assembler.empty"), STATS_X, STATS_Y, STATS_W, 0x8A939C);
            return;
        }
        if (!build.problems().isEmpty()) {
            g.drawWordWrap(font, Component.literal("\u2716 ").append(Component.translatable("message.spacebreaker.assembler." + build.problems().get(0))),
                    STATS_X, STATS_Y, STATS_W, 0xFF7A6A);
            return;
        }
        int reached = farthest();
        Component line = Component.translatable("gui.spacebreaker.assembler.reach." + (reached < 0 ? "none" : MILESTONES[reached]));
        g.drawWordWrap(font, line, STATS_X, STATS_Y, STATS_W, reached < 0 ? 0xFF7A6A : 0x8FD88A);
    }

    private static final String[] MILESTONES = {"orbit", "moon_orbit", "landing", "lunar_ascent", "home"};

    /** Index of the last milestone the design reaches, -1 for none. */
    private int farthest() {
        List<MissionPlanner.Milestone> ms = MissionPlanner.milestones(layout, SbConfig.realism());
        int last = -1;
        for (int i = 0; i < ms.size() && i < MILESTONES.length; i++) {
            if (!ms.get(i).reached()) {
                break;
            }
            last = i;
        }
        return last;
    }

    /** Tooltip of the verdict: delta-v, thrust/weight, mass and the mission ladder. */
    private List<Component> statLines() {
        List<Component> out = new ArrayList<>();
        double[] fuel = layout.fullFuel();
        double g0 = Environment.gravity(minecraft.level);
        double dv = RocketMath.totalDeltaV(layout, 0, fuel, RocketMath.ASCENT_ATMOSPHERE);
        double twr = RocketMath.twr(layout, 0, fuel, g0);
        out.add(Component.translatable("gui.spacebreaker.assembler.dv", Fmt.ms(dv)));
        out.add(Component.translatable("gui.spacebreaker.assembler.twr", Fmt.twr(twr))
                .withStyle(twr >= 1.05 ? net.minecraft.ChatFormatting.GRAY : net.minecraft.ChatFormatting.RED));
        out.add(Component.translatable("gui.spacebreaker.assembler.mass", Fmt.kg(RocketMath.mass(layout, 0, fuel))).withStyle(net.minecraft.ChatFormatting.GRAY));
        List<MissionPlanner.Milestone> ms = MissionPlanner.milestones(layout, SbConfig.realism());
        for (int i = 0; i < ms.size() && i < MILESTONES.length; i++) {
            boolean ok = ms.get(i).reached();
            out.add(Component.literal(ok ? "\u2714 " : "\u2716 ").append(Component.translatable("gui.spacebreaker.milestone." + MILESTONES[i]))
                    .withStyle(ok ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.DARK_GRAY));
        }
        return out;
    }

    /** Tooltip of a row icon. */
    private List<Component> rowLines(int row) {
        if (row == 0) {
            return List.of(Component.translatable("gui.spacebreaker.assembler.row.payload"));
        }
        return List.of(Component.translatable("gui.spacebreaker.assembler.row.stage", row),
                Component.translatable("gui.spacebreaker.assembler.row." + (row == 1 ? "first" : "later")).withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderPreview(g, partialTick);
        greyLockedParts(g);
        renderTooltip(g, mouseX, mouseY);
        if (layout != null && isHovering(PREVIEW_X, PREVIEW_Y, PREVIEW_W, PREVIEW_H, mouseX, mouseY)) {
            g.renderComponentTooltip(font, stageLines(), mouseX, mouseY);
        } else if (layout != null && !layout.stages().isEmpty() && build.problems().isEmpty()
                && isHovering(STATS_X - 2, STATS_Y - 2, STATS_W + 4, STATS_H, mouseX, mouseY)) {
            g.renderComponentTooltip(font, statLines(), mouseX, mouseY);
        } else {
            if (isHovering(ROW_ICON_X, RocketAssemblerMenu.PAYLOAD_Y, ROW_ICON_W, 16, mouseX, mouseY)) {
                g.renderComponentTooltip(font, rowLines(0), mouseX, mouseY);
            }
            for (int s = 0; s < AssemblerSlots.STAGES; s++) {
                int y = RocketAssemblerMenu.STAGE_Y0 + (AssemblerSlots.STAGES - 1 - s) * RocketAssemblerMenu.ROW_GAP;
                if (isHovering(ROW_ICON_X, y, ROW_ICON_W, 16, mouseX, mouseY)) {
                    g.renderComponentTooltip(font, rowLines(s + 1), mouseX, mouseY);
                }
            }
        }
    }

    private List<Component> stageLines() {
        List<Component> out = new ArrayList<>();
        double[] fuel = layout.fullFuel();
        double g0 = Environment.gravity(minecraft.level);
        double heightBlocks = Math.max(1, RocketGeometry.ofActive(layout, 0).height() / 16.0);
        out.add(Component.translatable("gui.spacebreaker.assembler.height", Fmt.unit(heightBlocks, 1, "m")));
        for (int i = 0; i < layout.stages().size(); i++) {
            RocketLayout.Stage s = layout.stages().get(i);
            double dv = RocketMath.stageDeltaV(layout, i, fuel, i == 0 ? RocketMath.ASCENT_ATMOSPHERE : 0);
            double twr = RocketMath.twr(layout, i, fuel, g0);
            out.add(Component.translatable("gui.spacebreaker.assembler.stage_line", i + 1, s.propellant().displayName(), Fmt.ms(dv), Fmt.twr(twr),
                    Fmt.kg(s.propellantKg()), Fmt.unit(RocketMath.burnTime(s, s.propellantKg(), 0), 0, "s")));
        }
        out.add(Component.translatable("gui.spacebreaker.assembler.payload", Fmt.kg(layout.payload().massKg()), layout.payload().seats()));
        String nuclear = com.pizzaparker07.spacebreaker.content.rocketry.entity.NuclearExhaust.designWarning(layout);
        if (nuclear != null) {
            out.add(Component.translatable("gui.spacebreaker.assembler." + nuclear)
                    .withStyle(nuclear.equals("nuclear_first") ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.GOLD));
        }
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
        String nuclear = com.pizzaparker07.spacebreaker.content.rocketry.entity.NuclearExhaust.designWarning(layout);
        if (nuclear != null) {
            // a short tag on the preview; hovering it shows the full warning
            Component tag = Component.translatable("gui.spacebreaker.assembler.nuclear_tag");
            g.drawString(font, tag, leftPos + PREVIEW_X + PREVIEW_W - 3 - font.width(tag), topPos + PREVIEW_Y + 3,
                    nuclear.equals("nuclear_first") ? 0xFF7A6A : 0xE8B84A, false);
        }
        float pulse = Mth.sin(time * 0.1F) * 0.5F + 0.5F;
        if (!build.valid()) {
            g.drawString(font, "!", leftPos + PREVIEW_X + PREVIEW_W - 8, topPos + PREVIEW_Y + 3, pulse > 0.5F ? 0xFF7A6A : 0xA04A40, false);
        }
    }
}
