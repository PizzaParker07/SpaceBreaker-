package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Generation scan of the outer worlds (dev only, no pictures that matter): for a block of fresh chunks in each world it
 * counts blocks floating free of everything (all six neighbours air) and steps in the surface that sit exactly on a chunk
 * border while the ground on both sides is smooth (a seam). Results go to the log as "[scan] ...". Run with
 * -Pscenario=scan_outer.
 */
final class OuterScanScenario implements ShowcaseScenario {
    private static final String[] WORLDS = {"titan", "triton", "charon", "pluto", "europa", "ganymede", "callisto", "io", "enceladus", "miranda",
            "ariel", "titania", "umbriel", "oberon", "ceres", "vesta", "proteus", "nereid", "nix", "hydra", "bennu", "ryugu", "eros", "itokawa",
            "churyumov_gerasimenko", "encke", "halley", "tempel_1"};
    /** Chunks scanned per world: (2R+1)^2 around two spots. */
    private static final int R = 3;

    @Override
    public void build(ServerLevel level) {
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        String only = System.getenv("SCAN_WORLDS");
        for (String world : only != null && !only.isBlank() ? only.split(",") : WORLDS) {
            s.add(new ShowcaseDriver.Shot("scan_" + world, 6000, 0, 0, 0, 0F, 0F, true, CameraType.FIRST_PERSON, 600, p -> {
                ServerLevel level = p.server.getLevel(ResourceKey.create(Registries.DIMENSION, Spacebreaker.id(world)));
                if (level == null) {
                    ShowcaseDriver.log("[scan] no world " + world);
                    return;
                }
                for (int[] spot : new int[][]{{0, 0}, {700, -400}}) {
                    scan(level, world, spot[0], spot[1]);
                }
            }, () -> { }));
        }
        return s;
    }

    private static void scan(ServerLevel level, String world, int x0, int z0) {
        int cx0 = x0 >> 4;
        int cz0 = z0 >> 4;
        // generate a ring of extra chunks so that every scanned chunk has its features from all neighbours
        for (int cx = -R - 1; cx <= R + 1; cx++) {
            for (int cz = -R - 1; cz <= R + 1; cz++) {
                level.getChunk(cx0 + cx, cz0 + cz);
            }
        }
        if (System.getenv("SCAN_HASH") != null) {
            // one hash of every block state per scanned chunk, to compare two generations of the same chunks
            for (int cx = -R; cx <= R; cx++) {
                for (int cz = -R; cz <= R; cz++) {
                    var chunk = level.getChunk(cx0 + cx, cz0 + cz);
                    long hash = 1125899906842597L;
                    for (var section : chunk.getSections()) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                for (int x = 0; x < 16; x++) {
                                    hash = 31 * hash + net.minecraft.world.level.block.Block.getId(section.getBlockState(x, y, z));
                                }
                            }
                        }
                    }
                    ShowcaseDriver.log("[hash] " + world + " " + (cx0 + cx) + " " + (cz0 + cz) + " " + Long.toHexString(hash));
                    String dump = System.getenv("SCAN_DUMP");
                    if (dump != null && dump.equals(world + ":" + (cx0 + cx) + ":" + (cz0 + cz))) {
                        // a hash per 16-block section (to find the height that differs), then every block near the surface
                        var secs = chunk.getSections();
                        for (int si = 0; si < secs.length; si++) {
                            long sh = 7;
                            java.util.Map<String, Integer> count = new java.util.TreeMap<>();
                            for (int y = 0; y < 16; y++) {
                                for (int z = 0; z < 16; z++) {
                                    for (int x = 0; x < 16; x++) {
                                        var st = secs[si].getBlockState(x, y, z);
                                        sh = 31 * sh + net.minecraft.world.level.block.Block.getId(st);
                                        String id = st.getBlock().getDescriptionId();
                                        count.merge(id.substring(id.lastIndexOf('.') + 1), 1, Integer::sum);
                                    }
                                }
                            }
                            ShowcaseDriver.log("[section] " + (chunk.getMinBuildHeight() + si * 16) + " " + Long.toHexString(sh) + " " + count);
                        }
                        // every non-air block above the lowest surface of the chunk, to see what differs between two runs
                        int bx = (cx0 + cx) << 4;
                        int bz = (cz0 + cz) << 4;
                        int low = Integer.MAX_VALUE;
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                low = Math.min(low, level.getHeight(Heightmap.Types.WORLD_SURFACE, bx + x, bz + z));
                            }
                        }
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                StringBuilder col = new StringBuilder();
                                for (int y = low - 6; y < level.getHeight(Heightmap.Types.WORLD_SURFACE, bx + x, bz + z); y++) {
                                    var st = chunk.getBlockState(new BlockPos(bx + x, y, bz + z));
                                    String id = st.getBlock().getDescriptionId();
                                    col.append(y).append('=').append(id.substring(id.lastIndexOf('.') + 1)).append(' ');
                                }
                                ShowcaseDriver.log("[dump] " + (((cx0 + cx) << 4) + x) + " " + (((cz0 + cz) << 4) + z) + " " + col);
                            }
                        }
                    }
                }
            }
        }
        int minX = (cx0 - R) << 4;
        int minZ = (cz0 - R) << 4;
        int size = (2 * R + 1) << 4;
        int[][] h = new int[size][size];
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                h[x][z] = level.getHeight(Heightmap.Types.WORLD_SURFACE, minX + x, minZ + z);
            }
        }
        int floating = 0;
        String firstFloat = "";
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                // only look above the lowest ground nearby: free blocks in the air, not in caves
                for (int y = h[x][z] - 1; y > h[x][z] - 40 && y > level.getMinBuildHeight(); y--) {
                    p.set(minX + x, y, minZ + z);
                    if (level.getBlockState(p).isAir()) {
                        continue;
                    }
                    boolean free = true;
                    for (Direction d : Direction.values()) {
                        if (!level.getBlockState(q.setWithOffset(p, d)).isAir()) {
                            free = false;
                            break;
                        }
                    }
                    if (free) {
                        floating++;
                        if (firstFloat.isEmpty()) {
                            firstFloat = " first at " + p.toShortString() + " " + level.getBlockState(p).getBlock().getDescriptionId();
                        }
                    }
                }
            }
        }
        // seams: a step across a chunk border that is much bigger than the steps just inside it on both sides
        int seams = 0;
        String firstSeam = "";
        for (int b = 16; b < size; b += 16) {
            for (int t = 2; t < size - 2; t++) {
                // border between x = b-1 and x = b (along z = t), and between z = b-1 and z = b (along x = t)
                for (int axis = 0; axis < 2; axis++) {
                    int a1 = axis == 0 ? h[b - 1][t] : h[t][b - 1];
                    int a2 = axis == 0 ? h[b][t] : h[t][b];
                    int in1 = axis == 0 ? Math.abs(h[b - 1][t] - h[b - 2][t]) : Math.abs(h[t][b - 1] - h[t][b - 2]);
                    int in2 = axis == 0 ? Math.abs(h[b + 1][t] - h[b][t]) : Math.abs(h[t][b + 1] - h[t][b]);
                    if (Math.abs(a1 - a2) >= 4 && in1 <= 1 && in2 <= 1) {
                        seams++;
                        if (firstSeam.isEmpty()) {
                            int wx = minX + (axis == 0 ? b : t);
                            int wz = minZ + (axis == 0 ? t : b);
                            firstSeam = " first at " + wx + " " + wz + " (" + a1 + " -> " + a2 + ")";
                        }
                        if (System.getenv("SCAN_DETAIL") != null && seams <= 3) {
                            StringBuilder row = new StringBuilder();
                            for (int k = -4; k <= 3; k++) {
                                int xx = axis == 0 ? b + k : t;
                                int zz = axis == 0 ? t : b + k;
                                if (xx < 0 || zz < 0 || xx >= size || zz >= size) {
                                    continue;
                                }
                                var top = level.getBlockState(new BlockPos(minX + xx, h[xx][zz] - 1, minZ + zz)).getBlock().getDescriptionId();
                                row.append(h[xx][zz]).append(':').append(top.substring(top.lastIndexOf('.') + 1)).append(' ');
                            }
                            ShowcaseDriver.log("[scan]   " + world + " seam " + (axis == 0 ? "x" : "z") + " at " + (minX + (axis == 0 ? b : t)) + ","
                                    + (minZ + (axis == 0 ? t : b)) + ": " + row);
                        }
                    }
                }
            }
        }
        ShowcaseDriver.log(String.format("[scan] %s @%d,%d: %d floating blocks%s; %d chunk-border steps%s", world, x0, z0, floating, firstFloat,
                seams, firstSeam));
    }
}
