package com.pizzaparker07.spacebreaker.client.rocketry;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Launch steam: a few big billows (3 to 8 blocks across) pushed out of the flame trench. They roll out fast, slow down
 * along the ground, swell, lift a little and thin out over several seconds, so the pad sits in one dense, low cloud.
 */
public class SteamParticle extends TextureSheetParticle {
    private final float size0;
    private final float grow;

    protected SteamParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = 0F;
        this.hasPhysics = false;
        this.friction = 0.955F;
        this.lifetime = 110 + random.nextInt(70);
        this.size0 = 1.5F + random.nextFloat() * 1.0F;
        this.grow = 1.4F + random.nextFloat() * 0.6F;
        float shade = 0.86F + random.nextFloat() * 0.12F;
        this.rCol = shade;
        this.gCol = shade;
        this.bCol = shade * 0.99F;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        shape();
        pickSprite(sprites);
    }

    private void shape() {
        float t = Mth.clamp(age / (float) lifetime, 0, 1);
        quadSize = size0 * (1 + (grow - 1) * (1 - (1 - t) * (1 - t)));
        float in = Mth.clamp(age / 8F, 0, 1);
        float out = 1 - Mth.clamp((t - 0.55F) / 0.45F, 0, 1);
        alpha = 0.9F * in * out;
    }

    @Override
    public void tick() {
        super.tick();
        yd += 0.0015; // warm: it lifts a little as it slows down
        oRoll = roll;
        roll += 0.004F;
        shape();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new SteamParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
