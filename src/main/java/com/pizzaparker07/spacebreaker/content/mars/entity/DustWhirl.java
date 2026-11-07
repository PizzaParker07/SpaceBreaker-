package com.pizzaparker07.spacebreaker.content.mars.entity;

import com.pizzaparker07.spacebreaker.content.mars.MarsParticles;
import com.pizzaparker07.spacebreaker.content.mars.MarsWeather;
import com.pizzaparker07.spacebreaker.core.env.DustLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A fair-weather dust whirl: a tall, thin column of dust that forms over warm plains on a calm Martian afternoon and
 * wanders slowly across the ground for a minute or two. Harmless; it nudges loose items and walkers aside and, like
 * the whirls that kept the Mars rovers alive, sweeps the dust off any solar panel it passes over. Not to be confused
 * with the storm-born Dust Devil, which attacks.
 * <p>
 * The server side is only a position and a heading; the column itself is drawn on the client with particles.
 */
public class DustWhirl extends Entity {
    /** How far it reaches to sweep panels and nudge things (blocks, horizontal). */
    public static final double REACH = 2.0;
    /** Wandering speed, blocks per tick. */
    public static final double SPEED = 0.06;
    /** Height of the dust column, blocks. */
    public static final int HEIGHT = 14;
    private int life = 1200 + (int) (Math.random() * 1200);
    private float heading = (float) (Math.random() * Math.PI * 2);

    public DustWhirl(EntityType<? extends DustWhirl> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** Whirls are weather, not things: they are never saved with the world. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 160 * 160;
    }

    public void setHeading(float radians) {
        this.heading = radians;
    }

    public void setLife(int ticks) {
        this.life = ticks;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientParticles();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        // dies away at the end of its life, when a storm comes, or when nobody is near enough to see it
        if (--life <= 0 || MarsWeather.stormStrength(level) > 0.3F
                || (tickCount % 20 == 0 && level.getNearestPlayer(this, 128) == null)) {
            discard();
            return;
        }
        // wander: a slowly turning heading, following the ground
        heading += (random.nextFloat() - 0.5F) * 0.12F;
        double nx = getX() + Mth.cos(heading) * SPEED;
        double nz = getZ() + Mth.sin(heading) * SPEED;
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(nx), Mth.floor(nz));
        double ny = getY() + Mth.clamp(ground - getY(), -0.25, 0.25);
        setPos(nx, ny, nz);
        if (tickCount % 4 == 0) {
            nudge(level);
        }
        if (tickCount % 10 == 0) {
            DustLoad.clean(level, blockPosition(), REACH);
        }
    }

    /** Pushes loose items and walkers gently around and away from the column. */
    private void nudge(ServerLevel level) {
        AABB box = new AABB(getX() - REACH, getY() - 1, getZ() - REACH, getX() + REACH, getY() + 4, getZ() + REACH);
        for (Entity e : level.getEntities(this, box, e -> e instanceof ItemEntity || e instanceof Player)) {
            if (e instanceof Player p && (p.isSpectator() || p.getAbilities().flying)) {
                continue;
            }
            Vec3 off = e.position().subtract(position());
            double d = Math.max(0.3, Math.sqrt(off.x * off.x + off.z * off.z));
            if (d > REACH) {
                continue;
            }
            // a swirl around the column plus a little outward push; items also get a small lift
            double tx = -off.z / d;
            double tz = off.x / d;
            double k = 0.05 * (1 - d / REACH);
            double up = e instanceof ItemEntity ? 0.04 : 0;
            e.push(tx * k + off.x / d * k * 0.5, up, tz * k + off.z / d * k * 0.5);
            e.hurtMarked = true;
        }
    }

    private void clientParticles() {
        Level level = level();
        // a narrow, slowly widening column of spiralling dust
        for (int i = 0; i < 4; i++) {
            double h = random.nextDouble() * HEIGHT;
            double r = 0.25 + h * 0.06;
            double a = (tickCount * 0.35 + h * 0.9 + random.nextDouble() * 0.6);
            double x = getX() + Math.cos(a) * r;
            double z = getZ() + Math.sin(a) * r;
            level.addParticle(MarsParticles.DUST_MOTE.get(), x, getY() + h, z,
                    -Math.sin(a) * 0.12, 0.03 + random.nextDouble() * 0.03, Math.cos(a) * 0.12);
        }
        // a skirt of dust kicked up at its foot
        if (tickCount % 2 == 0) {
            double a = random.nextDouble() * Math.PI * 2;
            level.addParticle(MarsParticles.DUST_MOTE.get(), getX() + Math.cos(a) * 0.9, getY() + 0.1, getZ() + Math.sin(a) * 0.9,
                    Math.cos(a) * 0.05, 0.02, Math.sin(a) * 0.05);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
