package com.pizzaparker07.spacebreaker.content.rocketry.entity;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketGeometry;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketLayout;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketParts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/**
 * A dropped, empty stage. It keeps the rocket's speed, slowly tumbles away and falls back (or drifts off
 * in orbit) and disappears after a few seconds. Purely visual; it never collides or breaks blocks.
 */
public class SpentStageEntity extends Entity implements IEntityWithComplexSpawn {
    public static final int LIFETIME = 220;

    private RocketDesign design = RocketDesign.EMPTY;
    private int stage;
    private float startTilt;
    private float spin;
    private boolean inOrbit;
    private RocketGeometry geometry;
    private RocketLayout layout;

    public SpentStageEntity(EntityType<? extends SpentStageEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static SpentStageEntity create(ServerLevel level, RocketDesign design, int stage, Vec3 pos, Vec3 velocity, float tilt, boolean inOrbit) {
        SpentStageEntity e = new SpentStageEntity(RocketryModule.SPENT_STAGE.get(), level);
        e.design = design;
        e.stage = stage;
        e.startTilt = tilt;
        e.inOrbit = inOrbit;
        e.spin = (level.random.nextFloat() - 0.5F) * 2.2F;
        e.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360F, 0);
        // a small push away from the rocket so separation reads clearly
        e.setDeltaMovement(inOrbit ? velocity.add(0.02, -0.06, 0.03) : velocity.multiply(0.6, 0.6, 0.6).add(0, -0.05, 0));
        return e;
    }

    public RocketLayout layout() {
        if (layout == null) {
            layout = RocketLayout.of(design, RocketParts.lookup(level().registryAccess()));
        }
        return layout;
    }

    public RocketGeometry geometry() {
        if (geometry == null && stage < layout().stages().size()) {
            geometry = RocketGeometry.ofStage(layout(), stage);
        }
        return geometry;
    }

    public float startTilt() {
        return startTilt;
    }

    public float spin() {
        return spin;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        if (!inOrbit) {
            v = v.add(0, -0.035, 0).scale(0.99);
        }
        setDeltaMovement(v);
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        if (level().isClientSide) {
            if (tickCount < 30 && !inOrbit) {
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY(), getZ(), 0, 0.02, 0);
            }
            return;
        }
        boolean landed = !inOrbit && !level().getBlockState(blockPosition()).isAir() && tickCount > 5;
        if (tickCount > LIFETIME || landed || getY() < level().getMinBuildHeight() - 32) {
            if (landed && level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 1.2, 1.0, 1.2, 0.05);
            }
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        design = RocketDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("design")).result().orElse(RocketDesign.EMPTY);
        stage = tag.getInt("stage");
        startTilt = tag.getFloat("tilt");
        spin = tag.getFloat("spin");
        inOrbit = tag.getBoolean("orbit");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        RocketDesign.CODEC.encodeStart(NbtOps.INSTANCE, design).result().ifPresent(t -> tag.put("design", t));
        tag.putInt("stage", stage);
        tag.putFloat("tilt", startTilt);
        tag.putFloat("spin", spin);
        tag.putBoolean("orbit", inOrbit);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        RocketDesign.STREAM_CODEC.encode(buffer, design);
        buffer.writeVarInt(stage);
        buffer.writeFloat(startTilt);
        buffer.writeFloat(spin);
        buffer.writeBoolean(inOrbit);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        design = RocketDesign.STREAM_CODEC.decode(buffer);
        stage = buffer.readVarInt();
        startTilt = buffer.readFloat();
        spin = buffer.readFloat();
        inOrbit = buffer.readBoolean();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 384 * 384;
    }
}
