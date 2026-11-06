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

    private int visibleRows() {
        return Math.max(1, (bottom() - top() - 4) / ROW);
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
        if (mx >= x && mx < x + LIST_W - 8 && my >= top() + 4) {
            int i = scroll + (int) ((my - top() - 4) / ROW);
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
        g.drawCenteredString(font, title, width / 2, 12, 0x9FE3EA);
        g.fill(x0, y0, x0 + w, bottom(), 0xA0101418);
        g.fill(x0, y0, x0 + w, y0 + 1, 0xFF7FD8E8);
        g.fill(x0 + LIST_W, y0 + 4, x0 + LIST_W + 1, bottom() - 4, 0x407FD8E8);
        if (entries.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.spacebreaker.blueprints.empty"), width / 2, y0 + 20, 0xAAAAAA);
            return;
        }
        // list
        int y = y0 + 4;
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
        g.drawString(font, Component.translatable(d.nameKey(e.getKey())), dx, y0 + 6, 0x9FE3EA, true);
        int rows = d.layers().get(0).size();
        int cols = d.layers().get(0).get(0).length();
        Map<Item, Integer> totals = new LinkedHashMap<>();
        for (MultiblockDefinition.Placement p : d.placements(BlockPos.ZERO, Direction.NORTH)) {
            Item item = p.spec().item();
            if (item != Items.AIR) {
                totals.merge(item, 1, Integer::sum);
            }
        }
        int blocks = totals.values().stream().mapToInt(Integer::intValue).sum();
        g.drawString(font, Component.translatable("gui.spacebreaker.blueprints.size", cols, d.height(), rows, blocks), dx, y0 + 17, 0xAAAAAA, true);
        int previewH = Math.max(60, (bottom() - y0) / 2 - 20);
        renderPreview(g, d, dx + dw / 2, y0 + 30 + previewH / 2, Math.min(dw, previewH), partialTick);
        int ly = y0 + 34 + previewH;
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
        d.tech().ifPresent(tech -> {
            boolean known = Tech.knows(minecraft.player, tech);
            g.drawString(font, Component.translatable("gui.spacebreaker.blueprints.tech", Component.translatable(TechNode.nameKey(tech))),
                    dx, bottom() - 46, 0xC8C8C8, true);
            g.drawString(font, Component.translatable(known ? "gui.spacebreaker.blueprints.tech_known" : "gui.spacebreaker.blueprints.tech_unknown"),
                    dx, bottom() - 36, known ? 0x7CD36A : 0xD8B050, true);
        });
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
