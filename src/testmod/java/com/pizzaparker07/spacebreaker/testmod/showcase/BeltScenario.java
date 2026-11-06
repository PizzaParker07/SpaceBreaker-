package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.belt.BeltData;
import com.pizzaparker07.spacebreaker.content.belt.BeltModule;
import com.pizzaparker07.spacebreaker.content.belt.dungeon.MiningOutpostPiece;
import com.pizzaparker07.spacebreaker.content.belt.entity.BeltEntities;
import com.pizzaparker07.spacebreaker.content.belt.entity.RockMite;
import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.CraterFloorFeature;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.worldgen.Crater;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * The asteroid belt. Ceres: dark cratered plains under a small sun, a crater with bright salt faculae, a cryovolcanic
 * dome, the blocks and rock mites, and the abandoned mining outpost. Vesta: grey highlands, the Rheasilvia basin wall
 * and its central mountain, the blocks. Camera positions are worked out on the server after the chunks are generated,
 * because the surface features reshape the ground beyond the planet terrain function.
 */
final class BeltScenario implements ShowcaseScenario {
    private final boolean vesta;
    private final List<ShowcaseDriver.Shot> shots = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.concurrent.CountDownLatch built = new java.util.concurrent.CountDownLatch(1);
    static final int SX = 24;
    static final int SZ = 20;

    BeltScenario(boolean vesta) {
        this.vesta = vesta;
    }

    @Override
    public ResourceKey<Level> dimension() {
        return vesta ? BeltModule.VESTA : BeltModule.CERES;
    }

    private static Consumer<ServerPlayer> at(double f) {
        return p -> PlanetTime.setDayFraction(p.serverLevel(), f);
    }

    private static double ground(ServerLevel level, double x, double z) {
        level.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
    }

    private void shot(String name, double x, double y, double z, float yaw, float pitch, int ticks, Consumer<ServerPlayer> setup, Runnable client) {
        shots.add(new ShowcaseDriver.Shot(name, 0, x, y, z, yaw, pitch, true, CameraType.FIRST_PERSON, ticks, setup, client));
    }

    /** Camera at (x, z) some blocks above the real ground, looking at (tx, tz) at its ground level. */
    private void look(ServerLevel level, String name, double x, double z, double above, double tx, double tz, double f, int ticks, Runnable client) {
        double y = ground(level, x, z) + above;
        double ty = ground(level, tx, tz) + 1;
        shot(name, x + 0.5, y, z + 0.5, MoonScenario.yaw(tx - x, tz - z), MoonScenario.pitch(tx - x, ty - y, tz - z), ticks, at(f), client);
    }

    @Override
    public void build(ServerLevel level) {
        PlanetTerrain t = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        shots.clear();
        try {
            if (vesta) {
                buildVesta(level, t);
            } else {
                buildCeres(level, t);
            }
        } finally {
            built.countDown();
        }
    }

    // ------------------------------------------------------------------ Ceres

    private void buildCeres(ServerLevel level, PlanetTerrain t) {
        look(level, "01_ceres_plains", 0, 0, 14, 90, 60, 0.04, 240, MoonScenario::farView);
        int y0 = stage(level, BeltModule.CERES_REGOLITH.get().defaultBlockState(), BeltModule.CEREAN_ROCK.base().get().defaultBlockState(),
                new Block[]{BeltModule.CERES_REGOLITH.get(), BeltModule.AMMONIATED_CLAY.get(), BeltModule.CARBONATE_CRUST.get(),
                        BeltModule.CEREAN_ROCK.base().get(), BeltModule.CEREAN_ICE.get(), BeltModule.MAGNETITE_ORE.get()},
                new Block[]{BeltModule.CEREAN_ROCK.stairs().get(), BeltModule.CEREAN_ROCK.slab().get(), BeltModule.CEREAN_ROCK.wall().get()});
        mite(level, SX - 3, y0 + 1, SZ + 5, 160F, false);
        mite(level, SX + 1, y0 + 1, SZ + 6, 200F, false);
        mite(level, SX + 4, y0 + 1, SZ + 4, 230F, true);
        shot("02_blocks_and_mites", SX + 0.5, y0 + 3.4, SZ + 12.5, 180F, 16F, 120, at(0.12), null);
        shot("03_rock_mite", SX - 1.5, y0 + 2.0, SZ + 8.2, 160F, 26F, 60, at(0.12), null);

        CraterFloorFeature.Config fac = BeltData.faculae();
        Crater host = t.cratersNear(-2500, -2500, 2500, 2500).stream()
                .filter(c -> CraterFloorFeature.holds(c, fac) && c.radius() > 22 && Math.abs(t.latitude(c.z())) < 50)
                .min(Comparator.comparingDouble(c -> Math.hypot(c.x(), c.z()))).orElse(null);
        if (host != null) {
            double r = host.radius();
            look(level, "04_faculae_crater", host.x() + r * 1.05, host.z() + r * 0.35, 9, host.x(), host.z(), 0.12, 260, MoonScenario::farView);
            look(level, "05_faculae_close", host.x() + 8, host.z() + 6, 4, host.x(), host.z(), 0.2, 160, null);
        }
        double[] dome = MoonScenario.find(t, (x, z) -> t.column(x, z).volcanoS < 0.5, 6000);
        if (dome != null) {
            double[] c = dome;
            look(level, "06_cryovolcanic_dome", c[0] + 150, c[1] + 120, 10, c[0], c[1], 0.07, 260, MoonScenario::farView);
        }
        int ox = SX + 40;
        int oz = SZ - 30;
        int oy = (int) ground(level, ox + 14, oz + 14);
        MarsScenario.placePiece(level, new MiningOutpostPiece(31L, ox, oy, oz, Direction.NORTH));
        shot("07_mining_outpost", ox + 36.5, oy + 12, oz + 38.5, MoonScenario.yaw(-22, -24), 20F, 200, at(0.1), null);
        shot("08_outpost_drift", ox + 14.5, oy - MiningOutpostPiece.DEPTH + 1.6, oz + 7.5, 270F, 8F, 100, p -> {
            at(0.1).accept(p);
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0, false, false));
        }, null);
    }

    // ------------------------------------------------------------------ Vesta

    private void buildVesta(ServerLevel level, PlanetTerrain t) {
        look(level, "01_vesta_highlands", 0, 0, 12, 100, 70, 0.05, 240, MoonScenario::farView);
        int y0 = stage(level, BeltModule.VESTAN_REGOLITH.get().defaultBlockState(), BeltModule.EUCRITE.base().get().defaultBlockState(),
                new Block[]{BeltModule.VESTAN_REGOLITH.get(), BeltModule.EUCRITE.base().get(), BeltModule.DIOGENITE.get(),
                        BeltModule.CHROMITE_ORE.get(), BeltModule.EUCRITE_BRICKS.base().get(),
                        com.pizzaparker07.spacebreaker.content.mars.MarsModule.CARBONACEOUS_CHONDRITE.get()},
                new Block[]{BeltModule.EUCRITE_BRICKS.stairs().get(), BeltModule.EUCRITE_BRICKS.slab().get(), BeltModule.EUCRITE_BRICKS.wall().get()});
        mite(level, SX + 2, y0 + 1, SZ + 5, 200F, false);
        shot("02_vesta_blocks", SX + 0.5, y0 + 3.4, SZ + 12.5, 180F, 16F, 120, at(0.12), null);
        BasinFeature.Config b = BeltData.rheasilvia();
        double cz = BasinFeature.centreZ(t, b);
        // the central mountain from the basin floor, and the terraced wall from inside the basin
        double r = b.radius();
        look(level, "03_rheasilvia_peak", b.x() + r * 0.17, cz - r * 0.1, 3, b.x(), cz, 0.08, 300, () -> {
            MoonScenario.farView();
            net.minecraft.client.Minecraft.getInstance().options.renderDistance().set(16);
        });
        {
            int x = SX - 50;
            int z = SZ - 10;
            int y = (int) ground(level, x + 6, z + 13);
            var piece = new com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece(91L,
                    com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece.Variant.VESTA, x, y, z, Direction.NORTH);
            MarsScenario.placePiece(level, piece);
            BlockPos eye = piece.world(6, 9, -8);
            BlockPos target = piece.world(6, 5, 14);
            shot("05_crashed_lander", eye.getX() + 0.5, eye.getY(), eye.getZ() + 0.5, MoonScenario.yaw(target.getX() - eye.getX(), target.getZ() - eye.getZ()),
                    MoonScenario.pitch(target.getX() - eye.getX(), target.getY() - eye.getY(), target.getZ() - eye.getZ()), 160, at(0.12), null);
        }
        look(level, "04_rheasilvia_wall", b.x() - r * 0.6, cz - r * 0.55, 4, b.x() - r * 0.92, cz - r * 0.86, 0.1, 260, MoonScenario::farView);
    }

    // ------------------------------------------------------------------ helpers

    /** A flat stage with a row of natural blocks (two high) and a row of building shapes; returns its ground y. */
    private int stage(ServerLevel level, BlockState soil, BlockState rock, Block[] natural, Block[] shapes) {
        int y0 = (int) ground(level, SX, SZ) - 1;
        for (int x = SX - 9; x <= SX + 9; x++) {
            for (int z = SZ - 5; z <= SZ + 12; z++) {
                for (int y = y0 - 2; y <= y0 + 10; y++) {
                    level.setBlock(new BlockPos(x, y, z), y < y0 ? rock : y == y0 ? soil : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (int i = 0; i < natural.length; i++) {
            for (int y = 1; y <= 2; y++) {
                level.setBlock(new BlockPos(SX - 6 + i * 2, y0 + y, SZ - 3), natural[i].defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int i = 0; i < shapes.length; i++) {
            level.setBlock(new BlockPos(SX - 2 + i * 2, y0 + 1, SZ), shapes[i].defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (int x = SX - 8; x <= SX + 8; x++) {
            BlockPos p = new BlockPos(x, y0, SZ + 3);
            level.setBlock(p, LunarRegolithBlock.printFor(level.getBlockState(p), p, Direction.EAST), Block.UPDATE_CLIENTS);
        }
        return y0;
    }

    private static void mite(ServerLevel level, double x, double y, double z, float yaw, boolean hidden) {
        RockMite m = BeltEntities.ROCK_MITE.get().create(level);
        if (m != null) {
            m.moveTo(x + 0.5, y, z + 0.5, yaw, 0F);
            m.setYHeadRot(yaw);
            m.setYBodyRot(yaw);
            m.setNoAi(true);
            m.setPersistenceRequired();
            m.setSilent(true);
            m.setHidden(hidden);
            level.addFreshEntity(m);
        }
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        // build() runs on the server thread; shot positions depend on the generated ground
        try {
            built.await(180, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return List.copyOf(shots);
    }
}
