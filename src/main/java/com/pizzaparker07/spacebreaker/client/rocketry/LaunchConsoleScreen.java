package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.item.Fmt;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchConsoleBlockEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchConsoleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Launch control: a checklist of the facility and the rocket, live readouts, the countdown and buttons. */
public class LaunchConsoleScreen extends AbstractContainerScreen<LaunchConsoleMenu> {
    private static final ResourceLocation BG = Spacebreaker.id("textures/gui/launch_console.png");
    private static final int SCREEN_X = 8, SCREEN_Y = 18;

    private Button launch;
    private Button abort;
    private Button target;
    private Button check;
    private boolean four;

    public LaunchConsoleScreen(LaunchConsoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 236;
        imageHeight = 176;
    }

    @Override
    protected void init() {
        super.init();
        // one strip along the bottom: Launch, Abort, Check pad, and Target... when the rocket is uncrewed
        int y = topPos + imageHeight - 26;
        launch = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.console.launch"),
                b -> press(LaunchConsoleMenu.BUTTON_LAUNCH)).bounds(leftPos + 8, y, 72, 18).build());
        abort = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.console.abort"),
                b -> press(LaunchConsoleMenu.BUTTON_ABORT)).bounds(leftPos + 82, y, 72, 18).build());
        check = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.console.check"),
                b -> press(LaunchConsoleMenu.BUTTON_REPORT)).bounds(leftPos + 156, y, 72, 18).build());
        target = addRenderableWidget(Button.builder(Component.translatable("gui.spacebreaker.console.mission_target"), b -> {
            RocketEntity r = uncrewed();
            if (r != null) {
                minecraft.setScreen(new StarMapScreen(r, true));
            }
        }).bounds(leftPos + 173, y, 55, 18).build());
        layoutStrip(false);
    }

    /** Four buttons when Target... shows (the longer labels get more room), else three of 72. */
    private void layoutStrip(boolean four) {
        Button[] bs = four ? new Button[]{launch, abort, check, target} : new Button[]{launch, abort, check};
        int[] ws = four ? new int[]{48, 48, 62, 56} : new int[]{72, 72, 72};
        int x = leftPos + 8;
        for (int i = 0; i < bs.length; i++) {
            bs[i].setX(x);
            bs[i].setWidth(ws[i]);
            x += ws[i] + 2;
        }
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
        if (four != (r != null)) {
            four = r != null;
            layoutStrip(four);
        }
        Component goal = r == null ? null : r.missionTarget().map(l -> (Component) Component.translatable("gui.spacebreaker.console.mission", l.name()))
                .orElse(Component.translatable("gui.spacebreaker.console.mission_none"));
        target.setTooltip(goal == null ? null : Tooltip.create(goal));
        net.minecraft.network.chat.MutableComponent list = Component.empty();
        for (Component line : fullList()) {
            list.append(line).append("\n");
        }
        check.setTooltip(Tooltip.create(list.append(Component.translatable("gui.spacebreaker.console.check.tip").withStyle(ChatFormatting.DARK_GRAY))));
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

    /** One checklist row: whether it passes, a short name, the full line with its numbers, and its colour when it fails. */
    private record Row(boolean ok, Component name, Component detail, int bad) {
    }

    private static final int GREEN = 0x7CE07A, RED = 0xF0705E, DIM = 0x5E8E68, TEXT = 0xB8F0B0, AMBER = 0xE8C060;

    private List<Row> rows() {
        List<Row> out = new ArrayList<>();
        int missing = menu.get(LaunchConsoleMenu.MISSING);
        out.add(new Row(menu.flag(LaunchConsoleBlockEntity.PAD), name("pad"), Component.translatable("gui.spacebreaker.console.pad", 25 - missing), RED));
        out.add(new Row(menu.flag(LaunchConsoleBlockEntity.TOWER), name("tower"),
                Component.translatable("gui.spacebreaker.console.tower", menu.get(LaunchConsoleMenu.TOWER_HEIGHT)), DIM));
        Component arm = Component.translatable("gui.spacebreaker.console.arm");
        out.add(new Row(menu.flag(LaunchConsoleBlockEntity.ARM), arm, arm, DIM));
        Component umb = Component.translatable("gui.spacebreaker.console.umbilical");
        out.add(new Row(menu.flag(LaunchConsoleBlockEntity.UMBILICAL), umb, umb, DIM));
        Component rocket = Component.translatable("gui.spacebreaker.console.rocket");
        out.add(new Row(menu.flag(LaunchConsoleBlockEntity.ROCKET), rocket, rocket, RED));
        RocketEntity drone = uncrewed();
        boolean route = drone != null && drone.hasCargoHold() && drone.routeStatus() != RocketEntity.RouteStatus.OFF;
        if (route) {
            Component next = drone.missionTarget().map(l -> (Component) l.name()).orElse(Component.literal("?"));
            Component line = Component.translatable("gui.spacebreaker.console.route", next, drone.holdFilled());
            out.add(new Row(true, line, line, RED));
        } else if (drone != null) {
            Component goal = drone.missionTarget().map(l -> (Component) Component.translatable("gui.spacebreaker.console.mission", l.name()))
                    .orElse(Component.translatable("gui.spacebreaker.console.mission_none"));
            out.add(new Row(true, goal, goal, RED));
        } else {
            out.add(new Row(menu.flag(LaunchConsoleBlockEntity.CREW), name("crew"),
                    Component.translatable("gui.spacebreaker.console.crew", menu.get(LaunchConsoleMenu.CREW)), RED));
        }
        out.add(new Row(menu.flag(LaunchConsoleBlockEntity.FUELLED), name("fuel"), Component.translatable("gui.spacebreaker.console.fuel",
                String.format(Locale.ROOT, "%.1f", menu.get(LaunchConsoleMenu.FUEL) / 10.0)), RED));
        out.add(new Row(menu.flag(LaunchConsoleBlockEntity.TWR_OK), name("twr"), Component.translatable("gui.spacebreaker.console.twr",
                Fmt.twr(menu.get(LaunchConsoleMenu.TWR) / 100.0)), RED));
        out.add(new Row(menu.flag(LaunchConsoleBlockEntity.DV_OK), name("dv"), Component.translatable("gui.spacebreaker.console.dv",
                Fmt.num(menu.get(LaunchConsoleMenu.DV)), Fmt.num(menu.get(LaunchConsoleMenu.COST))), RED));
        if (route) {
            RocketEntity.RouteStatus st = drone.routeStatus();
            Component line = Component.translatable("gui.spacebreaker.console.route." + st.name().toLowerCase(Locale.ROOT));
            out.add(new Row(st != RocketEntity.RouteStatus.WAIT_FUEL, line, line, AMBER));
        } else if (drone != null) {
            Component line = Component.translatable("gui.spacebreaker.console.one_way");
            out.add(new Row(true, line, line, AMBER));
        } else {
            Component line = Component.translatable("gui.spacebreaker.console.return");
            out.add(new Row(menu.flag(LaunchConsoleBlockEntity.RETURN_OK), line, line, AMBER));
        }
        return out;
    }

    private static Component name(String key) {
        return Component.translatable("gui.spacebreaker.console.name." + key);
    }

    /** Where each shown row was drawn, for its tooltip. */
    private final List<Row> shown = new ArrayList<>();

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        int x = SCREEN_X + 6, y = SCREEN_Y + 6;
        // only what is missing; with nothing missing, one "ready" line (the full list is in the tooltip and Check pad)
        shown.clear();
        for (Row r : rows()) {
            if (!r.ok()) {
                shown.add(r);
            }
        }
        if (shown.isEmpty()) {
            g.drawString(font, "\u2714 ", x, y, GREEN, false);
            g.drawString(font, Component.translatable("gui.spacebreaker.console.all_ready"), x + 10, y, TEXT, false);
        }
        for (int i = 0; i < shown.size(); i++) {
            Row r = shown.get(i);
            g.drawString(font, "\u2716", x, y + i * 11, r.bad(), false);
            g.drawString(font, r.name(), x + 10, y + i * 11, r.bad(), false);
        }
        // a cargo route says what it is doing only while it waits at this end
        RocketEntity drone = uncrewed();
        if (drone != null && drone.hasCargoHold()) {
            RocketEntity.RouteStatus st = drone.routeStatus();
            if (st == RocketEntity.RouteStatus.LOADING || st == RocketEntity.RouteStatus.WAIT_CARGO) {
                g.drawString(font, Component.translatable("gui.spacebreaker.console.route." + st.name().toLowerCase(Locale.ROOT)),
                        x, SCREEN_Y + 112, AMBER, false);
            }
        }
        // countdown clock on the right
        int state = menu.get(LaunchConsoleMenu.STATE);
        String clock;
        int col = TEXT;
        if (state == FlightState.COUNTDOWN.ordinal()) {
            int t = menu.get(LaunchConsoleMenu.TIMER);
            clock = String.format(Locale.ROOT, "T-%02d", (int) Math.ceil(t / 20.0));
            col = t <= 60 ? 0xFFB347 : TEXT;
        } else if (state >= 0 && state != FlightState.IDLE.ordinal()) {
            clock = "T+";
            col = 0xFFB347;
        } else {
            clock = "T-10";
            col = DIM;
        }
        g.pose().pushPose();
        g.pose().translate(SCREEN_X + 186, SCREEN_Y + 30, 0);
        g.pose().scale(2.2F, 2.2F, 1);
        g.drawCenteredString(font, clock, 0, 0, col);
        g.pose().popPose();
        Component status = state < 0 ? Component.translatable("gui.spacebreaker.console.status.no_rocket")
                : Component.translatable("hud.spacebreaker.state." + FlightState.byId(state).name().toLowerCase(Locale.ROOT));
        g.drawCenteredString(font, status, SCREEN_X + 186, SCREEN_Y + 58, TEXT);
    }

    /** Tooltip over the checklist: a missing row's numbers, or the whole list. */
    private List<Component> checklistTooltip(int mouseX, int mouseY) {
        int x = SCREEN_X + 6, y = SCREEN_Y + 6;
        for (int i = 0; i < shown.size(); i++) {
            if (isHovering(x, y + i * 11 - 1, 140, 11, mouseX, mouseY)) {
                return List.of(shown.get(i).detail());
            }
        }
        if (shown.isEmpty() && isHovering(x, y - 1, 140, 11, mouseX, mouseY)) {
            return fullList();
        }
        return List.of();
    }

    /** Every row with its numbers (the "ready" line's tooltip and the Check pad button's). */
    private List<Component> fullList() {
        List<Component> out = new ArrayList<>();
        for (Row r : rows()) {
            out.add(Component.literal(r.ok() ? "\u2714 " : "\u2716 ").append(r.detail())
                    .withStyle(r.ok() ? ChatFormatting.GREEN : r.bad() == DIM ? ChatFormatting.GRAY : r.bad() == AMBER ? ChatFormatting.GOLD : ChatFormatting.RED));
        }
        return out;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        List<Component> tip = checklistTooltip(mouseX, mouseY);
        if (!tip.isEmpty()) {
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }
}
