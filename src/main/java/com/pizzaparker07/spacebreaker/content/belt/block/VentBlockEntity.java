package com.pizzaparker07.spacebreaker.content.belt.block;

import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Drives a {@link VentBlock}: particles on the client, lifting (and burning) on the server. */
public class VentBlockEntity extends BlockEntity {
    private static final Vector3f SULFUR_YELLOW = new Vector3f(0.92F, 0.82F, 0.42F);
    private static final Vector3f SULFUR_PALE = new Vector3f(0.86F, 0.88F, 0.95F);
    private static final Vector3f ICE_WHITE = new Vector3f(0.93F, 0.96F, 1.0F);
    private static final Vector3f PLUME_DARK = new Vector3f(0.16F, 0.15F, 0.15F);
    private static final Vector3f PLUME_GREY = new Vector3f(0.42F, 0.4F, 0.4F);

    public VentBlockEntity(BlockPos pos, BlockState state) {
        super(OuterWorlds.VENT.get(), pos, state);
    }

    private static VentBlock.Kind kind(BlockState state) {
        return state.getBlock() instanceof VentBlock v ? v.kind() : null;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, VentBlockEntity be) {
        VentBlock.Kind kind = kind(state);
        if (kind == null) {
            return;
        }
        RandomSource r = level.random;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        if (!VentBlock.erupting(kind, level.getGameTime(), pos)) {
            // a faint wisp between eruptions
            if (r.nextInt(8) == 0) {
                level.addParticle(ParticleTypes.WHITE_ASH, x + r.nextGaussian() * 0.2, y, z + r.nextGaussian() * 0.2, 0, 0.05, 0);
            }
            return;
        }
        double speed = 0.35 + kind.height() / 40.0;
        for (int i = 0; i < 6; i++) {
            double vx = r.nextGaussian() * 0.04;
            double vz = r.nextGaussian() * 0.04;
            double vy = speed * (0.7 + r.nextDouble() * 0.6);
            if (kind.style() == VentBlock.Style.NITROGEN) {
                // a narrow dark column of nitrogen gas and dust; at the top the thin wind bends it over into a long streak
                level.addAlwaysVisibleParticle(new DustParticleOptions(i % 2 == 0 ? PLUME_DARK : PLUME_GREY, 3.2F), true,
                        x + vx * 6, y + r.nextDouble() * kind.height(), z + vz * 6, vx, 0.04, vz);
                if (i < 3) {
                    double along = r.nextDouble() * kind.height() * 0.8;
                    level.addAlwaysVisibleParticle(new DustParticleOptions(PLUME_DARK, 3.6F), true, x + along, y + kind.height() - along * 0.08,
                            z + r.nextGaussian() * 0.8, 0.12, -0.01, 0);
                }
            } else if (kind.style() == VentBlock.Style.ICE) {
                level.addAlwaysVisibleParticle(i % 3 == 0 ? ParticleTypes.CLOUD : ParticleTypes.SNOWFLAKE, true, x, y, z, vx, vy, vz);
                if (i == 0) {
                    level.addAlwaysVisibleParticle(new DustParticleOptions(ICE_WHITE, 2.4F), true, x, y + r.nextDouble() * kind.height(), z,
                            r.nextGaussian() * 0.3, 0.05, r.nextGaussian() * 0.3);
                }
            } else {
                level.addAlwaysVisibleParticle(i % 2 == 0 ? new DustParticleOptions(SULFUR_YELLOW, 3.0F) : new DustParticleOptions(SULFUR_PALE, 2.2F),
                        true, x + vx * 4, y + r.nextDouble() * kind.height(), z + vz * 4, vx * 6, 0.02, vz * 6);
                level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, x, y, z, vx, vy * 0.4, vz);
                if (i == 0) {
                    // the umbrella: fallout drifting outward from the top of the plume
                    double a = r.nextDouble() * Math.PI * 2;
                    level.addAlwaysVisibleParticle(new DustParticleOptions(SULFUR_YELLOW, 3.5F), true, x, y + kind.height(), z,
                            Math.cos(a) * 0.35, -0.02, Math.sin(a) * 0.35);
                }
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VentBlockEntity be) {
        VentBlock.Kind kind = kind(state);
        if (kind == null || level.getGameTime() % 4 != 0 || !VentBlock.erupting(kind, level.getGameTime(), pos)) {
            return;
        }
        erupt(level, pos, kind);
        if (state.getBlock() instanceof VentBlock v && v.fallout() != null && level.getGameTime() % FALLOUT_EVERY == 0) {
            fallout(level, pos, v);
        }
    }

    /** Ticks between two grains of fallout while a vent erupts. */
    public static final int FALLOUT_EVERY = 120;
    /** No more fallout while this many of its items already lie around the vent. */
    public static final int FALLOUT_CAP = 6;

    /**
     * Drops one item of the vent's fallout a few blocks from it, as if it rained out of the plume. Stops when enough is
     * already lying around, so an unattended geyser does not pile items up. Returns whether it dropped something.
     */
    public static boolean fallout(Level level, BlockPos pos, VentBlock vent) {
        net.minecraft.world.level.ItemLike item = vent.fallout();
        if (item == null) {
            return false;
        }
        AABB around = new AABB(pos).inflate(12, 8, 12);
        int lying = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, around, e -> e.getItem().is(item.asItem())).size();
        if (lying >= FALLOUT_CAP) {
            return false;
        }
        RandomSource r = level.random;
        double a = r.nextDouble() * Math.PI * 2;
        double d = 2 + r.nextDouble() * 6;
        var e = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 0.5 + Math.cos(a) * d, pos.getY() + 6,
                pos.getZ() + 0.5 + Math.sin(a) * d, new net.minecraft.world.item.ItemStack(item));
        e.setDeltaMovement(0, -0.05, 0);
        level.addFreshEntity(e);
        return true;
    }

    /** One eruption pulse: lifts (and, for hot vents, burns) everything in the column above the vent. */
    public static void erupt(Level level, BlockPos pos, VentBlock.Kind kind) {
        AABB column = new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1 + kind.height(), pos.getZ() + 1).inflate(0.4, 0, 0.4);
        for (Entity e : level.getEntities((Entity) null, column, en -> !en.isSpectator())) {
            Vec3 v = e.getDeltaMovement();
            e.setDeltaMovement(v.x, Math.max(v.y, 0) + kind.launch(), v.z);
            e.fallDistance = 0;
            e.hurtMarked = true;
            if (e instanceof ServerPlayer sp) {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
            }
            if (kind.damage() > 0 && e instanceof LivingEntity living && !living.fireImmune()) {
                living.hurt(level.damageSources().hotFloor(), kind.damage());
            }
        }
    }
}
