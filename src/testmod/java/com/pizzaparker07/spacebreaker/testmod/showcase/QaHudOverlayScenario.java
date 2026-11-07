package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.SuitTanks;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * QA (1.0.0 feedback): does the suit helmet HUD cover the block info overlay? Full EVA suit on the Moon, looking at
 * a dusty solar panel, so Jade or WTHIT draws its tooltip at the top of the screen with the HUD around it. Run with
 * {@code -Pscenario=qa_hud_overlay -PwithJade=true} (or {@code -PwithWthit=true}); without either it shows the HUD alone.
 */
final class QaHudOverlayScenario implements ShowcaseScenario {
    private static final int X = 212, Z = 212;

    @Override
    public void build(ServerLevel level) {
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        s.add(shot("01_hud_panel", 30F));
        s.add(shot("02_hud_panel_close", 55F));
        return s;
    }

    private static ShowcaseDriver.Shot shot(String name, float pitch) {
        return new ShowcaseDriver.Shot(name, 6000, 0, 0, 0, 0F, 0F, false, CameraType.FIRST_PERSON, 100, p -> setUp(p, pitch), null);
    }

    private static void setUp(ServerPlayer p, float pitch) {
        ServerLevel moon = p.server.getLevel(Location.MOON_SURFACE.dimension());
        if (moon == null) {
            return;
        }
        moon.getChunk(X >> 4, Z >> 4);
        int ground = moon.getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z);
        // a dusty panel two blocks ahead (south, yaw 0)
        BlockPos panel = new BlockPos(X, moon.getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z + 2), Z + 2);
        moon.setBlock(panel, MachinesModule.SOLAR_PANEL.get().defaultBlockState(), 3);
        if (moon.getBlockEntity(panel) instanceof SolarPanelBlockEntity be) {
            be.setDust(0.42F);
        }
        p.teleportTo(moon, X + 0.5, ground + 0.1, Z + 0.5, 0F, pitch);
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SuitModule.EVA_HELMET.get()));
        ItemStack torso = new ItemStack(SuitModule.EVA_CHESTPLATE.get());
        SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
        SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
        p.setItemSlot(EquipmentSlot.CHEST, torso);
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(SuitModule.EVA_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(SuitModule.EVA_BOOTS.get()));
    }
}
