package com.pizzaparker07.spacebreaker.content.venus.entity;

import com.pizzaparker07.spacebreaker.content.research.Research;
import com.pizzaparker07.spacebreaker.content.venus.VenusModule;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A Venus descent probe, released from the clouds: it sinks under its parachute through the hot, crushing lower air,
 * lands, and sends its owner surface samples and landing data at once, as the Venera landers did. Then it lasts only
 * {@link #SURVIVAL_TICKS} in the 464 °C furnace before its electronics cook and it falls silent.
 */
public class DescentProbe extends Entity implements ItemSupplier {
    /** Sinking speed under the parachute, blocks per tick. */
    public static final double SINK = 0.5;
    /** How long it keeps transmitting on the surface (in game, a minute; the real landers lasted about an hour). */
    public static final int SURVIVAL_TICKS = 1200;
    /** Landing data this probe sends (the same kind as a rocket-landed probe's). */
    public static final ResourceLocation DATA = com.pizzaparker07.spacebreaker.Spacebreaker.id("probe_landing");
    private static final EntityDataAccessor<Boolean> LANDED = SynchedEntityData.defineId(DescentProbe.class, EntityDataSerializers.BOOLEAN);
    @Nullable
    private UUID owner;
    private int surfaceTicks;

    public DescentProbe(EntityType<? extends DescentProbe> type, Level level) {
        super(type, level);
    }

    public static DescentProbe release(Level level, Vec3 at, @Nullable UUID owner) {
        DescentProbe p = VenusEntities.DESCENT_PROBE.get().create(level);
        p.moveTo(at.x, at.y, at.z, 0F, 0F);
        p.owner = owner;
        return p;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LANDED, false);
    }

    public boolean landed() {
        return entityData.get(LANDED);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(VenusModule.DESCENT_PROBE.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (!landed()) {
            setDeltaMovement(0, -SINK, 0);
            move(MoverType.SELF, getDeltaMovement());
            if (onGround()) {
                land();
            }
            return;
        }
        surfaceTicks++;
        if (level() instanceof ServerLevel server) {
            if (surfaceTicks % 10 == 0) {
                server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.4, getZ(), 2, 0.15, 0.1, 0.15, 0.01);
            }
            if (surfaceTicks >= SURVIVAL_TICKS) {
                server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.4, getZ(), 12, 0.3, 0.3, 0.3, 0.02);
                server.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1F, 0.6F);
                discard();
            }
        }
    }

    /** Touchdown: the surface samples and landing data go straight to the owner. */
    public void land() {
        entityData.set(LANDED, true);
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        server.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.6F, 1.6F);
        ServerPlayer player = owner == null ? null : server.getServer().getPlayerList().getPlayer(owner);
        if (player != null) {
            deliverTo(player);
        }
    }

    /** What the probe radios home on landing: two Venus surface samples and the landing data. */
    public static void deliverTo(ServerPlayer player) {
        give(player, Research.sample(com.pizzaparker07.spacebreaker.Spacebreaker.id("venus")).copyWithCount(2));
        give(player, Research.experimentData(DATA));
        player.displayClientMessage(Component.translatable("message.spacebreaker.descent_probe.landed").withStyle(ChatFormatting.GREEN), false);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            owner = tag.getUUID("Owner");
        }
        surfaceTicks = tag.getInt("SurfaceTicks");
        entityData.set(LANDED, tag.getBoolean("Landed"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putInt("SurfaceTicks", surfaceTicks);
        tag.putBoolean("Landed", landed());
    }
}
