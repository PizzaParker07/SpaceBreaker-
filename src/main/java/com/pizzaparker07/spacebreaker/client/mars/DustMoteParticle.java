package com.pizzaparker07.spacebreaker.client.mars;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Wind-blown dust grain: drifts downwind, tumbles slowly, fades out. */
public class DustMoteParticle extends TextureSheetParticle {
    protected DustMoteParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = 0.004F;
        this.friction = 0.99F;
        this.lifetime = 40 + random.nextInt(50);
        this.quadSize = 0.05F + random.nextFloat() * 0.07F;
        float shade = 0.85F + random.nextFloat() * 0.15F;
        this.rCol = shade;
        this.gCol = shade * 0.92F;
        this.bCol = shade * 0.85F;
        this.alpha = 0.9F;
        this.hasPhysics = true;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (age > lifetime - 15) {
            alpha = Math.max(0F, (lifetime - age) / 15F) * 0.9F;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            // ambient biome particles come with zero speed: give them the gentle prevailing wind
            if (vx == 0 && vz == 0) {
                vx = 0.04 + level.random.nextDouble() * 0.03;
                vz = 0.02;
                vy = -0.005;
            }
            return new DustMoteParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
