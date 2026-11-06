package com.pizzaparker07.spacebreaker.client.machines;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineLayout;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineMenu;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineTank;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.text.NumberFormat;
import java.util.List;

/** One screen for every machine, drawn from its {@link MachineLayout} with the shared vanilla-style widget sheet. */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final ResourceLocation TEX = Spacebreaker.id("textures/gui/machine.png");
    private static final NumberFormat NUM = NumberFormat.getIntegerInstance();

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    private MachineLayout layout() {
        return menu.machine.layout();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        renderGaugeTooltips(g, mouseX, mouseY);
        if (menu.data.get(6) == 7 && mouseX >= leftPos + 8 && mouseX < leftPos + imageWidth - 8 && mouseY >= topPos + 4 && mouseY < topPos + 15) {
            g.renderTooltip(font, requiresLine(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos, y0 = topPos;
        g.blit(TEX, x0, y0, 0, 0, imageWidth, imageHeight);
        MachineLayout l = layout();
        ContainerData d = menu.data;
        for (int i = 0; i < l.slots.size(); i++) {
            MachineLayout.SlotPos s = l.slots.get(i);
            g.blit(TEX, x0 + s.x(), y0 + s.y(), 176, 0, 18, 18);
            if (menu.slots.get(i).getItem().isEmpty()) {
                if (s.role() == MachineLayout.Role.TOOL) {
                    g.blit(TEX, x0 + s.x() + 1, y0 + s.y() + 1, 232, 0, 16, 16);
                } else if (s.role() == MachineLayout.Role.FUEL) {
                    g.blit(TEX, x0 + s.x() + 1, y0 + s.y() + 1, 232, 16, 16, 16);
                }
            }
        }
        int progress = d.get(0), max = d.get(1);
        float frac = max > 0 ? Math.min(1F, (float) progress / max) : 0F;
        switch (l.progress) {
            case ARROW -> {
                g.blit(TEX, x0 + l.progressX, y0 + l.progressY, 177, 46, 22, 13);
                int w = Math.round(22 * frac);
                if (w > 0) {
                    g.blit(TEX, x0 + l.progressX, y0 + l.progressY, 201, 46, w, 13);
                }
            }
            case FLAME -> {
                g.blit(TEX, x0 + l.progressX, y0 + l.progressY, 176, 62, 14, 14);
                int h = Math.round(14 * frac);
                if (h > 0) {
                    g.blit(TEX, x0 + l.progressX, y0 + l.progressY + 14 - h, 190, 62 + 14 - h, 14, h);
                }
            }
            default -> {
            }
        }
        if (l.hasEnergy()) {
            g.blit(TEX, x0 + l.energyX, y0 + l.energyY, 176, 76, 14, 54);
            int stored = MachineBlockEntity.combine(d, 2), cap = MachineBlockEntity.combine(d, 4);
            int h = cap > 0 ? Math.round(52F * stored / cap) : 0;
            if (h > 0) {
                g.blit(TEX, x0 + l.energyX + 1, y0 + l.energyY + 1 + 52 - h, 190, 77 + 52 - h, 12, h);
            }
        }
        for (int t = 0; t < l.tanks.size(); t++) {
            MachineLayout.TankPos p = l.tanks.get(t);
            g.blit(TEX, x0 + p.x(), y0 + p.y(), 202, 76, 18, 54);
            FluidStack fluid = MachineBlockEntity.fluid(d, t);
            MachineTank tank = menu.machine.tank(t);
            if (!fluid.isEmpty()) {
                int h = Math.max(1, Math.round(52F * fluid.getAmount() / tank.getCapacity()));
                drawFluid(g, fluid, x0 + p.x() + 1, y0 + p.y() + 1 + 52 - h, 16, h);
            }
            g.blit(TEX, x0 + p.x() + 1, y0 + p.y() + 1, 220, 77, 16, 52);
        }
    }

    static void drawFluid(GuiGraphics g, FluidStack fluid, int x, int y, int w, int h) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ext.getStillTexture(fluid));
        int tint = ext.getTintColor(fluid);
        float a = ((tint >>> 24) & 0xFF) / 255F, r = ((tint >> 16) & 0xFF) / 255F, gr = ((tint >> 8) & 0xFF) / 255F, b = (tint & 0xFF) / 255F;
        a = Math.max(a, 0.75F);
        for (int ty = 0; ty < h; ty += 16) {
            int th = Math.min(16, h - ty);
            for (int tx = 0; tx < w; tx += 16) {
                int tw = Math.min(16, w - tx);
                g.blit(x + tx, y + h - ty - th, 0, tw, th, sprite, r, gr, b, a);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int status = menu.data.get(6);
        String key = MachineBlockEntity.statusKey(status);
        if (key != null) {
            Component c = Component.translatable(key);
            if (status == 7) {
                c = requiresLine();
            }
            int room = imageWidth - 16 - font.width(title) - 6;
            String text = font.plainSubstrByWidth(c.getString(), room);
            g.drawString(font, text, imageWidth - 8 - font.width(text), titleLabelY, 0xB02E26, false);
        }
    }

    /** "Requires: <technology names>" for a machine its owner has not researched yet. */
    private Component requiresLine() {
        var req = menu.machine.techRequirement();
        net.minecraft.network.chat.MutableComponent names = Component.empty();
        for (int i = 0; i < req.techs().size(); i++) {
            if (i > 0) {
                names.append(" / ");
            }
            names.append(Component.translatable(com.pizzaparker07.spacebreaker.core.tech.TechNode.nameKey(req.techs().get(i))));
        }
        return Component.translatable("gui.spacebreaker.requires", names);
    }

    private void renderGaugeTooltips(GuiGraphics g, int mouseX, int mouseY) {
        MachineLayout l = layout();
        int mx = mouseX - leftPos, my = mouseY - topPos;
        ContainerData d = menu.data;
        if (l.hasEnergy() && mx >= l.energyX && mx < l.energyX + 14 && my >= l.energyY && my < l.energyY + 54) {
            g.renderTooltip(font, Component.translatable("gui.spacebreaker.energy",
                    NUM.format(MachineBlockEntity.combine(d, 2)), NUM.format(MachineBlockEntity.combine(d, 4))), mouseX, mouseY);
        }
        for (int t = 0; t < l.tanks.size(); t++) {
            MachineLayout.TankPos p = l.tanks.get(t);
            if (mx >= p.x() && mx < p.x() + 18 && my >= p.y() && my < p.y() + 54) {
                FluidStack f = MachineBlockEntity.fluid(d, t);
                int cap = menu.machine.tank(t).getCapacity();
                Component line = f.isEmpty()
                        ? Component.translatable("gui.spacebreaker.tank_empty", NUM.format(cap))
                        : Component.translatable("gui.spacebreaker.tank", f.getHoverName(), NUM.format(f.getAmount()), NUM.format(cap));
                g.renderComponentTooltip(font, List.of(line.copy().withStyle(ChatFormatting.WHITE)), mouseX, mouseY);
            }
        }
    }
}
