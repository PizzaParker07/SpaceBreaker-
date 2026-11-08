package com.pizzaparker07.spacebreaker.client.research;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.research.Research;
import com.pizzaparker07.spacebreaker.content.research.ResearchClientData;
import com.pizzaparker07.spacebreaker.content.research.ResearchTree;
import com.pizzaparker07.spacebreaker.content.research.SampleItem;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The research tree: one horizontal band per branch, technologies placed left to right by how deep their
 * prerequisite chain is. Drag to pan, scroll to zoom. Vanilla advancement-screen look.
 */
public class ResearchScreen extends Screen {
    private static final ResourceLocation WINDOW = Spacebreaker.id("research/window");
    private static final ResourceLocation NODE_KNOWN = Spacebreaker.id("research/node_known");
    private static final ResourceLocation NODE_AVAILABLE = Spacebreaker.id("research/node_available");
    private static final ResourceLocation NODE_LOCKED = Spacebreaker.id("research/node_locked");
    private static final ResourceLocation BACKGROUND = Spacebreaker.id("textures/gui/research/blueprint.png");
    private static final ResourceLocation LOCK = Spacebreaker.id("guide/lock");
    /** The move/zoom hint shows on the first opening of a play session only; after that it sits in the "?" tooltip. */
    private static boolean hintShown;
    /** Header progress bar (right side of the title row). */
    private static final int BAR_W = 60, BAR_H = 4;
    private static final int NODE = 26, STEP_X = 38, STEP_Y = 30, BAND_GAP = 14, LABEL_W = 0, PAD = 16;

    private final @Nullable Screen parent;
    private final Map<ResourceLocation, Placed> placed = new LinkedHashMap<>();
    private final List<Band> bands = new ArrayList<>();
    private int contentW, contentH;
    private int winX, winY, winW, winH;
    private double panX, panY;
    private float zoom = 1F;
    private boolean dragging;
    private int knownCount;

    private record Placed(ResourceLocation id, TechNode node, int x, int y) {
    }

    private record Band(String category, int y, int h) {
    }

    public ResearchScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.spacebreaker.research.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        winW = Math.min(width - 24, 520);
        winH = Math.min(height - 24, 320);
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        layout();
        panX = 0;
        panY = 0;
        clampPan();
    }

    // ------------------------------------------------------------------ layout

    private void layout() {
        placed.clear();
        bands.clear();
        Map<ResourceLocation, TechNode> all = minecraft.level == null ? Map.of() : Tech.all(minecraft.level.registryAccess());
        Map<ResourceLocation, Integer> depth = new HashMap<>();
        for (ResourceLocation id : all.keySet()) {
            depth(id, all, depth, 0);
        }
        List<String> categories = new ArrayList<>(ResearchTree.CATEGORIES);
        all.values().stream().map(TechNode::category).distinct().filter(c -> !categories.contains(c)).sorted().forEach(categories::add);
        int y = PAD + 12;
        int maxDepth = 0;
        for (String cat : categories) {
            Map<Integer, List<ResourceLocation>> columns = new HashMap<>();
            all.entrySet().stream().filter(e -> e.getValue().category().equals(cat))
                    .sorted(Comparator.comparing((Map.Entry<ResourceLocation, TechNode> e) -> e.getValue().tier()).thenComparing(e -> e.getKey()))
                    .forEach(e -> columns.computeIfAbsent(depth.get(e.getKey()), k -> new ArrayList<>()).add(e.getKey()));
            if (columns.isEmpty()) {
                continue;
            }
            int rows = columns.values().stream().mapToInt(List::size).max().orElse(1);
            for (Map.Entry<Integer, List<ResourceLocation>> col : columns.entrySet()) {
                List<ResourceLocation> ids = col.getValue();
                int offset = (rows - ids.size()) * STEP_Y / 2;
                for (int i = 0; i < ids.size(); i++) {
                    ResourceLocation id = ids.get(i);
                    int x = PAD + LABEL_W + col.getKey() * STEP_X;
                    placed.put(id, new Placed(id, all.get(id), x, y + offset + i * STEP_Y));
                    maxDepth = Math.max(maxDepth, col.getKey());
                }
            }
            int h = rows * STEP_Y;
            bands.add(new Band(cat, y, h));
            y += h + BAND_GAP + 12;
        }
        contentW = PAD * 2 + LABEL_W + maxDepth * STEP_X + NODE;
        contentH = y;
        knownCount = (int) placed.keySet().stream().filter(ResearchClientData::isKnownOnClient).count();
    }

    private static int depth(ResourceLocation id, Map<ResourceLocation, TechNode> all, Map<ResourceLocation, Integer> memo, int guard) {
        Integer d = memo.get(id);
        if (d != null) {
            return d;
        }
        TechNode node = all.get(id);
        int out = 0;
        if (node != null && guard < 64) {
            for (ResourceLocation r : node.requires()) {
                if (all.containsKey(r)) {
                    out = Math.max(out, depth(r, all, memo, guard + 1) + 1);
                }
            }
        }
        memo.put(id, out);
        return out;
    }

    private int innerX() {
        return winX + 9;
    }

    private int innerY() {
        return winY + 18;
    }

    private int innerW() {
        return winW - 18;
    }

    private int innerH() {
        return winH - 27;
    }

    private void clampPan() {
        double maxX = Math.max(0, contentW * zoom - innerW());
        double maxY = Math.max(0, contentH * zoom - innerH());
        panX = Mth.clamp(panX, -maxX, 0);
        panY = Mth.clamp(panY, -maxY, 0);
    }

    // ------------------------------------------------------------------ state

    enum State { KNOWN, AVAILABLE, LOCKED }

    private static State state(Placed p) {
        if (ResearchClientData.isKnownOnClient(p.id())) {
            return State.KNOWN;
        }
        Set<ResourceLocation> known = ResearchClientData.known();
        return known.containsAll(p.node().requires()) ? State.AVAILABLE : State.LOCKED;
    }

    public static ItemStack iconFor(ResourceLocation tech) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            var node = Tech.get(mc.level.registryAccess(), tech);
            if (node.isPresent() && node.get().icon().isPresent()) {
                var item = BuiltInRegistries.ITEM.getOptional(node.get().icon().get());
                if (item.isPresent() && item.get() != Items.AIR) {
                    return new ItemStack(item.get());
                }
            }
        }
        return Research.disk(tech);
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int ix = innerX(), iy = innerY(), iw = innerW(), ih = innerH();
        // background (tiled blueprint, scrolls with the view)
        g.enableScissor(ix, iy, ix + iw, iy + ih);
        int tile = 16;
        int ox = Mth.floor(panX * 0.5) % tile, oy = Mth.floor(panY * 0.5) % tile;
        for (int x = ix + ox - tile; x < ix + iw; x += tile) {
            for (int y = iy + oy - tile; y < iy + ih; y += tile) {
                g.blit(BACKGROUND, x, y, 0, 0, tile, tile, tile, tile);
            }
        }
        g.pose().pushPose();
        g.pose().translate(ix + panX, iy + panY, 0);
        g.pose().scale(zoom, zoom, 1F);
        double mx = (mouseX - ix - panX) / zoom, my = (mouseY - iy - panY) / zoom;
        boolean inside = mouseX >= ix && mouseX < ix + iw && mouseY >= iy && mouseY < iy + ih;
        for (Band b : bands) {
            int color = 0xFF000000 | ResearchTree.categoryColor(b.category());
            // the branch name sticks to the left edge of the view, so it is never cut off when the tree is scrolled
            int labelX = Math.max(PAD, (int) Math.ceil(-panX / zoom) + 4);
            g.drawString(font, Component.translatable(TechNode.categoryKey(b.category())), labelX, b.y() - 11, color, true);
            g.fill(PAD, b.y() - 2, contentW - PAD, b.y() - 1, (color & 0x00FFFFFF) | 0x55000000);
        }
        Placed hovered = null;
        for (Placed p : placed.values()) {
            if (inside && mx >= p.x() && mx < p.x() + NODE && my >= p.y() && my < p.y() + NODE) {
                hovered = p;
            }
        }
        // every link faint; the hovered technology's prerequisites and followers drawn bright on top
        for (Placed p : placed.values()) {
            for (ResourceLocation r : p.node().requires()) {
                Placed from = placed.get(r);
                if (from != null && hovered != p && hovered != from) {
                    drawLink(g, from, p, false);
                }
            }
        }
        if (hovered != null) {
            for (Placed p : placed.values()) {
                for (ResourceLocation r : p.node().requires()) {
                    Placed from = placed.get(r);
                    if (from != null && (hovered == p || hovered == from)) {
                        drawLink(g, from, p, true);
                    }
                }
            }
        }
        for (Placed p : placed.values()) {
            State s = state(p);
            g.blitSprite(switch (s) {
                case KNOWN -> NODE_KNOWN;
                case AVAILABLE -> NODE_AVAILABLE;
                case LOCKED -> NODE_LOCKED;
            }, p.x(), p.y(), NODE, NODE);
            int cat = 0xFF000000 | ResearchTree.categoryColor(p.node().category());
            g.fill(p.x() + 3, p.y() + NODE - 4, p.x() + NODE - 3, p.y() + NODE - 3, s == State.LOCKED ? 0xFF3A3A3A : cat);
            g.renderFakeItem(iconFor(p.id()), p.x() + 5, p.y() + 4);
            if (s == State.LOCKED) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.fill(p.x() + 4, p.y() + 3, p.x() + NODE - 4, p.y() + NODE - 5, 0xA0101418);
                g.blitSprite(LOCK, p.x() + NODE - 10, p.y() + 1, 9, 9);
                g.pose().popPose();
            }
        }
        g.pose().popPose();
        g.disableScissor();

        // window frame + header on top
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.blitSprite(WINDOW, winX, winY, winW, winH);
        g.drawString(font, title, winX + 8, winY + 6, 0x404040, false);
        // progress: a thin bar (the count is in its tooltip) and a "?" for the controls
        int bx = barX(), by = winY + 8;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, 0xFF373737);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, 0xFF8B8B8B);
        int filled = placed.isEmpty() ? 0 : Math.round((float) BAR_W * knownCount / placed.size());
        g.fill(bx, by, bx + filled, by + BAR_H, 0xFF5FB84A);
        g.drawString(font, "?", helpX(), winY + 6, 0x404040, false);
        g.pose().popPose();
        if (!hintShown) {
            Component hint = Component.translatable("gui.spacebreaker.research.hint");
            g.drawString(font, hint, (width - font.width(hint)) / 2, winY + winH + 3, 0xA0A0A0, false);
        }
        if (hovered == null) {
            if (mouseX >= bx - 1 && mouseX < bx + BAR_W + 1 && mouseY >= winY + 4 && mouseY < winY + 15) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 400);
                g.renderTooltip(font, Component.translatable("gui.spacebreaker.research.progress", knownCount, placed.size()), mouseX, mouseY);
                g.pose().popPose();
            } else if (mouseX >= helpX() - 2 && mouseX < helpX() + 8 && mouseY >= winY + 4 && mouseY < winY + 15) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 400);
                g.renderTooltip(font, Component.translatable("gui.spacebreaker.research.hint"), mouseX, mouseY);
                g.pose().popPose();
            }
        }

        if (hovered != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            g.renderTooltip(font, tooltip(hovered), mouseX, mouseY);
            g.pose().popPose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
    }

    private int barX() {
        return winX + winW - 18 - BAR_W;
    }

    private int helpX() {
        return winX + winW - 13;
    }

    @Override
    public void removed() {
        hintShown = true;
        super.removed();
    }

    /** A prerequisite link: faint by default, bright (and in the branch colour once learned) for the hovered node. */
    private void drawLink(GuiGraphics g, Placed from, Placed to, boolean bright) {
        boolean lit = ResearchClientData.isKnownOnClient(from.id());
        int inner = lit ? (0xFF000000 | ResearchTree.categoryColor(from.node().category())) : 0xFFC8CDD4;
        int outer = 0xFF101418;
        if (!bright) {
            inner = (inner & 0x00FFFFFF) | 0x40000000;
            outer = 0x30101418;
        }
        int x1 = from.x() + NODE, y1 = from.y() + NODE / 2;
        int x2 = to.x(), y2 = to.y() + NODE / 2;
        int mid = x2 - 6;
        if (mid < x1 + 2) {
            mid = x1 + 2;
        }
        hLine(g, x1, mid, y1, inner, outer);
        vLine(g, mid, y1, y2, inner, outer);
        hLine(g, mid, x2, y2, inner, outer);
    }

    private static void hLine(GuiGraphics g, int x1, int x2, int y, int inner, int outer) {
        int a = Math.min(x1, x2), b = Math.max(x1, x2);
        g.fill(a - 1, y - 1, b + 2, y + 2, outer);
        g.fill(a, y, b + 1, y + 1, inner);
    }

    private static void vLine(GuiGraphics g, int x, int y1, int y2, int inner, int outer) {
        int a = Math.min(y1, y2), b = Math.max(y1, y2);
        g.fill(x - 1, a - 1, x + 2, b + 2, outer);
        g.fill(x, a, x + 1, b + 1, inner);
    }

    private List<FormattedCharSequence> tooltip(Placed p) {
        List<FormattedCharSequence> out = new ArrayList<>();
        int cat = ResearchTree.categoryColor(p.node().category());
        out.add(Component.translatable(TechNode.nameKey(p.id())).withColor(cat).getVisualOrderText());
        State s = state(p);
        MutableComponent line = Component.translatable(TechNode.categoryKey(p.node().category())).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable("gui.spacebreaker.research.tier", p.node().tier()).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable(switch (s) {
                    case KNOWN -> "gui.spacebreaker.research.known";
                    case AVAILABLE -> "gui.spacebreaker.research.available";
                    case LOCKED -> "gui.spacebreaker.research.locked";
                }).withStyle(s == State.KNOWN ? ChatFormatting.GREEN : s == State.AVAILABLE ? ChatFormatting.YELLOW : ChatFormatting.RED));
        out.add(line.getVisualOrderText());
        out.addAll(font.split(Component.translatable(TechNode.descriptionKey(p.id())).withStyle(ChatFormatting.WHITE), 200));
        if (!p.node().requires().isEmpty()) {
            out.add(Component.translatable("gui.spacebreaker.research.requires").withStyle(ChatFormatting.GRAY).getVisualOrderText());
            for (ResourceLocation r : p.node().requires()) {
                boolean ok = ResearchClientData.isKnownOnClient(r);
                out.add(Component.literal(ok ? " ✔ " : " ✘ ").withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED)
                        .append(Component.translatable(TechNode.nameKey(r)).withStyle(ChatFormatting.GRAY)).getVisualOrderText());
            }
        }
        if (s != State.KNOWN) {
            Component src;
            if (p.node().samples().isEmpty()) {
                src = Component.translatable("gui.spacebreaker.research.sources");
            } else {
                MutableComponent kinds = Component.empty();
                for (int i = 0; i < p.node().samples().size(); i++) {
                    if (i > 0) {
                        kinds.append(", ");
                    }
                    kinds.append(Component.translatable(SampleItem.kindKey(p.node().samples().get(i))));
                }
                src = Component.translatable("gui.spacebreaker.research.sources_samples", kinds);
            }
            out.addAll(font.split(src.copy().withStyle(ChatFormatting.DARK_AQUA), 200));
            out.add(Component.translatable("gui.spacebreaker.research.cost", p.node().effectiveResearchTime() / 20,
                    p.node().effectiveEnergy()).withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
        }
        return out;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= innerX() && mouseX < innerX() + innerW() && mouseY >= innerY() && mouseY < innerY() + innerH()) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging) {
            panX += dx;
            panY += dy;
            clampPan();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float old = zoom;
        zoom = Mth.clamp(zoom + (float) scrollY * 0.1F, 0.5F, 1.5F);
        if (zoom != old) {
            double cx = mouseX - innerX(), cy = mouseY - innerY();
            panX = cx - (cx - panX) * zoom / old;
            panY = cy - (cy - panY) * zoom / old;
            clampPan();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ResearchClient.OPEN_RESEARCH.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * For the showcase: bring a technology into view. The view stays against the left (top) edge, with the branch
     * names readable, whenever the technology is already visible from there; otherwise it is centred.
     */
    public void focus(ResourceLocation tech) {
        Placed p = placed.get(tech);
        if (p != null) {
            int margin = 8;
            panX = (p.x() + NODE) * zoom + margin <= innerW() ? 0 : innerW() / 2.0 - (p.x() + NODE / 2.0) * zoom;
            panY = (p.y() + NODE) * zoom + margin <= innerH() ? 0 : innerH() / 2.0 - (p.y() + NODE / 2.0) * zoom;
            clampPan();
        }
    }
}
