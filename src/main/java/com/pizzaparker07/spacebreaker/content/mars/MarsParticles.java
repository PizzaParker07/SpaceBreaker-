package com.pizzaparker07.spacebreaker.content.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Particles of the inner planets. */
public final class MarsParticles {
    public static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, Spacebreaker.MOD_ID);
    /** A grain of wind-blown dust: drifts with the wind, faintly lit. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DUST_MOTE = TYPES.register("dust_mote", () -> new SimpleParticleType(false));

    /** Dust spiralling up a dust whirl; spawned with (centre x, rise speed, centre z) as its "velocity". */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WHIRL_DUST = TYPES.register("whirl_dust", () -> new SimpleParticleType(false));

    private MarsParticles() {
    }

    static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
