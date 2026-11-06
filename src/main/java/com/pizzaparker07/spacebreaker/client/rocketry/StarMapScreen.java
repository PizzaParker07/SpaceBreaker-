package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketMath;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.item.Fmt;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Transfers;
import com.pizzaparker07.spacebreaker.content.rocketry.net.RocketActionPayload;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * The star map: Earth, Earth orbit, the Moon and its orbit, with the Δv of every route. Pick a
 * destination that the rocket can reach and burn. Warns when the trip would leave too little to get home.
 */
public class StarMapScreen extends Screen {
    private final RocketEntity rocket;
    private final Map<Location, int[]> nodes = new EnumMap<>(Location.class);
    private Location selected;
    private Button burn;

    public StarMapScreen(RocketEntity rocket) {
        super(Component.translatable("gui.spacebreaker.star_map"));
        this.rocket = rocket;
    }

    private int earthX() {
        return (int) (width * 0.30F);
    }

    private int earthY() {
        return (int) (height * 0.52F);
    }

    private int moonX() {
        return (int) (width * 0.74F);
    }

    private int moonY() {
        return (int) (height * 0.40F);
    }

    @Override
    protected void init() {
        nodes.put(Location.EARTH_SURFACE, new int[]{earthX(), earthY()});
        nodes.put(Location.EARTH_ORBIT, new int[]{earthX(), earthY() - 62});
        nodes.put(Location.MOON_ORBIT, new int[]{moonX(), moonY() - 34});
        nodes.put(Location.MOON_SURFACE, new int[]{moonX(), moonY()});
        burn = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.star_map.burn"), b -> confirm())
                .bounds(width / 2 - 50, height - 30, 100, 20).build());
        burn.active = false;
    }

    private Location here() {
        return rocket.location();
    }

    private OptionalDouble cost(Location to) {
        return Transfers.cost(here(), to, rocket.layout(), rocket.activeStage(), SbConfig.realism());
    }

    private boolean reachable(Location to) {
        OptionalDouble c = cost(to);
        return c.isPresent() && rocket.deltaV() >= c.getAsDouble();
    }

    private boolean canTravel() {
        return rocket.state() == FlightState.ORBIT;
    }

    private void confirm() {
        if (selected != null && canTravel()) {
            PacketDistributor.sendToServer(new RocketActionPayload(rocket.getId(), RocketActionPayload.TRAVEL, selected.ordinal()));
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Map.Entry<Location, int[]> e : nodes.entrySet()) {
            int[] p = e.getValue();
            if (Math.abs(mx - p[0]) <= 9 && Math.abs(my - p[1]) <= 9) {
                Location l = e.getKey();
                if (l != here() && cost(l).isPresent()) {
                    selected = l;
                    burn.active = canTravel() && reachable(l);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF04060B);
        RandomSource rng = RandomSource.create(7);
        for (int i = 0; i < 260; i++) {
            int x = rng.nextInt(Math.max(1, width));
            int y = rng.nextInt(Math.max(1, height));
            int b = 90 + rng.nextInt(140);
            g.fill(x, y, x + 1, y + 1, 0xFF000000 | b << 16 | b << 8 | b);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        float time = rocket.tickCount + partialTick;
        // orbits
        ring(g, earthX(), earthY(), 62, 0x506A8CB0);
        ring(g, moonX(), moonY(), 34, 0x50A0A0A0);
        ring(g, earthX(), earthY(), (int) Math.hypot(moonX() - earthX(), moonY() - earthY()), 0x18FFFFFF);
        // bodies
        g.blit(OrbitSkyEffects.EARTH_DISC, earthX() - 40, earthY() - 40, 80, 80, 0, 0, 64, 64, 64, 64);
        g.blit(OrbitSkyEffects.MOON_DISC, moonX() - 20, moonY() - 20, 40, 40, 0, 0, 64, 64, 64, 64);
        // routes: each pair once, the selected one highlighted, its cost in a small badge clear of the bodies
        for (Location from : Location.values()) {
            for (Location to : Transfers.destinations(from)) {
                if (from.ordinal() > to.ordinal() && Transfers.model().deltaV(to, from).isPresent()) {
                    continue;
                }
                int[] a = nodes.get(from), b = nodes.get(to);
                boolean mine = from == here() || to == here();
                Location target = from == here() ? to : to == here() ? from : null;
                boolean isSelected = target != null && target == selected;
                int col = isSelected ? 0xFFFFD34A : mine ? (target != null && reachable(target) ? 0xC08FD88A : 0xC0D9673A) : 0x38FFFFFF;
                float bow = bowFor(from, to);
                route(g, a, b, bow, col, isSelected ? 3 : 2);
                Location costFrom = to == here() ? to : from;
                Location costTo = to == here() ? from : to;
                OptionalDouble c = Transfers.cost(costFrom, costTo, rocket.layout(), rocket.activeStage(), SbConfig.realism());
                if (c.isPresent()) {
                    int[] p = badgeAt(from, to, a, b, bow);
                    badge(g, p[0], p[1], Fmt.ms(c.getAsDouble()), isSelected ? 0xFFFFD34A : mine ? 0xFFE0E6EC : 0xFF808A96);
                }
            }
        }
        // nodes and labels (labels sit beside their node, never on a route)
        for (Map.Entry<Location, int[]> e : nodes.entrySet()) {
            int[] p = e.getValue();
            Location l = e.getKey();
            boolean isHere = l == here();
            boolean hover = Math.abs(mouseX - p[0]) <= 9 && Math.abs(mouseY - p[1]) <= 9;
            int col = isHere ? 0xFFFFD34A : l == selected ? 0xFF8FD88A : hover ? 0xFFFFFFFF : 0xFFB0BCC8;
            int r = isHere ? 4 + (int) (Mth.sin(time * 0.2F) * 1.5F + 1.5F) : 4;
            g.fill(p[0] - r, p[1] - r, p[0] + r, p[1] + r, col);
            g.fill(p[0] - 2, p[1] - 2, p[0] + 2, p[1] + 2, 0xFF04060B);
            Component name = Component.translatable(l.translationKey());
            int tw = font.width(name);
            int lx, ly;
            switch (l) {
                case EARTH_SURFACE -> { lx = earthX() - tw / 2; ly = earthY() + 44; }
                case EARTH_ORBIT -> { lx = p[0] - tw - 10; ly = p[1] - 4; }
                case MOON_ORBIT -> { lx = p[0] + 10; ly = p[1] - 4; }
                default -> { lx = moonX() - tw / 2; ly = moonY() + 24; }
            }
            g.fill(lx - 2, ly - 2, lx + tw + 2, ly + 10, 0xA004060B);
            g.drawString(font, name, lx, ly, isHere ? 0xFFD34A : 0xD0D8E0, false);
        }
        // info panel
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        SbConfig.Realism realism = SbConfig.realism();
        if (realism != SbConfig.Realism.REALISTIC) {
            g.drawCenteredString(font, Component.translatable("gui.spacebreaker.star_map.scaled." + realism.name().toLowerCase(java.util.Locale.ROOT),
                    Math.round(Transfers.scale(realism) * 100)), width / 2, 20, 0x8A96A4);
        }
        int x = 10, y = height - 80;
        g.fill(x - 4, y - 4, x + 250, y + 56, 0xA0000000);
        g.drawString(font, Component.translatable("gui.spacebreaker.star_map.available", Fmt.ms(rocket.deltaV())), x, y, 0x9FE0A0, false);
        if (selected != null) {
            double c = cost(selected).orElse(0);
            g.drawString(font, Component.translatable("gui.spacebreaker.star_map.route", Component.translatable(selected.translationKey()), Fmt.ms(c)),
                    x, y + 11, 0xE0E6EC, false);
            Transfers.real(here(), selected).ifPresent(real -> {
                if (SbConfig.realism() != SbConfig.Realism.REALISTIC) {
                    Component r = Component.translatable("gui.spacebreaker.star_map.real", String.format(java.util.Locale.ROOT, "%.2f", real / 1000.0));
                    g.drawString(font, r, x + 236 - font.width(r), y + 11, 0x8A96A4, false);
                }
            });
            boolean can = reachable(selected);
            boolean home = MissionPlanner.canReturnAfter(rocket.layout(), rocket.activeStage(), rocket.fuel(), here(), selected, SbConfig.realism());
            Component verdict = !can ? Component.translatable("gui.spacebreaker.star_map.too_far")
                    : home ? Component.translatable("gui.spacebreaker.star_map.return_ok")
                    : Component.translatable("gui.spacebreaker.star_map.no_return");
            g.drawString(font, verdict, x, y + 22, !can ? 0xF0705E : home ? 0x8FD88A : 0xE8C060, false);
            Component window = LaunchWindows.describe(minecraft.level, here(), selected);
            if (window != null) {
                g.drawString(font, window, x, y + 44, 0xA0C8F0, false);
            }
            if (Transfers.reentryRating(here(), selected) > rocket.layout().payload().heatShield()) {
                g.drawString(font, Component.translatable("message.spacebreaker.warn.heat_shield"), x, y + 33, 0xE8C060, false);
            }
        } else {
            g.drawString(font, Component.translatable(canTravel() ? "gui.spacebreaker.star_map.pick" : "gui.spacebreaker.star_map.reach_orbit"),
                    x, y + 11, 0xA0A8B0, false);
        }
    }

    private static void ring(GuiGraphics g, int cx, int cy, int r, int color) {
        int steps = Math.max(24, r * 3);
        for (int i = 0; i < steps; i += 2) {
            double a = i * Math.PI * 2 / steps;
            int x = cx + (int) Math.round(Math.cos(a) * r);
            int y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    /** Upward arc for the outbound transfer, downward for the direct return, straight for surface hops. */
    private static float bowFor(Location from, Location to) {
        boolean surfaceHop = from.body() == to.body();
        if (surfaceHop) {
            return 0;
        }
        return (from == Location.EARTH_SURFACE || to == Location.EARTH_SURFACE) ? -28 : 22;
    }

    private static int[] point(int[] a, int[] b, float bow, float t) {
        float arc = (float) Math.sin(t * Math.PI) * bow;
        return new int[]{Math.round(Mth.lerp(t, a[0], b[0])), Math.round(Mth.lerp(t, a[1], b[1]) - arc)};
    }

    private int[] badgeAt(Location from, Location to, int[] a, int[] b, float bow) {
        if (bow == 0) {
            // short hop between a surface and its orbit: badge beside the line, away from the other body
            int side = from.body() == Location.EARTH_SURFACE.body() ? -48 : 44;
            return new int[]{(a[0] + b[0]) / 2 + side, (a[1] + b[1]) / 2};
        }
        return point(a, b, bow, 0.5F);
    }

    private void badge(GuiGraphics g, int cx, int cy, String text, int color) {
        int w = font.width(text) + 6;
        int x0 = cx - w / 2, y0 = cy - 6;
        g.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + 12, color);
        g.fill(x0, y0, x0 + w, y0 + 11, 0xF0101620);
        g.drawString(font, text, x0 + 3, y0 + 2, color, false);
    }

    private static void route(GuiGraphics g, int[] a, int[] b, float bow, int color, int size) {
        int steps = (int) Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1]));
        for (int i = 0; i <= steps; i += 3) {
            int[] p = point(a, b, bow, steps == 0 ? 0 : (float) i / steps);
            g.fill(p[0], p[1], p[0] + size, p[1] + size, color);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
