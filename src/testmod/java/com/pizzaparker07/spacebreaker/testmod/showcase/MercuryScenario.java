package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.mercury.IceMinePiece;
import com.pizzaparker07.spacebreaker.content.mercury.MercuryEntities;
import com.pizzaparker07.spacebreaker.content.mercury.MercuryModule;
import com.pizzaparker07.spacebreaker.content.mercury.TerminatorCrawler;
import com.pizzaparker07.spacebreaker.content.worldgen.Crater;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Mercury: a huge low sun over the dark crust, a lobate scarp, hollows, the crawler, and the polar ice mine. */
final class MercuryScenario implements ShowcaseScenario {
    private PlanetTerrain terrain;
    static final int SX = 30;
    static final int SZ = 30;

    @Override
    public ResourceKey<Level> dimension() {
        return MercuryModule.MERCURY;
    }

    private PlanetTerrain terrain() {
        if (terrain == null) {
            ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getLevel(MercuryModule.MERCURY);
            terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        }
        return terrain;
    }

    private int[] mineSpot(PlanetTerrain t) {
        double p = t.profile().poleDistance();
        Crater c = t.cratersNear(-1500, p * 0.88, 1500, p).stream()
                .filter(cr -> cr.radius() > 14 && Math.abs(t.latitude(cr.z())) > 83 && t.column(cr.x(), cr.z()).shadowed(t.profile()))
                .min(Comparator.comparingDouble(cr -> Math.abs(cr.x()))).orElse(null);
        return c == null ? new int[]{0, (int) (p * 0.95)} : new int[]{(int) c.x() - 12, (int) c.z() - 20};
    }

    @Override
    public void build(ServerLevel level) {
        terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        int y0 = (int) Math.floor(terrain.height(SX, SZ));
        for (int x = SX - 8; x <= SX + 8; x++) {
            for (int z = SZ - 4; z <= SZ + 10; z++) {
                for (int y = y0 - 2; y <= y0 + 8; y++) {
                    level.setBlock(new BlockPos(x, y, z), y < y0 ? MercuryModule.MERCURIAN_CRUST.get().defaultBlockState()
                            : y == y0 ? MercuryModule.MERCURY_REGOLITH.get().defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        Block[] show = {MercuryModule.MERCURY_REGOLITH.get(), MercuryModule.HOLLOW_DEPOSIT.get(), MercuryModule.MERCURIAN_CRUST.get(),
                MercuryModule.LOW_REFLECTANCE_ROCK.get(), MercuryModule.GRAPHITE_ORE.get()};
        for (int i = 0; i < show.length; i++) {
            level.setBlock(new BlockPos(SX - 5 + i * 2, y0 + 1, SZ - 2), show[i].defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(new BlockPos(SX - 5 + i * 2, y0 + 2, SZ - 2), show[i].defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        TerminatorCrawler c = MercuryEntities.TERMINATOR_CRAWLER.get().create(level);
        if (c != null) {
            c.moveTo(SX + 0.5, y0 + 1, SZ + 4.5, 20F, 0F);
            c.setYBodyRot(20F);
            c.setNoAi(true);
            c.setPersistenceRequired();
            level.addFreshEntity(c);
        }
        // a hollow snail grazing a patch of hollow deposit beside the crawler
        for (int x = SX - 6; x <= SX - 3; x++) {
            for (int z = SZ + 4; z <= SZ + 7; z++) {
                level.setBlock(new BlockPos(x, y0, z), MercuryModule.HOLLOW_DEPOSIT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        var snail = MercuryEntities.HOLLOW_SNAIL.get().create(level);
        if (snail != null) {
            snail.moveTo(SX - 4.5, y0 + 1, SZ + 5.5, 200F, 0F);
            snail.setYBodyRot(200F);
            snail.setYHeadRot(200F);
            snail.setNoAi(true);
            snail.setPersistenceRequired();
            level.addFreshEntity(snail);
        }
        int[] m = mineSpot(terrain);
        int my = (int) Math.floor(terrain.height(m[0] + 12, m[1] + 4));
        MarsScenario.placePiece(level, new IceMinePiece(31L, m[0], my, m[1]));
    }

    private static Consumer<ServerPlayer> at(double f) {
        return p -> PlanetTime.setDayFraction(p.serverLevel(), f);
    }

    private double ground(double x, double z) {
        return Math.floor(terrain().height(x, z)) + 1;
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        PlanetTerrain t = terrain();
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        double gy = ground(0.5, 0.5);
        s.add(new ShowcaseDriver.Shot("01_huge_sunrise", 0, 0.5, gy + 10, 0.5, -90F, -2F, true, fp, 220, at(0.02), MoonScenario::farView));
        s.add(new ShowcaseDriver.Shot("02_noon_crust", 0, 0.5, gy + 14, 0.5, 40F, 18F, true, fp, 140, at(0.22), null));
        double[] scarp = MoonScenario.find(t, (x, z) -> t.height(x, z) - t.height(x + 8, z) > 9, 3000);
        if (scarp != null) {
            s.add(new ShowcaseDriver.Shot("03_lobate_scarp", 0, scarp[0] + 30, ground(scarp[0] + 30, scarp[1]) + 4, scarp[1] + 6, 100F, 6F, true, fp, 220,
                    at(0.06), null));
        }
        double[] hollow = MoonScenario.find(t, (x, z) -> t.column(x, z).patch == 0, 3000);
        if (hollow != null) {
            s.add(new ShowcaseDriver.Shot("04_hollows", 0, hollow[0] - 8, ground(hollow[0], hollow[1]) + 7, hollow[1] - 8, -45F, 30F, true, fp, 180,
                    at(0.15), null));
        }
        int y0 = (int) Math.floor(t.height(SX, SZ));
        s.add(new ShowcaseDriver.Shot("05_crawler_and_rocks", 0, SX + 1.5, y0 + 3, SZ + 9.5, 180F, 22F, true, fp, 80, at(0.03), null));
        s.add(new ShowcaseDriver.Shot("05b_hollow_snail", 0, SX - 3.0, y0 + 2.4, SZ + 8.6, 155F, 24F, true, fp, 60, at(0.03), null));
        int[] m = mineSpot(t);
        int my = (int) Math.floor(t.height(m[0] + 12, m[1] + 4));
        s.add(new ShowcaseDriver.Shot("06_polar_crater", 0, m[0] + 12.5, my + 12, m[1] + 34.5, 180F, 20F, true, fp, 260, at(0.25), null));
        s.add(new ShowcaseDriver.Shot("07_ice_mine", 0, m[0] + 12.5, my - 14 + 2 + 1.6, m[1] + 16.5, 180F, 6F, true, fp, 100, p -> {
            PlanetTime.setDayFraction(p.serverLevel(), 0.25);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 2400, 0, false, false));
        }, null));
        s.add(new ShowcaseDriver.Shot("07b_ice_mine_melt_room", 0, m[0] + 7.5, my - 14 + 2 + 1.6, m[1] + 15.5, 120F, 8F, true, fp, 100, p -> {
            PlanetTime.setDayFraction(p.serverLevel(), 0.25);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 2400, 0, false, false));
        }, null));
        s.add(new ShowcaseDriver.Shot("07c_ice_mine_pithead", 0, m[0] + 20.5, my + 14, m[1] + 20.5, 155F, 30F, true, fp, 160, at(0.25), null));
        return s;
    }
}
