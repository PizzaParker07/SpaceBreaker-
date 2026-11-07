package com.pizzaparker07.spacebreaker.client.mars;

import com.pizzaparker07.spacebreaker.content.mars.entity.DustWhirl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Dust caught in a dust whirl. It follows the funnel: circling the axis while it rises, on a radius that is narrow at
 * the ground and flares out with height, growing larger and fainter as it climbs. Spawned with (axis x, base y,
 * axis z) in place of a velocity, so the funnel's shape is the same for every grain.
 */
public class WhirlDustParticle extends TextureSheetParticle {
    /** Rise per tick, blocks. */
    private static final double RISE = 0.09;
    private final double cx;
    private final double cz;
    private final double base;
    private final float size0;
    private double angle;
    private final double spin;

    protected WhirlDustParticle(ClientLevel level, double x, double y, double z, double axisX, double baseY, double axisZ, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.cx = axisX;
        this.cz = axisZ;
        this.base = baseY;
        this.angle = Math.atan2(z - axisZ, x - axisX);
        this.spin = 0.26 + random.nextDouble() * 0.06;
        this.gravity = 0F;
        this.hasPhysics = false;
        this.lifetime = 34 + random.nextInt(20);
        this.size0 = 0.38F + random.nextFloat() * 0.16F;
        float shade = 0.82F + random.nextFloat() * 0.14F;
        this.rCol = shade;
        this.gCol = shade * 0.74F;
        this.bCol = shade * 0.56F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        shape();
        pickSprite(sprites);
    }

    /** Funnel radius at a height above the ground. */
    public static double radius(double h) {
        return 0.25 + 0.016 * h * h;
    }

    private void shape() {
        double h = Math.max(0, y - base);
        double t = Math.min(1, h / DustWhirl.HEIGHT);
        quadSize = size0 * (1F + 1.6F * (float) t);
        float fade = age > lifetime - 8 ? (lifetime - age) / 8F : 1F;
        alpha = (float) (0.7 - 0.55 * t) * Math.max(0F, fade);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime || y - base > DustWhirl.HEIGHT) {
            remove();
            return;
        }
        angle += spin;
        double ny = y + RISE;
        double r = radius(ny - base);
        setPos(cx + Math.cos(angle) * r, ny, cz + Math.sin(angle) * r);
        shape();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double ax, double baseY, double az) {
            return new WhirlDustParticle(level, x, y, z, ax, baseY, az, sprites);
        }
    }
}
