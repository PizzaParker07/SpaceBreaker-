package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.AstroTransferModel;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Transfers;
import com.pizzaparker07.spacebreaker.content.rocketry.net.RocketActionPayload;
import com.pizzaparker07.spacebreaker.core.body.BodyType;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
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

/**
 * The star map. The Sun sits on the left with every planet on its orbit; clicking a planet zooms into its
 * system, with its moons around it. The panel on the right describes the selected world (picture, a few
 * words, its dangers) and offers "Go to orbit" / "Land", each with a plain fuel check (fuel needed against
 * fuel on board, enough to come home?), the trip time, heat shield and leg warnings and the launch window.
 * The route from here is drawn step by step on the map and the big Go button says why when it is off.
 */
public class StarMapScreen extends Screen {
    private record Node(ResourceLocation body, int x, int y, int r) {
    }

    private static final int GREEN = 0xFF8FD88A, RED = 0xFFEF6A5A, AMBER = 0xFFE8C060, INK = 0xFFDDE4EC, DIM = 0xFF8A96A4;
    private static final int PANEL_BG = 0xE00B1018, ACCENT = 0xFFFFD34A;

    private final RocketEntity rocket;
    private final Map<ResourceLocation, Node> nodes = new LinkedHashMap<>();
    @Nullable
    private ResourceLocation focus;
    private ResourceLocation body;
    @Nullable
    private Location target;
    @Nullable
    private StarMapData.Trip trip;
    private Button orbitButton, surfaceButton, goButton, backButton;
    private int panelX, panelW, mapW;
    private int fuelBarY = -1;

    public StarMapScreen(RocketEntity rocket) {
        super(Component.translatable("gui.spacebreaker.star_map"));
        this.rocket = rocket;
        this.body = rocket.location().body();
    }

    // ------------------------------------------------------------------ state

    private Location here() {
        return rocket.location();
    }

    private boolean built(Location l) {
        var levels = minecraft == null || minecraft.getConnection() == null ? null : minecraft.getConnection().levels();
        return levels == null || levels.contains(l.dimension());
    }

    private void select(ResourceLocation b) {
        body = b;
        target = null;
        trip = null;
        updateButtons();
    }

    private void choose(@Nullable Location l) {
        target = l;
        trip = l == null ? null : StarMapData.trip(rocket.layout(), rocket.activeStage(), rocket.fuel(), rocket.deltaV(), here(), l,
                SbConfig.realism(), rocket.state() == FlightState.ORBIT, built(l));
        updateButtons();
    }

    private void updateButtons() {
        if (orbitButton == null) {
            return;
        }
        Location orbit = Locations.get(body, true).orElse(null);
        Location surface = Locations.get(body, false).orElse(null);
        orbitButton.active = orbit != null && !orbit.equals(here());
        surfaceButton.visible = surface != null;
        surfaceButton.active = surface != null && !surface.equals(here()) && built(surface);
        surfaceButton.setMessage(Component.translatable(surface != null && !built(surface)
                ? "gui.spacebreaker.star_map.soon" : "gui.spacebreaker.star_map.land"));
        goButton.active = trip != null && trip.canGo();
        goButton.visible = trip != null;
        backButton.visible = focus != null;
    }

    private void go() {
        if (trip != null && trip.canGo()) {
            PacketDistributor.sendToServer(new RocketActionPayload(rocket.getId(), RocketActionPayload.TRAVEL, 0, trip.next().id()));
            onClose();
        }
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        panelW = Mth.clamp((int) (width * 0.36F), 150, 214);
        panelX = width - panelW;
        mapW = panelX;
        int bx = panelX + 8, bw = (panelW - 20) / 2;
        int by = 148;
        orbitButton = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.star_map.to_orbit"),
                b -> choose(Locations.get(body, true).orElse(null))).bounds(bx, by, bw, 18).build());
        surfaceButton = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.star_map.land"),
                b -> choose(Locations.get(body, false).orElse(null))).bounds(bx + bw + 4, by, bw, 18).build());
        goButton = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.star_map.go"), b -> go())
                .bounds(panelX + 8, height - 30, panelW - 16, 22).build());
        backButton = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.star_map.back"), b -> {
            focus = null;
            layoutMap();
            updateButtons();
        }).bounds(8, height - 26, 110, 18).build());
        layoutMap();
        if (target != null) {
            choose(target);
        }
        updateButtons();
    }

    private void layoutMap() {
        nodes.clear();
        int cy = height / 2 + 6;
        if (focus == null) {
            List<ResourceLocation> planets = StarMapData.planets();
            int n = Math.max(1, planets.size());
            int left = sunEdge() + 26, right = mapW - 22;
            float step = n == 1 ? 0 : (right - left) / (float) (n - 1);
            for (int i = 0; i < planets.size(); i++) {
                ResourceLocation p = planets.get(i);
                int x = Math.round(left + i * step);
                // stagger up and down along the orbits so the names never collide
                int y = cy + (i % 2 == 0 ? -1 : 1) * (int) (height * 0.09F);
                nodes.put(p, new Node(p, x, y, systemRadius(p)));
            }
        } else {
            int cx = mapW / 2;
            int pr = Mth.clamp(Math.min(mapW, height) / 9, 14, 30);
            nodes.put(focus, new Node(focus, cx, cy, pr));
            List<ResourceLocation> moons = StarMapData.moons(focus);
            int first = pr + 46;
            int ringStep = Math.max(22, (Math.min(mapW, height) / 2 - first - 16) / Math.max(1, moons.size()));
            for (int i = 0; i < moons.size(); i++) {
                ResourceLocation m = moons.get(i);
                int ring = first + i * ringStep;
                double a = 0.35 + i * 2.4;
                nodes.put(m, new Node(m, cx + (int) (Math.cos(a) * ring), cy + (int) (Math.sin(a) * ring * 0.8), moonRadius(m)));
            }
        }
    }

    private int sunEdge() {
        return (int) (height * 0.13F);
    }

    private static int systemRadius(ResourceLocation body) {
        if (Location.EARTH.equals(body)) {
            return 9;
        }
        CelestialBody b = Locations.body(body).orElse(null);
        if (b == null) {
            return 6;
        }
        if (b.type() == BodyType.GAS_GIANT) {
            return 15;
        }
        if (b.type() == BodyType.ICE_GIANT) {
            return 12;
        }
        return Mth.clamp((int) Math.round(3 + Math.sqrt(b.physical().radiusKm()) / 9.0), 4, 9);
    }

    private static int moonRadius(ResourceLocation body) {
        double km = Locations.body(body).map(b -> b.physical().radiusKm()).orElse(500.0);
        return Mth.clamp((int) Math.round(3 + Math.sqrt(km) / 8.0), 4, 10);
    }

    /** Where a location shows on the current view (moons of other systems sit on their planet). */
    @Nullable
    private int[] posOf(Location l) {
        Node n = nodes.get(l.body());
        if (n == null) {
            n = nodes.get(StarMapData.system(l.body()));
        }
        if (n == null) {
            return null;
        }
        return l.orbit() && nodes.containsKey(l.body()) ? new int[]{n.x + n.r + 4, n.y - n.r - 4} : new int[]{n.x, n.y};
    }

    @Nullable
    private Node nodeAt(double mx, double my) {
        Node best = null;
        double bestD = Double.MAX_VALUE;
        for (Node n : nodes.values()) {
            double d = Math.hypot(mx - n.x, my - n.y);
            if (d <= n.r + 6 && d < bestD) {
                best = n;
                bestD = d;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (mx >= panelX) {
            return false;
        }
        if (button == 1 && focus != null) {
            focus = null;
            layoutMap();
            updateButtons();
            return true;
        }
        Node n = nodeAt(mx, my);
        if (n == null) {
            return false;
        }
        if (focus == null && !StarMapData.moons(n.body).isEmpty()) {
            focus = n.body;
            layoutMap();
        }
        select(n.body);
        return true;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF03050A);
        RandomSource rng = RandomSource.create(7);
        for (int i = 0; i < 260; i++) {
            int x = rng.nextInt(Math.max(1, width));
            int y = rng.nextInt(Math.max(1, height));
            int b = 70 + rng.nextInt(120);
            g.fill(x, y, x + 1, y + 1, 0xFF000000 | b << 16 | b << 8 | b);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        float time = rocket.tickCount + partialTick;
        if (focus == null) {
            drawSystem(g);
        } else {
            drawFocus(g);
        }
        drawRoute(g, time);
        drawNodes(g, mouseX, mouseY, time);
        drawHere(g, time);
        drawHeader(g);
        drawPanel(g, mouseX, mouseY);
        for (var r : renderables) {
            r.render(g, mouseX, mouseY, partialTick);
        }
        if (trip != null && fuelBarY > 0 && mouseX >= panelX && mouseY >= fuelBarY - 12 && mouseY <= fuelBarY + 22) {
            g.renderTooltip(font, font.split(Component.translatable("gui.spacebreaker.star_map.delta_v_help"), 200), mouseX, mouseY);
        }
    }

    private void drawSystem(GuiGraphics g) {
        int cy = height / 2 + 6;
        int sunR = (int) (height * 0.30F);
        int sunX = sunEdge() - sunR;
        // the Sun, cut by the left edge, with a soft glow
        for (int i = 6; i > 0; i--) {
            disc(g, sunX, cy, sunR + i * 5, 0x0CFFC860);
        }
        disc(g, sunX, cy, sunR, 0xFFFFC14A);
        disc(g, sunX, cy, sunR - 4, 0xFFFFD873);
        g.drawString(font, Component.translatable("gui.spacebreaker.star_map.sun"), 3, cy - 4, 0xFF6A4208, false);
        // orbits as dotted arcs around the Sun
        for (Node n : nodes.values()) {
            double r = Math.hypot(n.x - sunX, n.y - cy);
            int steps = (int) (r * 2.2);
            for (int i = 0; i < steps; i += 3) {
                double a = -Math.PI / 2 + Math.PI * i / steps;
                int x = sunX + (int) (Math.cos(a) * r), y = cy + (int) (Math.sin(a) * r);
                if (x > sunEdge() && x < mapW - 2 && y > 22 && y < height - 4) {
                    g.fill(x, y, x + 1, y + 1, 0x30A0B4C8);
                }
            }
        }
    }

    private void drawFocus(GuiGraphics g) {
        Node p = nodes.get(focus);
        if (p == null) {
            return;
        }
        for (Node n : nodes.values()) {
            if (n != p) {
                double r = Math.hypot(n.x - p.x, (n.y - p.y) / 0.8);
                ellipse(g, p.x, p.y, (int) r, (int) (r * 0.8), 0x30A0B4C8);
            }
        }
        g.drawCenteredString(font, Component.translatable("gui.spacebreaker.star_map.system_of", Location.bodyName(focus)), mapW / 2, 34, INK);
    }

    private void drawNodes(GuiGraphics g, int mx, int my, float time) {
        Node hover = mx < panelX ? nodeAt(mx, my) : null;
        for (Node n : nodes.values()) {
            boolean sel = n.body.equals(body);
            if (sel || n == hover) {
                ring(g, n.x, n.y, n.r + 4, sel ? 0xC0FFD34A : 0x80FFFFFF);
            }
            planet(g, n.body, n.x, n.y, n.r);
            Component name = Location.bodyName(n.body);
            int col = sel ? 0xFFFFE9A8 : 0xFFC8D0D8;
            g.drawCenteredString(font, name, n.x, n.y + n.r + 5, col);
            if (focus == null) {
                int moons = StarMapData.moons(n.body).size();
                for (int i = 0; i < Math.min(moons, 8); i++) {
                    g.fill(n.x - moons * 2 + i * 4, n.y + n.r + 15, n.x - moons * 2 + i * 4 + 2, n.y + n.r + 17, 0xFF8A96A4);
                }
            }
        }
    }

    private void drawHere(GuiGraphics g, float time) {
        int[] p = posOf(here());
        if (p == null) {
            return;
        }
        int bob = (int) (Mth.sin(time * 0.15F) * 2);
        int x = p[0], y = p[1] - 14 + bob;
        g.fill(x - 3, y - 7, x + 4, y, ACCENT);
        g.fill(x - 1, y, x + 2, y + 5, ACCENT);
        g.fill(x - 1, y - 5, x + 2, y - 2, 0xFF3A2A00);
        Component label = Component.translatable("gui.spacebreaker.star_map.you_are_here");
        int tw = font.width(label);
        g.fill(x - tw / 2 - 3, y - 20, x + tw / 2 + 3, y - 9, 0xE0201606);
        g.drawCenteredString(font, label, x, y - 18, ACCENT);
    }

    private void drawRoute(GuiGraphics g, float time) {
        if (trip == null) {
            return;
        }
        int colour = trip.enough() ? 0xFFFFD34A : 0xFFEF6A5A;
        List<int[]> pts = new ArrayList<>();
        for (Location l : trip.route()) {
            int[] p = posOf(l);
            if (p != null && (pts.isEmpty() || pts.get(pts.size() - 1)[0] != p[0] || pts.get(pts.size() - 1)[1] != p[1])) {
                pts.add(p);
            }
        }
        int dash = (int) (time / 2) % 6;
        for (int i = 0; i + 1 < pts.size(); i++) {
            int[] a = pts.get(i), b = pts.get(i + 1);
            int steps = (int) Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1]));
            float bow = Math.min(30, steps * 0.18F);
            for (int s = 0; s <= steps; s++) {
                if ((s + dash) % 6 >= 3) {
                    continue;
                }
                float t = steps == 0 ? 0 : (float) s / steps;
                int x = Math.round(Mth.lerp(t, a[0], b[0]));
                int y = Math.round(Mth.lerp(t, a[1], b[1]) - (float) Math.sin(t * Math.PI) * bow);
                g.fill(x, y, x + 2, y + 2, colour);
            }
            // numbered stop
            g.fill(b[0] - 5, b[1] - 5, b[0] + 6, b[1] + 6, colour);
            g.fill(b[0] - 4, b[1] - 4, b[0] + 5, b[1] + 5, 0xFF0B1018);
            g.drawCenteredString(font, String.valueOf(i + 1), b[0] + 1, b[1] - 3, colour);
        }
    }

    private void drawHeader(GuiGraphics g) {
        g.drawString(font, title.copy().withStyle(ChatFormatting.BOLD), 8, 8, INK, false);
        SbConfig.Realism realism = SbConfig.realism();
        if (realism != SbConfig.Realism.REALISTIC) {
            Component note = Component.translatable("gui.spacebreaker.star_map.mode." + realism.name().toLowerCase(Locale.ROOT),
                    Math.round(Transfers.scale(realism) * 100));
            g.drawString(font, note, 8, 19, DIM, false);
        }
    }

    // ------------------------------------------------------------------ panel

    private void drawPanel(GuiGraphics g, int mx, int my) {
        int x = panelX + 8, w = panelW - 16;
        g.fill(panelX, 0, width, height, PANEL_BG);
        g.fill(panelX, 0, panelX + 1, height, 0x60A0B4C8);
        // picture and name
        int disc = 44;
        planet(g, body, x + disc / 2, 8 + disc / 2, disc / 2);
        g.drawString(font, Location.bodyName(body).copy().withStyle(ChatFormatting.BOLD), x + disc + 6, 12, 0xFFFFFFFF, false);
        Component kind = Component.translatable("gui.spacebreaker.star_map.kind." + Locations.body(body).map(b -> b.type().name()).orElse("TERRESTRIAL")
                .toLowerCase(Locale.ROOT));
        g.drawString(font, kind, x + disc + 6, 24, DIM, false);
        // a few words about it
        int y = 8 + disc + 6;
        Component desc = Component.translatableWithFallback("gui.spacebreaker.star_map.about." + body.getPath(),
                Component.translatable("gui.spacebreaker.star_map.about_kind." + Locations.body(body).map(b -> b.type().name()).orElse("TERRESTRIAL")
                        .toLowerCase(Locale.ROOT)).getString());
        for (FormattedCharSequence line : font.split(desc, w)) {
            if (y > 112) {
                break;
            }
            g.drawString(font, line, x, y, 0xFFB8C2CC, false);
            y += 9;
        }
        // dangers
        y = Math.max(y + 3, 116);
        int hx = x;
        for (StarMapData.Hazard hz : Locations.body(body).map(StarMapData::hazards).orElse(java.util.Set.of())) {
            Component t = Component.translatable(hz.key());
            int tw = font.width(t) + 8;
            if (hx + tw > x + w) {
                hx = x;
                y += 13;
            }
            int col = hazardColour(hz);
            g.fill(hx, y, hx + tw, y + 11, (col & 0xFFFFFF) | 0x40000000);
            g.fill(hx, y, hx + 2, y + 11, col);
            g.drawString(font, t, hx + 5, y + 2, col, false);
            hx += tw + 3;
        }
        orbitButton.setY(Math.max(148, y + 16));
        surfaceButton.setY(orbitButton.getY());
        if (!surfaceButton.visible) {
            g.drawString(font, Component.translatable("gui.spacebreaker.star_map.no_surface"), x + w / 2 + 4, orbitButton.getY() + 5, DIM, false);
        }
        fuelBarY = -1;
        if (trip == null) {
            g.drawWordWrap(font, Component.translatable("gui.spacebreaker.star_map.pick_target"), x, orbitButton.getY() + 26, w, DIM);
            return;
        }
        drawTrip(g, x, orbitButton.getY() + 26, w);
    }

    private void drawTrip(GuiGraphics g, int x, int y, int w) {
        StarMapData.Trip t = trip;
        g.drawString(font, Component.translatable("gui.spacebreaker.star_map.trip_to", t.to().name()), x, y, 0xFFFFFFFF, false);
        y += 12;
        // fuel: needed against on board, as one bar
        fuelBarY = y;
        g.drawString(font, Component.translatable("gui.spacebreaker.star_map.fuel_line", num(t.needed()), num(t.have())), x, y, INK, false);
        y += 11;
        float fill = t.needed() <= 0 ? 1F : (float) Math.min(1, t.have() / t.needed());
        g.fill(x, y, x + w, y + 6, 0xFF1C222A);
        g.fill(x, y, x + (int) (w * fill), y + 6, t.enough() ? GREEN : RED);
        if (t.needed() > 0 && t.have() > t.needed()) {
            int mark = x + (int) (w * t.needed() / t.have());
            g.fill(mark, y - 1, mark + 1, y + 7, 0xFFFFFFFF);
        }
        y += 9;
        Component verdict = t.enough() ? Component.translatable("gui.spacebreaker.star_map.enough")
                : Component.translatable("gui.spacebreaker.star_map.short", num(t.shortBy()));
        g.drawString(font, verdict, x, y, t.enough() ? GREEN : RED, false);
        y += 12;
        g.drawString(font, Component.translatable(t.returnHome() ? "gui.spacebreaker.star_map.return_yes" : "gui.spacebreaker.star_map.return_no"),
                x, y, t.returnHome() ? GREEN : AMBER, false);
        y += 11;
        OptionalDouble days = travelDays(t);
        if (days.isPresent()) {
            g.drawString(font, Component.translatable("gui.spacebreaker.star_map.travel_time", decimal(days.getAsDouble())), x, y, INK, false);
            y += 11;
        }
        Component window = LaunchWindows.describe(minecraft.level, here(), t.to());
        if (window != null) {
            for (FormattedCharSequence line : font.split(window, w)) {
                g.drawString(font, line, x, y, 0xFFA0C8F0, false);
                y += 9;
            }
            y += 2;
        }
        if (!t.shieldOk()) {
            y = warn(g, Component.translatable("gui.spacebreaker.star_map.need_shield"), x, y, w);
        }
        if (t.legsMissing()) {
            y = warn(g, Component.translatable("gui.spacebreaker.star_map.need_legs"), x, y, w);
        }
        if (!t.to().orbit() && t.to().atmosphere() && !rocket.layout().payload().parachute()) {
            y = warn(g, Component.translatable("gui.spacebreaker.star_map.need_chute"), x, y, w);
        }
        if (!t.next().equals(t.to()) && t.canGo()) {
            y = warn(g, Component.translatable("gui.spacebreaker.star_map.first_stop", t.next().name()), x, y, w);
        }
        if (!t.canGo()) {
            Component why = Component.translatable(t.block().key());
            List<FormattedCharSequence> lines = font.split(why, w);
            int ry = height - 34 - lines.size() * 9;
            for (FormattedCharSequence line : lines) {
                g.drawString(font, line, x, ry, RED, false);
                ry += 9;
            }
        }
    }

    private OptionalDouble travelDays(StarMapData.Trip t) {
        if (!(Transfers.model() instanceof AstroTransferModel astro)) {
            return OptionalDouble.empty();
        }
        double secs = 0;
        List<Location> path = StarMapData.flightPath(here(), t.to());
        for (int i = 0; i + 1 < path.size(); i++) {
            secs += astro.transferSeconds(path.get(i), path.get(i + 1)).orElse(0);
        }
        return secs > 3600 ? OptionalDouble.of(secs / 86400.0) : OptionalDouble.empty();
    }

    private int warn(GuiGraphics g, Component text, int x, int y, int w) {
        for (FormattedCharSequence line : font.split(Component.literal("! ").append(text), w)) {
            g.drawString(font, line, x, y, AMBER, false);
            y += 9;
        }
        return y + 1;
    }

    private static int hazardColour(StarMapData.Hazard h) {
        return switch (h) {
            case NO_AIR, THIN_AIR -> 0xFF9FC4E8;
            case CRUSHING_AIR -> 0xFFE89F6A;
            case HOT -> 0xFFF07A4A;
            case COLD -> 0xFF8ED8F0;
            case RADIATION -> 0xFFD8E85A;
        };
    }

    private boolean turkish() {
        return minecraft != null && minecraft.getLanguageManager().getSelected().startsWith("tr");
    }

    /** Whole number with thousands separators in the reader's language ("3.900" in Turkish, "3,900" in English). */
    private String num(double v) {
        String s = String.format(Locale.ROOT, "%,d", Math.round(v));
        return turkish() ? s.replace(',', '.') : s;
    }

    private String decimal(double v) {
        String s = String.format(Locale.ROOT, "%.1f", v);
        return turkish() ? s.replace('.', ',') : s;
    }

    // ------------------------------------------------------------------ primitives

    private static void planet(GuiGraphics g, ResourceLocation body, int cx, int cy, int r) {
        ResourceLocation tex = Location.MOON.equals(body) ? OrbitSkyEffects.MOON_DISC : OrbitSkyEffects.disc(body);
        g.blit(tex, cx - r, cy - r, r * 2, r * 2, 0F, 0F, 1, 1, 1, 1);
    }

    private static void disc(GuiGraphics g, int cx, int cy, int r, int colour) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, colour);
        }
    }

    private static void ring(GuiGraphics g, int cx, int cy, int r, int colour) {
        ellipse(g, cx, cy, r, r, colour);
    }

    private static void ellipse(GuiGraphics g, int cx, int cy, int rx, int ry, int colour) {
        int steps = Math.max(24, Math.max(rx, ry) * 4);
        for (int i = 0; i < steps; i += 2) {
            double a = i * Math.PI * 2 / steps;
            int x = cx + (int) Math.round(Math.cos(a) * rx);
            int y = cy + (int) Math.round(Math.sin(a) * ry);
            g.fill(x, y, x + 1, y + 1, colour);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Opened from the HUD hint or the key; also used by the showcase to preselect a destination. */
    public void preselect(ResourceLocation b, boolean orbit) {
        if (Locations.parentPlanet(b).isPresent()) {
            focus = StarMapData.system(b);
            layoutMap();
        }
        select(b);
        choose(Locations.get(b, orbit).orElse(null));
    }
}
