package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;

import static com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;

/** The suit tiers side by side (front and back rows), plus dyed accents. */
final class SuitTiersScenario implements ShowcaseScenario {
    private static final int FLOOR = ShowcaseBuilder.FLOOR;
    private static final double G = FLOOR + 1;

    @SuppressWarnings("unchecked")
    static final List<DeferredItem<SpaceSuitItem>>[] SETS = new List[]{
            List.of(SuitModule.EVA_HELMET, SuitModule.EVA_CHESTPLATE, SuitModule.EVA_LEGGINGS, SuitModule.EVA_BOOTS),
            List.of(SuitModule.THERMAL_HELMET, SuitModule.THERMAL_CHESTPLATE, SuitModule.THERMAL_LEGGINGS, SuitModule.THERMAL_BOOTS),
            List.of(SuitModule.HEATED_HELMET, SuitModule.HEATED_CHESTPLATE, SuitModule.HEATED_LEGGINGS, SuitModule.HEATED_BOOTS),
            List.of(SuitModule.RADIATION_HELMET, SuitModule.RADIATION_CHESTPLATE, SuitModule.RADIATION_LEGGINGS, SuitModule.RADIATION_BOOTS),
    };

    @Override
    public void build(ServerLevel level) {
        for (int x = -12; x <= 12; x++) {
            for (int z = -10; z <= 14; z++) {
                for (int y = FLOOR + 1; y <= FLOOR + 12; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                level.setBlock(new BlockPos(x, FLOOR, z), (Math.abs(x) <= 8 && z >= -6 && z <= 6 ? Blocks.SMOOTH_STONE
                        : MoonModule.LUNAR_REGOLITH.get()).defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int i = 0; i < 4; i++) {
            double x = -4.5 + i * 3;
            stand(level, x, 0.5, 0F, SETS[i], -1);      // front row facing the camera
            stand(level, x + 1.5, -3.5, 0F, SETS[i], -1); // back row, seen from behind in shot 2
        }
        int[] dyes = {DyeColor.RED.getTextureDiffuseColor(), DyeColor.LIME.getTextureDiffuseColor(), DyeColor.PURPLE.getTextureDiffuseColor()};
        for (int i = 0; i < dyes.length; i++) {
            stand(level, -7.5 + i * 1.5, 4.0, 20F, SETS[0], dyes[i]);
        }
    }

    private static void stand(ServerLevel level, double x, double z, float yaw, List<DeferredItem<SpaceSuitItem>> set, int dye) {
        ArmorStand stand = new ArmorStand(level, x, G, z);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ShowArms", true);
        tag.putBoolean("NoBasePlate", true);
        stand.readAdditionalSaveData(tag);
        stand.setYRot(yaw);
        stand.setYBodyRot(yaw);
        stand.setYHeadRot(yaw);
        stand.setLeftArmPose(new Rotations(-10F, 0F, -8F));
        stand.setRightArmPose(new Rotations(-10F, 0F, 8F));
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            ItemStack s = new ItemStack(set.get(i).get());
            if (dye != -1) {
                s.set(net.minecraft.core.component.DataComponents.DYED_COLOR, new DyedItemColor(dye, true));
            }
            stand.setItemSlot(slots[i], s);
        }
        level.addFreshEntity(stand);
    }

    @Override
    public List<Shot> shots() {
        List<Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        s.add(new Shot("tiers_01_front", 5000, 0.0, G + 2.2, 6.5, 180F, 10F, true, fp, 140, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("tiers_02_back", 5000, 0.0, G + 2.2, -10.0, 0F, 10F, true, fp, 60, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("tiers_03_thermal_heated", 5000, 0.0, G + 1.7, 3.6, 180F, 6F, true, fp, 40, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("tiers_04_radiation_close", 5000, 3.0, G + 1.7, 2.9, 180F, 6F, true, fp, 40, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("tiers_05_three_quarter", 5000, 5.5, G + 2.0, 4.5, 140F, 12F, true, fp, 40, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("tiers_06_dyed", 5000, -6.0, G + 1.8, 8.5, 185F, 8F, true, fp, 40, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("tiers_07_inventory", 5000, 0.0, G + 1.62, 7.0, 180F, 5F, false, fp, 30, null, SuitShots.clearToastsThen(ShowcaseDriver::openCreativeTab)));
        return s;
    }
}
