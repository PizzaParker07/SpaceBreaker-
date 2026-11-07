package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import net.minecraft.client.CameraType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;

/** The suit HUD inside a crewed capsule in Earth orbit: the cabin is pressurised, the helmet may come off. */
final class SuitCapsuleScenario implements ShowcaseScenario {
    @Override
    public ResourceKey<Level> dimension() {
        return Location.Orbits.EARTH_ORBIT;
    }

    @Override
    public void build(ServerLevel level) {
    }

    private static void seated(ServerPlayer p, boolean helmet) {
        p.setGameMode(GameType.SURVIVAL);
        ServerLevel level = p.serverLevel();
        RocketEntity r = RocketEntity.create(level, RocketryModule.MOON_ROCKET, new Vec3(0.5, RocketEntity.ORBIT_Y, 0.5), 0F);
        r.fillCompletely();
        level.addFreshEntity(r);
        r.stageScene(FlightState.ORBIT, 0, 1, false);
        SuitScenario.dress(p, SuitScenario.fullSuit(false, false));
        if (!helmet) {
            p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
        p.startRiding(r, true);
    }

    @Override
    public List<Shot> shots() {
        CameraType fp = CameraType.FIRST_PERSON;
        return List.of(
                // the first boarding shows the rocket's chase camera; the cockpit view follows on the next shots
                new Shot("capsule_00_boarding", 6000, 0.5, RocketEntity.ORBIT_Y + 1, 0.5, 30F, 20F, true, fp, 120,
                        p -> seated(p, false), SuitShots.CLEAR_TOASTS),
                new Shot("capsule_01_helmet_off", 6000, 0.5, RocketEntity.ORBIT_Y + 1, 0.5, 30F, 20F, false, fp, 120,
                        p -> seated(p, false), SuitShots.CLEAR_TOASTS),
                new Shot("capsule_02_hud_pressurised", 6000, 0.5, RocketEntity.ORBIT_Y + 1, 0.5, 30F, 20F, false, fp, 160,
                        p -> seated(p, true), SuitShots.CLEAR_TOASTS));
    }
}
