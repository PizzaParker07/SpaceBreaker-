package com.pizzaparker07.spacebreaker.content.rocketry.mission;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A satellite released in orbit: its part model hangs where it was released, turning slowly and drifting a
 * little. The satellite itself (what it does) is kept in {@link SatelliteData}; this is how it looks.
 */
public class SatelliteEntity extends Entity {
    private static final EntityDataAccessor<String> DATA_PART = SynchedEntityData.defineId(SatelliteEntity.class, EntityDataSerializers.STRING);

    public SatelliteEntity(EntityType<? extends SatelliteEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static SatelliteEntity create(ServerLevel level, ResourceLocation part, Vec3 pos) {
        SatelliteEntity e = new SatelliteEntity(RocketryModule.SATELLITE_ENTITY.get(), level);
        e.entityData.set(DATA_PART, part.toString());
        e.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360F, 0);
        return e;
    }

    public ResourceLocation part() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(DATA_PART));
        return id == null ? RocketryModule.COMM_SATELLITE.getId() : id;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PART, "spacebreaker:comm_satellite");
    }

    @Override
    public void tick() {
        super.tick();
        // a slow bob, so the satellite does not look pasted onto the sky
        double t = tickCount * 0.01;
        setPos(getX(), getY() + Math.sin(t) * 0.002, getZ());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_PART, tag.getString("part"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("part", entityData.get(DATA_PART));
    }
}
