package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.suit.ManeuveringUnitItem;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.SuitTanks;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

import static com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;

/**
 * The EVA suit: a row of armour stands in the full Mk1 suit from several angles (one with the gold sun
 * visor down and a Maneuvering Unit), the player in the suit, lamps at night and the helmet HUD.
 */
final class SuitScenario implements ShowcaseScenario {
    static final int FLOOR = ShowcaseBuilder.FLOOR;
    private static final double G = FLOOR + 1;

    @Override
    public void build(ServerLevel level) {
        for (int x = -12; x <= 12; x++) {
            for (int z = -10; z <= 14; z++) {
                for (int y = FLOOR + 1; y <= FLOOR + 12; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                boolean pad = Math.abs(x) <= 7 && z >= -3 && z <= 3;
                level.setBlock(new BlockPos(x, FLOOR, z), pad ? Blocks.SMOOTH_STONE.defaultBlockState()
                        : MoonModule.LUNAR_REGOLITH.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int x = -8; x <= 8; x++) {
            for (int y = FLOOR + 1; y <= FLOOR + 5; y++) {
                level.setBlock(new BlockPos(x, y, -5), MoonModule.MARE_BASALT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        stand(level, -4.5, 0F, false, false);
        stand(level, -2.0, 35F, false, false);
        stand(level, 0.5, 90F, false, false);
        stand(level, 3.0, 180F, false, true);
        stand(level, 5.5, -20F, true, true);
    }

    static ItemStack[] fullSuit(boolean sunVisor, boolean mmu) {
        ItemStack helmet = new ItemStack(SuitModule.EVA_HELMET.get());
        if (sunVisor) {
            helmet.set(SuitModule.SUN_VISOR.get(), true);
        }
        ItemStack torso = new ItemStack(SuitModule.EVA_CHESTPLATE.get());
        SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
        SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
        if (mmu) {
            SuitTanks.setManeuveringUnit(torso, ManeuveringUnitItem.filled(SuitModule.MANEUVERING_UNIT.get()));
        }
        return new ItemStack[]{helmet, torso, new ItemStack(SuitModule.EVA_LEGGINGS.get()), new ItemStack(SuitModule.EVA_BOOTS.get())};
    }

    static void dress(LivingEntity entity, ItemStack[] suit) {
        entity.setItemSlot(EquipmentSlot.HEAD, suit[0]);
        entity.setItemSlot(EquipmentSlot.CHEST, suit[1]);
        entity.setItemSlot(EquipmentSlot.LEGS, suit[2]);
        entity.setItemSlot(EquipmentSlot.FEET, suit[3]);
    }

    private static void stand(ServerLevel level, double x, float yaw, boolean sunVisor, boolean mmu) {
        ArmorStand stand = new ArmorStand(level, x, G, 0.5);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ShowArms", true);
        tag.putBoolean("NoBasePlate", true);
        stand.readAdditionalSaveData(tag);
        stand.setYRot(yaw);
        stand.setYBodyRot(yaw);
        stand.setYHeadRot(yaw);
        stand.setLeftArmPose(new Rotations(-10F, 0F, -8F));
        stand.setRightArmPose(new Rotations(-10F, 0F, 8F));
        dress(stand, fullSuit(sunVisor, mmu));
        level.addFreshEntity(stand);
    }

    @Override
    public List<Shot> shots() {
        List<Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        s.add(new Shot("suit_01_lineup", 5000, 0.5, G + 2.0, 7.0, 180F, 8F, true, fp, 140, null, null));
        s.add(new Shot("suit_02_front_close", 5000, -4.5, G + 1.5, 2.6, 180F, 4F, true, fp, 40, null, null));
        s.add(new Shot("suit_03_helmet_macro", 5000, -4.4, G + 1.75, 1.75, 175F, 8F, true, fp, 40, null, null));
        s.add(new Shot("suit_04_three_quarter", 5000, -1.4, G + 1.5, 2.9, 190F, 6F, true, fp, 40, null, null));
        s.add(new Shot("suit_05_back_plss", 5000, 3.0, G + 1.6, 2.7, 180F, 8F, true, fp, 40, null, null));
        s.add(new Shot("suit_06_gold_visor_mmu", 5000, 5.0, G + 1.6, 2.6, 170F, 6F, true, fp, 40, null, null));
        s.add(new Shot("suit_07_night_lamps", 17800, 0.5, G + 1.8, 5.0, 180F, 6F, true, fp, 80, null, null));
        s.add(new Shot("suit_08_player", 5200, 0.5, G + 1.62, 6.0, 0F, 0F, true, CameraType.THIRD_PERSON_FRONT, 40,
                SuitScenario::suitUp, null));
        s.add(new Shot("suit_09_player_back", 5200, 0.5, G + 1.62, 6.0, 0F, 0F, true, CameraType.THIRD_PERSON_BACK, 40,
                SuitScenario::suitUp, null));
        s.add(new Shot("suit_10_hud", 6000, 0.5, G + 1.62, 7.0, 180F, 5F, false, fp, 60, SuitScenario::suitUp, null));
        return s;
    }

    private static void suitUp(ServerPlayer player) {
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        dress(player, fullSuit(false, false));
        player.setItemSlot(EquipmentSlot.MAINHAND, OxygenTankItem.filled(SuitModule.LARGE_OXYGEN_TANK.get()));
    }
}
