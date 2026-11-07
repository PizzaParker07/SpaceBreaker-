package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;

/**
 * The EVA suit where it belongs: on the Moon. Suited figures on the regolith in lunar day and night, and
 * the helmet HUD in real vacuum (sealed suit, then an incomplete suit with its warning).
 */
final class SuitMoonScenario implements ShowcaseScenario {
    private PlanetTerrain terrain;

    @Override
    public ResourceKey<Level> dimension() {
        return MoonModule.MOON;
    }

    private PlanetTerrain terrain() {
        if (terrain == null) {
            ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getLevel(MoonModule.MOON);
            terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        }
        return terrain;
    }

    private double ground(double x, double z) {
        return Math.floor(terrain().height(x, z)) + 1;
    }

    @Override
    public void build(ServerLevel level) {
        terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        int y = (int) ground(0.5, 0.5);
        // flatten a small work site
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                for (int dy = 0; dy < 8; dy++) {
                    level.setBlock(new BlockPos(x, y + dy, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                }
                for (int dy = 1; dy <= 3; dy++) {
                    level.setBlock(new BlockPos(x, y - dy, z), MoonModule.LUNAR_REGOLITH.get().defaultBlockState(), 2);
                }
            }
        }
        stand(level, -1.5, y, 1.5, 20F, false);
        stand(level, 1.5, y, 0.5, -30F, true);
    }

    private static void stand(ServerLevel level, double x, int y, double z, float yaw, boolean sunVisor) {
        ArmorStand stand = new ArmorStand(level, x, y, z);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ShowArms", true);
        tag.putBoolean("NoBasePlate", true);
        stand.readAdditionalSaveData(tag);
        stand.setYRot(yaw);
        stand.setYBodyRot(yaw);
        stand.setYHeadRot(yaw);
        stand.setLeftArmPose(new Rotations(-10F, 0F, -8F));
        stand.setRightArmPose(new Rotations(-25F, 0F, 8F));
        SuitScenario.dress(stand, SuitScenario.fullSuit(sunVisor, !sunVisor));
        level.addFreshEntity(stand);
    }

    private static Consumer<ServerPlayer> at(double dayFraction, Consumer<ServerPlayer> more) {
        return p -> {
            PlanetTime.setDayFraction(p.serverLevel(), dayFraction);
            if (more != null) {
                more.accept(p);
            }
        };
    }

    private static void suited(ServerPlayer p) {
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
        SuitScenario.dress(p, SuitScenario.fullSuit(false, true));
        p.setItemSlot(EquipmentSlot.MAINHAND, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
    }

    private static void incomplete(ServerPlayer p) {
        suited(p);
        p.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
    }

    @Override
    public List<Shot> shots() {
        double gy = ground(0.5, 0.5);
        List<Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        s.add(new Shot("suit_moon_01_day", 0, 0.0, gy + 1.9, 6.0, 180F, 8F, true, fp, 200, at(0.2, null), SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_moon_02_night_lamps", 0, 0.0, gy + 1.9, 5.0, 180F, 8F, true, fp, 80, at(0.75, null), SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_moon_03_hud_vacuum", 0, 0.5, gy + 1.62, 6.5, 180F, 6F, false, fp, 60, at(0.2, SuitMoonScenario::suited), SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_moon_04_hud_warning", 0, 0.5, gy + 1.62, 6.5, 180F, 6F, false, fp, 60, at(0.2, SuitMoonScenario::incomplete), SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_moon_05_player", 0, 0.5, gy + 1.62, 6.5, 180F, 0F, true, CameraType.THIRD_PERSON_FRONT, 60,
                at(0.2, SuitMoonScenario::suited), SuitShots.CLEAR_TOASTS));
        return s;
    }
}
