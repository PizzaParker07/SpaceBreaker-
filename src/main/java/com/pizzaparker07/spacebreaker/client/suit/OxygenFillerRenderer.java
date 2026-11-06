package com.pizzaparker07.spacebreaker.client.suit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.content.suit.block.GasMachineBlock;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenFillerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Shows the tank standing in the filler's cradle (its 3D hand model). */
public class OxygenFillerRenderer implements BlockEntityRenderer<OxygenFillerBlockEntity> {
    public OxygenFillerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(OxygenFillerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = be.getStack();
        if (stack.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5F, 11F / 16F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-be.getBlockState().getValue(GasMachineBlock.FACING).toYRot()));
        pose.scale(0.62F, 0.62F, 0.62F);
        pose.translate(-0.5F, 0F, -0.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, overlay, pose, buffers,
                be.getLevel(), (int) be.getBlockPos().asLong());
        pose.popPose();
    }
}
