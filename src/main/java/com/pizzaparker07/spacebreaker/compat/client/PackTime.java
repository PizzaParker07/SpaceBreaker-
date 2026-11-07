package com.pizzaparker07.spacebreaker.compat.client;

import com.pizzaparker07.spacebreaker.compat.SpaceWorlds;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * The time of day shader packs see in orbit. Packs light the scene from vanilla's sun angle (Iris's sunAngle,
 * sunPosition, shadow direction and worldTime), but day and night in orbit follow the planet's shadow
 * ({@link OrbitDaylight}), not the dimension's clock: without this the station stayed in full sun on the night side.
 * While a pack is active in an orbit world, packs get orbital noon in sunlight and midnight in the shadow, passing
 * through dusk across the penumbra. Vanilla rendering never reads this (our sky and light map have their own
 * daylight).
 */
public final class PackTime {
    private PackTime() {
    }

    /** Day time (vanilla ticks, 6000 = noon) the pack should see, or -1 to keep the world's own. */
    public static long packDayTime(ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        if (level != mc.level || SpaceSky.kind(level) != SpaceWorlds.Kind.ORBIT || !ShaderState.shaderPackInUse()) {
            return -1;
        }
        double x = mc.gameRenderer.getMainCamera().getPosition().x;
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(false);
        return OrbitDaylight.orbitedBody(level, x)
                .map(body -> OrbitDaylight.sunlit(OrbitDaylight.system(level.registryAccess()), body, level.getGameTime() + pt))
                .map(lit -> Math.round(Mth.lerp(Mth.clamp(lit, 0.0, 1.0), 18000.0, 6000.0)))
                .orElse(-1L);
    }

    /** Vanilla's sun angle for a day time (DimensionType.timeOfDay without a fixed time). */
    public static float timeOfDay(long dayTime) {
        double d = Mth.frac(dayTime / 24000.0 - 0.25);
        double e = 0.5 - Math.cos(d * Math.PI) / 2.0;
        return (float) (d * 2.0 + e) / 3.0F;
    }
}
