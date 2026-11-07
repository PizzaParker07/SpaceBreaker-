package com.pizzaparker07.spacebreaker.testmod.qa;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.mission.SatelliteEntity;
import com.pizzaparker07.spacebreaker.content.vehicles.RoverEntity;
import com.pizzaparker07.spacebreaker.content.vehicles.VehiclesModule;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Long-session endurance: many rovers, rockets (idle on the ground and parked in orbit state), satellites and running
 * machines (combustion generators burning coal, crushers fed with ore and power, solar panels) tick together for
 * {@link #TICKS} ticks in their own batch. Measures the server's tick time per 100-tick window and the heap before and
 * after, checks that no entity is duplicated or lost, and writes {@code run-gametest/qa/soak.md}.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoakTests {
    static final int TICKS = 3000;
    static final int ROVERS = 24, ROCKETS = 12, SATELLITES = 24, GENERATORS = 16, CRUSHERS = 16, PANELS = 16;

    @GameTest(template = "empty", timeoutTicks = TICKS + 400, batch = "qa_soak")
    public static void longSessionStaysFlat(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos base = h.absolutePos(new BlockPos(0, 1, 0));
        // a 48 x 48 floor so everything sits in loaded, ticking chunks around the test
        for (int x = -4; x < 44; x++) {
            for (int z = -4; z < 44; z++) {
                level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
            }
        }
        // keep the area ticking: with no player and no forced chunk a level stops ticking entities and block
        // entities after 300 empty ticks, which would make the numbers meaningless
        List<net.minecraft.world.level.ChunkPos> forced = new ArrayList<>();
        for (int cx = (base.getX() - 8) >> 4; cx <= (base.getX() + 48) >> 4; cx++) {
            for (int cz = (base.getZ() - 8) >> 4; cz <= (base.getZ() + 48) >> 4; cz++) {
                if (level.setChunkForced(cx, cz, true)) {
                    forced.add(new net.minecraft.world.level.ChunkPos(cx, cz));
                }
            }
        }
        List<Entity> spawned = new ArrayList<>();
        for (int i = 0; i < ROVERS; i++) {
            RoverEntity rover = VehiclesModule.ROVER.get().create(level);
            rover.moveTo(base.getX() + 2 + (i % 6) * 5, base.getY(), base.getZ() + 2 + (i / 6) * 5, i * 15F, 0F);
            rover.setEnergy(5_000);
            level.addFreshEntity(rover);
            spawned.add(rover);
        }
        for (int i = 0; i < ROCKETS; i++) {
            Vec3 at = new Vec3(base.getX() + 32.5 + (i % 3) * 4, base.getY(), base.getZ() + 2.5 + (i / 3) * 6);
            RocketEntity rocket = RocketEntity.create(level, RocketryModule.MOON_ROCKET, at, 0F);
            level.addFreshEntity(rocket);
            rocket.fillCompletely();
            if (i % 2 == 1) {
                rocket.stageScene(FlightState.ORBIT, 0, 0, false);
            }
            spawned.add(rocket);
        }
        for (int i = 0; i < SATELLITES; i++) {
            SatelliteEntity sat = SatelliteEntity.create(level, RocketryModule.COMM_SATELLITE.getId(),
                    new Vec3(base.getX() + 4 + (i % 8) * 4, base.getY() + 30, base.getZ() + 36 + (i / 8) * 2));
            level.addFreshEntity(sat);
            spawned.add(sat);
        }
        List<BlockPos> crushers = new ArrayList<>();
        List<BlockPos> generators = new ArrayList<>();
        for (int i = 0; i < GENERATORS; i++) {
            BlockPos p = base.offset(i * 2, 0, 30);
            generators.add(p);
            level.setBlock(p, MachinesModule.COMBUSTION_GENERATOR.block().get().defaultBlockState(), 3);
            var items = level.getCapability(Capabilities.ItemHandler.BLOCK, p, null);
            if (items != null) {
                for (int s = 0; s < items.getSlots(); s++) {
                    items.insertItem(s, new ItemStack(Items.COAL, 64), false);
                }
            }
        }
        for (int i = 0; i < CRUSHERS; i++) {
            BlockPos p = base.offset(i * 2, 0, 33);
            level.setBlock(p, MachinesModule.CRUSHER.block().get().defaultBlockState(), 3);
            crushers.add(p);
            var items = level.getCapability(Capabilities.ItemHandler.BLOCK, p, null);
            if (items != null) {
                items.insertItem(0, new ItemStack(Items.IRON_ORE, 64), false);
            }
        }
        for (int i = 0; i < PANELS; i++) {
            level.setBlock(base.offset(i * 2, 0, 27), MachinesModule.SOLAR_PANEL.get().defaultBlockState(), 3);
        }
        var server = level.getServer();
        AABB area = new AABB(base).inflate(80, 60, 80);
        // only the kinds of entity this test spawns: other mod creatures may spawn naturally around the area
        java.util.Set<net.minecraft.world.entity.EntityType<?>> kinds = new java.util.HashSet<>();
        spawned.forEach(e -> kinds.add(e.getType()));
        // baseline = what the test spawned (a freshly added entity can be missing from the area query for a tick)
        java.util.Map<String, Integer> startByType = new java.util.TreeMap<>();
        spawned.forEach(e -> startByType.merge(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(), 1, Integer::sum));
        int startEntities = startByType.values().stream().mapToInt(Integer::intValue).sum();
        long heapStart = usedHeap();
        List<Double> windows = new ArrayList<>();
        int[] tick = {0};
        h.onEachTick(() -> {
            tick[0]++;
            for (BlockPos p : crushers) {
                var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, p, null);
                if (energy != null) {
                    energy.receiveEnergy(200, false);
                }
            }
            if (tick[0] % 100 == 0) {
                long sum = 0;
                long[] times = server.getTickTimesNanos();
                for (long t : times) {
                    sum += t;
                }
                windows.add(sum / (double) times.length / 1_000_000.0);
            }
        });
        h.runAfterDelay(TICKS, () -> {
            long heapEnd = usedHeap();
            java.util.Map<String, Integer> endByType = byType(level, area, kinds);
            int endEntities = endByType.values().stream().mapToInt(Integer::intValue).sum();
            List<String> changed = new ArrayList<>();
            java.util.Set<String> names = new java.util.TreeSet<>(startByType.keySet());
            names.addAll(endByType.keySet());
            for (String n : names) {
                int a = startByType.getOrDefault(n, 0), b = endByType.getOrDefault(n, 0);
                if (a != b) {
                    changed.add(n + " " + a + " -> " + b);
                }
            }
            int alive = (int) spawned.stream().filter(Entity::isAlive).count();
            double ticked = spawned.stream().mapToInt(e -> e.tickCount).average().orElse(0);
            int crushed = 0;
            int oreLeft = 0;
            for (BlockPos p : crushers) {
                var items = level.getCapability(Capabilities.ItemHandler.BLOCK, p, null);
                for (int s = 0; items != null && s < items.getSlots(); s++) {
                    ItemStack st = items.getStackInSlot(s);
                    if (st.is(Items.IRON_ORE)) {
                        oreLeft += st.getCount();
                    } else if (!st.isEmpty()) {
                        crushed += st.getCount();
                    }
                }
            }
            int coalLeft = 0;
            for (BlockPos p : generators) {
                var items = level.getCapability(Capabilities.ItemHandler.BLOCK, p, null);
                for (int s = 0; items != null && s < items.getSlots(); s++) {
                    coalLeft += items.getStackInSlot(s).is(Items.COAL) ? items.getStackInSlot(s).getCount() : 0;
                }
            }
            double first = windows.isEmpty() ? 0 : windows.get(Math.min(1, windows.size() - 1));
            double last = windows.isEmpty() ? 0 : windows.get(windows.size() - 1);
            double max = windows.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double mean = windows.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            StringBuilder md = new StringBuilder("## Long-session endurance (`qa/SoakTests`)\n\n");
            md.append(String.format(java.util.Locale.ROOT,
                    "%d ticks with %d rovers, %d rockets (half in orbit state), %d satellites, %d combustion generators, "
                            + "%d powered crushers, %d solar panels.\n\n", TICKS, ROVERS, ROCKETS, SATELLITES, GENERATORS, CRUSHERS, PANELS));
            md.append(String.format(java.util.Locale.ROOT, "- server tick time (100-tick windows): mean %.2f ms, first %.2f ms, last %.2f ms, worst %.2f ms\n",
                    mean, first, last, max));
            md.append(String.format(java.util.Locale.ROOT, "- heap in use after GC: %d MB at start, %d MB at the end (%+d MB)\n",
                    heapStart >> 20, heapEnd >> 20, (heapEnd - heapStart) >> 20));
            md.append(String.format(java.util.Locale.ROOT, "- entities in the area: %d at start, %d at the end; spawned and still alive: %d of %d\n",
                    startEntities, endEntities, alive, spawned.size()));
            md.append(String.format(java.util.Locale.ROOT, "- work done: entities ticked %.0f times on average; crushers hold %d products, %d of %d ore left; "
                    + "generators have %d of %d coal left\n", ticked, crushed, oreLeft, CRUSHERS * 64, coalLeft, GENERATORS * 64));
            java.util.Map<String, Integer> types = new java.util.TreeMap<>();
            level.getEntitiesOfClass(Entity.class, area, e -> !(e instanceof net.minecraft.world.entity.player.Player))
                    .forEach(e -> types.merge(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(), 1, Integer::sum));
            md.append("- entity types at the end: ").append(types).append('\n');
            md.append("- windows (ms): ").append(windows.stream().map(d -> String.format(java.util.Locale.ROOT, "%.1f", d)).toList()).append('\n');
            ProgressionReport.write("soak.md", md.toString());
            spawned.forEach(Entity::discard);
            forced.forEach(c -> level.setChunkForced(c.x, c.z, false));
            // generous limits: this guards against leaks and runaway growth, not against a slow test machine
            h.assertTrue(ticked > TICKS * 0.8, "the entities really ticked: " + ticked);
            h.assertTrue(alive == spawned.size(), "entities lost during the session: " + (spawned.size() - alive));
            h.assertTrue(changed.isEmpty(), "spawned entity kinds changed in number: " + changed);
            h.assertTrue(last <= Math.max(3 * first, first + 20), "tick time grew from " + first + " ms to " + last + " ms");
            h.assertTrue(heapEnd - heapStart < 512L << 20, "heap grew by " + ((heapEnd - heapStart) >> 20) + " MB");
            h.succeed();
        });
    }

    private static java.util.Map<String, Integer> byType(ServerLevel level, AABB area, java.util.Set<net.minecraft.world.entity.EntityType<?>> kinds) {
        java.util.Map<String, Integer> out = new java.util.TreeMap<>();
        for (Entity e : level.getEntitiesOfClass(Entity.class, area, e -> kinds.contains(e.getType()))) {
            out.merge(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(), 1, Integer::sum);
        }
        return out;
    }

    private static long usedHeap() {
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < 3; i++) {
            System.gc();
        }
        return rt.totalMemory() - rt.freeMemory();
    }

    private SoakTests() {
    }
}
