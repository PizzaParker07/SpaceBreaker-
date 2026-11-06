package com.pizzaparker07.spacebreaker.client.guide;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Structure snapshots for the guide. The multiblock definitions ({@code data/<ns>/spacebreaker/multiblock/<id>.json}, the same
 * data the Hologram Projector uses) come first; {@code assets/<ns>/guide/structures/<name>.nbt} (vanilla template format) is
 * the fallback for decorative scenes.
 */
public final class GuideStructures {
    public record Placed(BlockPos pos, BlockState state) {
    }

    /** multiblock: true when the snapshot comes from a multiblock definition (the projector can show it). */
    public record Snapshot(List<Placed> blocks, int sx, int sy, int sz, boolean multiblock) {
    }

    private static final Map<ResourceLocation, Snapshot> CACHE = new HashMap<>();

    static void clear() {
        CACHE.clear();
    }

    public static Snapshot get(ResourceLocation id) {
        Snapshot s = CACHE.get(id);
        if (s == null) {
            s = fromMultiblock(id);
            if (s == null) {
                s = load(id);
            }
            // multiblocks arrive with the world's data; don't cache a fallback made before that
            if (s.multiblock() || Minecraft.getInstance().level != null) {
                CACHE.put(id, s);
            }
        }
        return s;
    }

    private static @org.jetbrains.annotations.Nullable Snapshot fromMultiblock(ResourceLocation id) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        var def = com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks.get(level.registryAccess(), id).orElse(null);
        if (def == null) {
            return null;
        }
        List<Placed> blocks = new ArrayList<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        var placements = def.placements(BlockPos.ZERO, net.minecraft.core.Direction.NORTH);
        for (var p : placements) {
            minX = Math.min(minX, p.pos().getX());
            minY = Math.min(minY, p.pos().getY());
            minZ = Math.min(minZ, p.pos().getZ());
            maxX = Math.max(maxX, p.pos().getX());
            maxY = Math.max(maxY, p.pos().getY());
            maxZ = Math.max(maxZ, p.pos().getZ());
        }
        for (var p : placements) {
            if (p.spec().isAir()) {
                continue;
            }
            BlockState state = p.spec().display(net.minecraft.world.level.block.Rotation.NONE);
            if (!state.isAir()) {
                blocks.add(new Placed(p.pos().offset(-minX, -minY, -minZ), state));
            }
        }
        if (blocks.isEmpty()) {
            return null;
        }
        return new Snapshot(blocks, maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1, true);
    }

    /** True when a guide structure id names something that exists (multiblock or snapshot file). */
    public static boolean exists(ResourceLocation id) {
        return !get(id).blocks().isEmpty();
    }

    private static Snapshot load(ResourceLocation id) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "guide/structures/" + id.getPath() + ".nbt");
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(file)) {
            CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            ListTag size = tag.getList("size", Tag.TAG_INT);
            ListTag palette = tag.getList("palette", Tag.TAG_COMPOUND);
            List<BlockState> states = new ArrayList<>();
            for (int i = 0; i < palette.size(); i++) {
                states.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palette.getCompound(i)));
            }
            List<Placed> blocks = new ArrayList<>();
            ListTag list = tag.getList("blocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag b = list.getCompound(i);
                ListTag pos = b.getList("pos", Tag.TAG_INT);
                BlockState state = states.get(b.getInt("state"));
                if (!state.isAir()) {
                    blocks.add(new Placed(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), state));
                }
            }
            return new Snapshot(blocks, size.getInt(0), size.getInt(1), size.getInt(2), false);
        } catch (Exception e) {
            Spacebreaker.LOGGER.warn("Guide structure {} missing: {}", file, e.toString());
            return new Snapshot(List.of(), 1, 1, 1, false);
        }
    }

    /** Draws a slowly turning structure centred in a box (book coordinates). */
    public static void render(GuiGraphics g, Snapshot s, int x, int y, int w, int h, float time) {
        if (s.blocks().isEmpty()) {
            return;
        }
        float diag = (float) Math.sqrt(s.sx() * s.sx() + s.sz() * s.sz());
        float fit = Math.min(w / (diag * 1.05F), h / ((s.sy() + diag * 0.5F) * 0.95F));
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x + w / 2F, y + h / 2F + s.sy() * fit * 0.15F, 150);
        pose.scale(fit, -fit, fit);
        pose.mulPose(Axis.XP.rotationDegrees(28));
        pose.mulPose(Axis.YP.rotationDegrees(time * 0.6F % 360F + 45F));
        pose.translate(-s.sx() / 2F, -s.sy() / 2F, -s.sz() / 2F);
        Lighting.setupFor3DItems();
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        for (Placed p : s.blocks()) {
            pose.pushPose();
            pose.translate(p.pos().getX(), p.pos().getY(), p.pos().getZ());
            dispatcher.renderSingleBlock(p.state(), pose, g.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        g.flush();
        pose.popPose();
        Lighting.setupForFlatItems();
    }

    private GuideStructures() {
    }
}
