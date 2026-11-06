package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.client.GhostBlocks;
import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Showcase helper (dev only): draws a small ghost structure with {@link GhostBlocks} for the compat shots. */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
final class CompatShowcaseClient {
    /** Minimum corner of the ghost structure, or null for none. */
    static volatile BlockPos ghostOrigin;

    private CompatShowcaseClient() {
    }

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        BlockPos origin = ghostOrigin;
        if (origin == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        BlockState steel = EarthModule.STEEL.block.get().defaultBlockState();
        BlockState tank = MachinesModule.FLUID_TANK.get().defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        for (int dx = 0; dx < 4; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                for (int dy = 0; dy < 2; dy++) {
                    BlockState state = dy == 0 ? steel : (dx == 1 || dx == 2) ? tank : glass;
                    ps.pushPose();
                    ps.translate(origin.getX() + dx - cam.x, origin.getY() + dy - cam.y, origin.getZ() + dz - cam.z);
                    GhostBlocks.render(ps, buffers, state, 0.55F, 0.85F, 1.0F, 0.45F);
                    ps.popPose();
                }
            }
        }
        GhostBlocks.flush(buffers);
    }
}
