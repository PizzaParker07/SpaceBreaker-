package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.item.Fmt;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchConsoleBlockEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchConsoleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/** Launch control: a checklist of the facility and the rocket, live readouts, the countdown and buttons. */
public class LaunchConsoleScreen extends AbstractContainerScreen<LaunchConsoleMenu> {
    private static final ResourceLocation BG = Spacebreaker.id("textures/gui/launch_console.png");
    private static final int SCREEN_X = 8, SCREEN_Y = 18;

    private Button launch;
    private Button abort;
    private Button target;

    public LaunchConsoleScreen(LaunchConsoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 236;
        imageHeight = 176;
    }

    @Override
    protected void init() {
        super.init();
        int y = topPos + imageHeight - 26;
        launch = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.console.launch"),
                b -> press(LaunchConsoleMenu.BUTTON_LAUNCH)).bounds(leftPos + 8, y, 72, 18).build());
        abort = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.console.abort"),
                b -> press(LaunchConsoleMenu.BUTTON_ABORT)).bounds(leftPos + 82, y, 72, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.console.check"),
                b -> press(LaunchConsoleMenu.BUTTON_REPORT)).bounds(leftPos + 156, y, 72, 18).build());
        target = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.console.mission_target"), b -> {
            RocketEntity r = uncrewed();
            if (r != null) {
                minecraft.setScreen(new StarMapScreen(r, true));
            }
        }).bounds(leftPos + 156, y - 22, 72, 18).build());
    }

    private void press(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int state = menu.get(LaunchConsoleMenu.STATE);
        boolean counting = state == FlightState.COUNTDOWN.ordinal();
        launch.active = state == FlightState.IDLE.ordinal() && menu.flag(LaunchConsoleBlockEntity.PAD) && menu.flag(LaunchConsoleBlockEntity.CREW);
        abort.active = counting && menu.get(LaunchConsoleMenu.TIMER) > 0;
        RocketEntity r = uncrewed();
        target.visible = r != null;
        target.active = r != null && state == FlightState.IDLE.ordinal();
    }

    /** The rocket on the pad if it carries a satellite or probe. */
    @org.jetbrains.annotations.Nullable
    private RocketEntity uncrewed() {
        if (minecraft == null || minecraft.level == null || menu.get(LaunchConsoleMenu.STATE) < 0) {
            return null;
        }
        return minecraft.level.getEntity(menu.get(LaunchConsoleMenu.ROCKET_ID)) instanceof RocketEntity r && r.isUncrewed() ? r : null;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        int x = SCREEN_X + 6, y = SCREEN_Y + 6;
        int green = 0x7CE07A, red = 0xF0705E, dim = 0x5E8E68, text = 0xB8F0B0;
        int missing = menu.get(LaunchConsoleMenu.MISSING);
        check(g, x, y, menu.flag(LaunchConsoleBlockEntity.PAD), Component.translatable("gui.spacebreaker.console.pad", 25 - missing), green, red);
        check(g, x, y + 11, menu.flag(LaunchConsoleBlockEntity.TOWER), Component.translatable("gui.spacebreaker.console.tower",
                menu.get(LaunchConsoleMenu.TOWER_HEIGHT)), green, dim);
        check(g, x, y + 22, menu.flag(LaunchConsoleBlockEntity.ARM), Component.translatable("gui.spacebreaker.console.arm"), green, dim);
        check(g, x, y + 33, menu.flag(LaunchConsoleBlockEntity.UMBILICAL), Component.translatable("gui.spacebreaker.console.umbilical"), green, dim);
        check(g, x, y + 44, menu.flag(LaunchConsoleBlockEntity.ROCKET), Component.translatable("gui.spacebreaker.console.rocket"), green, red);
        RocketEntity drone = uncrewed();
        boolean route = drone != null && drone.hasCargoHold() && drone.routeStatus() != RocketEntity.RouteStatus.OFF;
        if (route) {
            Component next = drone.missionTarget().map(l -> (Component) l.name()).orElse(Component.literal("?"));
            check(g, x, y + 55, true, Component.translatable("gui.spacebreaker.console.route", next, drone.holdFilled()), green, red);
        } else if (drone != null) {
            Component goal = drone.missionTarget().map(l -> (Component) Component.translatable("gui.spacebreaker.console.mission", l.name()))
                    .orElse(Component.translatable("gui.spacebreaker.console.mission_none"));
            check(g, x, y + 55, true, goal, green, red);
        } else {
            check(g, x, y + 55, menu.flag(LaunchConsoleBlockEntity.CREW), Component.translatable("gui.spacebreaker.console.crew", menu.get(LaunchConsoleMenu.CREW)), green, red);
        }
        check(g, x, y + 66, menu.flag(LaunchConsoleBlockEntity.FUELLED), Component.translatable("gui.spacebreaker.console.fuel",
                String.format(Locale.ROOT, "%.1f", menu.get(LaunchConsoleMenu.FUEL) / 10.0)), green, red);
        check(g, x, y + 77, menu.flag(LaunchConsoleBlockEntity.TWR_OK), Component.translatable("gui.spacebreaker.console.twr",
                Fmt.twr(menu.get(LaunchConsoleMenu.TWR) / 100.0)), green, red);
        check(g, x, y + 88, menu.flag(LaunchConsoleBlockEntity.DV_OK), Component.translatable("gui.spacebreaker.console.dv",
                Fmt.num(menu.get(LaunchConsoleMenu.DV)), Fmt.num(menu.get(LaunchConsoleMenu.COST))), green, red);
        if (route) {
            // automatic cargo route: what it is doing at this end
            RocketEntity.RouteStatus st = drone.routeStatus();
            check(g, x, y + 99, st != RocketEntity.RouteStatus.WAIT_FUEL,
                    Component.translatable("gui.spacebreaker.console.route." + st.name().toLowerCase(Locale.ROOT)), green, 0xE8C060);
        } else if (drone != null) {
            check(g, x, y + 99, true, Component.translatable("gui.spacebreaker.console.one_way"), green, 0xE8C060);
        } else {
            check(g, x, y + 99, menu.flag(LaunchConsoleBlockEntity.RETURN_OK), Component.translatable("gui.spacebreaker.console.return"), green, 0xE8C060);
        }
        // countdown clock on the right
        int state = menu.get(LaunchConsoleMenu.STATE);
        String clock;
        int col = text;
        if (state == FlightState.COUNTDOWN.ordinal()) {
            int t = menu.get(LaunchConsoleMenu.TIMER);
            clock = String.format(Locale.ROOT, "T-%02d", (int) Math.ceil(t / 20.0));
            col = t <= 60 ? 0xFFB347 : text;
        } else if (state >= 0 && state != FlightState.IDLE.ordinal()) {
            clock = "T+";
            col = 0xFFB347;
        } else {
            clock = "T-10";
            col = dim;
        }
        g.pose().pushPose();
        g.pose().translate(SCREEN_X + 186, SCREEN_Y + 30, 0);
        g.pose().scale(2.2F, 2.2F, 1);
        g.drawCenteredString(font, clock, 0, 0, col);
        g.pose().popPose();
        Component status = state < 0 ? Component.translatable("gui.spacebreaker.console.status.no_rocket")
                : Component.translatable("hud.spacebreaker.state." + FlightState.byId(state).name().toLowerCase(Locale.ROOT));
        g.drawCenteredString(font, status, SCREEN_X + 186, SCREEN_Y + 58, text);
    }

    private void check(GuiGraphics g, int x, int y, boolean ok, Component label, int okColor, int badColor) {
        g.drawString(font, ok ? "✔" : "✖", x, y, ok ? okColor : badColor, false);
        g.drawString(font, label, x + 10, y, ok ? 0xB8F0B0 : badColor, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
