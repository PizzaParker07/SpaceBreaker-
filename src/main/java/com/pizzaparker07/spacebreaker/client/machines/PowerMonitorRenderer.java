package com.pizzaparker07.spacebreaker.client.machines;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.content.machines.PowerMonitorBlock;
import com.pizzaparker07.spacebreaker.content.machines.PowerMonitorBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** The monitor's screen: charge in big digits, net flow and time to full or empty, glowing like a backlit display. */
public class PowerMonitorRenderer implements BlockEntityRenderer<PowerMonitorBlockEntity> {
    private final Font font;

    public PowerMonitorRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    @Override
    public void render(PowerMonitorBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-be.getBlockState().getValue(PowerMonitorBlock.FACING).toYRot()));
        pose.translate(0F, 0F, 0.503F);
        float s = 1F / 96F;
        pose.scale(s, -s, s);
        if (!be.found()) {
            line(pose, buffers, Component.literal("--"), 0, 0xFF707A84, 1.4F);
        } else {
            int pct = Math.round(be.fraction() * 100);
            int color = pct > 50 ? 0xFF6CE07A : pct > 20 ? 0xFFF0C040 : 0xFFF05040;
            line(pose, buffers, Component.literal(pct + "%"), -22, color, 2.2F);
            line(pose, buffers, PowerMonitorBlockEntity.flowText(be.net()), 4, be.net() >= 0 ? 0xFF9FE8FF : 0xFFFFB080, 1F);
            Component eta = PowerMonitorBlockEntity.etaText(be.stored(), be.capacity(), be.net());
            if (eta != null) {
                line(pose, buffers, eta, 18, 0xFFC8D0D8, 1F);
            }
        }
        pose.popPose();
    }

    private void line(PoseStack pose, MultiBufferSource buffers, Component text, float y, int color, float scale) {
        FormattedCharSequence seq = text.getVisualOrderText();
        pose.pushPose();
        pose.translate(0F, y, 0F);
        pose.scale(scale, scale, scale);
        float w = font.width(seq);
        font.drawInBatch(seq, -w / 2F, -4F, color, false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0,
                LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
}
