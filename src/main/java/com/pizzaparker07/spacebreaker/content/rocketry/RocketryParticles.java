package com.pizzaparker07.spacebreaker.content.rocketry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Particles of the launch pad. */
public final class RocketryParticles {
    public static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, Spacebreaker.MOD_ID);
    /** A big, slow billow of steam and exhaust rolling out of the flame trench (spawned with its velocity). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STEAM = TYPES.register("rocket_steam", () -> new SimpleParticleType(true));

    private RocketryParticles() {
    }

    static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
