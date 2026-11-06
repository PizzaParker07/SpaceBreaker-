package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.content.audio.SbSounds;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketLayout;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * Looping rocket sounds that follow the rocket: the engine roar while it burns (louder and deeper with more
 * thrust, muffled in vacuum unless you ride it) and the roar of the plasma during atmospheric entry.
 */
public class EngineSound extends AbstractTickableSoundInstance {
    public enum Kind {
        ENGINE, REENTRY
    }

    private final RocketEntity rocket;
    private final Kind kind;

    public EngineSound(RocketEntity rocket, Kind kind) {
        super(kind == Kind.ENGINE ? SbSounds.ROCKET_ENGINE_LOOP.get() : SbSounds.ROCKET_REENTRY_LOOP.get(), SoundSource.NEUTRAL,
                SoundInstance.createUnseededRandom());
        this.rocket = rocket;
        this.kind = kind;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
        this.pitch = 1F;
        this.x = rocket.getX();
        this.y = rocket.getY();
        this.z = rocket.getZ();
    }

    /** Whether this sound should be playing for the rocket right now. */
    public static boolean wanted(RocketEntity rocket, Kind kind) {
        return kind == Kind.ENGINE ? rocket.burning() : rocket.state() == FlightState.REENTRY;
    }

    @Override
    public void tick() {
        if (rocket.isRemoved() || !wanted(rocket, kind)) {
            stop();
            return;
        }
        x = rocket.getX();
        y = rocket.getY();
        z = rocket.getZ();
        boolean riding = Minecraft.getInstance().player != null && Minecraft.getInstance().player.getVehicle() == rocket;
        boolean vacuum = !rocket.location().atmosphere() || rocket.location().orbit();
        float target;
        if (kind == Kind.ENGINE) {
            target = riding ? (vacuum ? 0.35F : 1.0F) : (vacuum ? 0F : 1.6F);
            // more thrust: a deeper roar
            RocketLayout l = rocket.layout();
            int s = Math.min(rocket.activeStage(), l.stages().size() - 1);
            double kn = s >= 0 ? l.stages().get(s).thrustKn() : 0;
            pitch = (float) Mth.clamp(1.15 - kn / 4000.0, 0.7, 1.15);
        } else {
            float t = rocket.timer();
            target = Mth.clamp(t / 30F, 0, 1) * Mth.clamp((170 - t) / 50F, 0, 1) * (riding ? 1.0F : 1.4F);
            pitch = 0.9F + (float) Math.min(0.2, rocket.getDeltaMovement().length() * 0.04);
        }
        volume = Mth.lerp(0.1F, volume, target);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
