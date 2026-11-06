package com.pizzaparker07.spacebreaker.client.blueprint;

import com.pizzaparker07.spacebreaker.content.blueprint.BlueprintPayload;
import com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition;
import com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;

/** Pick the structure to project. Every blueprint is listed; researched ones carry a mark. */
public class BlueprintScreen extends Screen {
    private static final int ROW = 24;
    private List<Map.Entry<ResourceLocation, MultiblockDefinition>> entries = List.of();
    private int scroll;

    public BlueprintScreen() {
        super(Component.translatable("gui.spacebreaker.blueprints"));
    }

    @Override
    protected void init() {
        entries = Multiblocks.all(minecraft.level.registryAccess());
        rebuild();
    }

    private int visibleRows() {
        return Math.max(1, (height - 70) / ROW);
    }

    private void rebuild() {
        clearWidgets();
        int w = 240;
        int x = (width - w) / 2;
        int y = 40;
        for (int i = scroll; i < Math.min(entries.size(), scroll + visibleRows()); i++) {
            var e = entries.get(i);
            Component name = Component.translatable(e.getValue().nameKey(e.getKey()));
            boolean known = e.getValue().tech().map(t -> Tech.knows(minecraft.player, t)).orElse(true);
            Component label = known ? name : Component.literal("").append(name).withStyle(net.minecraft.ChatFormatting.GRAY);
            addRenderableWidget(Button.builder(label, b -> {
                PacketDistributor.sendToServer(new BlueprintPayload(BlueprintPayload.SELECT, e.getKey()));
                onClose();
            }).bounds(x + 22, y, w - 22, 20).build());
            y += ROW;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int max = Math.max(0, entries.size() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
        rebuild();
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 18, 0x9FE3EA);
        if (entries.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.spacebreaker.blueprints.empty"), width / 2, 60, 0xAAAAAA);
            return;
        }
        int w = 240;
        int x = (width - w) / 2;
        int y = 40;
        for (int i = scroll; i < Math.min(entries.size(), scroll + visibleRows()); i++) {
            MultiblockDefinition d = entries.get(i).getValue();
            ItemStack icon = new ItemStack(d.controllerSpec().item());
            g.renderItem(icon, x, y + 2);
            boolean hovered = mouseX >= x + 22 && mouseX < x + w && mouseY >= y && mouseY < y + 20;
            if (hovered) {
                int rows = d.layers().get(0).size();
                int cols = d.layers().get(0).get(0).length();
                long blocks = d.layers().stream().flatMap(List::stream).flatMapToInt(String::chars).filter(c -> c != ' ' && c != '_').count();
                g.renderTooltip(font, Component.translatable("gui.spacebreaker.blueprints.size", cols, d.height(), rows, blocks), mouseX, mouseY);
            }
            y += ROW;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
