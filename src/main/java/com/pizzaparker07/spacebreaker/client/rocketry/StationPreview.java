package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.station.StationHatchBlock;
import com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule;
import com.pizzaparker07.spacebreaker.content.rocketry.station.StationModuleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Holding a station module and looking at a mouth's hatch: an outline of where the module will go, green when there is
 * room for it, red when something is in the way.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
final class StationPreview {
    private StationPreview() {
    }

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        StationModule module = null;
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            if (mc.player.getItemInHand(hand).getItem() instanceof StationModuleItem item) {
                module = item.module().orElse(null);
                break;
            }
        }
        if (module == null) {
            return;
        }
        BlockPos mouth = StationHatchBlock.mouthCenter(mc.level, hit.getBlockPos());
        if (mouth == null) {
            return;
        }
        Direction facing = mc.level.getBlockState(mouth).getValue(StationHatchBlock.FACING);
        if (facing.getAxis().isVertical()) {
            return;
        }
        BlockPos wall = StationModule.wallAt(mouth, facing);
        boolean fits = module.fits(mc.level, wall, facing);
        AABB box = module.bounds(wall, facing).inflate(0.02);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        var buffers = mc.renderBuffers().bufferSource();
        // clipped at the camera: a shader pack's line program would turn an edge behind the viewer into a huge quad
        com.pizzaparker07.spacebreaker.compat.client.ClippedLines.box(pose, buffers.getBuffer(RenderType.lines()), box, event.getCamera(),
                fits ? 0.45F : 1F, fits ? 1F : 0.35F, fits ? 0.5F : 0.3F, 1F);
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }
}
