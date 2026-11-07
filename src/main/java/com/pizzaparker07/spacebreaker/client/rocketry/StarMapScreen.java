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
 * The star map: one calm, zoomable picture of the Solar System. Every world sits where it really is on its orbit
 * (distances on a log scale so Mercury and Neptune both fit), drawn as a small disc in its own colours on a thin
 * orbit line. Scroll to zoom, drag to move; clicking a world glides the view to it, and to its moons if it has any.
 * The panel on the right stays short: the name, orbit or land, and for the chosen trip just the Δv, the time, one
 * fuel bar and a word on getting home. The route from here is a thin line on the map.
 */
public class StarMapScreen extends Screen {
    /** A world on the current frame: screen position and disc radius, both in GUI pixels. */
    private record Node(ResourceLocation body, float x, float y, int r, boolean small) {
    }

    private static final int GREEN = 0xFF8FD88A, RED = 0xFFEF6A5A, AMBER = 0xFFE8C060, INK = 0xFFDDE4EC, DIM = 0xFF8A96A4;
    private static final int PANEL_BG = 0xE00B1018, ACCENT = 0xFFFFD34A;
    private static final int ORBIT = 0x50A0B4C8, ORBIT_SEL = 0x90FFD34A;
    /** Map units per log-step of distance from the Sun (see {@link #radial}). */
    private static final double UNIT = 60;
    private static final double AU_KM = 149_597_870.7;

    private final RocketEntity rocket;
    private final Map<ResourceLocation, Node> nodes = new LinkedHashMap<>();
    private ResourceLocation body;
    @Nullable
    private Location target;
    @Nullable
    private StarMapData.Trip trip;
    private Button orbitButton, surfaceButton, goButton, backButton, starsButton;
    /** The stars around the Sun (information only, no interstellar travel yet). */
    private boolean starsView;
    @Nullable
    private com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.Star star;
    @Nullable
    private int[][] galaxyColours;
    private int panelX, panelW, mapW;

    // camera: map position at the centre of the map area, and GUI pixels per map unit; eased towards the targets
    private double camX, camY, scale = -1, toX, toY, toScale;
    private long lastFrame;
    private boolean dragging;
    private double dragFromX, dragFromY;
    private boolean dragged;

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
        backButton.visible = starsView;
        starsButton.visible = !starsView;
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
        panelW = Mth.clamp((int) (width * 0.3F), 140, 184);
        panelX = width - panelW;
        mapW = panelX;
        int bx = panelX + 8, bw = (panelW - 20) / 2;
        orbitButton = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.star_map.orbit_short"),
                b -> choose(Locations.get(body, true).orElse(null))).bounds(bx, 44, bw, 16).build());
        surfaceButton = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.star_map.land"),
                b -> choose(Locations.get(body, false).orElse(null))).bounds(bx + bw + 4, 44, bw, 16).build());
        goButton = addRenderableWidget(Button.builder(Component.translatable(planning ? "gui.spacebreaker.star_map.set_target" : "gui.spacebreaker.star_map.go"), b -> go())
                .bounds(panelX + 8, height - 28, panelW - 16, 20).build());
        starsButton = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.star_map.nearby_stars"), b -> enterStars())
                .bounds(8, height - 24, 96, 16).build());
        backButton = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.star_map.back"), b -> {
            starsView = false;
            updateButtons();
        }).bounds(8, height - 24, 96, 16).build());
        if (scale < 0) {
            // open on where the rocket is: its system if it is at a moon, otherwise the whole map
            ResourceLocation sys = StarMapData.system(here().body());
            if (!StarMapData.moons(sys).isEmpty() && !sys.equals(StarMapData.COMETS) && !sys.equals(StarMapData.NEAR_EARTH_ASTEROIDS)
                    && Locations.parentPlanet(here().body()).isPresent()) {
                focusOn(sys, true);
            } else {
                overview();
            }
            camX = toX;
            camY = toY;
            scale = toScale;
        }
        if (target != null) {
            choose(target);
        }
        updateButtons();
    }

    /** Fit the whole system (out to the last planet) into the map area. */
    private void overview() {
        double outer = 0;
        for (ResourceLocation p : StarMapData.planets()) {
            if (!StarMapData.COMETS.equals(p) && !StarMapData.NEAR_EARTH_ASTEROIDS.equals(p)) {
                outer = Math.max(outer, radial(semiMajorAu(p)));
            }
        }
        toX = 0;
        toY = 0;
        toScale = Math.max(0.05, (Math.min(mapW, height - 30) / 2.0 - 14) / Math.max(1, outer));
    }

    /** Glide the view to a world; with {@code moons}, close enough to see its moons around it. */
    private void focusOn(ResourceLocation b, boolean moons) {
        double[] p = mapPos(b, time());
        toX = p[0];
        toY = p[1];
        List<ResourceLocation> kids = StarMapData.COMETS.equals(b) || StarMapData.NEAR_EARTH_ASTEROIDS.equals(b) ? List.of() : StarMapData.moons(b);
        if (moons && !kids.isEmpty()) {
            toScale = (Math.min(mapW, height) * 0.36) / moonOffset(kids.size() - 1);
        } else {
            toScale = Math.max(toScale, overviewScale() * 2.2);
        }
    }

    private double overviewScale() {
        double old = toScale, ox = toX, oy = toY;
        overview();
        double s = toScale;
        toScale = old;
        toX = ox;
        toY = oy;
        return s;
    }

    /** Distance from the Sun on the map for a real distance in AU: logarithmic, so the inner worlds get room. */
    private static double radial(double au) {
        return UNIT * Math.log(1 + au / 0.3) / Math.log(2);
    }

    /** Distance of the i-th moon from its planet, in map units. */
    private static double moonOffset(int i) {
        return 1.6 + i * 1.15;
    }

    private static double semiMajorAu(ResourceLocation b) {
        var comet = com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.byId(b.getPath());
        if (StarMapData.comet(b) && comet.isPresent()) {
            return comet.get().semiMajorAxisAu();
        }
        return Locations.body(b).map(c -> c.orbit().semiMajorAxisKm() / AU_KM).orElse(1.0);
    }

    /** Simulated days since J2000 (the map shows the worlds where they are now). */
    private double time() {
        return minecraft == null || minecraft.level == null ? 0
                : com.pizzaparker07.spacebreaker.core.astro.BodyAdapter.simTime(minecraft.level) / 86400.0;
    }

    /** Where a world is on the map (map units, y down), moons beside their planet. */
    private double[] mapPos(ResourceLocation b, double days) {
        if (StarMapData.COMETS.equals(b) || StarMapData.NEAR_EARTH_ASTEROIDS.equals(b)) {
            List<ResourceLocation> m = StarMapData.moons(b);
            return m.isEmpty() ? new double[]{0, 0} : mapPos(m.get(0), days);
        }
        var parent = Locations.parentPlanet(b);
        if (parent.isPresent()) {
            double[] p = mapPos(parent.get(), days);
            List<ResourceLocation> kids = StarMapData.moons(parent.get());
            int i = Math.max(0, kids.indexOf(b));
            double period = Locations.body(b).map(c -> c.orbit().periodDays()).filter(d -> d > 0).orElse(10.0);
            // the moons turn slowly around their planet (a whole circuit per 20 real orbits keeps them readable)
            double a = 0.6 + i * 2.4 + 2 * Math.PI * days / (period * 20);
            double r = moonOffset(i);
            return new double[]{p[0] + Math.cos(a) * r, p[1] - Math.sin(a) * r};
        }
        double[] xy = heliocentric(b, days);
        double r = Math.hypot(xy[0], xy[1]);
        double k = r <= 0 ? 0 : radial(r) / r;
        return new double[]{xy[0] * k, -xy[1] * k};
    }

    /** Heliocentric position in the ecliptic plane, AU (Kepler's equation from the catalogue's elements). */
    private static double[] heliocentric(ResourceLocation b, double days) {
        double a, e, peri, mean;
        var comet = StarMapData.comet(b) ? com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.byId(b.getPath()) : java.util.Optional.<com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet>empty();
        if (comet.isPresent()) {
            var c = comet.get();
            a = c.semiMajorAxisAu();
            e = c.eccentricity();
            peri = Math.toRadians(c.nodeDeg() + c.periapsisArgDeg());
            double period = c.periodSeconds() / 86400.0;
            mean = 2 * Math.PI * (((days - c.perihelionDays()) / period) % 1.0);
        } else {
            CelestialBody.Orbit o = Locations.body(b).map(CelestialBody::orbit).orElse(CelestialBody.Orbit.NONE);
            a = o.semiMajorAxisKm() / AU_KM;
            e = o.eccentricity();
            peri = Math.toRadians(o.longitudeAscendingNodeDeg() + o.argumentPeriapsisDeg());
            mean = Math.toRadians(o.meanAnomalyDeg()) + (o.periodDays() > 0 ? 2 * Math.PI * days / o.periodDays() : 0);
            if (a <= 0) {
                a = 1;
            }
        }
        double E = mean;
        for (int it = 0; it < 30; it++) {
            E -= (E - e * Math.sin(E) - mean) / (1 - e * Math.cos(E));
        }
        double nu = 2 * Math.atan2(Math.sqrt(1 + e) * Math.sin(E / 2), Math.sqrt(1 - e) * Math.cos(E / 2));
        double r = a * (1 - e * Math.cos(E));
        return new double[]{r * Math.cos(peri + nu), r * Math.sin(peri + nu)};
    }

    private float sx(double mx) {
        return (float) (mapW / 2.0 + (mx - camX) * scale);
    }

    private float sy(double my) {
        return (float) (height / 2.0 + 6 + (my - camY) * scale);
    }

    /** Places every world for this frame. */
    private void layoutFrame(double days) {
        nodes.clear();
        float zoom = (float) (scale / Math.max(1e-6, overviewScale()));
        float grow = Mth.clamp(1 + (float) Math.log(Math.max(1, zoom)) * 0.22F, 1, 2.6F);
        for (ResourceLocation p : StarMapData.planets()) {
            if (StarMapData.COMETS.equals(p) || StarMapData.NEAR_EARTH_ASTEROIDS.equals(p)) {
                for (ResourceLocation m : StarMapData.moons(p)) {
                    double[] q = mapPos(m, days);
                    nodes.put(m, new Node(m, sx(q[0]), sy(q[1]), Math.round(2 * Math.min(grow, 2)), true));
                }
                continue;
            }
            double[] q = mapPos(p, days);
            int pr = Math.round(systemRadius(p) * grow);
            nodes.put(p, new Node(p, sx(q[0]), sy(q[1]), pr, false));
            List<ResourceLocation> kids = StarMapData.moons(p);
            for (ResourceLocation m : kids) {
                // a moon shows once it is clear of its planet's disc
                if (moonOffset(0) * scale < pr + 6) {
                    break;
                }
                double[] mq = mapPos(m, days);
                nodes.put(m, new Node(m, sx(mq[0]), sy(mq[1]), Math.round(moonRadius(m) * Math.min(grow, 2.2F)), false));
            }
        }
    }

    private static int systemRadius(ResourceLocation body) {
        if (Location.EARTH.equals(body)) {
            return 7;
        }
        CelestialBody b = Locations.body(body).orElse(null);
        if (b == null) {
            return 5;
        }
        if (b.type() == BodyType.GAS_GIANT) {
            return 12;
        }
        if (b.type() == BodyType.ICE_GIANT) {
            return 9;
        }
        return Mth.clamp((int) Math.round(3 + Math.sqrt(b.physical().radiusKm()) / 18.0), 5, 7);
    }

    private static int moonRadius(ResourceLocation body) {
        double km = Locations.body(body).map(b -> b.physical().radiusKm()).orElse(500.0);
        return Mth.clamp((int) Math.round(4 + Math.sqrt(km) / 14.0), 5, 8);
    }

    /** Where a location shows on the current frame. */
    @Nullable
    private float[] posOf(Location l) {
        Node n = nodes.get(l.body());
        if (n == null) {
            n = nodes.get(Locations.parentPlanet(l.body()).orElse(l.body()));
        }
        return n == null ? null : new float[]{n.x, n.y};
    }

    @Nullable
    private Node nodeAt(double mx, double my) {
        Node best = null;
        double bestD = Double.MAX_VALUE;
        for (Node n : nodes.values()) {
            double d = Math.hypot(mx - n.x, my - n.y);
            if (d <= n.r + 5 && d < bestD) {
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
        if (button == 1) {
            overview();
            return true;
        }
        dragging = true;
        dragged = false;
        dragFromX = mx;
        dragFromY = my;
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && !starsView) {
            if (Math.hypot(mx - dragFromX, my - dragFromY) > 3) {
                dragged = true;
            }
            if (dragged) {
                toX -= dx / scale;
                toY -= dy / scale;
                camX = toX;
                camY = toY;
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            dragging = false;
            if (!dragged) {
                Node n = nodeAt(mx, my);
                if (n != null) {
                    select(n.body);
                    focusOn(n.body, Locations.parentPlanet(n.body).isEmpty() && !n.small);
                }
            }
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sxAmount, double syAmount) {
        if (starsView || mx >= panelX || syAmount == 0) {
            return super.mouseScrolled(mx, my, sxAmount, syAmount);
        }
        // zoom about the point under the mouse
        double f = Math.pow(1.25, syAmount);
        double min = overviewScale() * 0.6, max = overviewScale() * 400;
        double next = Mth.clamp(toScale * f, min, max);
        double wx = toX + (mx - mapW / 2.0) / toScale, wy = toY + (my - height / 2.0 - 6) / toScale;
        toX = wx - (mx - mapW / 2.0) / next;
        toY = wy - (my - height / 2.0 - 6) / next;
        toScale = next;
        return true;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF03050A);
        RandomSource rng = RandomSource.create(7);
        for (int i = 0; i < 220; i++) {
            int x = rng.nextInt(Math.max(1, width));
            int y = rng.nextInt(Math.max(1, height));
            int b = 50 + rng.nextInt(100);
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
        // ease the camera (frame-rate independent)
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 1F : Math.min(0.1F, (now - lastFrame) / 1e9F);
        lastFrame = now;
        double k = 1 - Math.exp(-dt * 9);
        camX += (toX - camX) * k;
        camY += (toY - camY) * k;
        scale = Math.exp(Math.log(scale) + (Math.log(toScale) - Math.log(scale)) * k);
        double days = time();
        layoutFrame(days);
        g.enableScissor(0, 0, mapW, height);
        drawSystem(g, days);
        drawRoute(g, time);
        drawNodes(g, mouseX, mouseY);
        drawHere(g, time);
        g.disableScissor();
        drawHeader(g);
        drawPanel(g);
        for (var r : renderables) {
            r.render(g, mouseX, mouseY, partialTick);
        }
    }

    private void drawSystem(GuiGraphics g, double days) {
        float cx = sx(0), cy = sy(0);
        // the Sun, with a soft glow
        int sunR = Mth.clamp((int) (scale * 14), 7, 48);
        for (int i = 9; i > 0; i--) {
            disc(g, Math.round(cx), Math.round(cy), sunR + i * 4, 0x0BFFC860);
        }
        disc(g, Math.round(cx), Math.round(cy), sunR, 0xFFFFC14A);
        disc(g, Math.round(cx), Math.round(cy), Math.max(1, sunR - 2), 0xFFFFD873);
        // the asteroid belt: a faint band of dots between Mars and Jupiter
        RandomSource rng = RandomSource.create(11);
        for (int i = 0; i < 360; i++) {
            double a = rng.nextDouble() * Math.PI * 2, r = radial(2.2 + rng.nextDouble() * 1.1);
            g.fill(Math.round(sx(Math.cos(a) * r)), Math.round(sy(Math.sin(a) * r)), Math.round(sx(Math.cos(a) * r)) + 1,
                    Math.round(sy(Math.sin(a) * r)) + 1, 0x40A09080);
        }
        // orbits: thin lines, planets always, small bodies only when chosen
        for (Node n : nodes.values()) {
            boolean sel = n.body.equals(body) || n.body.equals(Locations.parentPlanet(body).orElse(null));
            if (n.small && !n.body.equals(body)) {
                continue;
            }
            var parent = Locations.parentPlanet(n.body);
            if (parent.isPresent()) {
                Node p = nodes.get(parent.get());
                if (p != null) {
                    ellipse(g, Math.round(p.x), Math.round(p.y), (int) Math.round(Math.hypot(n.x - p.x, n.y - p.y)),
                            (int) Math.round(Math.hypot(n.x - p.x, n.y - p.y)), n.body.equals(body) ? ORBIT_SEL : ORBIT);
                }
                continue;
            }
            // close in on a planet's moons the Sun's orbits only cut across the picture: leave them out
            if (scale < overviewScale() * 12) {
                orbitLine(g, n.body, days, sel ? ORBIT_SEL : ORBIT);
            }
        }
    }

    /** One orbit around the Sun, sampled along the real ellipse and drawn with the map's log scale. */
    private void orbitLine(GuiGraphics g, ResourceLocation b, double days, int colour) {
        double period = StarMapData.comet(b)
                ? com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.byId(b.getPath()).map(c -> c.periodSeconds() / 86400.0).orElse(365.0)
                : Locations.body(b).map(c -> c.orbit().periodDays()).filter(d -> d > 0).orElse(365.0);
        int steps = 160;
        float px = Float.NaN, py = Float.NaN;
        for (int i = 0; i <= steps; i++) {
            double[] q = mapPos(b, days + period * i / steps);
            float x = sx(q[0]), y = sy(q[1]);
            boolean off = Math.max(px, x) < 0 || Math.min(px, x) > mapW || Math.max(py, y) < 0 || Math.min(py, y) > height;
            if (!Float.isNaN(px) && !off) {
                line(g, px, py, x, y, colour, 1);
            }
            px = x;
            py = y;
        }
    }

    private void drawNodes(GuiGraphics g, int mx, int my) {
        Node hover = mx < panelX ? nodeAt(mx, my) : null;
        List<int[]> taken = new ArrayList<>();
        List<Runnable> labels = new ArrayList<>();
        for (Node n : nodes.values()) {
            if (n.x < -20 || n.x > mapW + 20 || n.y < -20 || n.y > height + 20) {
                continue;
            }
            boolean sel = n.body.equals(body);
            int x = Math.round(n.x), y = Math.round(n.y);
            if (sel) {
                // a soft glow and a thin gold ring around the chosen world
                for (int i = 3; i > 0; i--) {
                    disc(g, x, y, n.r + 3 + i * 2, 0x14FFD34A);
                }
                ring(g, x, y, n.r + 4, 0xF0FFD34A);
            } else if (n == hover) {
                ring(g, x, y, n.r + 3, 0x90FFFFFF);
            }
            if (StarMapData.comet(n.body)) {
                double dx = n.x - sx(0), dy = n.y - sy(0), d = Math.max(1, Math.hypot(dx, dy));
                comet(g, x, y, Math.max(2, n.r + 1), dx / d, dy / d, 0.6);
            } else {
                planet(g, n.body, x, y, Math.max(1, n.r));
            }
            var around = com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.around(n.body);
            if (!around.isEmpty()) {
                stationIcon(g, x + n.r + 1, y - n.r - 5);
                if (around.stream().anyMatch(com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.Station::shipyard)) {
                    yardIcon(g, x + n.r + 11, y - n.r - 5);
                }
            }
            if (!com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.basesOn(n.body).isEmpty()) {
                baseIcon(g, x + n.r + 1, y + n.r - 2);
            }
            // names: quiet, and only where they fit; small bodies only when pointed at or chosen
            if (n.small && !sel && n != hover) {
                continue;
            }
            Component name = Location.bodyName(n.body);
            if (StarMapData.comet(n.body)) {
                name = com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.byId(n.body.getPath())
                        .map(c -> (Component) Component.literal(c.designation())).orElse(name);
            }
            int tw = font.width(name);
            int[] box = {x - tw / 2 - 1, y + n.r + 3, x + tw / 2 + 1, y + n.r + 12};
            boolean free = taken.stream().noneMatch(b -> box[0] < b[2] && b[0] < box[2] && box[1] < b[3] && b[1] < box[3]);
            if (free || sel || n == hover) {
                Component label = name;
                int colour = sel ? 0xFFFFE9A8 : n == hover ? 0xFFFFFFFF : 0xB0B8C2CC;
                labels.add(() -> g.drawCenteredString(font, label, x, y + n.r + 4, colour));
                taken.add(box);
            }
        }
        // names last, so no disc covers them
        labels.forEach(Runnable::run);
    }

    /** A tiny station: white module with blue solar wings, top-right of a world that has one in orbit. */
    private static void stationIcon(GuiGraphics g, int x, int y) {
        g.fill(x, y + 1, x + 3, y + 4, 0xFF4A6AA8);
        g.fill(x + 6, y + 1, x + 9, y + 4, 0xFF4A6AA8);
        g.fill(x + 3, y + 2, x + 6, y + 3, 0xFF8A96A4);
        g.fill(x + 3, y, x + 6, y + 5, 0xFFEEF0F2);
    }

    /** A tiny shipyard: an open grey frame with a striped corner, beside the station of a world that has one. */
    private static void yardIcon(GuiGraphics g, int x, int y) {
        int c = 0xFFA0A8B0;
        g.fill(x, y, x + 7, y + 1, c);
        g.fill(x, y + 5, x + 7, y + 6, c);
        g.fill(x, y, x + 1, y + 6, c);
        g.fill(x + 6, y, x + 7, y + 6, c);
        g.fill(x, y, x + 2, y + 1, 0xFFE8C443);
    }

    /** A tiny surface base: a landing pad with a yellow ring and a little dome. */
    private static void baseIcon(GuiGraphics g, int x, int y) {
        g.fill(x, y + 3, x + 9, y + 5, 0xFFCFA823);
        g.fill(x + 3, y, x + 6, y + 3, 0xFFEEF0F2);
        g.fill(x + 2, y + 1, x + 7, y + 3, 0xFFEEF0F2);
    }

    /** Where the rocket is: a small gold marker bobbing above it. */
    private void drawHere(GuiGraphics g, float time) {
        float[] p = posOf(here());
        if (p == null) {
            return;
        }
        Node n = nodes.get(here().body());
        int r = n == null ? 4 : n.r;
        int bob = Math.round(Mth.sin(time * 0.15F) * 1.5F);
        int x = Math.round(p[0]), y = Math.round(p[1]) - r - 9 + bob;
        g.fill(x - 3, y, x + 4, y + 1, ACCENT);
        g.fill(x - 2, y + 1, x + 3, y + 2, ACCENT);
        g.fill(x - 1, y + 2, x + 2, y + 3, ACCENT);
        g.fill(x, y + 3, x + 1, y + 4, ACCENT);
    }

    /** The route from here: a thin line with a small dot at every stop. */
    private void drawRoute(GuiGraphics g, float time) {
        if (trip == null) {
            return;
        }
        int colour = trip.enough() ? 0xD0FFD34A : 0xD0EF6A5A;
        List<float[]> pts = new ArrayList<>();
        for (Location l : trip.route()) {
            float[] p = posOf(l);
            if (p != null && (pts.isEmpty() || Math.hypot(pts.get(pts.size() - 1)[0] - p[0], pts.get(pts.size() - 1)[1] - p[1]) > 1)) {
                pts.add(p);
            }
        }
        float phase = (time * 0.6F) % 8;
        for (int i = 0; i + 1 < pts.size(); i++) {
            float[] a = pts.get(i), b = pts.get(i + 1);
            float len = (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
            // a gentle arc, dashed, moving towards the destination
            float bow = Math.min(24, len * 0.15F);
            int steps = Math.max(2, (int) len);
            for (int s = 0; s < steps; s++) {
                if ((s - phase + 800) % 8 >= 5) {
                    continue;
                }
                float t0 = s / (float) steps;
                float x = Mth.lerp(t0, a[0], b[0]);
                float y = Mth.lerp(t0, a[1], b[1]) - (float) Math.sin(t0 * Math.PI) * bow;
                g.fill(Math.round(x), Math.round(y), Math.round(x) + 1, Math.round(y) + 1, colour);
            }
            g.fill(Math.round(b[0]) - 1, Math.round(b[1]) - 1, Math.round(b[0]) + 2, Math.round(b[1]) + 2, colour);
        }
    }

    private void drawHeader(GuiGraphics g) {
        g.drawString(font, title.copy().withStyle(ChatFormatting.BOLD), 8, 8, INK, false);
        if (!starsView) {
            g.drawString(font, Component.translatable("gui.spacebreaker.star_map.controls"), 8, 19, 0xFF5A6674, false);
        }
    }

    // ------------------------------------------------------------------ panel

    private void drawPanel(GuiGraphics g) {
        int x = panelX + 8, w = panelW - 16;
        g.fill(panelX, 0, width, height, PANEL_BG);
        g.fill(panelX, 0, panelX + 1, height, 0x40A0B4C8);
        // the world: a disc, its name, what it is
        planet(g, body, x + 12, 22, 12);
        Component name = Location.bodyName(body).copy().withStyle(ChatFormatting.BOLD);
        int room = w - 30;
        if (font.width(name) > room) {
            // long names (67P/Churyumov–Gerasimenko) are cut with an ellipsis; bold text is wider, hence the margin
            name = Component.literal(font.plainSubstrByWidth(Location.bodyName(body).getString(), (int) (room * 0.86F) - font.width("…")) + "…")
                    .withStyle(ChatFormatting.BOLD);
        }
        g.drawString(font, name, x + 30, 13, 0xFFFFFFFF, false);
        Component kind = Component.translatable("gui.spacebreaker.star_map.kind." + Locations.body(body).map(b -> b.type().name()).orElse("TERRESTRIAL")
                .toLowerCase(Locale.ROOT));
        g.drawString(font, kind, x + 30, 24, DIM, false);
        if (!surfaceButton.visible) {
            g.drawString(font, Component.translatable("gui.spacebreaker.star_map.no_surface_short"), x + w / 2 + 6, 48, DIM, false);
        }
        int y = 68;
        // dangers as a row of short coloured words
        int hx = x;
        for (StarMapData.Hazard hz : Locations.body(body).map(StarMapData::hazards).orElse(java.util.Set.of())) {
            Component t = Component.translatable(hz.key());
            int tw = font.width(t);
            if (hx + tw > x + w) {
                hx = x;
                y += 10;
            }
            g.drawString(font, t, hx, y, hazardColour(hz), false);
            hx += tw + 6;
        }
        y += 14;
        if (trip == null) {
            g.drawWordWrap(font, Component.translatable("gui.spacebreaker.star_map.pick_short"), x, y, w, DIM);
            y += 22;
        } else {
            y = drawTrip(g, x, y, w);
        }
        // what is there: stations, bases, a refuelling world, comet activity; one short line each
        y += 4;
        for (var st : com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.around(body)) {
            stationIcon(g, x, y + 1);
            Component line = st.modules() > 0
                    ? Component.translatable(st.shipyard() ? "gui.spacebreaker.star_map.station_yard" : "gui.spacebreaker.star_map.station_modules",
                    st.modules(), st.ports())
                    : Component.translatable("gui.spacebreaker.star_map.station_short", st.ports());
            g.drawString(font, line, x + 13, y, 0xFFC8D0D8, false);
            y += 10;
        }
        for (var st : com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.basesOn(body)) {
            baseIcon(g, x, y + 1);
            g.drawString(font, Component.translatable("gui.spacebreaker.star_map.base_short", st.ports()), x + 13, y, 0xFFC8D0D8, false);
            y += 10;
        }
        if (com.pizzaparker07.spacebreaker.content.rocketry.mission.Coverage.observed(body) && Locations.canRefuel(body)) {
            g.drawString(font, Component.translatable("gui.spacebreaker.star_map.refuel_short"), x, y, 0xFF8FD8C8, false);
            y += 10;
        }
        if (StarMapData.comet(body)) {
            var info = StarMapData.cometInfo(body, com.pizzaparker07.spacebreaker.core.astro.BodyAdapter.simTime(minecraft.level)).orElse(null);
            if (info != null) {
                String level = turkish() ? info.level().nameTr : info.level().nameEn;
                g.drawString(font, Component.translatable("gui.spacebreaker.star_map.comet_short", decimal(info.sunAu()), level), x, y, 0xFFB8D8F0, false);
                y += 10;
            }
        }
        if (!com.pizzaparker07.spacebreaker.content.rocketry.mission.Coverage.hasSignal(body)) {
            g.drawString(font, Component.translatable("gui.spacebreaker.star_map.no_signal_short"), x, y, 0xFFB0A070, false);
        }
        // realism mode, small at the bottom
        SbConfig.Realism realism = SbConfig.realism();
        if (realism != SbConfig.Realism.REALISTIC) {
            Component note = Component.translatable("gui.spacebreaker.star_map.mode_short." + realism.name().toLowerCase(Locale.ROOT),
                    Math.round(Transfers.scale(realism) * 100));
            g.drawString(font, note, x, height - 40, 0xFF5A6674, false);
        }
    }

    /** The chosen trip: two numbers, one bar, one line about getting home, at most one warning. */
    private int drawTrip(GuiGraphics g, int x, int y, int w) {
        StarMapData.Trip t = trip;
        int half = w / 2;
        g.drawString(font, Component.translatable("gui.spacebreaker.star_map.stat_dv"), x, y, DIM, false);
        g.drawString(font, Component.translatable("gui.spacebreaker.star_map.stat_time"), x + half, y, DIM, false);
        y += 10;
        big(g, num(t.needed()), x, y, t.enough() ? 0xFFFFFFFF : RED);
        g.drawString(font, "m/s", x + Math.round(font.width(num(t.needed())) * 1.5F) + 2, y + 5, DIM, false);
        OptionalDouble secs = travelSeconds(t);
        String time = secs.isEmpty() ? "—" : secs.getAsDouble() >= 86400
                ? Component.translatable("gui.spacebreaker.star_map.unit_days", decimalOrWhole(secs.getAsDouble() / 86400.0)).getString()
                : Component.translatable("gui.spacebreaker.star_map.unit_hours", (int) (secs.getAsDouble() / 3600),
                (int) (secs.getAsDouble() % 3600 / 60)).getString();
        big(g, time, x + half, y, 0xFFFFFFFF);
        y += 18;
        // fuel: needed against on board
        float fill = t.needed() <= 0 ? 1F : (float) Math.min(1, t.have() / t.needed());
        g.fill(x, y, x + w, y + 3, 0xFF1C222A);
        g.fill(x, y, x + (int) (w * fill), y + 3, t.enough() ? GREEN : RED);
        if (t.needed() > 0 && t.have() > t.needed()) {
            int mark = x + (int) (w * t.needed() / t.have());
            g.fill(mark, y - 2, mark + 1, y + 5, 0xFFFFFFFF);
        }
        y += 6;
        g.drawString(font, Component.translatable("gui.spacebreaker.star_map.on_board", num(t.have())), x, y, DIM, false);
        y += 12;
        if (!t.enough()) {
            g.drawString(font, Component.translatable("gui.spacebreaker.star_map.short", num(t.shortBy())), x, y, RED, false);
            y += 10;
        }
        if (!planning) {
            String back = t.returnHome() ? "gui.spacebreaker.star_map.home_yes"
                    : t.refuelThere() ? "gui.spacebreaker.star_map.home_refuel" : "gui.spacebreaker.star_map.home_no";
            g.drawString(font, Component.translatable(back), x, y, t.returnHome() ? GREEN : t.refuelThere() ? 0xFFB8D86A : AMBER, false);
            y += 10;
        }
        Component window = LaunchWindows.describe(minecraft.level, here(), t.to());
        if (window != null) {
            for (FormattedCharSequence line : font.split(window, w)) {
                g.drawString(font, line, x, y, 0xFF7FA8D0, false);
                y += 9;
            }
        }
        // the most important warning only
        Component warning = t.needsNuclear() ? Component.translatable("gui.spacebreaker.star_map.need_nuclear")
                : !t.shieldOk() ? Component.translatable("gui.spacebreaker.star_map.need_shield")
                : t.legsMissing() ? Component.translatable("gui.spacebreaker.star_map.need_legs")
                : !t.to().orbit() && t.to().atmosphere() && !rocket.layout().payload().parachute() ? Component.translatable("gui.spacebreaker.star_map.need_chute")
                : t.boilOff() >= 0.001 ? Component.translatable("gui.spacebreaker.star_map.boil_off", decimal(t.boilOff() * 100))
                : !t.next().equals(t.to()) && t.canGo() ? Component.translatable("gui.spacebreaker.star_map.first_stop", t.next().name())
                : null;
        if (warning != null) {
            y += 2;
            for (FormattedCharSequence line : font.split(warning, w)) {
                g.drawString(font, line, x, y, AMBER, false);
                y += 9;
            }
        }
        if (!t.canGo()) {
            List<FormattedCharSequence> lines = font.split(Component.translatable(t.block().key()), w);
            int ry = height - 32 - lines.size() * 9;
            for (FormattedCharSequence line : lines) {
                g.drawString(font, line, x, ry, RED, false);
                ry += 9;
            }
        }
        return y;
    }

    private void big(GuiGraphics g, String text, int x, int y, int colour) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(1.5F, 1.5F, 1F);
        g.drawString(font, text, 0, 0, colour, false);
        g.pose().popPose();
    }

    private String decimalOrWhole(double v) {
        return v >= 100 ? num(v) : decimal(v);
    }

    /** Real flight time of the whole route in seconds: transfers, plus landings and launches (astro surface legs). */
    private OptionalDouble travelSeconds(StarMapData.Trip t) {
        if (!(Transfers.model() instanceof AstroTransferModel astro)) {
            return OptionalDouble.empty();
        }
        double secs = 0;
        List<Location> path = StarMapData.flightPath(here(), t.to());
        for (int i = 0; i + 1 < path.size(); i++) {
            secs += astro.transferSeconds(path.get(i), path.get(i + 1)).orElse(0);
        }
        return secs >= 60 ? OptionalDouble.of(secs) : OptionalDouble.empty();
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

    /** A 1-pixel line (stepped). */
    private static void line(GuiGraphics g, float x0, float y0, float x1, float y1, int colour, int width) {
        int steps = (int) Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps > 4000) {
            return;
        }
        for (int s = 0; s <= steps; s++) {
            float t = steps == 0 ? 0 : (float) s / steps;
            int x = Math.round(Mth.lerp(t, x0, x1)), y = Math.round(Mth.lerp(t, y0, y1));
            g.fill(x, y, x + width, y + width, colour);
        }
    }

    // ------------------------------------------------------------------ nearby stars

    private void enterStars() {
        starsView = true;
        star = null;
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
        if (s.habitablePlanets() > 0) {
            y = fact(g, x, y, w, Component.translatable("gui.spacebreaker.star_map.star_habitable", s.habitablePlanets()));
        }
        if (s.binary()) {
            y = fact(g, x, y, w, Component.translatable("gui.spacebreaker.star_map.star_binary"));
        }
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
        if (StarMapData.COMETS.equals(body) || StarMapData.comet(body)) {
            comet(g, cx, cy, r, 0.8, -0.6, 1.0);   // a picture: tail to the upper right
            return;
        }
        if (StarMapData.NEAR_EARTH_ASTEROIDS.equals(body)) {
            // a loose handful of grey rocks
            int[][] rocks = {{-4, -3, 3}, {3, -4, 2}, {5, 2, 3}, {-2, 4, 2}, {0, 0, 2}};
            for (int[] k : rocks) {
                disc(g, cx + k[0] * r / 6, cy + k[1] * r / 6, Math.max(1, k[2] * r / 7), 0xFF8A8478);
                disc(g, cx + k[0] * r / 6 - 1, cy + k[1] * r / 6 - 1, Math.max(1, k[2] * r / 7 - 1), 0xFFA8A296);
            }
            return;
        }
        if (BodyLook.drawFlat(g, body, cx, cy, r, 0.6F)) {
            return;
        }
        ResourceLocation tex = Location.MOON.equals(body) ? OrbitSkyEffects.MOON_DISC : OrbitSkyEffects.disc(body);
        g.blit(tex, cx - r, cy - r, r * 2, r * 2, 0F, 0F, 1, 1, 1, 1);
    }

    /** A comet: bright nucleus and a fading tail along (ux, uy), {@code length} 1 for an active comet. */
    private static void comet(GuiGraphics g, int cx, int cy, int r, double ux, double uy, double length) {
        int n = Math.max(1, (int) Math.round(r * 2 * length));
        for (int i = n; i > 0; i--) {
            int a = 0x10 + 0xA0 * (n - i) / n;
            double t = i * 0.85;
            disc(g, cx + (int) Math.round(ux * t), cy + (int) Math.round(uy * t), Math.max(1, r / 3 + i / 5), (a << 24) | 0xB8D8F0);
        }
        disc(g, cx, cy, Math.max(2, r / 3), 0xFFEEF4FA);
    }

    private static void disc(GuiGraphics g, int cx, int cy, int r, int colour) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, colour);
        }
    }

    /** A solid thin circle. */
    private static void ring(GuiGraphics g, int cx, int cy, int r, int colour) {
        int steps = Math.max(24, r * 7);
        int lx = Integer.MIN_VALUE, ly = Integer.MIN_VALUE;
        for (int i = 0; i < steps; i++) {
            double a = i * Math.PI * 2 / steps;
            int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
            if (x != lx || y != ly) {
                g.fill(x, y, x + 1, y + 1, colour);
                lx = x;
                ly = y;
            }
        }
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
        select(b);
        choose(Locations.get(b, orbit).orElse(null));
        if (mapW > 0) {
            var parent = Locations.parentPlanet(b);
            if (parent.isPresent()) {
                focusOn(parent.get(), true);
            } else {
                overview();
            }
            camX = toX;
            camY = toY;
            scale = toScale;
        }
    }
}
