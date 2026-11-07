package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPorts;
import com.pizzaparker07.spacebreaker.content.rocketry.station.StationCoreBlockEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.SuitTanks;
import com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenDistributorBlockEntity;
import com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * QA: the space station the way a player builds and uses it, in Earth orbit. Every step goes through the player-facing
 * code (the core module item used in open space, module items used on a mouth, the airlock doors clicked, a rocket
 * that docks with a module on board) and logs what happened to the client log ("[showcase] qa_station: ..."):
 * <ol>
 *   <li>the core unfolds from the item in front of the player;</li>
 *   <li>an uncrewed rocket carrying a docking module flies in to the roof port and hands the module to the console;</li>
 *   <li>the docking and living-quarters modules go on two mouths, the airlock on a third, the solar wings on the last;</li>
 *   <li>out through the airlock and back in; a hole in the quarters' hull, then mended;</li>
 *   <li>the solar wings charge the battery; the star map shows the station; a crewed rocket's docking approach.</li>
 * </ol>
 */
final class QaStationPlayScenario implements ShowcaseScenario {
    private static final double X0 = 3000.5, Z0 = 0.5;
    private static final double Y = RocketEntity.ORBIT_Y - 2;
    private static BlockPos centre;
    private static BlockPos port;
    private static RocketEntity cargo;
    private static RocketEntity crewed;

    @Override
    public ResourceKey<Level> dimension() {
        return Location.EARTH_ORBIT.dimension();
    }

    @Override
    public void build(ServerLevel level) {
    }

    private static void log(String s) {
        ShowcaseDriver.log("qa_station: " + s);
    }

    private static ServerLevel orbit(ServerPlayer p) {
        return p.server.getLevel(Location.EARTH_ORBIT.dimension());
    }

    private static void suit(ServerPlayer p) {
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SuitModule.EVA_HELMET.get()));
        ItemStack torso = new ItemStack(SuitModule.EVA_CHESTPLATE.get());
        SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
        SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
        p.setItemSlot(EquipmentSlot.CHEST, torso);
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(SuitModule.EVA_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(SuitModule.EVA_BOOTS.get()));
    }

    private static void at(ServerPlayer p, double x, double y, double z, float yaw, float pitch) {
        ServerLevel o = orbit(p);
        if (p.level() != o) {
            p.teleportTo(o, x, y, z, yaw, pitch);
        } else {
            p.connection.teleport(x, y, z, yaw, pitch);
        }
    }

    /** Uses a module item on the middle hatch of a mouth, as a right click would. */
    private static boolean attach(ServerPlayer p, BlockPos mouth, ItemStack stack, Direction face) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var r = stack.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(mouth.getCenter(), face, mouth, false)));
        return r.consumesAction();
    }

    private static void click(ServerPlayer p, BlockPos pos) {
        orbit(p).getBlockState(pos).useWithoutItem(orbit(p), p, new BlockHitResult(pos.getCenter(), Direction.UP, pos, false));
    }

    private static String room(ServerPlayer p) {
        BlockPos dist = centre.relative(Direction.NORTH, 2).relative(Direction.EAST, 2).above();
        if (orbit(p).getBlockEntity(dist) instanceof OxygenDistributorBlockEntity be) {
            return (be.room().sealed() ? "sealed" : "NOT sealed") + " " + be.room().volume();
        }
        return "no distributor at " + dist;
    }

    @Override
    public List<Shot> shots() {
        CameraType fp = CameraType.FIRST_PERSON;
        List<Shot> s = new ArrayList<>();
        // 1: the core from the item, in front of the player
        s.add(new Shot("01_core_from_item", 6000, X0, Y + 1.62, Z0, 0F, 15F, false, fp, 120, p -> {
            at(p, X0, Y, Z0, 0F, 15F);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            suit(p);
            ItemStack core = new ItemStack(RocketryModule.STATION_CORE_MODULE.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, core);
            var r = core.use(orbit(p), p, InteractionHand.MAIN_HAND);
            centre = BlockPos.containing(p.getEyePosition()).relative(Direction.SOUTH, 7).below(3);
            port = DockingPorts.get(orbit(p)).nearest(centre.getCenter(), q -> true).map(DockingPorts.Port::pos).orElse(null);
            log("core item used: " + r.getResult() + ", core at " + centre + ", roof port " + port);
        }, null));
        // 2: a cargo rocket with a docking module flies in to the roof port
        s.add(new Shot("02_module_rocket_approach", 6000, X0 + 14.5, Y + 6, Z0 + 7, 90F, 10F, true, fp, 80, p -> {
            if (port == null) {
                log("no port, skipping the delivery");
                return;
            }
            ServerLevel o = orbit(p);
            RocketDesign design = new RocketDesign(List.of(Spacebreaker.id("vacuum_engine"), Spacebreaker.id("fuel_tank_large"),
                    Spacebreaker.id("docking_module")));
            cargo = RocketEntity.create(o, design, port.getCenter().add(0, 14, 0), 0F);
            cargo.fillCompletely();
            o.addFreshEntity(cargo);
            cargo.stageScene(FlightState.ORBIT, 0, 0, false);
            cargo.approachPort(port, Direction.UP, 12);
            log("cargo rocket carries " + cargo.carriedModule().map(StationModule::name).orElse("nothing") + ", docking " + cargo.docking());
        }, null));
        s.add(new Shot("03_module_delivered", 6000, X0 + 14.5, Y + 6, Z0 + 7, 90F, 10F, true, fp, 400, p -> {
        }, null));
        // 4: modules on the mouths (the console's module, then quarters, airlock, wings from the inventory)
        s.add(new Shot("04_modules_attached", 6000, X0 + 18.5, Y + 9, Z0 - 8, 45F, 25F, true, fp, 200, p -> {
            ServerLevel o = orbit(p);
            var core = StationCoreBlockEntity.near(o, port);
            ItemStack fromConsole = ItemStack.EMPTY;
            if (core != null) {
                for (int i = 0; i < core.getContainerSize(); i++) {
                    if (core.getItem(i).is(RocketryModule.DOCKING_MODULE.get())) {
                        fromConsole = core.removeItem(i, 1);
                    }
                }
            }
            log("rocket docked " + (cargo != null && cargo.docked()) + ", docking module in the console: " + !fromConsole.isEmpty());
            BlockPos front = centre.relative(Direction.SOUTH, 3).above(2);
            BlockPos east = centre.relative(Direction.EAST, 3).above(2);
            BlockPos west = centre.relative(Direction.WEST, 3).above(2);
            boolean dock = attach(p, east, fromConsole.isEmpty() ? new ItemStack(RocketryModule.DOCKING_MODULE.get()) : fromConsole, Direction.EAST);
            boolean hab = attach(p, front, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Spacebreaker.id("habitation_module"))), Direction.SOUTH);
            boolean lock = attach(p, west, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Spacebreaker.id("airlock_module"))), Direction.WEST);
            log("attached: docking " + dock + ", quarters " + hab + ", airlock " + lock);
        }, null));
        s.add(new Shot("05_station_inside", 6000, X0 + 0.5, Y, Z0 + 5, 180F, 5F, false, fp, 200, p -> {
            at(p, centre.getX() + 0.5, centre.getY() + 1, centre.getZ() + 0.5, 180F, 5F);
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            log("inside the core: room " + room(p));
        }, null));
        // 6-8: out through the airlock (west side) and back in
        s.add(new Shot("06_airlock_chamber", 6000, 0, 0, 0, 90F, 0F, false, fp, 120, p -> {
            BlockPos west = centre.relative(Direction.WEST, 3).above(2);
            BlockPos inner = west.relative(Direction.WEST, 1).below(), outer = west.relative(Direction.WEST, 5).below();
            click(p, inner);
            at(p, west.getX() - 2.5, inner.getY(), west.getZ() + 0.5, 90F, 0F);
            click(p, inner);
            log("in the airlock chamber: inner door " + doorState(p, inner) + ", room " + room(p));
        }, null));
        s.add(new Shot("07_outside_eva", 6000, 0, 0, 0, 270F, 10F, false, fp, 160, p -> {
            BlockPos west = centre.relative(Direction.WEST, 3).above(2);
            BlockPos outer = west.relative(Direction.WEST, 5).below();
            click(p, outer);
            at(p, west.getX() - 7.5, outer.getY(), west.getZ() + 0.5, 90F, 10F);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            log("outside: outer door " + doorState(p, outer) + ", station room " + room(p));
        }, null));
        s.add(new Shot("08_back_inside", 6000, 0, 0, 0, 90F, 0F, false, fp, 160, p -> {
            BlockPos west = centre.relative(Direction.WEST, 3).above(2);
            BlockPos inner = west.relative(Direction.WEST, 1).below(), outer = west.relative(Direction.WEST, 5).below();
            at(p, west.getX() - 2.5, outer.getY(), west.getZ() + 0.5, 90F, 0F);
            click(p, outer);
            click(p, inner);
            at(p, centre.getX() + 0.5, centre.getY() + 1, centre.getZ() + 0.5, 90F, 0F);
            click(p, inner);
            log("back inside: doors " + doorState(p, outer) + "/" + doorState(p, inner) + ", room " + room(p));
        }, null));
        // 9-10: a hole in the quarters, then mended
        s.add(new Shot("09_hull_breach", 6000, 0, 0, 0, 180F, 0F, false, fp, 120, p -> {
            // stand in the quarters, facing their east wall; the hole is the first hull block out from the middle
            BlockPos mid = centre.relative(Direction.SOUTH, 8).above(2);
            at(p, mid.getX() + 0.5, mid.getY() - 1, mid.getZ() + 0.5, 270F, 0F);
            BlockPos hole = mid;
            for (int i = 1; i <= 5; i++) {
                var bs = orbit(p).getBlockState(mid.east(i));
                if (!bs.isAir() && com.pizzaparker07.spacebreaker.content.suit.block.SealedRoom.airtight(orbit(p), mid.east(i), bs)) {
                    hole = mid.east(i);
                    break;
                }
            }
            breach = hole;
            breachState = orbit(p).getBlockState(hole);
            orbit(p).removeBlock(hole, false);
            log("hole at " + hole + " (was " + breachState.getBlock() + "), room " + room(p));
        }, null));
        s.add(new Shot("10_hull_mended", 6000, 0, 0, 0, 180F, 0F, false, fp, 160, p -> {
            BlockPos mid = centre.relative(Direction.SOUTH, 8).above(2);
            at(p, mid.getX() + 0.5, mid.getY() - 1, mid.getZ() + 0.5, 270F, 0F);
            log("before mending: room " + room(p));
            orbit(p).setBlock(breach, breachState, 3);
        }, null));
        // 11: solar wings on the last mouth (north), charging
        s.add(new Shot("11_solar_wings", 6000, X0 - 10.5, Y + 8, Z0 - 14, 210F, 20F, true, fp, 400, p -> {
            log("after mending: room " + room(p));
            BlockPos north = centre.relative(Direction.NORTH, 3).above(2);
            boolean wings = attach(p, north, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Spacebreaker.id("solar_wings"))), Direction.NORTH);
            log("solar wings attached " + wings);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }, null));
        // 12: the star map from a seat in the docked rocket
        s.add(new Shot("12_star_map", 6000, 0, 0, 0, 0F, 0F, false, fp, 160, p -> {
            BlockPos north = centre.relative(Direction.NORTH, 3).above(2);
            var be = orbit(p).getBlockEntity(north);
            var cap = be == null ? null : orbit(p).getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, north, null);
            log("battery in the wings' mouth: " + (cap == null ? "no energy storage" : cap.getEnergyStored() + " FE") + " after ~20 s");
            ServerLevel o = orbit(p);
            var free = DockingPorts.get(o).nearest(centre.getCenter(), q -> !q.pos().equals(port)).orElse(null);
            Vec3 start = free == null ? new Vec3(centre.getX() + 0.5, centre.getY() + 30, centre.getZ() + 40.5)
                    : RocketEntity.dockPoint(free.pos(), free.facing(), 0, 0).add(Vec3.atLowerCornerOf(free.facing().getNormal()).scale(16));
            crewed = RocketEntity.create(o, RocketryModule.MOON_ROCKET, start, 0F);
            crewed.fillCompletely();
            o.addFreshEntity(crewed);
            crewed.stageScene(FlightState.ORBIT, 0, 1, false);
            at(p, start.x, start.y + 1, start.z, 0F, 0F);
            p.startRiding(crewed, true);
            RocketryShowcaseClient.openMapWhenRiding = true;
        }, null));
        // 13: the docking approach from the seat (flight HUD)
        s.add(new Shot("13_docking_hud", 6000, 0, 0, 0, 0F, 0F, false, fp, 80, p -> {
            RocketryShowcaseClient.openMapWhenRiding = false;
            p.closeContainer();
            reseat(p);
            var free = DockingPorts.get(orbit(p)).nearest(centre.getCenter(), q -> !q.pos().equals(port)).orElse(null);
            if (free != null && crewed != null) {
                crewed.approachPort(free.pos(), free.facing(), 16);
                log("crewed rocket approaching port " + free.pos() + " facing " + free.facing() + ", docking " + crewed.docking());
            } else {
                log("no free port for the crewed rocket");
            }
        }, null));
        s.add(new Shot("14_docked", 6000, 0, 0, 0, 0F, 0F, false, fp, 400, QaStationPlayScenario::reseat, null));
        s.add(new Shot("15_end", 6000, 0, 0, 0, 0F, 0F, false, fp, 20, p -> {
            reseat(p);
            var free = DockingPorts.get(orbit(p)).nearest(centre.getCenter(), q -> !q.pos().equals(port)).orElse(null);
            String where = crewed == null || free == null ? "" : String.format(java.util.Locale.ROOT,
                    " at %.1f %.1f %.1f, %.2f blocks from the dock point, still docking %s, player riding %s",
                    crewed.getX(), crewed.getY(), crewed.getZ(),
                    crewed.position().distanceTo(RocketEntity.dockPoint(free.pos(), free.facing(), crewed.getBbHeight(), crewed.getBbWidth())),
                    crewed.docking(), p.getVehicle() == crewed);
            log("crewed rocket docked " + (crewed != null && crewed.docked()) + where);
            p.stopRiding();
        }, null));
        return s;
    }

    /** The driver moves the player to each shot's spot: put them back in the crewed rocket's seat. */
    private static void reseat(ServerPlayer p) {
        if (crewed != null && p.getVehicle() != crewed) {
            at(p, crewed.getX(), crewed.getY() + 1, crewed.getZ(), 0F, 0F);
            boolean ok = p.startRiding(crewed, true);
            log("re-seated in the crewed rocket: " + ok + " (rocket alive " + crewed.isAlive() + " at " + crewed.blockPosition()
                    + ", state " + crewed.state() + ")");
        }
    }

    private static BlockPos breach;
    private static net.minecraft.world.level.block.state.BlockState breachState;

    private static String doorState(ServerPlayer p, BlockPos pos) {
        var st = orbit(p).getBlockState(pos);
        return st.getBlock() instanceof AirlockDoorBlock ? (st.getValue(AirlockDoorBlock.OPEN) ? "open" : "shut") : "no door";
    }
}
