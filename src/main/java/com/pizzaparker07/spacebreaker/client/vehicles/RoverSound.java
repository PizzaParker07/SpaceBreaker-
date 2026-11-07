package com.pizzaparker07.spacebreaker.client.vehicles;

import com.pizzaparker07.spacebreaker.content.audio.SbSounds;
import com.pizzaparker07.spacebreaker.content.vehicles.RoverEntity;
import com.pizzaparker07.spacebreaker.content.vehicles.RoverPhysics;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * Electric hub-motor whine that follows a rover: higher and louder with speed (pitch 0.8 to 1.3), silent with a
 * flat battery. In vacuum only the people riding it hear it, through the frame.
 */
public class RoverSound extends AbstractTickableSoundInstance {
    private final RoverEntity rover;

    public RoverSound(RoverEntity rover) {
        super(SbSounds.ROVER_MOTOR_LOOP.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.rover = rover;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
        this.pitch = 0.8F;
        this.x = rover.getX();
        this.y = rover.getY();
        this.z = rover.getZ();
    }

    /** Whether the motors are running: powered and either driven or still rolling. */
    public static boolean wanted(RoverEntity rover, double speed) {
        return !rover.isRemoved() && rover.energy() > 0 && (rover.getControllingPassenger() != null || speed > 0.01);
    }

    @Override
    public void tick() {
        double speed = Math.hypot(rover.getX() - rover.xo, rover.getZ() - rover.zo);
        boolean on = wanted(rover, speed);
        x = rover.getX();
        y = rover.getY();
        z = rover.getZ();
        boolean riding = Minecraft.getInstance().player != null && Minecraft.getInstance().player.getVehicle() == rover;
        float target = !on ? 0F : (Environment.isVacuum(rover.level()) && !riding ? 0F
                : (0.25F + 0.65F * (float) Mth.clamp(speed / rover.roverType().maxSpeed(), 0, 1)) * rover.roverType().loudness());
        // lighter motors spin faster: a higher, thinner whine
        float tune = rover.roverType().loudness() < 1F ? 1.25F : 1F;
        pitch = Mth.lerp(0.2F, pitch, RoverPhysics.motorPitch(speed, rover.roverType().maxSpeed()) * tune);
        volume = Mth.lerp(0.15F, volume, target);
        if (rover.isRemoved() || (!on && volume < 0.02F)) {
            stop();
        }
    }
}
