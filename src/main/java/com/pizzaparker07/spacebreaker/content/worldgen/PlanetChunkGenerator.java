package com.pizzaparker07.spacebreaker.content.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Chunk generator for rocky, airless (or thin-air) worlds, driven by a {@link TerrainProfile}.
 * Heights come from {@link PlanetTerrain}; the crust is layered soil over rock with bare rock on steep
 * slopes, breccia lenses, bright crater rays, volcanic glass patches and ice in permanently shadowed
 * polar craters. Fully deterministic for the world seed and safe to run on worldgen threads.
 */
public class PlanetChunkGenerator extends ChunkGenerator {
    public static final com.mojang.serialization.Codec<Holder<TerrainProfile>> PROFILE_CODEC =
            RegistryFileCodec.create(TerrainProfile.REGISTRY, TerrainProfile.CODEC);

    public static final MapCodec<PlanetChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
            PROFILE_CODEC.fieldOf("terrain").forGetter(PlanetChunkGenerator::profileHolder)
    ).apply(i, i.stable(PlanetChunkGenerator::new)));

    private final Holder<TerrainProfile> profile;
    private volatile PlanetTerrain terrain;

    public PlanetChunkGenerator(BiomeSource biomeSource, Holder<TerrainProfile> profile) {
        super(biomeSource);
        this.profile = profile;
        if (biomeSource instanceof PlanetBiomeSource planet) {
            planet.attach(this::terrain);
        }
    }

    public Holder<TerrainProfile> profileHolder() {
        return profile;
    }

    /** The seeded terrain (seed 0 until the world binds it). */
    public PlanetTerrain terrain() {
        PlanetTerrain t = terrain;
        if (t == null) {
            synchronized (this) {
                if (terrain == null) {
                    terrain = new PlanetTerrain(profile.value(), 0L);
                }
                t = terrain;
            }
        }
        return t;
    }

    /** Binds the world seed; called once by the chunk map before anything generates. */
    public void bindSeed(long worldSeed) {
        TerrainProfile p = profile.value();
        long s = PlanetTerrain.mix(worldSeed, p.body().toString().hashCode());
        PlanetTerrain current = terrain;
        if (current == null || current.seed() != s) {
            terrain = new PlanetTerrain(p, s);
        }
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSets, RandomState randomState, long seed) {
        bindSeed(seed);
        return super.createState(structureSets, randomState, seed);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ------------------------------------------------------------------ terrain

    /** Surface heights and materials of one chunk (with a 1-block border for slopes). */
    record ChunkPlan(int[] heights, PlanetTerrain.Column[] columns) {
        static final int W = 18;

        int height(int lx, int lz) {
            return heights[(lz + 1) * W + lx + 1];
        }

        PlanetTerrain.Column column(int lx, int lz) {
            return columns[(lz + 1) * W + lx + 1];
        }

        int slope(int lx, int lz) {
            int h = height(lx, lz);
            int s = 0;
            s = Math.max(s, Math.abs(height(lx + 1, lz) - h));
            s = Math.max(s, Math.abs(height(lx - 1, lz) - h));
            s = Math.max(s, Math.abs(height(lx, lz + 1) - h));
            s = Math.max(s, Math.abs(height(lx, lz - 1) - h));
            return s;
        }
    }

    ChunkPlan plan(int chunkX, int chunkZ, int minY, int maxY) {
        PlanetTerrain t = terrain();
        int x0 = (chunkX << 4) - 1;
        int z0 = (chunkZ << 4) - 1;
        List<Crater> craters = t.cratersNear(x0, z0, x0 + ChunkPlan.W - 1, z0 + ChunkPlan.W - 1);
        int[] heights = new int[ChunkPlan.W * ChunkPlan.W];
        PlanetTerrain.Column[] cols = new PlanetTerrain.Column[heights.length];
        for (int dz = 0; dz < ChunkPlan.W; dz++) {
            for (int dx = 0; dx < ChunkPlan.W; dx++) {
                PlanetTerrain.Column c = new PlanetTerrain.Column();
                t.sample(x0 + dx, z0 + dz, craters, c);
                int i = dz * ChunkPlan.W + dx;
                cols[i] = c;
                heights[i] = Mth.clamp(Mth.floor(c.height), minY + 6, maxY - 12);
            }
        }
        return new ChunkPlan(heights, cols);
    }

    /** Fills one column top-down into {@code sink}. */
    private void column(int wx, int wz, int top, int slope, PlanetTerrain.Column c, int minY, ColumnSink sink) {
        PlanetTerrain t = terrain();
        TerrainProfile p = t.profile();
        TerrainProfile.Materials mat = p.materials();
        Landforms lf = p.landforms();
        long h = PlanetTerrain.mix(t.seed() ^ 0xC0FFEEL, PlanetTerrain.mix(wx, wz));
        double jitter = (PlanetTerrain.unit(h, 0) - 0.5) * 0.18;
        boolean lowland = c.lowland + jitter > 0.5;
        boolean shadowed = c.shadowed(p) && mat.polarSoil().isPresent();
        boolean wall = c.anyCraterS > 0.5 && c.anyCraterS < 1.08 && slope >= 2;

        BlockState soil;
        if (shadowed) {
            soil = mat.polarSoil().get();
        } else if (c.volcanic && mat.volcanicSoil().isPresent()) {
            soil = mat.volcanicSoil().get();
        } else if (c.ray + jitter > 0.45 && PlanetTerrain.unit(h, 1) < 0.92) {
            soil = mat.raySoil();
        } else {
            soil = lowland ? mat.lowlandSoil() : mat.highlandSoil();
        }
        int soilDepth = slope >= 4 ? 0 : slope >= 3 ? 1 : mat.soilDepth() - (PlanetTerrain.unit(h, 2) < 0.4 ? 1 : 0);
        int iceDepth = shadowed && mat.ice().isPresent() ? p.poles().iceDepth() : 0;
        int bedrockTop = minY + (int) (PlanetTerrain.unit(h, 3) * 4);
        BlockState lowRock = mat.lowlandRock();
        BlockState highRock = mat.highlandRock();
        BlockState breccia = mat.breccia();
        boolean strata = lf.strata().isPresent();

        // ---- optional landforms: soils and rock of volcanoes, valleys, dunes, rifts, ice caps
        int capDepth = 0;
        if (!shadowed && !c.volcanic && c.ray + jitter <= 0.45) {
            if (c.patch >= 0) {
                soil = lf.patches().get(c.patch).soil();
            }
            if (lf.valleys().isPresent()) {
                Landforms.Valleys v = lf.valleys().get();
                if (c.valley + jitter > 0.5) {
                    soil = v.floorSoil();
                } else if (c.delta + jitter > 0.45 && v.deltaSoil().isPresent()) {
                    soil = v.deltaSoil().get();
                }
            }
            if (c.rift < 0.45 && lf.rift().flatMap(Landforms.Rift::floorSoil).isPresent()) {
                soil = lf.rift().get().floorSoil().get();
            }
        }
        if (c.volcanoS < 1.0 + jitter && lf.volcanoes().isPresent()) {
            Landforms.Volcanoes v = lf.volcanoes().get();
            lowRock = highRock = v.rock();
            strata = false;
            if (v.soil().isPresent() && !shadowed) {
                soil = v.soil().get();
            }
            if (c.volcanoS < v.caldera() * 1.1) {
                soilDepth = Math.min(soilDepth, 1);
            }
        }
        if (c.dune > 0.35 + jitter && lf.dunes().isPresent()) {
            Landforms.Dunes d = lf.dunes().get();
            soil = c.altDune && d.altSoil().isPresent() ? d.altSoil().get() : d.soil();
            soilDepth = Math.max(mat.soilDepth(), (int) Math.ceil(c.duneHeight) + 2);
        }
        if (c.rift > 0.42 && c.rift < 1 && slope >= 2) {
            soilDepth = 0; // rift walls: bare layered rock
        }
        if (c.cap >= 1 && lf.iceCap().isPresent()) {
            capDepth = (int) c.cap;
        }
        BlockState surfaceRock = wall || (c.breccia > 0.45 && !lowland) ? breccia : lowland ? lowRock : highRock;

        for (int y = top; y >= minY; y--) {
            int depth = top - y;
            BlockState state;
            if (y <= bedrockTop) {
                state = mat.bedrock();
            } else if (depth < capDepth) {
                Landforms.IceCap cap = lf.iceCap().get();
                if (depth == 0 && c.frost && cap.frost().isPresent()) {
                    state = cap.frost().get();
                } else if (cap.dustyIce().isPresent() && Math.floorMod(y + (int) (c.trough * 0.5), 5) == 0) {
                    state = cap.dustyIce().get();
                } else {
                    state = cap.ice();
                }
            } else {
                int d = depth - capDepth;
                if (d < soilDepth) {
                    state = soil;
                } else if (d < soilDepth + iceDepth) {
                    state = mat.ice().get();
                } else if (d < soilDepth + 2 && !(strata && surfaceRock == highRock)) {
                    state = surfaceRock;
                } else if (lowland && d < mat.lowlandRockDepth()) {
                    state = lowRock;
                } else if (d < 12 && c.breccia > 0.25 && !lowland && !strata) {
                    state = breccia;
                } else if (strata && highRock == mat.highlandRock()) {
                    state = t.stratum(wx, y, wz);
                } else {
                    state = highRock;
                }
            }
            sink.set(y, state);
        }
    }

    @FunctionalInterface
    private interface ColumnSink {
        void set(int y, BlockState state);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structures, ChunkAccess chunk) {
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();
        ChunkPlan plan = plan(chunk.getPos().x, chunk.getPos().z, minY, maxY);
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int top = plan.height(lx, lz);
                final int fx = lx;
                final int fz = lz;
                column(baseX + lx, baseZ + lz, top, plan.slope(lx, lz), plan.column(lx, lz), minY, (y, state) -> {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
                    section.setBlockState(fx, y & 15, fz, state, false);
                });
                BlockState topState = chunk.getBlockState(new BlockPos(lx, top, lz));
                ocean.update(lx, top, lz, topState);
                surface.update(lx, top, lz, topState);
            }
        }
        scatterEjecta(chunk, plan, baseX, baseZ, ocean, surface);
        return CompletableFuture.completedFuture(chunk);
    }

    /**
     * Ejecta blocks: loose rocks thrown out by impacts, thickest just outside crater rims.
     * Placed inside the chunk only, from a per-column hash, so it stays deterministic.
     */
    private void scatterEjecta(ChunkAccess chunk, ChunkPlan plan, int baseX, int baseZ, Heightmap ocean, Heightmap surface) {
        TerrainProfile.Materials mat = terrain().profile().materials();
        long seed = terrain().seed() ^ 0xE1EC7AL;
        for (int lz = 0; lz < 15; lz++) {
            for (int lx = 0; lx < 15; lx++) {
                PlanetTerrain.Column c = plan.column(lx, lz);
                if (c.anyCraterRadius < 6 || c.anyCraterS < 0.85 || c.anyCraterS > 1.9 || c.shadowed(terrain().profile())) {
                    continue;
                }
                long h = PlanetTerrain.mix(seed, PlanetTerrain.mix(baseX + lx, baseZ + lz));
                double density = 0.03 * (1.9 - c.anyCraterS) * Math.min(1.5, c.anyCraterRadius / 20);
                if (PlanetTerrain.unit(h, 0) >= density) {
                    continue;
                }
                double pick = PlanetTerrain.unit(h, 1);
                BlockState rock = pick < 0.45 ? mat.breccia() : (c.lowland > 0.5 ? mat.lowlandRock() : mat.highlandRock());
                int[][] cells = PlanetTerrain.unit(h, 2) < 0.5 ? new int[][]{{0, 0}} : new int[][]{{0, 0}, {1, 0}, {0, 1}};
                for (int[] d : cells) {
                    int x = lx + d[0];
                    int z = lz + d[1];
                    int y = plan.height(x, z) + 1;
                    if (Math.abs(plan.height(x, z) - plan.height(lx, lz)) > 1) {
                        continue;
                    }
                    chunk.getSection(chunk.getSectionIndex(y)).setBlockState(x, y & 15, z, rock, false);
                    ocean.update(x, y, z, rock);
                    surface.update(x, y, z, rock);
                }
            }
        }
    }

    /** Surface height (y of the top solid block) at a world column. */
    public int surfaceY(int x, int z, LevelHeightAccessor level) {
        return Mth.clamp(Mth.floor(terrain().height(x, z)), level.getMinBuildHeight() + 6, level.getMaxBuildHeight() - 12);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return surfaceY(x, z, level) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int minY = level.getMinBuildHeight();
        BlockState[] states = new BlockState[level.getHeight()];
        java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
        PlanetTerrain.Column c = terrain().column(x, z);
        int top = surfaceY(x, z, level);
        int slope = 0;
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            slope = Math.max(slope, Math.abs(surfaceY(x + d[0], z + d[1], level) - top));
        }
        column(x, z, top, slope, c, minY, (y, state) -> states[y - minY] = state);
        return new NoiseColumn(minY, states);
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        // the crust is layered in fillFromNoise
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        // no carvers: caves on airless worlds are lava tubes, placed as structures
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return terrain().profile().baseHeight() + 1;
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        PlanetTerrain t = terrain();
        PlanetTerrain.Column c = t.column(pos.getX(), pos.getZ());
        info.add(String.format(Locale.ROOT, "Planet: lat %.2f lon %.2f zone %s lowland %.2f crater %.2f%s",
                c.latitude, t.longitude(pos.getX()), c.zone(t.profile()).name().toLowerCase(Locale.ROOT), c.lowland,
                Math.min(c.anyCraterS, 9.99), c.shadowed(t.profile()) ? " shadowed" : ""));
        if (!t.profile().landforms().equals(Landforms.NONE)) {
            info.add(String.format(Locale.ROOT, "Landforms: dune %.2f valley %.2f delta %.2f volcano %.2f rift %.2f cap %.1f patch %d",
                    c.dune, c.valley, c.delta, Math.min(c.volcanoS, 9.99), Math.min(c.rift, 9.99), c.cap, c.patch));
        }
    }
}
