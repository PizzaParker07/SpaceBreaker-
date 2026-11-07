package com.pizzaparker07.spacebreaker.client.blueprint;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.content.blueprint.BlueprintPayload;
import com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition;
import com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pick the structure to project: the list on the left, a slowly turning 3D preview on the right with its size,
 * material totals and the technology that teaches it (a note, not a lock: every blueprint can be projected).
 */
public class BlueprintScreen extends Screen {
    private static final int ROW = 20;
    private static final int LIST_W = 150;
    /** The title row inside the panel, above the list and the details. */
    private static final int HEADER = 16;
    private List<Map.Entry<ResourceLocation, MultiblockDefinition>> entries = List.of();
    private int scroll;
    private int selected;
    private Button project;

    public BlueprintScreen() {
        super(Component.translatable("gui.spacebreaker.blueprints"));
    }

    private int panelW() {
        return Math.min(380, width - 20);
    }

    private int left() {
        return (width - panelW()) / 2;
    }

    private int top() {
        return 30;
    }

    private int bottom() {
        return height - 30;
    }

    private int listTop() {
        return top() + HEADER + 2;
    }

    private int visibleRows() {
        return Math.max(1, (bottom() - listTop()) / ROW);
    }

    @Override
    protected void init() {
        entries = Multiblocks.all(minecraft.level.registryAccess());
        selected = Math.min(selected, Math.max(0, entries.size() - 1));
        project = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.blueprints.project"), b -> confirm())
                .bounds(left() + panelW() - 90, bottom() - 22, 86, 20).build());
        project.active = !entries.isEmpty();
    }

    private void confirm() {
        if (selected < entries.size()) {
            PacketDistributor.sendToServer(new BlueprintPayload(BlueprintPayload.SELECT, entries.get(selected).getKey()));
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = left() + 4;
        if (mx >= x && mx < x + LIST_W - 8 && my >= listTop()) {
            int i = scroll + (int) ((my - listTop()) / ROW);
            if (i < entries.size() && i < scroll + visibleRows()) {
                if (i == selected && button == 0) {
                    confirm(); // second click on the same entry projects it
                } else {
                    selected = i;
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int max = Math.max(0, entries.size() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x0 = left();
        int y0 = top();
        int w = panelW();
        g.fill(x0, y0, x0 + w, bottom(), 0xA0101418);
        g.fill(x0, y0, x0 + w, y0 + 1, 0xFF7FD8E8);
        // the title inside the panel, top left, like the other screens
        g.drawString(font, title, x0 + 6, y0 + 5, 0x9FE3EA, true);
        g.fill(x0 + 4, y0 + HEADER, x0 + w - 4, y0 + HEADER + 1, 0x407FD8E8);
        g.fill(x0 + LIST_W, y0 + HEADER + 4, x0 + LIST_W + 1, bottom() - 4, 0x407FD8E8);
        if (entries.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.spacebreaker.blueprints.empty"), width / 2, y0 + 20, 0xAAAAAA);
            return;
        }
        // list
        int y = listTop();
        for (int i = scroll; i < Math.min(entries.size(), scroll + visibleRows()); i++) {
            var e = entries.get(i);
            boolean sel = i == selected;
            boolean hover = mouseX >= x0 + 4 && mouseX < x0 + LIST_W - 4 && mouseY >= y && mouseY < y + ROW;
            if (sel || hover) {
                g.fill(x0 + 4, y, x0 + LIST_W - 4, y + ROW - 2, sel ? 0x6037A0B4 : 0x30FFFFFF);
            }
            g.renderItem(new ItemStack(e.getValue().controllerSpec().item()), x0 + 6, y + 1);
            String name = font.plainSubstrByWidth(Component.translatable(e.getValue().nameKey(e.getKey())).getString(), LIST_W - 34);
            g.drawString(font, name, x0 + 26, y + 6, sel ? 0xFFFFFF : 0xC8C8C8, true);
            y += ROW;
        }
        // details of the selected blueprint
        var e = entries.get(selected);
        MultiblockDefinition d = e.getValue();
        int dx = x0 + LIST_W + 8;
        int dw = w - LIST_W - 12;
        int dy0 = y0 + HEADER + 2;
        Component dname = Component.translatable(d.nameKey(e.getKey()));
        g.drawString(font, dname, dx, dy0 + 4, 0x9FE3EA, true);
        int rows = d.layers().get(0).size();
        int cols = d.layers().get(0).get(0).length();
        Map<Item, Integer> totals = new LinkedHashMap<>();
        for (MultiblockDefinition.Placement p : d.placements(BlockPos.ZERO, Direction.NORTH)) {
            Item item = p.spec().item();
            if (item != Items.AIR && (!d.selfAssembling() || d.isController(p))) {
                totals.merge(item, 1, Integer::sum);
            }
        }
        int blocks = totals.values().stream().mapToInt(Integer::intValue).sum();
        // research: a small warning mark beside the name when not known yet, the words in its tooltip
        List<Component> techTip = d.tech().filter(tech -> !Tech.knows(minecraft.player, tech))
                .map(tech -> List.<Component>of(
                        Component.translatable("gui.spacebreaker.blueprints.tech", Component.translatable(TechNode.nameKey(tech))),
                        Component.translatable("gui.spacebreaker.blueprints.tech_unknown")))
                .orElse(null);
        int markX = dx + font.width(dname) + 5, markY = dy0 + 3;
        if (techTip != null) {
            g.fill(markX, markY, markX + 9, markY + 9, 0xC0D8B050);
            g.drawString(font, "!", markX + 3, markY + 1, 0xFF101418, false);
        }
        int previewH = Math.max(60, (bottom() - y0) / 2 - 20);
        int pcx = dx + dw / 2, pcy = dy0 + 18 + previewH / 2, psize = Math.min(dw, previewH);
        renderPreview(g, d, pcx, pcy, psize, partialTick);
        int ly = dy0 + 22 + previewH;
        g.drawString(font, Component.translatable("gui.spacebreaker.blueprints.materials"), dx, ly, 0x9FE3EA, true);
        ly += 11;
        int col = 0;
        for (Map.Entry<Item, Integer> t : totals.entrySet()) {
            if (ly > bottom() - 50) {
                break;
            }
            int cx = dx + col * (dw / 2);
            g.renderItem(new ItemStack(t.getKey()), cx, ly);
            g.drawString(font, "× " + t.getValue(), cx + 18, ly + 5, 0xE0E0E0, true);
            col++;
            if (col == 2) {
                col = 0;
                ly += 18;
            }
        }
        if (d.selfAssembling()) {
            g.drawString(font, Component.translatable("hud.spacebreaker.materials.self_assembling"), dx, bottom() - 36, 0x9FE3EA, true);
        }
        // tooltips: the research mark, and the size and block count on the preview
        if (techTip != null && mouseX >= markX && mouseX < markX + 9 && mouseY >= markY && mouseY < markY + 9) {
            g.renderComponentTooltip(font, techTip, mouseX, mouseY);
        } else if (Math.abs(mouseX - pcx) < psize / 2 && Math.abs(mouseY - pcy) < psize / 2) {
            g.renderTooltip(font, Component.translatable("gui.spacebreaker.blueprints.size", cols, d.height(), rows, blocks), mouseX, mouseY);
        }
    }

    /** The whole structure as real block models, turning slowly. */
    private void renderPreview(GuiGraphics g, MultiblockDefinition d, int cx, int cy, int size, float partialTick) {
        List<MultiblockDefinition.Placement> placements = d.placements(BlockPos.ZERO, Direction.NORTH);
        if (placements.isEmpty()) {
            return;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (var p : placements) {
            minX = Math.min(minX, p.pos().getX());
            minY = Math.min(minY, p.pos().getY());
            minZ = Math.min(minZ, p.pos().getZ());
            maxX = Math.max(maxX, p.pos().getX());
            maxY = Math.max(maxY, p.pos().getY());
            maxZ = Math.max(maxZ, p.pos().getZ());
        }
        float extent = Math.max(maxY - minY + 1, (float) Math.hypot(maxX - minX + 1, maxZ - minZ + 1));
        float scale = size / (extent * 1.15F);
        float spin = (minecraft.level.getGameTime() + partialTick) * 0.8F;
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 150);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.translate(-(minX + maxX + 1) / 2F, -(minY + maxY + 1) / 2F, -(minZ + maxZ + 1) / 2F);
        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        var dispatcher = minecraft.getBlockRenderer();
        for (var p : placements) {
            var state = p.spec().display(Rotation.NONE);
            if (state.isAir()) {
                continue;
            }
            pose.pushPose();
            pose.translate(p.pos().getX(), p.pos().getY(), p.pos().getZ());
            dispatcher.renderSingleBlock(state, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            pose.popPose();
        }
        buffers.endBatch();
        Lighting.setupFor3DItems();
        pose.popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
