package com.pizzaparker07.spacebreaker.content.uranus.entity;

import com.pizzaparker07.spacebreaker.content.belt.entity.OuterGrazer;
import com.pizzaparker07.spacebreaker.content.uranus.UranusModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

import java.util.function.Supplier;

/**
 * Gloam Hopper: a small soot-dark grazer of Umbriel, all long hind legs, that crosses the plains in slow, high bounds.
 * Hit, it kicks up a cloud of the carbon dust it lives in: whoever is close is blinded for a moment while it bounds
 * away. Fed coal, two hoppers breed.
 */
public class GloamHopper extends OuterGrazer {
    /** How far the dust cloud reaches (blocks). */
    public static final double CLOUD_RADIUS = 4.0;
    /** How long the cloud blinds (ticks). */
    public static final int CLOUD_TIME = 60;
    /** Ticks before it can kick up another cloud. */
    public static final int CLOUD_COOLDOWN = 100;
    private int cloudCooldown;

    public GloamHopper(EntityType<? extends GloamHopper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.JUMP_STRENGTH, 0.6);
    }

    public static boolean checkSpawn(EntityType<? extends Animal> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return onGround(UranusModule.MIRANDAN_GROUND, level, reason, pos, random);
    }

    @Override
    protected Item food() {
        return Items.COAL;
    }

    @Override
    protected Supplier<? extends EntityType<? extends OuterGrazer>> ownType() {
        return UranusEntities.GLOAM_HOPPER;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        if (cloudCooldown > 0) {
            cloudCooldown--;
        }
        // bounds: hops whenever it is on the move
        if (onGround() && getNavigation().isInProgress() && random.nextInt(8) == 0) {
            getJumpControl().jump();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hit = super.hurt(source, amount);
        if (hit && level() instanceof ServerLevel server && cloudCooldown == 0) {
            dustCloud(server);
        }
        return hit;
    }

    /** Blinds everything close by (but other hoppers) and shows the cloud. */
    void dustCloud(ServerLevel server) {
        cloudCooldown = CLOUD_COOLDOWN;
        server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.4, getZ(), 30, 1.2, 0.5, 1.2, 0.02);
        playSound(SoundEvents.SAND_BREAK, 1F, 0.6F);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(CLOUD_RADIUS),
                e -> e != this && !(e instanceof GloamHopper) && e.distanceTo(this) <= CLOUD_RADIUS)) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, CLOUD_TIME, 0), this);
        }
    }

    public boolean cloudReady() {
        return cloudCooldown == 0;
    }
}
