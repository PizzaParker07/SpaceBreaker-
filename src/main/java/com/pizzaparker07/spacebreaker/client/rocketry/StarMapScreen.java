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
    private Button orbitButton, surfaceButton, goButton, backButton, starsButton;
    /** Third zoom level: the stars around the Sun (information only, no interstellar travel yet). */
    private boolean starsView;
    @Nullable
    private com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.Star star;
    @Nullable
    private int[][] galaxyColours;
    private int panelX, panelW, mapW;
    private int fuelBarY = -1;

    /** Opened from the launch console of an uncrewed rocket: Go sets the mission target instead of flying. */
    private final boolean planning;

    public StarMapScreen(RocketEntity rocket) {
        this(rocket, false);
    }

    public StarMapScreen(RocketEntity rocket, boolean planning) {
        super(Component.translatable(planning ? "gui.spacebreaker.star_map.planning" : "gui.spacebreaker.star_map"));
        this.rocket = rocket;
        this.planning = planning;
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
                SbConfig.realism(), planning || rocket.state() == FlightState.ORBIT, built(l));
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
        surfaceButton.active = surface != null && !surface.equals(here()) && built(surface)
                && !(planning && com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.payload(rocket)
                .map(p -> p.type() == com.pizzaparker07.spacebreaker.content.rocketry.part.PartType.SATELLITE).orElse(false));
        surfaceButton.setMessage(Component.translatable(surface != null && !built(surface)
                ? "gui.spacebreaker.star_map.soon" : "gui.spacebreaker.star_map.land"));
        goButton.active = trip != null && trip.canGo();
        goButton.visible = trip != null;
        backButton.visible = focus != null || starsView;
        starsButton.visible = focus == null && !starsView;
        if (starsView) {
            orbitButton.visible = false;
            surfaceButton.visible = false;
            goButton.visible = star != null;
            goButton.active = false;
        } else {
            orbitButton.visible = true;
        }
    }

    private void go() {
        if (planning && trip != null && trip.canGo()) {
            PacketDistributor.sendToServer(new RocketActionPayload(rocket.getId(), RocketActionPayload.SET_TARGET, 0, trip.to().id()));
            onClose();
            return;
        }
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
        goButton = addRenderableWidget(Button.builder(Component.translatable(planning ? "gui.spacebreaker.star_map.set_target" : "gui.spacebreaker.star_map.go"), b -> go())
                .bounds(panelX + 8, height - 30, panelW - 16, 22).build());
        starsButton = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.star_map.nearby_stars"), b -> enterStars())
                .bounds(8, height - 26, 110, 18).build());
        backButton = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.star_map.back"), b -> {
            starsView = false;
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
        if (starsView) {
            if (button == 1) {
                starsView = false;
                updateButtons();
            } else {
                star = starAt(mx, my);
                updateButtons();
            }
            return true;
        }
        if (button == 1 && focus != null) {
            focus = null;
            layoutMap();
            updateButtons();
            return true;
        }
        if (button == 1) {
            enterStars();
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
        if (starsView) {
            drawStars(g, mouseX, mouseY, time);
            drawHeader(g);
            drawStarPanel(g);
            for (var r : renderables) {
                r.render(g, mouseX, mouseY, partialTick);
            }
            return;
        }
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
        y += 14;
        if (!com.pizzaparker07.spacebreaker.content.rocketry.mission.Coverage.hasSignal(body)) {
            for (FormattedCharSequence line : font.split(Component.translatable("gui.spacebreaker.star_map.no_signal"), w)) {
                g.drawString(font, line, x, y, 0xFFB0A070, false);
                y += 9;
            }
        }
        if (com.pizzaparker07.spacebreaker.content.rocketry.mission.Coverage.observed(body)) {
            Component mapped = Component.translatable(Locations.canRefuel(body) ? "gui.spacebreaker.star_map.mapped_fuel" : "gui.spacebreaker.star_map.mapped");
            for (FormattedCharSequence line : font.split(mapped, w)) {
                g.drawString(font, line, x, y, 0xFF8FD8C8, false);
                y += 9;
            }
        }
        orbitButton.setY(Math.max(148, y + 2));
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
        if (!planning) {
            String back = t.returnHome() ? "gui.spacebreaker.star_map.return_yes"
                    : t.refuelThere() ? "gui.spacebreaker.star_map.return_refuel" : "gui.spacebreaker.star_map.return_no";
            for (FormattedCharSequence line : font.split(Component.translatable(back), w)) {
                g.drawString(font, line, x, y, t.returnHome() ? GREEN : t.refuelThere() ? 0xFFB8D86A : AMBER, false);
                y += 9;
            }
            y += 2;
        }
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

    // ------------------------------------------------------------------ nearby stars

    private void enterStars() {
        starsView = true;
        star = null;
        focus = null;
        if (com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.received().isEmpty()) {
            PacketDistributor.sendToServer(new com.pizzaparker07.spacebreaker.content.rocketry.net.NearbyStarsPayload.Request());
        }
        updateButtons();
    }

    private float starScale() {
        return (Math.min(mapW, height - 40) / 2F - 16) / (float) com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.RADIUS_LY;
    }

    private int[] starPos(com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.Star s) {
        float k = starScale();
        return new int[]{mapW / 2 + Math.round(s.x() * k), height / 2 + 8 - Math.round(s.y() * k)};
    }

    private static int starSize(com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.Star s) {
        return s.magnitude() < 1 ? 4 : s.magnitude() < 4 ? 3 : s.magnitude() < 8 ? 2 : 1;
    }

    @Nullable
    private com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.Star starAt(double mx, double my) {
        com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.Star best = null;
        double bestD = 8;
        for (var s : com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.received()) {
            int[] p = starPos(s);
            double d = Math.hypot(mx - p[0], my - p[1]);
            if (d < bestD) {
                bestD = d;
                best = s;
            }
        }
        return best;
    }

    private void drawStars(GuiGraphics g, int mx, int my, float time) {
        int cx = mapW / 2, cy = height / 2 + 8;
        float k = starScale();
        // distance rings, labelled small and faint at their upper-right end; star names keep clear of the labels
        List<int[]> taken = new ArrayList<>();
        for (int ly : new int[]{5, 10, 15, 20, 25}) {
            int r = Math.round(ly * k);
            ring(g, cx, cy, r, 0x28A0B4C8);
            int lx = cx + (int) (r * 0.71F) + 2, lyPos = cy - (int) (r * 0.71F) - 6;
            Component label = Component.translatable("gui.spacebreaker.star_map.light_years", ly);
            g.pose().pushPose();
            g.pose().translate(lx, lyPos, 0);
            g.pose().scale(0.75F, 0.75F, 1F);
            g.drawString(font, label, 0, 0, 0xFF4A5868, false);
            g.pose().popPose();
            taken.add(new int[]{lx, lyPos, lx + (int) (font.width(label) * 0.75F) + 1, lyPos + 7});
        }
        // the Sun in the middle
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFFFFD873);
        g.drawCenteredString(font, Component.translatable("gui.spacebreaker.star_map.sun"), cx, cy + 5, 0xFFFFE9A8);
        var stars = com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.received();
        if (stars.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.spacebreaker.star_map.stars_loading"), cx, cy + 30, DIM);
        }
        var hover = mx < panelX ? starAt(mx, my) : null;
        int labels = 0;
        taken.add(new int[]{cx - 20, cy + 3, cx + 20, cy + 14}); // the Sun's own name
        for (var s : stars) {
            int[] p = starPos(s);
            int sz = starSize(s);
            int col = 0xFF000000 | s.colorRgb();
            if (s == star || s == hover) {
                ring(g, p[0], p[1], sz + 3, s == star ? 0xC0FFD34A : 0x80FFFFFF);
            }
            g.fill(p[0] - sz / 2, p[1] - sz / 2, p[0] - sz / 2 + Math.max(1, sz), p[1] - sz / 2 + Math.max(1, sz), col);
            boolean wanted = s == star || s == hover || labels < 14 && s.magnitude() < 7;
            if (wanted) {
                Component name = starName(s);
                int[] box = {p[0] + 3, p[1] - 5, p[0] + 5 + font.width(name), p[1] + 5};
                boolean free = taken.stream().noneMatch(b -> box[0] < b[2] && b[0] < box[2] && box[1] < b[3] && b[1] < box[3]);
                if (free || s == star || s == hover) {
                    g.drawString(font, name, p[0] + 4, p[1] - 4, s == star ? 0xFFFFE9A8 : 0xFFB8C2CC, false);
                    taken.add(box);
                    labels++;
                }
            }
        }
        drawGalaxyInset(g);
        g.drawString(font, Component.translatable("gui.spacebreaker.star_map.stars_title"), 8, 32, INK, false);
    }

    /** A small picture of the Milky Way from above, with the Sun marked: where these stars are. */
    private void drawGalaxyInset(GuiGraphics g) {
        int cells = 48;
        if (galaxyColours == null) {
            var o = com.pizzaparker07.spacebreaker.core.astro.galaxy.GalaxyMap.overview(cells);
            int[][] packed = new int[cells][cells];
            for (int x = 0; x < cells; x++) {
                for (int y = 0; y < cells; y++) {
                    int a = (int) Mth.clamp(o.brightness()[x][y] * 255, 0, 255);
                    packed[x][y] = a << 24 | (o.color()[x][y] & 0xFFFFFF);
                }
            }
            galaxyColours = packed;
            sunCell = o.sun();
        }
        int size = Math.min(72, mapW / 4), x0 = mapW - size - 8, y0 = height - size - 8;
        float c = size / (float) cells;
        g.fill(x0 - 1, y0 - 1, x0 + size + 1, y0 + size + 1, 0x80000000);
        for (int x = 0; x < cells; x++) {
            for (int y = 0; y < cells; y++) {
                int px = x0 + (int) (x * c), py = y0 + size - (int) ((y + 1) * c);
                g.fill(px, py, x0 + (int) ((x + 1) * c), y0 + size - (int) (y * c), galaxyColours[x][y]);
            }
        }
        if (sunCell != null) {
            int sx = x0 + (int) ((sunCell[0] + 0.5F) * c), sy = y0 + size - (int) ((sunCell[1] + 0.5F) * c);
            g.fill(sx - 1, sy - 1, sx + 2, sy + 2, ACCENT);
        }
        g.drawString(font, Component.translatable("gui.spacebreaker.star_map.milky_way"), x0, y0 - 10, DIM, false);
    }

    @Nullable
    private int[] sunCell;
    @Nullable
    private String pendingStar;

    /** Opens the star view with a star selected (by English name) once the list has arrived: for previews. */
    public void showStar(String nameEn) {
        enterStars();
        pendingStar = nameEn;
    }

    @Override
    public void tick() {
        super.tick();
        if (pendingStar != null) {
            for (var s : com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.received()) {
                if (s.nameEn().equals(pendingStar)) {
                    star = s;
                    pendingStar = null;
                    updateButtons();
                    break;
                }
            }
        }
    }

    private Component starName(com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.Star s) {
        return Component.literal(turkish() ? s.nameTr() : s.nameEn());
    }

    private void drawStarPanel(GuiGraphics g) {
        int x = panelX + 8, w = panelW - 16;
        g.fill(panelX, 0, width, height, PANEL_BG);
        g.fill(panelX, 0, panelX + 1, height, 0x60A0B4C8);
        if (star == null) {
            g.drawWordWrap(font, Component.translatable("gui.spacebreaker.star_map.pick_star"), x, 12, w, DIM);
            return;
        }
        var s = star;
        // a glowing disc in the star's colour
        int cx = x + 22, cy = 30;
        for (int i = 4; i > 0; i--) {
            disc(g, cx, cy, 14 + i * 3, ((0x18 * (5 - i)) << 24) | s.colorRgb());
        }
        disc(g, cx, cy, 14, 0xFF000000 | s.colorRgb());
        g.drawString(font, starName(s).copy().withStyle(ChatFormatting.BOLD), x + 50, 18, 0xFFFFFFFF, false);
        if (!s.designation().equals(s.nameEn())) {
            g.drawString(font, s.designation(), x + 50, 30, DIM, false);
        }
        int y = 58;
        Component kind = Component.translatable("gui.spacebreaker.star_map.star_kind." + s.kind().toLowerCase(Locale.ROOT), s.spectralClass());
        for (FormattedCharSequence line : font.split(kind, w)) {
            g.drawString(font, line, x, y, INK, false);
            y += 9;
        }
        y += 4;
        y = fact(g, x, y, w, Component.translatable("gui.spacebreaker.star_map.star_distance", decimal(s.distanceLy())));
        int unknown = Math.max(0, s.planets() - s.knownPlanets());
        y = fact(g, x, y, w, unknown > 0
                ? Component.translatable("gui.spacebreaker.star_map.star_planets_mixed", s.knownPlanets(), unknown)
                : Component.translatable("gui.spacebreaker.star_map.star_planets", s.planets()));
        y = fact(g, x, y, w, Component.translatable("gui.spacebreaker.star_map.star_mass", decimal(s.mass())));
        y = fact(g, x, y, w, Component.translatable("gui.spacebreaker.star_map.star_light", s.luminosity() >= 0.1F ? decimal(s.luminosity())
                : String.format(Locale.ROOT, "%.4f", s.luminosity()).replace('.', turkish() ? ',' : '.')));
        Component seen = Component.translatable(s.magnitude() <= 6 ? "gui.spacebreaker.star_map.star_naked_eye" : "gui.spacebreaker.star_map.star_telescope");
        fact(g, x, y, w, seen);
        List<FormattedCharSequence> why = font.split(Component.translatable("gui.spacebreaker.star_map.block.interstellar"), w);
        int ry = height - 34 - why.size() * 9;
        for (FormattedCharSequence line : why) {
            g.drawString(font, line, x, ry, RED, false);
            ry += 9;
        }
    }

    private int fact(GuiGraphics g, int x, int y, int w, Component text) {
        for (FormattedCharSequence line : font.split(text, w)) {
            g.drawString(font, line, x, y, 0xFFB8C2CC, false);
            y += 9;
        }
        return y + 2;
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
