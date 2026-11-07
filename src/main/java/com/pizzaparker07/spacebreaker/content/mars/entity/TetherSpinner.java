package com.pizzaparker07.spacebreaker.content.mars.entity;

import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Tether Spinner: a small six-legged grazer of Phobos and Deimos that gets about in long hops. On a moon this small a
 * hard push could carry it off into space, so before every hop it glues a silk line to the ground and pays it out; when
 * the line runs taut it reels itself back down. It nibbles the carbon-rich chondrite and leaves silk (string) behind.
 * Lured and bred with carbonaceous chondrite.
 */
public class TetherSpinner extends Animal {
    private static final Ingredient FOOD = Ingredient.of(MarsModule.CARBONACEOUS_CHONDRITE.get());
    /** Length of the silk line, in blocks. */
    public static final double TETHER = 6.0;
    /** How hard a taut line pulls back, per tick. */
    public static final double PULL = 0.06;
    @Nullable
    private Vec3 anchor;
    private int hopCooldown;

    public TetherSpinner(EntityType<? extends TetherSpinner> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    public static boolean checkSpawn(EntityType<? extends Animal> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(MarsModule.PHOBOS_REGOLITH.get()) || below.is(MarsModule.DEIMOS_REGOLITH.get()) || reason != MobSpawnType.NATURAL)
                && level.getBlockState(pos).isAir();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, FOOD, false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6F));
    }

    /** The point its line is glued to while it is in the air, or null. */
    @Nullable
    public Vec3 anchor() {
        return anchor;
    }

    /** Glues the line here (used when it hops, and by tests). */
    public void anchorAt(Vec3 point) {
        this.anchor = point;
    }

    /** Pulls it back toward the anchor when the line is taut: the outward part of its speed is cancelled, then reeled in. */
    void reel() {
        if (anchor == null) {
            return;
        }
        Vec3 out = position().subtract(anchor);
        double len = out.length();
        if (len <= TETHER || len < 1e-6) {
            return;
        }
        Vec3 dir = out.scale(1.0 / len);
        Vec3 v = getDeltaMovement();
        double outward = v.dot(dir);
        if (outward > 0) {
            v = v.subtract(dir.scale(outward));
        }
        setDeltaMovement(v.subtract(dir.scale(PULL)));
        hasImpulse = true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        if (onGround() && anchor != null && hopCooldown < 14) {
            anchor = null; // landed: the line is bitten off
        }
        reel();
        if (anchor != null && level() instanceof ServerLevel server && tickCount % 3 == 0) {
            // the silk line, glinting in the sunlight
            Vec3 a = anchor;
            Vec3 b = position().add(0, 0.2, 0);
            for (int i = 1; i <= 5; i++) {
                Vec3 p = a.lerp(b, i / 6.0);
                server.sendParticles(ParticleTypes.WHITE_ASH, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
        if (isNoAi()) {
            return;
        }
        if (hopCooldown > 0) {
            hopCooldown--;
        }
        if (onGround() && hopCooldown == 0 && !getNavigation().isDone()) {
            anchor = position();
            Vec3 look = Vec3.directionFromRotation(0, getYRot());
            setDeltaMovement(look.x * 0.4, 0.38, look.z * 0.4);
            hasImpulse = true;
            hopCooldown = 16;
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return FOOD.test(stack);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return MarsEntities.TETHER_SPINNER.get().create(level);
    }
}
