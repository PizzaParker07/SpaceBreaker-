package com.pizzaparker07.spacebreaker.content.saturn;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Particles of Saturn's moons. */
public final class SaturnParticles {
    public static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, Spacebreaker.MOD_ID);
    /** A methane raindrop on Titan: big, slow, pale amber in the orange light; splashes where it lands. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHANE_DROP = TYPES.register("methane_drop",
            () -> new SimpleParticleType(false));

    private SaturnParticles() {
    }

    static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
