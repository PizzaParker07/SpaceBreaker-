package com.pizzaparker07.spacebreaker.content.jupiter;

import com.pizzaparker07.spacebreaker.core.env.Environment;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Locale;

/**
 * Local physics of the Jovian moons on top of the body data: the heat of Io's lava (a lava lake bakes the ground around
 * it while the plains stay at -140 °C), and a warning about the radiation belts when a player arrives on Io or Europa
 * (the dose is real and halves with every solid block overhead, so ice and rock are the shield).
 */
public final class JupiterRules {
    /** Extra °C right next to lava on Io, fading over {@link #LAVA_REACH} blocks. */
    public static final double LAVA_HEAT = 900;
    public static final int LAVA_REACH = 4;

    private JupiterRules() {
    }

    static void register() {
        Environment.addTemperatureModifier(JupiterRules::lavaHeat);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p && (e.getTo() == JupiterModule.EUROPA || e.getTo() == JupiterModule.IO)) {
                double dose = Environment.radiation(p.level(), BlockPos.containing(p.getX(), 400, p.getZ()));
                p.displayClientMessage(Component.translatable("message.spacebreaker.jovian_radiation",
                        String.format(Locale.ROOT, "%.1f", dose / 1000.0)).withStyle(ChatFormatting.GOLD), false);
            }
        });
    }

    /** Heat from nearby lava on Io: the nearest lava block within reach sets the offset. */
    public static double lavaHeat(Level level, BlockPos pos) {
        if (level.dimension() != JupiterModule.IO) {
            return 0;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        double best = Double.MAX_VALUE;
        for (int dx = -LAVA_REACH; dx <= LAVA_REACH; dx += 2) {
            for (int dz = -LAVA_REACH; dz <= LAVA_REACH; dz += 2) {
                for (int dy = -2; dy <= 1; dy++) {
                    p.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (level.isLoaded(p) && level.getFluidState(p).is(FluidTags.LAVA)) {
                        best = Math.min(best, Math.sqrt(dx * dx + dy * dy + dz * dz));
                    }
                }
            }
        }
        return best == Double.MAX_VALUE ? 0 : LAVA_HEAT * Math.max(0, 1 - best / (LAVA_REACH + 1));
    }

    static void translations(Lang lang) {
        lang.add("message.spacebreaker.jovian_radiation",
                "Jupiter's radiation belts: about %s Sv a day on open ground. Every solid block overhead halves it - dig in under ice or rock.",
                "Jüpiter'in radyasyon kuşakları: açık arazide günde yaklaşık %s Sv. Tepedeki her katı blok bunu yarıya indirir - buzun ya da kayanın altına sığın.");
    }
}
