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
            if (kind.style() == VentBlock.Style.ICE) {
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
