package com.pizzaparker07.spacebreaker.content.mars.entity;

import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * Rust Beetle: a neutral, palm-sized grazer of iron oxide. It wanders the dust and chews hematite spherules out of
 * the soil (the soil turns back into plain regolith) and sheds tiny iron flakes. Hit one and it bites back.
 */
public class RustBeetle extends PathfinderMob {
    private int eating;

    public RustBeetle(EntityType<? extends RustBeetle> type, Level level) {
        super(type, level);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    public static boolean checkSpawn(EntityType<? extends PathfinderMob> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos,
                                     RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(MarsModule.MARS_SOILS) || below.is(MarsModule.MARS_STONE) || reason != MobSpawnType.NATURAL)
                && level.getBlockState(pos).isAir();
    }

    public int eatingTicks() {
        return eating;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(4, new GrazeRustGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 10) {
            eating = 40;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void aiStep() {
        if (eating > 0) {
            eating--;
        }
        super.aiStep();
    }

    /** Finds rusty soil nearby, walks there and eats the hematite out of it. */
    class GrazeRustGoal extends Goal {
        private BlockPos target;
        private int time;

        GrazeRustGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || random.nextInt(level().getDifficulty() == Difficulty.PEACEFUL ? 60 : 120) != 0) {
                return false;
            }
            BlockPos base = blockPosition();
            for (int i = 0; i < 16; i++) {
                BlockPos p = base.offset(random.nextInt(13) - 6, random.nextInt(5) - 3, random.nextInt(13) - 6);
                BlockState s = level().getBlockState(p);
                if ((s.is(MarsModule.HEMATITE_SOIL.get()) || s.is(MarsModule.MARTIAN_REGOLITH.get())) && level().getBlockState(p.above()).isAir()) {
                    target = p;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            time = 0;
            getNavigation().moveTo(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5, 0.9);
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && time < 200 && getTarget() == null;
        }

        @Override
        public void tick() {
            time++;
            getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
            if (distanceToSqr(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5) < 2.2) {
                getNavigation().stop();
                if (eating == 0) {
                    eating = 40;
                    level().broadcastEntityEvent(RustBeetle.this, (byte) 10);
                }
                if (eating == 1 && level() instanceof ServerLevel server) {
                    BlockState s = server.getBlockState(target);
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), target.getX() + 0.5, target.getY() + 1.05,
                            target.getZ() + 0.5, 12, 0.2, 0.05, 0.2, 0.05);
                    if (s.is(MarsModule.HEMATITE_SOIL.get())) {
                        server.setBlock(target, MarsModule.MARTIAN_REGOLITH.get().defaultBlockState(), Block.UPDATE_ALL);
                        spawnAtLocation(Items.IRON_NUGGET);
                    } else if (random.nextInt(6) == 0) {
                        spawnAtLocation(Items.IRON_NUGGET);
                    }
                    heal(2F);
                    target = null;
                }
            }
        }
    }
}
