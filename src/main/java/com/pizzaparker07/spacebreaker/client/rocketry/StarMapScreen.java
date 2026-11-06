package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.item.Fmt;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Transfers;
import com.pizzaparker07.spacebreaker.content.rocketry.net.RocketActionPayload;
import com.pizzaparker07.spacebreaker.core.body.BodyType;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * The star map: every body rockets know about (from the celestial body data), planets in order of their
 * distance from the sun with their moons underneath. Each disc is a surface you can land on (if the body
 * has a dimension) and its ring is the low orbit. Destinations reachable from here are green, too
 * expensive ones red; hovering or selecting one shows the route, its Δv, the real value, transfer time and
 * launch window, and whether the trip still leaves enough to come home.
 */
public class StarMapScreen extends Screen {
    private record Disc(ResourceLocation body, int x, int y, int r) {
    }

    private final RocketEntity rocket;
    private final Map<ResourceLocation, Disc> discs = new LinkedHashMap<>();
    private Location selected;
    private Button burn;

    public StarMapScreen(RocketEntity rocket) {
        super(Component.translatable("gui.spacebreaker.star_map"));
        this.rocket = rocket;
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        discs.clear();
        List<ResourceLocation> planets = new ArrayList<>();
        Map<ResourceLocation, List<ResourceLocation>> moons = new LinkedHashMap<>();
        for (Location l : Locations.all()) {
            ResourceLocation b = l.body();
            if (discs.containsKey(b) || planets.contains(b) || moons.values().stream().anyMatch(m -> m.contains(b))) {
                continue;
            }
            var parent = Locations.parentPlanet(b);
            if (parent.isPresent()) {
                moons.computeIfAbsent(parent.get(), k -> new ArrayList<>()).add(b);
            } else {
                planets.add(b);
            }
        }
        int n = Math.max(1, planets.size());
        int left = 40, right = width - 40;
        int top = (int) (height * 0.30F);
        int step = n == 1 ? 0 : (right - left) / (n - 1);
        for (int i = 0; i < planets.size(); i++) {
            ResourceLocation p = planets.get(i);
            int x = n == 1 ? width / 2 : left + i * step;
            int r = radiusFor(p);
            discs.put(p, new Disc(p, x, top, r));
            List<ResourceLocation> ms = moons.getOrDefault(p, List.of());
            int my = top + r + 34;
            for (ResourceLocation m : ms) {
                int mr = radiusFor(m);
                discs.put(m, new Disc(m, x, my + mr, mr));
                my += mr * 2 + 30;
            }
        }
        burn = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.star_map.burn"), b -> confirm())
                .bounds(width - 112, height - 28, 100, 20).build());
        updateBurn();
    }

    private static int radiusFor(ResourceLocation body) {
        if (Location.EARTH.equals(body)) {
            return 16;
        }
        CelestialBody b = Locations.body(body).orElse(null);
        if (b == null) {
            return 8;
        }
        if (b.type() == BodyType.GAS_GIANT || b.type() == BodyType.ICE_GIANT) {
            return 18;
        }
        double km = b.physical().radiusKm();
        return Mth.clamp((int) Math.round(4 + Math.sqrt(km) / 7.0), 5, 14);
    }

    private int[] nodePos(Location l) {
        Disc d = discs.get(l.body());
        if (d == null) {
            return new int[]{-100, -100};
        }
        return l.orbit() ? new int[]{d.x + d.r + 5, d.y - d.r - 5} : new int[]{d.x, d.y};
    }

    @Nullable
    private Location nodeAt(double mx, double my) {
        Location best = null;
        double bestD = 64;
        for (Location l : Locations.all()) {
            int[] p = nodePos(l);
            double d = (mx - p[0]) * (mx - p[0]) + (my - p[1]) * (my - p[1]);
            if (d < bestD) {
                bestD = d;
                best = l;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ state

    private Location here() {
        return rocket.location();
    }

    private OptionalDouble cost(Location to) {
        return Transfers.cost(here(), to, rocket.layout(), rocket.activeStage(), SbConfig.realism());
    }

    private boolean installed(Location l) {
        var conn = minecraft == null || minecraft.getConnection() == null ? null : minecraft.getConnection().levels();
        return conn == null || conn.contains(l.dimension());
    }

    private boolean reachable(Location to) {
        OptionalDouble c = cost(to);
        return c.isPresent() && rocket.deltaV() >= c.getAsDouble() && installed(to);
    }

    private boolean canTravel() {
        return rocket.state() == FlightState.ORBIT;
    }

    private void updateBurn() {
        if (burn != null) {
            burn.active = selected != null && canTravel() && reachable(selected);
        }
    }

    private void confirm() {
        if (selected != null && canTravel()) {
            PacketDistributor.sendToServer(new RocketActionPayload(rocket.getId(), RocketActionPayload.TRAVEL, 0, selected.id()));
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Location l = nodeAt(mx, my);
        if (l != null && !l.equals(here()) && cost(l).isPresent()) {
            selected = l;
            updateBurn();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    // ------------------------------------------------------------------ drawing

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
        Location hover = nodeAt(mouseX, mouseY);
        Set<Location> dests = Set.copyOf(Transfers.destinations(here()));
        // the plane of the planets
        int top = (int) (height * 0.30F);
        for (int x = 20; x < width - 20; x += 4) {
            g.fill(x, top, x + 2, top + 1, 0x18FFFFFF);
        }
        // bodies with their orbit rings and names
        for (Disc d : discs.values()) {
            ring(g, d.x, d.y, d.r + 5, 0x40A0B4C8);
            int size = d.r * 2;
            g.blit(OrbitSkyEffects.disc(d.body), d.x - d.r, d.y - d.r, size, size, 0, 0, 64, 64, 64, 64);
            Component name = Location.bodyName(d.body);
            int tw = font.width(name);
            g.drawString(font, name, d.x - tw / 2, d.y + d.r + 8, 0xC8D0D8, false);
        }
        // route to the hovered or selected destination
        Location focus = hover != null && dests.contains(hover) ? hover : selected;
        if (focus != null && dests.contains(focus)) {
            route(g, nodePos(here()), nodePos(focus), reachable(focus) ? 0xFFFFD34A : 0xC0D9673A);
            cost(focus).ifPresent(c -> {
                int[] a = nodePos(here()), b = nodePos(focus);
                badge(g, (a[0] + b[0]) / 2, (a[1] + b[1]) / 2 - 14, Fmt.ms(c), reachable(focus) ? 0xFFFFD34A : 0xFFD9673A);
            });
        }
        // nodes: here (pulsing), destinations (green / red), others (grey)
        for (Location l : Locations.all()) {
            int[] p = nodePos(l);
            boolean isHere = l.equals(here());
            int col;
            if (isHere) {
                col = 0xFFFFD34A;
            } else if (dests.contains(l)) {
                col = !installed(l) ? 0xFF5A6470 : reachable(l) ? 0xFF8FD88A : 0xFFD9673A;
            } else {
                col = 0xFF5A6470;
            }
            if (l.equals(selected)) {
                col = 0xFFFFFFFF;
            }
            int r = isHere ? 3 + (int) (Mth.sin(time * 0.2F) * 1.5F + 1.5F) : (l.equals(hover) ? 4 : 3);
            g.fill(p[0] - r, p[1] - r, p[0] + r, p[1] + r, col);
            g.fill(p[0] - 1, p[1] - 1, p[0] + 1, p[1] + 1, 0xFF04060B);
        }
        if (hover != null) {
            Component h = hover.name();
            int[] p = nodePos(hover);
            int tw = font.width(h);
            g.fill(p[0] + 6, p[1] - 14, p[0] + tw + 10, p[1] - 3, 0xE0101620);
            g.drawString(font, h, p[0] + 8, p[1] - 12, 0xFFFFFF, false);
        }
        drawHeader(g);
        drawInfo(g);
    }

    private void drawHeader(GuiGraphics g) {
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        SbConfig.Realism realism = SbConfig.realism();
        if (realism != SbConfig.Realism.REALISTIC) {
            g.drawCenteredString(font, Component.translatable("gui.spacebreaker.star_map.scaled." + realism.name().toLowerCase(Locale.ROOT),
                    Math.round(Transfers.scale(realism) * 100)), width / 2, 20, 0x8A96A4);
        }
    }

    private void drawInfo(GuiGraphics g) {
        int x = 10, y = height - 70;
        g.fill(x - 4, y - 4, x + 290, y + 62, 0xC0000000);
        g.drawString(font, Component.translatable("gui.spacebreaker.star_map.here", here().name()), x, y, 0xFFD34A, false);
        g.drawString(font, Component.translatable("gui.spacebreaker.star_map.available", Fmt.ms(rocket.deltaV())), x + 150, y, 0x9FE0A0, false);
        if (selected == null) {
            g.drawString(font, Component.translatable(canTravel() ? "gui.spacebreaker.star_map.pick" : "gui.spacebreaker.star_map.reach_orbit"),
                    x, y + 12, 0xA0A8B0, false);
            return;
        }
        double c = cost(selected).orElse(0);
        g.drawString(font, Component.translatable("gui.spacebreaker.star_map.route", selected.name(), Fmt.ms(c)), x, y + 12, 0xE0E6EC, false);
        Transfers.real(here(), selected).ifPresent(real -> {
            if (SbConfig.realism() != SbConfig.Realism.REALISTIC) {
                Component r = Component.translatable("gui.spacebreaker.star_map.real", String.format(Locale.ROOT, "%.2f", real / 1000.0));
                g.drawString(font, r, x + 282 - font.width(r), y + 12, 0x8A96A4, false);
            }
        });
        Component verdict;
        int col;
        if (!installed(selected)) {
            verdict = Component.translatable("message.spacebreaker.travel.unavailable", selected.name());
            col = 0xF0705E;
        } else if (!reachable(selected)) {
            verdict = Component.translatable("gui.spacebreaker.star_map.too_far");
            col = 0xF0705E;
        } else if (MissionPlanner.canReturnAfter(rocket.layout(), rocket.activeStage(), rocket.fuel(), here(), selected, SbConfig.realism())) {
            verdict = Component.translatable("gui.spacebreaker.star_map.return_ok");
            col = 0x8FD88A;
        } else {
            verdict = Component.translatable("gui.spacebreaker.star_map.no_return");
            col = 0xE8C060;
        }
        g.drawString(font, verdict, x, y + 24, col, false);
        if (Transfers.reentryRating(here(), selected) > rocket.layout().payload().heatShield()) {
            g.drawString(font, Component.translatable("message.spacebreaker.warn.heat_shield"), x, y + 36, 0xE8C060, false);
        } else if (!selected.orbit() && !selected.isEarth() && !rocket.layout().hasLegsFrom(rocket.activeStage())) {
            g.drawString(font, Component.translatable("message.spacebreaker.warn.no_legs"), x, y + 36, 0xE8C060, false);
        }
        Component window = LaunchWindows.describe(minecraft.level, here(), selected);
        if (window != null) {
            g.drawString(font, window, x, y + 48, 0xA0C8F0, false);
        }
    }

    private void badge(GuiGraphics g, int cx, int cy, String text, int color) {
        int w = font.width(text) + 6;
        int x0 = cx - w / 2, y0 = cy - 6;
        g.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + 12, color);
        g.fill(x0, y0, x0 + w, y0 + 11, 0xF0101620);
        g.drawString(font, text, x0 + 3, y0 + 2, color, false);
    }

    private static void ring(GuiGraphics g, int cx, int cy, int r, int color) {
        int steps = Math.max(24, r * 4);
        for (int i = 0; i < steps; i += 2) {
            double a = i * Math.PI * 2 / steps;
            int x = cx + (int) Math.round(Math.cos(a) * r);
            int y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void route(GuiGraphics g, int[] a, int[] b, int color) {
        int steps = (int) Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1]));
        float bow = Math.min(40, steps * 0.15F);
        for (int i = 0; i <= steps; i += 3) {
            float t = steps == 0 ? 0 : (float) i / steps;
            float arc = (float) Math.sin(t * Math.PI) * bow;
            int x = Math.round(Mth.lerp(t, a[0], b[0]));
            int y = Math.round(Mth.lerp(t, a[1], b[1]) - arc);
            g.fill(x, y, x + 2, y + 2, color);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
