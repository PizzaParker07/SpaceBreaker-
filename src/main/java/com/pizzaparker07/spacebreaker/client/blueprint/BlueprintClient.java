package com.pizzaparker07.spacebreaker.client.blueprint;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.blueprint.BlueprintModule;
import com.pizzaparker07.spacebreaker.content.blueprint.BlueprintPayload;
import com.pizzaparker07.spacebreaker.content.blueprint.Hologram;
import com.pizzaparker07.spacebreaker.content.blueprint.HologramProjectorItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

/**
 * Client side of the Hologram Projector: draws the projection of the projector the local player holds
 * (nobody else sees it), lists the materials still needed, turns the projection with sneak + wheel and
 * celebrates a finished structure. Ghosts are drawn after translucent blocks with the block atlas, so they
 * also work with shader packs; the cache is refreshed a few times per second, not every frame.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class BlueprintClient {
    private static final HologramCache CACHE = new HologramCache();

    private BlueprintClient() {
    }

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        HologramProjectorItem.openSelection = () -> Minecraft.getInstance().setScreen(new BlueprintScreen());
    }

    static Hologram held(LocalPlayer player) {
        ItemStack s = BlueprintModule.projectorInHand(player);
        return s.isEmpty() ? null : HologramProjectorItem.hologram(s);
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (CACHE.update(mc.level, held(mc.player))) {
            // structure just completed
            mc.player.playSound(SoundEvents.PLAYER_LEVELUP, 0.6F, 1.6F);
            mc.player.displayClientMessage(Component.translatable("message.spacebreaker.hologram.complete",
                    Component.translatable(CACHE.def().nameKey(CACHE.key().blueprint()))), true);
            for (int i = 0; i < 40; i++) {
                double x = Mth.lerp(mc.level.random.nextDouble(), CACHE.min.getX(), CACHE.max.getX() + 1.0);
                double y = Mth.lerp(mc.level.random.nextDouble(), CACHE.min.getY(), CACHE.max.getY() + 1.0);
                double z = Mth.lerp(mc.level.random.nextDouble(), CACHE.min.getZ(), CACHE.max.getZ() + 1.0);
                mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0, 0.02, 0);
            }
        }
    }

    @SubscribeEvent
    static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.player.isShiftKeyDown()) {
            return;
        }
        Hologram h = held(mc.player);
        if (h == null || h.anchor().isEmpty() || event.getScrollDeltaY() == 0) {
            return;
        }
        int action = event.getScrollDeltaY() > 0 ? BlueprintPayload.ROTATE_CW : BlueprintPayload.ROTATE_CCW;
        PacketDistributor.sendToServer(new BlueprintPayload(action, h.blueprint()));
        event.setCanceled(true);
    }

    // ------------------------------------------------------------------ world rendering

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || CACHE.key() == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || held(mc.player) == null) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float t = (mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false)) * 0.15F;
        float alpha = 0.38F + 0.08F * Mth.sin(t);

        ModelBlockRenderer renderer = mc.getBlockRenderer().getModelRenderer();
        VertexConsumer ghost = new GhostVertexConsumer(buffers.getBuffer(Sheets.translucentCullBlockSheet()), 0.45F, 0.9F, 1.0F, alpha);
        for (HologramCache.Ghost g : CACHE.missing) {
            if (g.pos().distToCenterSqr(cam) > 96 * 96) {
                continue;
            }
            pose.pushPose();
            pose.translate(g.pos().getX() - cam.x, g.pos().getY() - cam.y, g.pos().getZ() - cam.z);
            // shrink slightly so ghosts never z-fight with real neighbours
            pose.translate(0.5, 0.5, 0.5);
            pose.scale(0.98F, 0.98F, 0.98F);
            pose.translate(-0.5, -0.5, -0.5);
            renderer.renderModel(pose.last(), ghost, g.state(), mc.getBlockRenderer().getBlockModel(g.state()), 1F, 1F, 1F,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            pose.popPose();
        }
        buffers.endBatch(Sheets.translucentCullBlockSheet());

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (BlockPos p : CACHE.wrong) {
            AABB box = new AABB(p).inflate(0.004).move(-cam.x, -cam.y, -cam.z);
            LevelRenderer.renderLineBox(pose, lines, box, 1F, 0.25F, 0.2F, 1F);
        }
        // faint frame around the whole projection
        AABB all = new AABB(Vec3.atLowerCornerOf(CACHE.min), Vec3.atLowerCornerOf(CACHE.max).add(1, 1, 1)).move(-cam.x, -cam.y, -cam.z);
        LevelRenderer.renderLineBox(pose, lines, all, 0.45F, 0.85F, 1F, 0.35F);
        buffers.endBatch(RenderType.lines());
    }

    // ------------------------------------------------------------------ material list

    @SubscribeEvent
    static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, Spacebreaker.id("hologram_materials"), BlueprintClient::materials);
    }

    static void materials(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || CACHE.key() == null || CACHE.def() == null || held(mc.player) == null) {
            return;
        }
        var font = mc.font;
        Component title = Component.translatable(CACHE.def().nameKey(CACHE.key().blueprint()));
        Component progress = Component.translatable("hud.spacebreaker.materials.done", CACHE.built, CACHE.total);
        int rows = Math.min(8, CACHE.needed.size());
        int w = Math.max(110, Math.max(font.width(title), font.width(progress)) + 8);
        for (Item item : CACHE.needed.keySet()) {
            w = Math.max(w, font.width(new ItemStack(item).getHoverName()) + 64);
        }
        w = Math.min(w, 200);
        int h = 26 + rows * 18;
        int x = g.guiWidth() - w - 6;
        int y = g.guiHeight() / 2 - h / 2;
        g.fill(x, y, x + w, y + h, 0x90000000);
        g.fill(x, y, x + w, y + 1, 0xFF7FD8E8);
        g.drawString(font, title, x + 4, y + 4, 0x9FE3EA, true);
        g.drawString(font, progress, x + 4, y + 14, CACHE.complete ? 0x7CD36A : 0xAAAAAA, true);
        int ry = y + 25;
        int shown = 0;
        for (Map.Entry<Item, Integer> e : CACHE.needed.entrySet()) {
            if (shown++ >= rows) {
                break;
            }
            ItemStack stack = new ItemStack(e.getKey());
            int have = mc.player.getInventory().countItem(e.getKey());
            int need = e.getValue();
            g.renderItem(stack, x + 4, ry);
            String count = have + " / " + need;
            g.drawString(font, count, x + w - 4 - font.width(count), ry + 5, have >= need ? 0x7CD36A : 0xFF7060, true);
            String name = font.plainSubstrByWidth(stack.getHoverName().getString(), w - 30 - font.width(count));
            g.drawString(font, name, x + 23, ry + 5, 0xE0E0E0, true);
            ry += 18;
        }
    }
}
