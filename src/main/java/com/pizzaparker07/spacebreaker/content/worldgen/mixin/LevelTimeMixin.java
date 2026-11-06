package com.pizzaparker07.spacebreaker.content.worldgen.mixin;

import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelTimeAccess;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Lets planet dimensions keep their own solar time. Vanilla derives every dimension's time of day from the
 * shared overworld clock; this override asks {@link PlanetTime} first, so the sun, sky light and spawning on
 * the Moon follow the Moon's long day.
 */
@Mixin(Level.class)
public abstract class LevelTimeMixin implements LevelTimeAccess {
    @Override
    public float getTimeOfDay(float partialTick) {
        float planet = PlanetTime.timeOfDay((Level) (Object) this);
        return planet >= 0F ? planet : this.dimensionType().timeOfDay(this.dayTime());
    }
}
