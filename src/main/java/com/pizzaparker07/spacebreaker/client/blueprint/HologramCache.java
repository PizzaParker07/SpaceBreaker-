package com.pizzaparker07.spacebreaker.client.blueprint;

import com.pizzaparker07.spacebreaker.content.blueprint.Hologram;
import com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition;
import com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The current projection, worked out once and refreshed a few times per second (not every frame): which
 * positions are missing (drawn as ghosts), which hold a wrong block (red frame) and which are done (hidden).
 */
final class HologramCache {
    record Ghost(BlockPos pos, BlockState state) {
    }

    static final int REFRESH_TICKS = 5;

    private Hologram key;
    private List<MultiblockDefinition.Placement> placements = List.of();
    private MultiblockDefinition def;
    private Rotation rotation = Rotation.NONE;
    private long lastRefresh = -1;
    private boolean dirty = true;

    final List<Ghost> missing = new ArrayList<>();
    final List<BlockPos> wrong = new ArrayList<>();
    /** Needed items for what is still missing, in placement order. */
    final Map<Item, Integer> needed = new LinkedHashMap<>();
    int total;
    int built;
    boolean complete;
    BlockPos min = BlockPos.ZERO;
    BlockPos max = BlockPos.ZERO;

    /** Returns true when the projection just became complete. */
    boolean update(ClientLevel level, Hologram h) {
        if (h == null || h.anchor().isEmpty()) {
            key = null;
            missing.clear();
            wrong.clear();
            needed.clear();
            return false;
        }
        boolean changed = !h.equals(key);
        if (changed) {
            key = h;
            def = Multiblocks.get(level.registryAccess(), h.blueprint()).orElse(null);
            placements = def == null ? List.of() : def.placements(h.anchor().get(), h.facing());
            rotation = MultiblockDefinition.rotation(h.facing());
            dirty = true;
            complete = false;
            bounds();
        }
        if (def == null || (!dirty && level.getGameTime() - lastRefresh < REFRESH_TICKS)) {
            return false;
        }
        lastRefresh = level.getGameTime();
        dirty = false;
        boolean wasComplete = complete && !changed;
        missing.clear();
        wrong.clear();
        needed.clear();
        total = placements.size();
        built = 0;
        for (MultiblockDefinition.Placement p : placements) {
            BlockState actual = level.getBlockState(p.pos());
            if (p.spec().test(actual, rotation)) {
                built++;
                continue;
            }
            boolean shown = h.layer() < 0 || h.layer() == p.layer();
            boolean free = actual.isAir() || actual.canBeReplaced();
            if (def.selfAssembling() && !def.isController(p)) {
                // built by the controller itself: only the space has to be free
                if (!free && shown) {
                    wrong.add(p.pos());
                }
                continue;
            }
            if (free) {
                if (shown && !p.spec().isAir()) {
                    missing.add(new Ghost(p.pos(), p.spec().display(rotation)));
                }
            } else if (shown) {
                wrong.add(p.pos());
            }
            Item item = p.spec().item();
            if (item != net.minecraft.world.item.Items.AIR) {
                needed.merge(item, 1, Integer::sum);
            }
        }
        complete = built == total && total > 0;
        return complete && !wasComplete && !changed;
    }

    private void bounds() {
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        for (MultiblockDefinition.Placement p : placements) {
            x0 = Math.min(x0, p.pos().getX());
            y0 = Math.min(y0, p.pos().getY());
            z0 = Math.min(z0, p.pos().getZ());
            x1 = Math.max(x1, p.pos().getX());
            y1 = Math.max(y1, p.pos().getY());
            z1 = Math.max(z1, p.pos().getZ());
        }
        if (!placements.isEmpty()) {
            min = new BlockPos(x0, y0, z0);
            max = new BlockPos(x1, y1, z1);
        }
    }

    Hologram key() {
        return key;
    }

    MultiblockDefinition def() {
        return def;
    }
}
