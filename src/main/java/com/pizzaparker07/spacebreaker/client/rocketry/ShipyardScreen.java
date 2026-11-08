package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.content.rocketry.ship.ShipDesign;
import com.pizzaparker07.spacebreaker.content.rocketry.ship.ShipDesignPayload;
import com.pizzaparker07.spacebreaker.content.rocketry.ship.ShipPart;
import com.pizzaparker07.spacebreaker.content.rocketry.station.Berths;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;

/**
 * The shipyard console: put a spacecraft together section by section, nose to tail, and see at once what it weighs,
 * how much Δv it has and whether it fits the berth (13 × 13 × 29). The craft is drawn from the side and from above
 * inside the berth's outline. "Save" keeps the design on the console for the spacecraft to be built from.
 */
public class ShipyardScreen extends Screen {
    private static final int INK = 0xFFDDE4EC, DIM = 0xFF8A96A4, RED = 0xFFEF6A5A, GREEN = 0xFF8FD88A, PANEL = 0xE00B1018;
    private final BlockPos console;
    private ShipDesign design;
    private int panelX;

    public ShipyardScreen(BlockPos console, ShipDesign design) {
        super(Component.translatable("ship.spacebreaker.shipyard"));
        this.console = console;
        this.design = design;
    }

    @Override
    protected void init() {
        int y = 30;
        for (ShipPart p : ShipPart.values()) {
            if (p.kind == ShipPart.Kind.RADIAL) {
                continue;
            }
            addRenderableWidget(Button.builder(Component.literal("+ ").append(Component.translatable(p.translationKey())),
                    b -> design = design.with(p)).bounds(8, y, 104, 16).build());
            y += 18;
        }
        y += 4;
        addRenderableWidget(Button.builder(Component.literal("−"), b -> design = design.withRcs(design.rcs() - 1)).bounds(8, y, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> design = design.withRcs(design.rcs() + 1)).bounds(96, y, 16, 16).build());
        y += 18;
        addRenderableWidget(Button.builder(Component.literal("−"), b -> design = design.withWings(design.solarWings() - 1)).bounds(8, y, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> design = design.withWings(design.solarWings() + 1)).bounds(96, y, 16, 16).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.translatable("ship.spacebreaker.remove_last"), b -> design = design.withoutLast()).bounds(8, y, 104, 16).build());
        y += 18;
        addRenderableWidget(Button.builder(Component.translatable("ship.spacebreaker.clear"), b -> design = ShipDesign.EMPTY).bounds(8, y, 104, 16).build());
        panelX = width - 150;
        addRenderableWidget(Button.builder(Component.translatable("ship.spacebreaker.save"), b -> {
            PacketDistributor.sendToServer(new ShipDesignPayload(console, design));
            onClose();
        }).bounds(panelX + 8, height - 28, 134, 20).build());
    }

    private int rcsRowY() {
        int parts = 0;
        for (ShipPart p : ShipPart.values()) {
            parts += p.kind == ShipPart.Kind.RADIAL ? 0 : 1;
        }
        return 30 + parts * 18 + 4;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF070B12);
        // blueprint grid
        for (int x = 0; x < width; x += 12) {
            g.fill(x, 0, x + 1, height, 0x14A0C8FF);
        }
        for (int y = 0; y < height; y += 12) {
            g.fill(0, y, width, y + 1, 0x14A0C8FF);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        g.drawString(font, title.copy().withStyle(ChatFormatting.BOLD), 8, 8, INK, false);
        int ry = rcsRowY();
        g.drawCenteredString(font, Component.translatable("ship.spacebreaker.rcs", design.rcs()), 60, ry + 4, INK);
        g.drawCenteredString(font, Component.translatable("ship.spacebreaker.wings", design.solarWings()), 60, ry + 22, INK);
        drawCraft(g);
        drawPanel(g);
        for (var r : renderables) {
            r.render(g, mouseX, mouseY, partialTick);
        }
        // the numbers live in tooltips: the delta-v figure, and the berth's size on its frame
        if (previewStats) {
            g.renderComponentTooltip(font, statLines(), dvBox[0] - 160, dvBox[1] + 30);
        } else if (in(dvBox, mouseX, mouseY)) {
            g.renderComponentTooltip(font, statLines(), mouseX, mouseY);
        } else if (in(sideBox, mouseX, mouseY) || in(topBox, mouseX, mouseY)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("ship.spacebreaker.berth", Berths.MAX_WIDTH, Berths.MAX_HEIGHT, Berths.MAX_LENGTH)), mouseX, mouseY);
        }
    }

    private int[] dvBox = new int[4], sideBox = new int[4], topBox = new int[4];

    private static boolean in(int[] r, int mx, int my) {
        return mx >= r[0] && my >= r[1] && mx < r[0] + r[2] && my < r[1] + r[3];
    }

    private List<Component> statLines() {
        List<Component> out = new java.util.ArrayList<>();
        out.add(stat("size", design.length() + " × " + design.width() + " × " + design.height()));
        out.add(stat("mass", String.format(Locale.ROOT, "%,.1f t → %,.1f t", design.wetMassKg() / 1000, design.dryMassKg() / 1000)));
        out.add(stat("propellant", String.format(Locale.ROOT, "%,.1f t", design.propellantKg() / 1000)));
        if (design.stack().stream().anyMatch(p -> p.kind == ShipPart.Kind.ENGINE)) {
            out.add(Component.literal("  ").append(Component.translatable(design.hydrogen() ? "ship.spacebreaker.fuel_hydrogen" : "ship.spacebreaker.fuel_metalox"))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        out.add(stat("thrust", String.format(Locale.ROOT, "%,.0f kN", design.thrustKn())));
        out.add(stat("isp", String.format(Locale.ROOT, "%,.0f s", design.isp())));
        out.add(stat("accel", String.format(Locale.ROOT, "%.1f m/s²", design.acceleration())));
        // the bill the build step will charge
        var bill = com.pizzaparker07.spacebreaker.content.rocketry.ship.ShipCost.of(design);
        if (!bill.isEmpty()) {
            out.add(Component.empty());
            out.add(Component.translatable("ship.spacebreaker.bill", com.pizzaparker07.spacebreaker.content.rocketry.ship.ShipCost.kfe(design))
                    .withStyle(ChatFormatting.GRAY));
            for (var line : bill) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(line.item());
                out.add(Component.literal("  " + line.count() + "× ").append(item.getDescription()).withStyle(ChatFormatting.WHITE));
            }
        }
        return out;
    }

    private static Component stat(String key, String value) {
        return Component.translatable("ship.spacebreaker.stat_" + key).withStyle(ChatFormatting.GRAY)
                .append(Component.literal("  " + value).withStyle(ChatFormatting.WHITE));
    }

    /** Side and top views of the craft, nose on the left, inside the berth's outline. */
    private void drawCraft(GuiGraphics g) {
        int left = 124, right = panelX - 12;
        int scale = Math.max(2, Math.min((right - left) / Berths.MAX_LENGTH, (height - 76) / (Berths.MAX_HEIGHT + Berths.MAX_WIDTH)));
        int x0 = left + (right - left - Berths.MAX_LENGTH * scale) / 2;
        int sideY = 40, topY = sideY + Berths.MAX_HEIGHT * scale + 24;
        g.drawString(font, Component.translatable("ship.spacebreaker.view_side"), x0, sideY - 10, DIM, false);
        g.drawString(font, Component.translatable("ship.spacebreaker.view_top"), x0, topY - 10, DIM, false);
        frame(g, x0, sideY, Berths.MAX_LENGTH * scale, Berths.MAX_HEIGHT * scale);
        frame(g, x0, topY, Berths.MAX_LENGTH * scale, Berths.MAX_WIDTH * scale);
        sideBox = new int[]{x0, sideY, Berths.MAX_LENGTH * scale, Berths.MAX_HEIGHT * scale};
        topBox = new int[]{x0, topY, Berths.MAX_LENGTH * scale, Berths.MAX_WIDTH * scale};
        int midSide = sideY + Berths.MAX_HEIGHT * scale / 2, midTop = topY + Berths.MAX_WIDTH * scale / 2;
        int x = x0;
        for (ShipPart p : design.stack()) {
            int w = p.length * scale;
            int hs = p.height * scale, ws = p.width * scale;
            int c = colour(p);
            section(g, x, midSide - hs / 2, w, hs, c);
            section(g, x, midTop - ws / 2, w, ws, c);
            if (p == ShipPart.COCKPIT) {
                // the docking port on the nose
                g.fill(x - scale, midSide - scale, x, midSide + scale, 0xFFE8C443);
                g.fill(x - scale, midTop - scale, x, midTop + scale, 0xFFE8C443);
            }
            x += w;
        }
        if (design.solarWings() > 0 && !design.stack().isEmpty()) {
            int hullW = design.stack().stream().mapToInt(p -> p.width).max().orElse(5) * scale;
            int cx = x0 + Math.min(design.length(), Berths.MAX_LENGTH) * scale / 2;
            for (int i = 0; i < design.solarWings(); i++) {
                int wx = cx - (design.solarWings() * 5 * scale) / 2 + i * 5 * scale;
                g.fill(wx, midTop - hullW / 2 - 3 * scale, wx + 4 * scale, midTop - hullW / 2, 0xFF2D4A90);
                g.fill(wx, midTop + hullW / 2, wx + 4 * scale, midTop + hullW / 2 + 3 * scale, 0xFF2D4A90);
            }
        }
        // RCS clusters: small marks along the hull
        for (int i = 0; i < design.rcs() && !design.stack().isEmpty(); i++) {
            int rx = x0 + (int) ((i + 0.5) * design.length() * scale / (double) design.rcs());
            g.fill(rx - 1, midSide - design.height() * scale / 2 - 2, rx + 1, midSide - design.height() * scale / 2, 0xFFDDE4EC);
        }
    }

    private static int colour(ShipPart p) {
        return switch (p.kind) {
            case COCKPIT -> 0xFFE1E3E6;
            case HULL -> 0xFFB4B8BD;
            case TANK -> 0xFFC8A060;
            case ENGINE -> p == ShipPart.ENGINE_NUCLEAR ? 0xFF8F7AD8 : 0xFF707880;
            case RADIAL -> 0xFF2D4A90;
        };
    }

    private static void section(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + h, c);
        g.fill(x, y, x + w, y + 1, 0x60FFFFFF);
        g.fill(x, y + h - 1, x + w, y + h, 0x60000000);
        g.fill(x + w - 1, y, x + w, y + h, 0x80101418);
    }

    private static void frame(GuiGraphics g, int x, int y, int w, int h) {
        int c = 0x6090B8E0;
        for (int i = 0; i < w; i += 4) {
            g.fill(x + i, y, x + i + 2, y + 1, c);
            g.fill(x + i, y + h - 1, x + i + 2, y + h, c);
        }
        for (int i = 0; i < h; i += 4) {
            g.fill(x, y + i, x + 1, y + i + 2, c);
            g.fill(x + w - 1, y + i, x + w, y + i + 2, c);
        }
    }

    private void drawPanel(GuiGraphics g) {
        g.fill(panelX, 0, width, height, PANEL);
        int x = panelX + 8, y = 10, w = 134;
        g.drawString(font, Component.translatable("ship.spacebreaker.stat_dv"), x, y, DIM, false);
        y += 10;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(1.5F, 1.5F, 1F);
        g.drawString(font, String.format(Locale.ROOT, "%,.0f m/s", design.deltaV()), 0, 0, design.problems().isEmpty() ? 0xFFFFFFFF : RED, false);
        g.pose().popPose();
        String dv = String.format(Locale.ROOT, "%,.0f m/s", design.deltaV());
        dvBox = new int[]{x, y - 10, Math.max(w, (int) (font.width(dv) * 1.5F)), 24};
        y += 20;
        // only what is wrong; with nothing wrong, one "ready" line
        boolean port = !design.stack().isEmpty() && design.stack().get(0) == ShipPart.COCKPIT;
        boolean bad = false;
        if (!port && !design.problems().contains("ship.spacebreaker.problem.cockpit_front")) {
            g.drawString(font, Component.translatable("ship.spacebreaker.port_no"), x, y, RED, false);
            y += 12;
            bad = true;
        }
        for (String problem : design.problems()) {
            List<FormattedCharSequence> lines = font.split(Component.literal("\u2716 ").append(Component.translatable(problem)), w);
            for (FormattedCharSequence line : lines) {
                g.drawString(font, line, x, y, RED, false);
                y += 9;
            }
            y += 3;
            bad = true;
        }
        if (!bad) {
            g.drawString(font, Component.translatable("ship.spacebreaker.ready"), x, y, GREEN, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean previewStats;

    /** For previews: show the delta-v tooltip (numbers and bill of materials) without hovering. */
    public void previewStats(boolean on) {
        previewStats = on;
    }

    /** For previews: the design being edited. */
    public void setDesign(ShipDesign d) {
        design = d;
    }
}
