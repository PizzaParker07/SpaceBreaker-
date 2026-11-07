package com.pizzaparker07.spacebreaker.compat.dynlights;

import com.pizzaparker07.spacebreaker.client.light.LightBeams;
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import dev.lambdaurora.lambdynlights.api.behavior.LineLightBehavior;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * LambDynamicLights bridge (loaded only when that mod is present, through its {@code lambdynlights:initializer}
 * entrypoint declared in neoforge.mods.toml): every {@link LightBeams} beam becomes a line light from the lamp to where
 * it lands, so the blocks along it are really lit, smooth and steady, also under shader packs. The lines are updated
 * once per client tick and only moved when an end shifts by more than a quarter block, to keep light updates calm.
 */
public final class SbDynamicLights implements DynamicLightsInitializer {
    /** An end must move this far before the light is recomputed (blocks, squared). */
    private static final double MOVE_SQ = 0.25 * 0.25;
    private final List<LineLightBehavior> lines = new ArrayList<>();
    private DynamicLightBehaviorManager manager;

    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        manager = context.dynamicLightBehaviorManager();
        LightBeams.setDynamicLights(true);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> tick());
    }

    @Override
    public void onInitializeDynamicLights(ItemLightSourceManager itemLightSources) {
        // nothing: the helmet's light is a moving beam, set up in onInitializeDynamicLights(context)
    }

    private void tick() {
        Minecraft mc = Minecraft.getInstance();
        List<LightBeams.Beam> beams = mc.level == null || mc.isPaused() ? List.of() : LightBeams.collect(mc, 1F);
        for (int i = 0; i < beams.size(); i++) {
            LightBeams.Beam b = beams.get(i);
            if (i < lines.size()) {
                LineLightBehavior line = lines.get(i);
                if (moved(line.getStartPoint(), b.from()) || moved(line.getEndPoint(), b.to())) {
                    line.setStartPoint(b.from().x, b.from().y, b.from().z);
                    line.setEndPoint(b.to().x, b.to().y, b.to().z);
                }
                if (line.getLuminance() != b.luminance()) {
                    line.setLuminance(b.luminance());
                }
            } else {
                LineLightBehavior line = new LineLightBehavior(new Vector3d(b.from().x, b.from().y, b.from().z),
                        new Vector3d(b.to().x, b.to().y, b.to().z), b.luminance());
                lines.add(line);
                manager.add(line);
            }
        }
        while (lines.size() > beams.size()) {
            manager.remove(lines.remove(lines.size() - 1));
        }
    }

    private static boolean moved(Vector3d was, Vec3 now) {
        double dx = was.x - now.x, dy = was.y - now.y, dz = was.z - now.z;
        return dx * dx + dy * dy + dz * dz > MOVE_SQ;
    }
}
