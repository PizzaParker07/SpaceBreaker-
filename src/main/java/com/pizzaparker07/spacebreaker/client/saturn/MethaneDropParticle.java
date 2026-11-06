package com.pizzaparker07.spacebreaker.client.saturn;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A methane raindrop: in Titan's thick air and low gravity drops are large and fall slowly (about a sixth of the speed
 * of rain on Earth). Pale amber in the orange daylight; on landing it shows a short, flat splash.
 */
public class MethaneDropParticle extends TextureSheetParticle {
    private static final int SPLASH = 5;
    private final SpriteSet sprites;
    private int splash = -1;

    protected MethaneDropParticle(ClientLevel level, double x, double y, double z, double vy, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.sprites = sprites;
        this.xd = 0;
        this.zd = 0;
        this.yd = vy;
        this.gravity = 0.006F;
        this.friction = 0.96F;
        this.lifetime = 200;
        this.quadSize = 0.09F + random.nextFloat() * 0.04F;
        float shade = 0.9F + random.nextFloat() * 0.1F;
        this.rCol = shade;
        this.gCol = shade * 0.86F;
        this.bCol = shade * 0.62F;
        this.alpha = 0.75F;
        this.hasPhysics = true;
        setSprite(sprites.get(0, 1));
    }

    @Override
    public void tick() {
        if (splash >= 0) {
            xo = x;
            yo = y;
            zo = z;
            if (++splash > SPLASH) {
                remove();
            }
            alpha = 0.7F * (1 - splash / (float) (SPLASH + 1));
            return;
        }
        super.tick();
        if (onGround || level.getFluidState(net.minecraft.core.BlockPos.containing(x, y, z)).isSource()) {
            splash = 0;
            quadSize *= 1.3F;
            setSprite(sprites.get(1, 1));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new MethaneDropParticle(level, x, y, z, vy, sprites);
        }
    }
}
