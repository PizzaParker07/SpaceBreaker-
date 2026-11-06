package com.pizzaparker07.spacebreaker.content.venus.entity;

import com.pizzaparker07.spacebreaker.content.venus.VenusModule;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Acid Ray: a broad, gliding flyer of the clouds that soaks acid into its wing membranes. Neutral: it glides in wide
 * curves and ignores travellers, but if attacked it swoops at the attacker and sprays acid (poison, and it eats
 * into uncoated armour).
 */
public class AcidRay extends SkyFloater {
    private int cooldown;
    /** Guardians of the derelict aerostat attack trespassers on sight. */
    private boolean guardian;

    public AcidRay(EntityType<? extends FlyingMob> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected double cruise() {
        return getTarget() != null ? 0.32 : 0.16;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new SwoopGoal());
        goalSelector.addGoal(5, new DriftGoal(this));
        targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.player.Player.class, 10, true, false, p -> guardian && p.distanceToSqr(this) < 20 * 20));
    }

    public void setGuardian(boolean guardian) {
        this.guardian = guardian;
        if (guardian) {
            setPersistenceRequired();
        }
    }

    public boolean isGuardian() {
        return guardian;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Guardian", guardian);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        guardian = tag.getBoolean("Guardian");
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof net.minecraft.world.entity.player.Player p && p.isCreative())) {
            setTarget(attacker);
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (cooldown > 0) {
            cooldown--;
        }
    }

    /** Fly at the target; on contact spray acid and pull away. */
    class SwoopGoal extends Goal {
        SwoopGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && distanceToSqr(t) < 48 * 48;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) {
                return;
            }
            if (cooldown > 20) {
                // pull up and circle before the next pass
                getMoveControl().setWantedPosition(t.getX() + Math.cos(tickCount * 0.1) * 10, clampHeight(t.getY() + 6),
                        t.getZ() + Math.sin(tickCount * 0.1) * 10, 1.0);
                return;
            }
            getMoveControl().setWantedPosition(t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 1.0);
            if (cooldown == 0 && distanceToSqr(t) < 4.5 && level() instanceof net.minecraft.server.level.ServerLevel) {
                if (t.hurt(damageSources().mobAttack(AcidRay.this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                    t.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), AcidRay.this);
                    for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST}) {
                        ItemStack s = t.getItemBySlot(slot);
                        if (!s.isEmpty() && s.isDamageableItem() && !Boolean.TRUE.equals(s.get(VenusModule.ACID_COATED.get()))) {
                            s.hurtAndBreak(4, t, slot);
                        }
                    }
                }
                cooldown = 60;
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_FLAP;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }
}
