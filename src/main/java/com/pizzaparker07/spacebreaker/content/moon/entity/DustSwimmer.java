package com.pizzaparker07.spacebreaker.content.moon.entity;

import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dust Swimmer: a flat, finned grazer that glides just under the loose regolith, filtering it for
 * frozen volatiles. Only its back and dorsal fin break the surface, leaving a trail of ripples. Harmless and shy.
 */
public class DustSwimmer extends PathfinderMob {
    public DustSwimmer(EntityType<? extends DustSwimmer> type, Level level) {
        super(type, level);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.SAFE_FALL_DISTANCE, 16.0);
    }

    public static boolean checkSpawn(EntityType<? extends DustSwimmer> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(MoonModule.LUNAR_SOILS) && level.getBlockState(pos).isAir();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PanicGoal(this, 1.8));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 6F, 1.2, 1.6, p -> !p.isCrouching()));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.03F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            return;
        }
        // ripples: dust kicked up along the body while it glides
        double speed = getDeltaMovement().horizontalDistance();
        BlockState below = level().getBlockState(BlockPos.containing(getX(), getY() - 0.2, getZ()));
        if (below.isAir() || !(speed > 0.02 || random.nextInt(25) == 0)) {
            return;
        }
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        double sx = Mth.cos(yaw);
        double sz = Mth.sin(yaw);
        int n = speed > 0.02 ? 2 : 1;
        for (int i = 0; i < n; i++) {
            double side = random.nextBoolean() ? 0.5 : -0.5;
            level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below),
                    getX() + sx * side + (random.nextDouble() - 0.5) * 0.3, getY() + 0.05, getZ() + sz * side + (random.nextDouble() - 0.5) * 0.3,
                    0, 0.02, 0);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SAND_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SAND_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SAND_STEP, 0.08F, 0.6F);
    }
}
