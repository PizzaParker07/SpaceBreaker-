package com.pizzaparker07.spacebreaker.content.moon.entity;

import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Heat Siphon: a frost-crystal creature of the two-week lunar night. It hunts warmth: its touch chills a player
 * to the bone, and it latches onto batteries and generators to drink their stored charge, glowing brighter as it
 * fills. It cannot stand sunlight and sublimates at dawn (except in the cold traps of polar craters, where it lives
 * all the time), and it never appears where a lamp shines, so a lit base
 * and a guarded battery room matter when the solar panels go dark.
 */
public class HeatSiphon extends Monster {
    private static final EntityDataAccessor<Integer> FED = SynchedEntityData.defineId(HeatSiphon.class, EntityDataSerializers.INT);
    /** FE it can hold; a full siphon stops feeding. */
    public static final int CAPACITY = 20000;
    /** FE drained per tick from a battery or generator. */
    public static final int DRAIN_PER_TICK = 40;
    /** Freeze ticks added per hit (vanilla frost: slows, then hurts). */
    public static final int CHILL = 90;

    public HeatSiphon(EntityType<? extends HeatSiphon> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.SAFE_FALL_DISTANCE, 16.0);
    }

    /** True during the lunar night (sun below the horizon). */
    public static boolean isNight(Level level) {
        return Environment.sunElevation(level) < -0.05F;
    }

    /** Natural spawning: at night or in a cold trap, on lunar ground, never in lamp light, never on peaceful. */
    public static boolean checkSpawn(EntityType<? extends HeatSiphon> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getBlockState(pos).isAir()) {
            return false;
        }
        if (reason == MobSpawnType.SPAWNER || reason == MobSpawnType.STRUCTURE) {
            return true;
        }
        BlockState below = level.getBlockState(pos.below());
        return (below.is(MoonModule.LUNAR_SOILS) || below.is(MoonModule.MOON_STONE))
                && (isNight(level.getLevel()) || com.pizzaparker07.spacebreaker.content.worldgen.ColdTraps.inColdTrap(level.getLevel(), pos))
                && level.getBrightness(LightLayer.BLOCK, pos) < 6;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FED, 0);
    }

    /** FE drunk so far. */
    public int fed() {
        return entityData.get(FED);
    }

    /** Glow strength 0..1 for the renderer: faint when hungry, bright when full of stolen energy. */
    public float glow() {
        return 0.3F + 0.7F * Math.min(1F, fed() / (float) CAPACITY);
    }

    /** Drinks from an energy storage; returns the FE taken. */
    public int drink(IEnergyStorage storage) {
        int want = Math.min(DRAIN_PER_TICK, CAPACITY - fed());
        if (want <= 0 || !storage.canExtract()) {
            return 0;
        }
        int got = storage.extractEnergy(want, false);
        if (got > 0) {
            entityData.set(FED, fed() + got);
        }
        return got;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(4, new DrainGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living && living.canFreeze()) {
            living.setTicksFrozen(Math.min(living.getTicksFrozen() + CHILL, living.getTicksRequiredToFreeze() + 140));
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(6) == 0) {
                level().addParticle(ParticleTypes.SNOWFLAKE, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, -0.01, 0);
            }
            return;
        }
        // sunlight boils the frost body away
        if (!isNoAi() && tickCount % 20 == 0 && !isNight(level()) && level().canSeeSky(blockPosition())
                && !com.pizzaparker07.spacebreaker.content.worldgen.ColdTraps.inColdTrap(level(), blockPosition())) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.4, getZ(), 6, 0.3, 0.2, 0.3, 0.01);
            hurt(damageSources().dryOut(), 3F);
        }
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        // the stolen charge crystallises
        int dust = fed() / 4000;
        if (dust > 0) {
            spawnAtLocation(new ItemStack(Items.REDSTONE, dust));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return fed() == 0 && super.removeWhenFarAway(distance);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fed", fed());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FED, Math.max(0, Math.min(CAPACITY, tag.getInt("Fed"))));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GLASS_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F + 0.3F * glow();
    }

    /** Finds the nearest battery or generator with charge, walks to it and drinks. */
    static final class DrainGoal extends Goal {
        private static final int RANGE = 12;
        private final HeatSiphon mob;
        private @Nullable BlockPos source;
        private int search;

        DrainGoal(HeatSiphon mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (mob.getTarget() != null || mob.fed() >= CAPACITY || --search > 0) {
                return false;
            }
            search = 20 + mob.getRandom().nextInt(20);
            source = find();
            return source != null;
        }

        @Override
        public boolean canContinueToUse() {
            return source != null && mob.getTarget() == null && mob.fed() < CAPACITY && storage(source) != null;
        }

        @Override
        public void start() {
            mob.getNavigation().moveTo(source.getX() + 0.5, source.getY(), source.getZ() + 0.5, 1.0);
        }

        @Override
        public void stop() {
            source = null;
            mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            mob.getLookControl().setLookAt(source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5);
            if (mob.distanceToSqr(source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5) > 4.5) {
                if (mob.getNavigation().isDone()) {
                    mob.getNavigation().moveTo(source.getX() + 0.5, source.getY(), source.getZ() + 0.5, 1.0);
                }
                return;
            }
            mob.getNavigation().stop();
            IEnergyStorage storage = storage(source);
            if (storage == null || mob.drink(storage) == 0) {
                source = null;
            } else if (mob.tickCount % 10 == 0 && mob.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, source.getX() + 0.5, source.getY() + 0.8, source.getZ() + 0.5, 3, 0.25, 0.2, 0.25, 0.02);
            }
        }

        private @Nullable IEnergyStorage storage(BlockPos pos) {
            if (!mob.level().isLoaded(pos)) {
                return null;
            }
            IEnergyStorage s = mob.level().getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            return s != null && s.canExtract() && s.getEnergyStored() > 0 ? s : null;
        }

        private @Nullable BlockPos find() {
            BlockPos at = mob.blockPosition();
            BlockPos best = null;
            double bestD = Double.MAX_VALUE;
            ChunkPos c = new ChunkPos(at);
            for (int cx = c.x - 1; cx <= c.x + 1; cx++) {
                for (int cz = c.z - 1; cz <= c.z + 1; cz++) {
                    if (!mob.level().hasChunk(cx, cz)) {
                        continue;
                    }
                    for (BlockEntity be : mob.level().getChunk(cx, cz).getBlockEntities().values()) {
                        BlockPos p = be.getBlockPos();
                        double d = p.distSqr(at);
                        if (d < RANGE * RANGE && d < bestD && storage(p) != null) {
                            best = p;
                            bestD = d;
                        }
                    }
                }
            }
            return best;
        }
    }
}
