package com.pizzaparker07.spacebreaker.client.mars;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Dust caught in a dust whirl: circles the column's axis while it rises, the circle widening with height, then fades.
 * Spawned with the axis position and rise speed in place of a velocity: (axis x, rise, axis z).
 */
public class WhirlDustParticle extends TextureSheetParticle {
    private final double cx;
    private final double cz;
    private final double rise;
    private double radius;
    private double angle;
    private final double spin;

    protected WhirlDustParticle(ClientLevel level, double x, double y, double z, double axisX, double rise, double axisZ, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.cx = axisX;
        this.cz = axisZ;
        this.rise = rise;
        this.radius = Math.hypot(x - axisX, z - axisZ);
        this.angle = Math.atan2(z - axisZ, x - axisX);
        this.spin = 0.22 + random.nextDouble() * 0.12;
        this.gravity = 0F;
        this.hasPhysics = false;
        this.lifetime = 30 + random.nextInt(25);
        this.quadSize = 0.22F + random.nextFloat() * 0.3F;
        float shade = 0.78F + random.nextFloat() * 0.2F;
        this.rCol = shade;
        this.gCol = shade * 0.74F;
        this.bCol = shade * 0.55F;
        this.alpha = 0.75F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        angle += spin;
        radius += rise * 0.07; // the column widens upward
        setPos(cx + Math.cos(angle) * radius, y + rise, cz + Math.sin(angle) * radius);
        if (age > lifetime - 10) {
            alpha = Math.max(0F, (lifetime - age) / 10F) * 0.75F;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double ax, double rise, double az) {
            return new WhirlDustParticle(level, x, y, z, ax, rise, az, sprites);
        }
    }
}
