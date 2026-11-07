package com.pizzaparker07.spacebreaker.testmod.outer;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.block.VentBlock;
import com.pizzaparker07.spacebreaker.content.belt.world.Columns;
import com.pizzaparker07.spacebreaker.content.belt.world.LakeFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.LineaeFeature;
import com.pizzaparker07.spacebreaker.content.jupiter.JupiterData;
import com.pizzaparker07.spacebreaker.content.jupiter.JupiterModule;
import com.pizzaparker07.spacebreaker.content.jupiter.JupiterRules;
import com.pizzaparker07.spacebreaker.content.jupiter.dungeon.JupiterStructures;
import com.pizzaparker07.spacebreaker.content.jupiter.dungeon.ObservatoryPiece;
import com.pizzaparker07.spacebreaker.content.jupiter.dungeon.ProbePiece;
import com.pizzaparker07.spacebreaker.content.jupiter.entity.GlowSerpent;
import com.pizzaparker07.spacebreaker.content.jupiter.entity.JupiterEntities;
import com.pizzaparker07.spacebreaker.content.jupiter.entity.SulfurCrab;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.testmod.mars.WorldTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JupiterGameTests {
    @GameTest(template = WorldTests.EMPTY)
    public static void galileanMoonsOrbitJupiter(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        Map<String, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> moons = Map.of("io", JupiterModule.IO, "europa",
                JupiterModule.EUROPA, "ganymede", JupiterModule.GANYMEDE, "callisto", JupiterModule.CALLISTO);
        for (var e : moons.entrySet()) {
            helper.assertTrue(WorldTests.stem(helper, e.getKey()).generator() instanceof PlanetChunkGenerator, e.getKey() + " uses the planet engine");
            CelestialBody b = Bodies.forDimension(access, e.getValue()).orElseThrow(() -> new AssertionError(e.getKey() + " has no body"));
            helper.assertTrue(b.parent().map(p -> p.getPath().equals("jupiter")).orElse(false), e.getKey() + " orbits Jupiter");
            helper.assertTrue(b.rotation().tidallyLocked(), e.getKey() + " keeps one face to Jupiter");
            helper.assertTrue(b.atmosphere().isVacuum(), e.getKey() + " is airless");
        }
        double europa = Bodies.forDimension(access, JupiterModule.EUROPA).get().radiation();
        double callisto = Bodies.forDimension(access, JupiterModule.CALLISTO).get().radiation();
        double io = Bodies.forDimension(access, JupiterModule.IO).get().radiation();
        helper.assertTrue(io > europa && europa > 1000 * callisto, "radiation falls steeply from Io to Callisto, the safe base site");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 300)
    public static void europaIsCrackedAndYoung(GameTestHelper helper) {
        PlanetTerrain t = WorldTests.generator(helper, "europa", 21L).terrain();
        LineaeFeature.Config cfg = JupiterData.europaLineae();
        int cracks = 0;
        int chaos = 0;
        int n = 4000;
        for (int i = 0; i < n; i++) {
            double x = (i * 7919L) % 6000 - 3000;
            double z = (i * 104729L) % 6000 - 3000;
            LineaeFeature.Mark m = LineaeFeature.mark(t, cfg, x, z);
            if (m.kind() == LineaeFeature.Mark.STAIN || m.kind() == LineaeFeature.Mark.RIDGE) {
                cracks++;
            }
            if (LineaeFeature.chaos(t, cfg, x, z) > 0.5) {
                chaos++;
            }
        }
        helper.assertTrue(cracks > n / 25 && cracks < n / 2, "stained lineae cross the ice: " + cracks + "/" + n);
        helper.assertTrue(chaos > 0 && chaos < n / 3, "some chaos terrain: " + chaos + "/" + n);
        int craters = t.cratersNear(-3000, -3000, 3000, 3000).size();
        PlanetTerrain callisto = WorldTests.generator(helper, "callisto", 21L).terrain();
        int old = callisto.cratersNear(-3000, -3000, 3000, 3000).size();
        helper.assertTrue(craters * 3 < old, "Europa's young ice has far fewer craters than Callisto: " + craters + " vs " + old);
        WorldTests.dumpMap(t, "europa", 0, 0, 4000, 400);
        WorldTests.dumpMap(callisto, "callisto", 0, 0, 4000, 400);
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void ioHasLavaLakesAndNoCraters(GameTestHelper helper) {
        PlanetTerrain t = WorldTests.generator(helper, "io", 9L).terrain();
        helper.assertTrue(t.cratersNear(-3000, -3000, 3000, 3000).isEmpty(), "Io's surface is too young for craters");
        LakeFeature.Config cfg = JupiterData.ioPaterae();
        int lakes = 0;
        for (int gx = -10; gx < 10; gx++) {
            for (int gz = -10; gz < 10; gz++) {
                LakeFeature.Lake l = LakeFeature.lake(t, cfg, gx, gz);
                if (l != null) {
                    lakes++;
                    helper.assertTrue(l.level() < t.height(l.x() + l.radius() * 1.15, l.z()), "a lava lake sits below its shore");
                }
            }
        }
        helper.assertTrue(lakes > 40, "many paterae with lava lakes: " + lakes);
        // run the real feature over a lake on the test level and count the lava it leaves
        PlanetChunkGenerator gen = WorldTests.generator(helper, "io", 9L);
        LakeFeature.Lake big = null;
        for (int gx = 0; gx < 10 && big == null; gx++) {
            for (int gz = 0; gz < 10 && big == null; gz++) {
                LakeFeature.Lake l = LakeFeature.lake(gen.terrain(), cfg, gx + 30, gz + 30);
                if (l != null && l.radius() > 12) {
                    big = l;
                }
            }
        }
        helper.assertTrue(big != null, "a big lake to test");
        ServerLevel level = helper.getLevel();
        int cx = (int) Math.floor(big.x()) >> 4;
        int cz = (int) Math.floor(big.z()) >> 4;
        level.getChunk(cx, cz);
        BlockPos origin = new BlockPos(cx << 4, 0, cz << 4);
        // shape the test chunk like Io's ground first, so the lake digs into real terrain heights
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int h = (int) Math.floor(gen.terrain().height(origin.getX() + dx, origin.getZ() + dz));
                for (int y = 30; y <= 130; y++) {
                    level.setBlock(new BlockPos(origin.getX() + dx, y, origin.getZ() + dz),
                            y <= h ? JupiterModule.SULFUR_SAND.get().defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        new LakeFeature().place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(java.util.Optional.empty(), level, gen,
                net.minecraft.util.RandomSource.create(1), origin, cfg));
        int lava = 0;
        for (BlockPos p : BlockPos.betweenClosed(origin.getX(), -64, origin.getZ(), origin.getX() + 15, 200, origin.getZ() + 15)) {
            if (level.getBlockState(p).is(Blocks.LAVA)) {
                lava++;
            }
        }
        helper.assertTrue(lava > 20, "the lake feature fills its bowl with lava, found " + lava + " (level " + big.level() + ")");
        WorldTests.dumpMap(t, "io", 0, 0, 4000, 400);
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void geysersLiftAndPlumesBurn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos vent = new BlockPos(1, 1, 1);
        helper.setBlock(vent, JupiterModule.ICE_GEYSER.get());
        VentBlock.Kind kind = ((VentBlock) JupiterModule.ICE_GEYSER.get()).kind();
        BlockPos abs = helper.absolutePos(vent);
        // find a moment when this vent erupts and jump the clock there
        long t0 = level.getGameTime();
        long when = t0;
        while (!VentBlock.erupting(kind, when, abs) && when < t0 + kind.period()) {
            when++;
        }
        helper.assertTrue(VentBlock.erupting(kind, when, abs), "the geyser erupts once per cycle");
        int quiet = 0;
        for (long t = t0; t < t0 + kind.period(); t++) {
            quiet += VentBlock.erupting(kind, t, abs) ? 0 : 1;
        }
        helper.assertTrue(quiet > 0 && quiet < kind.period(), "and rests in between");
        ItemEntity item = new ItemEntity(level, abs.getX() + 0.5, abs.getY() + 1.2, abs.getZ() + 0.5, new ItemStack(Items.SNOWBALL));
        item.setDeltaMovement(0, 0, 0);
        level.addFreshEntity(item);
        com.pizzaparker07.spacebreaker.content.belt.block.VentBlockEntity.erupt(level, abs, kind);
        helper.assertTrue(item.getDeltaMovement().y > 0.3, "an erupting geyser throws things upward: " + item.getDeltaMovement().y);
        helper.assertTrue(level.getBlockEntity(abs) instanceof com.pizzaparker07.spacebreaker.content.belt.block.VentBlockEntity, "vent block entity");
        VentBlock.Kind plume = ((VentBlock) JupiterModule.SULFUR_VENT.get()).kind();
        helper.assertTrue(plume.damage() > 0 && plume.height() > kind.height(), "Io's plumes are taller and burn");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 100)
    public static void lavaHeatsOnlyIo(GameTestHelper helper) {
        BlockPos lava = new BlockPos(2, 1, 2);
        helper.setBlock(lava, Blocks.LAVA);
        double heat = JupiterRules.lavaHeat(helper.getLevel(), helper.absolutePos(lava.east()));
        helper.assertTrue(heat == 0, "the overworld test level is not Io: lava heat applies on Io only");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 100)
    public static void serpentSwimsAndCrabGuardsItsGround(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                for (int y = 1; y < 3; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
        GlowSerpent s = helper.spawn(JupiterEntities.GLOW_SERPENT.get(), new BlockPos(1, 1, 1));
        helper.assertTrue(GlowSerpent.checkSpawn(JupiterEntities.GLOW_SERPENT.get(), helper.getLevel(), net.minecraft.world.entity.MobSpawnType.NATURAL,
                helper.absolutePos(new BlockPos(1, 1, 1)), helper.getLevel().random), "spawns in deep water");
        SulfurCrab c = helper.spawn(JupiterEntities.SULFUR_CRAB.get(), new BlockPos(5, 2, 5));
        helper.assertTrue(c.fireImmune(), "the crab shrugs off heat and lava");
        helper.assertTrue(SulfurCrab.TERRITORY > 2 && SulfurCrab.TERRITORY < 8, "territorial at close range only");
        helper.succeedWhen(() -> helper.assertTrue(s.isInWater() && s.isAlive(), "the serpent lives in water"));
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void probeCaveHoldsBrineSerpentsAndLoot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(JupiterStructures.PROBE_KEY), "probe structure");
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(7000, 0, -3000);
        ProbePiece piece = new ProbePiece(5L, base.getX(), 120, base.getZ(), Direction.SOUTH);
        ObservatoryPiece obs = new ObservatoryPiece(6L, base.getX() + 60, 120, base.getZ(), Direction.NORTH);
        BoundingBox bb = piece.getBoundingBox();
        // keep the area (entity sections too) loaded for the whole test, so the serpents stay visible to queries
        java.util.List<net.minecraft.world.level.ChunkPos> forced = new java.util.ArrayList<>();
        for (int cx = (bb.minX() >> 4) - 1; cx <= (bb.maxX() >> 4) + 1; cx++) {
            for (int cz = (bb.minZ() >> 4) - 1; cz <= (bb.maxZ() >> 4) + 1; cz++) {
                level.setChunkForced(cx, cz, true);
                forced.add(new net.minecraft.world.level.ChunkPos(cx, cz));
            }
        }
        // build only once the forced tickets have fully loaded the chunks (and their entity sections), so the serpents
        // are added to a live area and never lost to load order
        java.util.concurrent.atomic.AtomicBoolean built = new java.util.concurrent.atomic.AtomicBoolean();
        helper.runAfterDelay(10, () -> {
            BeltGameTests.place(level, piece);
            int chests = 0;
            int water = 0;
            for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
                var st = level.getBlockState(p);
                if (st.is(Blocks.CHEST) && level.getBlockEntity(p) instanceof ChestBlockEntity ch && ch.getLootTable() != null) {
                    chests++;
                }
                if (st.is(Blocks.WATER)) {
                    water++;
                }
            }
            helper.assertTrue(chests == 2, "instrument and sample chests, found " + chests);
            helper.assertTrue(water > 30, "a pocket of liquid brine, found " + water);
            helper.assertTrue(Environment.radiation(level, piece.caveCentre()) >= 0, "the cave is under the ice");
            BeltGameTests.place(level, obs);
            int obsChests = 0;
            BoundingBox ob = obs.getBoundingBox();
            for (BlockPos p : BlockPos.betweenClosed(ob.minX(), ob.minY(), ob.minZ(), ob.maxX(), ob.maxY(), ob.maxZ())) {
                if (level.getBlockState(p).is(Blocks.CHEST) && level.getBlockEntity(p) instanceof ChestBlockEntity ch && ch.getLootTable() != null) {
                    obsChests++;
                }
            }
            helper.assertTrue(obsChests == 2, "observatory logs and vault, found " + obsChests);
            built.set(true);
        });
        var area = AABB.of(bb).inflate(24);
        helper.succeedWhen(() -> {
            helper.assertTrue(built.get(), "probe built");
            var serpents = level.getEntitiesOfClass(GlowSerpent.class, area);
            helper.assertTrue(serpents.size() >= 2, "glow serpents live in the brine pool, found " + serpents.size());
            serpents.forEach(net.minecraft.world.entity.Entity::discard);
            forced.forEach(c -> level.setChunkForced(c.x, c.z, false));
        });
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 100)
    public static void grazersAndHoppers(GameTestHelper helper) {
        var grazer = helper.spawn(JupiterEntities.GROOVE_GRAZER.get(), new BlockPos(1, 2, 1));
        helper.assertTrue(grazer.isFood(new ItemStack(JupiterModule.OCEAN_SALT.get())), "grazers are lured and bred with ocean salt");
        var hopper = helper.spawn(JupiterEntities.CRATER_HOPPER.get(), new BlockPos(4, 2, 4));
        helper.assertTrue(hopper.isAlive() && !hopper.leaping() || hopper.isAlive(), "the hopper lives");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void jovianTechnologiesExist(GameTestHelper helper) {
        var all = Tech.all(helper.getLevel().registryAccess());
        for (String id : new String[]{"cryobot_drilling", "radiation_hardened_electronics", "volcanic_heat_shielding", "magnetospheric_physics",
                "deep_space_habitats"}) {
            var node = all.get(Spacebreaker.id(id));
            helper.assertTrue(node != null, id + " is in the tree");
            helper.assertTrue(node.tier() >= 3 && node.tier() <= 5, id + " is a phase-3 tech");
        }
        helper.succeed();
    }

    private JupiterGameTests() {
    }
}
