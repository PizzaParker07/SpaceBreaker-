package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.belt.block.VentBlock;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.content.worldgen.SkyMath;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Shared showcase plumbing for the outer-system worlds: the scene is built on the server, then camera positions are
 * worked out from the real, reshaped ground (surface features change it beyond the terrain function), and the shot list
 * is handed to the client once building has finished.
 */
abstract class OuterShowcase implements ShowcaseScenario {
    protected final List<ShowcaseDriver.Shot> shots = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final CountDownLatch built = new CountDownLatch(1);
    protected static final int SX = 24;
    protected static final int SZ = 20;

    @Override
    public final void build(ServerLevel level) {
        shots.clear();
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        try {
            build(level, ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain());
        } finally {
            built.countDown();
        }
    }

    protected abstract void build(ServerLevel level, PlanetTerrain t);

    @Override
    public final List<ShowcaseDriver.Shot> shots() {
        try {
            built.await(240, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return List.copyOf(shots);
    }

    static Consumer<ServerPlayer> at(double f) {
        return p -> PlanetTime.setDayFraction(p.serverLevel(), f);
    }

    static Consumer<ServerPlayer> dark(double f) {
        return p -> {
            PlanetTime.setDayFraction(p.serverLevel(), f);
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0, false, false));
        };
    }

    static double ground(ServerLevel level, double x, double z) {
        level.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
    }

    void shot(String name, double x, double y, double z, float yaw, float pitch, int ticks, Consumer<ServerPlayer> setup, Runnable client) {
        shots.add(new ShowcaseDriver.Shot(name, 0, x, y, z, yaw, pitch, true, CameraType.FIRST_PERSON, ticks, setup, client));
    }

    /** Camera some blocks above the real ground at (x, z), looking at the ground at (tx, tz). */
    void look(ServerLevel level, String name, double x, double z, double above, double tx, double tz, Consumer<ServerPlayer> setup, int ticks,
              Runnable client) {
        double y = ground(level, x, z) + above;
        double ty = ground(level, tx, tz) + 1;
        shot(name, x + 0.5, y, z + 0.5, MoonScenario.yaw(tx - x, tz - z), MoonScenario.pitch(tx - x, ty - y, tz - z), ticks, setup, client);
    }

    /** Camera on the ground looking at the parent planet, which hangs still above the near side of a tidally locked moon. */
    void parent(ServerLevel level, PlanetTerrain t, String name, double x, double z, double dayFraction, double tilt) {
        Vector3d e = SkyMath.parentDirection(t.latitude(z), t.longitude(x), dayFraction);
        float yaw = MoonScenario.yaw(e.x, e.z);
        float pitch = (float) -Math.toDegrees(Math.asin(e.y)) + (float) tilt;
        shot(name, x + 0.5, ground(level, x, z) + 1.7, z + 0.5, yaw, pitch, 160, at(dayFraction), MoonScenario::farView);
    }

    /** Jumps the world clock to 40 ticks into an eruption of the vent at {@code pos} (plumes in showcase shots). */
    static Consumer<ServerPlayer> erupting(BlockPos pos, double dayFraction, boolean nightVision) {
        return p -> {
            ServerLevel level = p.serverLevel();
            PlanetTime.setDayFraction(level, dayFraction);
            if (level.getBlockState(pos).getBlock() instanceof VentBlock v) {
                long now = level.getGameTime();
                long t = now;
                // the start of the next eruption (not a moment late in one already under way)
                while (!(VentBlock.erupting(v.kind(), t, pos) && !VentBlock.erupting(v.kind(), t - 1, pos)) && t < now + 2L * v.kind().period()) {
                    t++;
                }
                level.getServer().getWorldData().overworldData().setGameTime(t + 40);
            }
            if (nightVision) {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0, false, false));
            }
        };
    }

    /** Places a crashed supply lander next to the stage and adds a shot of it. */
    void lander(ServerLevel level, com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece.Variant variant, String name, double dayFraction) {
        lander(level, variant, name, dayFraction, SX - 50, SZ - 10);
    }

    /** As above, with the lander's corner at (x, z). */
    void lander(ServerLevel level, com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece.Variant variant, String name, double dayFraction,
                int x, int z) {
        int y = (int) ground(level, x + 6, z + 13);
        var piece = new com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece(91L, variant, x, y, z, Direction.NORTH);
        MarsScenario.placePiece(level, piece);
        BlockPos eye = piece.world(6, 9, -8);
        // on lumpy little worlds the ground behind the trench can rise above the camera: keep the eye clear of it
        double clear = ground(level, eye.getX() + 0.5, eye.getZ() + 0.5) + 2.5;
        if (eye.getY() < clear) {
            eye = new BlockPos(eye.getX(), (int) Math.ceil(clear), eye.getZ());
        }
        BlockPos target = piece.world(6, 5, 14);
        shot(name, eye.getX() + 0.5, eye.getY(), eye.getZ() + 0.5, MoonScenario.yaw(target.getX() - eye.getX(), target.getZ() - eye.getZ()),
                MoonScenario.pitch(target.getX() - eye.getX(), target.getY() - eye.getY(), target.getZ() - eye.getZ()), 160, at(dayFraction), null);
        if (System.getenv("LANDER_DETAIL") != null) {
            // close: from beside the wreck, the descent stage, foil band, engine and cabin; inside: the seats and console
            int ax = com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece.AX;
            BlockPos side = piece.world(ax - 9, 9, 13);
            BlockPos mid = piece.world(ax, 7, 19);
            shot(name + "_close", side.getX() + 0.5, side.getY(), side.getZ() + 0.5, MoonScenario.yaw(mid.getX() - side.getX(), mid.getZ() - side.getZ()),
                    MoonScenario.pitch(mid.getX() - side.getX(), mid.getY() - side.getY(), mid.getZ() - side.getZ()), 120, at(dayFraction), null);
            // from just inside the airlock door, looking forward over the seats at the console
            BlockPos door = piece.world(ax, 6, com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece.CABIN1 - 1);
            BlockPos console = piece.world(ax, 6, com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece.CABIN0 + 1);
            shot(name + "_inside", door.getX() + 0.5, door.getY() + 1.4, door.getZ() + 0.5, MoonScenario.yaw(console.getX() - door.getX(), console.getZ() - door.getZ()),
                    28F, 100, at(dayFraction), null);
        }
    }

    /** Topmost block of a kind within a radius of (x, z), or null. */
    static BlockPos findBlock(ServerLevel level, int x, int z, int radius, Block block) {
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int y = (int) ground(level, x + dx, z + dz) - 1;
                    BlockPos p = new BlockPos(x + dx, y, z + dz);
                    if (level.getBlockState(p).is(block)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    /** A flat stage with a row of natural blocks (two high), a row of building shapes and a trail of boot prints. */
    int stage(ServerLevel level, BlockState soil, BlockState rock, Block[] natural, Block[] shapes) {
        int y0 = (int) ground(level, SX, SZ) - 1;
        for (int x = SX - 9; x <= SX + 9; x++) {
            for (int z = SZ - 5; z <= SZ + 12; z++) {
                for (int y = y0 - 3; y <= y0 + 48; y++) {
                    level.setBlock(new BlockPos(x, y, z), y < y0 ? rock : y == y0 ? soil : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (int i = 0; i < natural.length; i++) {
            for (int y = 1; y <= 2; y++) {
                level.setBlock(new BlockPos(SX - natural.length + 1 + i * 2, y0 + y, SZ - 3), natural[i].defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int i = 0; i < shapes.length; i++) {
            level.setBlock(new BlockPos(SX - 2 + i * 2, y0 + 1, SZ), shapes[i].defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        if (soil.getBlock() instanceof LunarRegolithBlock) {
            for (int x = SX - 8; x <= SX + 8; x++) {
                BlockPos p = new BlockPos(x, y0, SZ + 3);
                level.setBlock(p, LunarRegolithBlock.printFor(level.getBlockState(p), p, Direction.EAST), Block.UPDATE_CLIENTS);
            }
        }
        return y0;
    }

    static <T extends Mob> T mob(ServerLevel level, EntityType<T> type, double x, double y, double z, float yaw) {
        T m = type.create(level);
        if (m != null) {
            m.moveTo(x + 0.5, y, z + 0.5, yaw, 0F);
            m.setYHeadRot(yaw);
            m.setYBodyRot(yaw);
            m.setNoAi(true);
            m.setPersistenceRequired();
            m.setSilent(true);
            level.addFreshEntity(m);
        }
        return m;
    }

    static List<int[]> grid(int from, int to) {
        List<int[]> l = new ArrayList<>();
        for (int a = from; a <= to; a++) {
            for (int b = from; b <= to; b++) {
                l.add(new int[]{a, b});
            }
        }
        l.sort((p, q) -> Integer.compare(p[0] * p[0] + p[1] * p[1], q[0] * q[0] + q[1] * q[1]));
        return l;
    }
}
