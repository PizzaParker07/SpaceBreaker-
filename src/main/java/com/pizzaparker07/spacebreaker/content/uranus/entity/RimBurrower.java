package com.pizzaparker07.spacebreaker.content.uranus.entity;

import com.pizzaparker07.spacebreaker.content.belt.entity.OuterGrazer;
import com.pizzaparker07.spacebreaker.content.pluto.PlutoModule;
import com.pizzaparker07.spacebreaker.content.uranus.UranusModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Rim Burrower: a squat, broad-clawed grazer of Oberon's crater rims. Hit while standing on loose ground (regolith,
 * dark crater fill, any shovel ground), it digs itself in within a moment: buried, it cannot be hurt and does not move,
 * and after a while it pops back out. On hard ice it can only run. Fed ammonia crystals, two burrowers breed.
 */
public class RimBurrower extends OuterGrazer {
    private static final EntityDataAccessor<Integer> BURIED = SynchedEntityData.defineId(RimBurrower.class, EntityDataSerializers.INT);
    /** Ticks it stays buried. */
    public static final int BURY_TIME = 100;

    public RimBurrower(EntityType<? extends RimBurrower> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.17);
    }

    public static boolean checkSpawn(EntityType<? extends Animal> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return onGround(UranusModule.MIRANDAN_GROUND, level, reason, pos, random);
    }

    @Override
    protected Item food() {
        return PlutoModule.AMMONIA_CRYSTAL.get();
    }

    @Override
    protected Supplier<? extends EntityType<? extends OuterGrazer>> ownType() {
        return UranusEntities.RIM_BURROWER;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURIED, 0);
    }

    public boolean isBuried() {
        return entityData.get(BURIED) > 0;
    }

    /** True if it can dig into the block under it. */
    public boolean canDig() {
        BlockState below = level().getBlockState(blockPosition().below());
        return onGround() && below.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || isBuried();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (isBuried()) {
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
            if (!level().isClientSide) {
                int left = entityData.get(BURIED) - 1;
                entityData.set(BURIED, left);
                if (left == 0) {
                    playSound(SoundEvents.SAND_BREAK, 1F, 1.1F);
                }
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isBuried() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        boolean hit = super.hurt(source, amount);
        if (hit && isAlive() && level() instanceof ServerLevel server && canDig()) {
            entityData.set(BURIED, BURY_TIME);
            getNavigation().stop();
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(blockPosition().below())),
                    getX(), getY() + 0.2, getZ(), 24, 0.4, 0.2, 0.4, 0.1);
            playSound(SoundEvents.SAND_PLACE, 1F, 0.7F);
        }
        return hit;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Buried", entityData.get(BURIED));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(BURIED, tag.getInt("Buried"));
    }
}
