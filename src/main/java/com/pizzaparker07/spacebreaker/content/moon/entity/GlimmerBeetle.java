package com.pizzaparker07.spacebreaker.content.moon.entity;

import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Glimmer Beetle: a slow, harmless beetle of lava tubes and dark polar craters. It feeds on KREEP-rich dust and
 * glows softly from its abdomen. Brush it to collect a shed glimmer shell (it grows a new one in a few minutes);
 * glimmer shells make lamps that need no power through the long lunar night. It follows anyone holding KREEP.
 */
public class GlimmerBeetle extends PathfinderMob {
    private static final EntityDataAccessor<Integer> REGROW = SynchedEntityData.defineId(GlimmerBeetle.class, EntityDataSerializers.INT);
    public static final int REGROW_TICKS = 6000;

    public GlimmerBeetle(EntityType<? extends GlimmerBeetle> type, Level level) {
        super(type, level);
        this.xpReward = 1;
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.16)
                .add(Attributes.SAFE_FALL_DISTANCE, 16.0);
    }

    public static boolean checkSpawn(EntityType<? extends GlimmerBeetle> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(MoonModule.LUNAR_SOILS) || below.is(MoonModule.MOON_STONE)) && level.getBlockState(pos).isAir()
                && level.getRawBrightness(pos, 0) < 8;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(REGROW, 0);
    }

    /** True while the glimmer shell is fully grown (and can be brushed off). */
    public boolean hasShell() {
        return entityData.get(REGROW) <= 0;
    }

    /** Glow strength 0..1 for the renderer: dimmer while the shell regrows. */
    public float glow() {
        return hasShell() ? 1F : 0.35F + 0.65F * (1F - entityData.get(REGROW) / (float) REGROW_TICKS);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new TemptGoal(this, 1.1, Ingredient.of(MoonModule.KREEP_FRAGMENT.get()), false));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && entityData.get(REGROW) > 0) {
            entityData.set(REGROW, entityData.get(REGROW) - 1);
        }
    }

    /** Brushing sheds the glimmer shell without harming the beetle. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.BRUSH)) {
            if (!hasShell()) {
                return InteractionResult.FAIL;
            }
            if (!level().isClientSide) {
                spawnAtLocation(new ItemStack(MoonModule.GLIMMER_SHELL.get()), 0.3F);
                entityData.set(REGROW, REGROW_TICKS);
                held.hurtAndBreak(8, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                level().playSound(null, this, SoundEvents.ARMADILLO_BRUSH, SoundSource.NEUTRAL, 1F, 1.2F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ShellRegrow", entityData.get(REGROW));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(REGROW, tag.getInt("ShellRegrow"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.5F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SILVERFISH_STEP, 0.06F, 1.6F);
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }
}
