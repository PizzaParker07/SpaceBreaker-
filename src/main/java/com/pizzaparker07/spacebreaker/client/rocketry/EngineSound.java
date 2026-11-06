package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/** Low engine rumble that follows a burning rocket (a slowed-down vanilla wind/flight sound). */
public class EngineSound extends AbstractTickableSoundInstance {
    private final RocketEntity rocket;

    public EngineSound(RocketEntity rocket) {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.rocket = rocket;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
        this.pitch = 0.5F;
        this.x = rocket.getX();
        this.y = rocket.getY();
        this.z = rocket.getZ();
    }

    @Override
    public void tick() {
        if (rocket.isRemoved() || !rocket.burning()) {
            stop();
            return;
        }
        x = rocket.getX();
        y = rocket.getY();
        z = rocket.getZ();
        boolean riding = Minecraft.getInstance().player != null && Minecraft.getInstance().player.getVehicle() == rocket;
        boolean vacuum = rocket.location() != com.pizzaparker07.spacebreaker.content.rocketry.nav.Location.EARTH_SURFACE;
        float target = riding ? (vacuum ? 0.35F : 1.0F) : (vacuum ? 0F : 1.6F);
        volume = Mth.lerp(0.1F, volume, target);
        pitch = 0.45F + (float) Math.min(0.2, rocket.getDeltaMovement().length() * 0.05);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
