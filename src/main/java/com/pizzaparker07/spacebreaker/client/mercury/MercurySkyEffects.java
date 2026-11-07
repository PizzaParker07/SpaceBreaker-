package com.pizzaparker07.spacebreaker.client.mercury;

import com.pizzaparker07.spacebreaker.client.sky.PlanetSkyEffects;
import com.pizzaparker07.spacebreaker.content.mercury.MercuryDay;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Mercury's sky: the airless sky, with a sun that swells and brightens toward perihelion (1.5 times wider than at
 * aphelion, 2.3 times the light) as Mercury's eccentric orbit carries it in and out.
 */
public class MercurySkyEffects extends PlanetSkyEffects {
    public MercurySkyEffects() {
        super(Bodies.key("mercury"));
    }

    private static double distance(ClientLevel level) {
        return PlanetTime.clock(level).map(c -> MercuryDay.distance(c.meanFraction(level.getDayTime()))).orElse(1.0);
    }

    @Override
    protected double sunScale(ClientLevel level) {
        return 1.0 / distance(level);
    }

    @Override
    protected float sunGlow(ClientLevel level) {
        // flux grows with 1/r²: a soft halo that only shows near perihelion
        double flux = 1.0 / (distance(level) * distance(level));
        return (float) Math.max(0, (flux - 1.0) * 0.35);
    }
}
