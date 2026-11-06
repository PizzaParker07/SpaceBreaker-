package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.item.OxygenTankItem;
import com.pizzaparker07.spacebreaker.registry.ModItems;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;

/** Camera script for each scenario. Positions are eye positions; yaw 180 looks north. */
final class ShowcaseShots {
    private static final double G = ShowcaseBuilder.FLOOR + 1; // standing height

    private ShowcaseShots() {
    }

    static List<Shot> forScenario(String scenario) {
        List<Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        s.add(new Shot("01_wide_morning", 3200, 0.5, G + 4.2, 13.5, 180F, 18F, true, fp, 140, null, null));
        s.add(new Shot("02_wide_noon", 6000, 0.5, G + 4.2, 13.5, 180F, 18F, true, fp, 160, null, null));
        s.add(new Shot("03_ores_close", 6000, 0.5, G + 2.4, 4.6, 180F, 22F, true, fp, 40, null, null));
        s.add(new Shot("04_item_wall", 6000, 0.5, G + 2.6, 1.6, 180F, 2F, true, fp, 40, null, null));
        s.add(new Shot("05_helmets_close", 4500, 4.5, G + 1.6, 7.9, 180F, 4F, true, fp, 40, null, null));
        s.add(new Shot("05b_helmet_macro", 4500, 3.7, G + 1.75, 6.95, 172F, 6F, true, fp, 40, null, null));
        s.add(new Shot("05c_sun_visor_macro", 4500, 5.7, G + 1.75, 6.95, 172F, 6F, true, fp, 40, null, null));
        s.add(new Shot("06_solar_morning", 2600, -2.6, G + 2.4, 9.2, 145F, 20F, true, fp, 160, null, null));
        s.add(new Shot("07_solar_afternoon", 9400, -2.6, G + 2.4, 9.2, 145F, 20F, true, fp, 200, null, null));
        s.add(new Shot("08_footprints", 5000, 1.5, G + 3.4, 12.0, 175F, 48F, true, fp, 40, null, null));
        s.add(new Shot("09_night", 15200, 3.0, G + 2.2, 10.5, 195F, 10F, true, fp, 60, null, null));
        s.add(new Shot("10_player_helmet", 5200, 1.5, G + 1.62, 8.0, 0F, 0F, true, CameraType.THIRD_PERSON_FRONT, 40,
                ShowcaseShots::equipPlayer, null));
        s.add(new Shot("11_hand_tank", 6000, -0.5, G + 1.62, 7.5, 200F, 8F, false, fp, 40,
                ShowcaseShots::fillHotbar, null));
        s.add(new Shot("12_creative_tab", 6000, -0.5, G + 1.62, 7.5, 200F, 8F, false, fp, 30,
                ShowcaseShots::fillHotbar, ShowcaseDriver::openCreativeTab));
        return s;
    }

    private static void equipPlayer(ServerPlayer player) {
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.EVA_HELMET.get()));
        player.setItemSlot(EquipmentSlot.MAINHAND, OxygenTankItem.filled(ModItems.OXYGEN_TANK.get()));
    }

    private static void fillHotbar(ServerPlayer player) {
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        var inv = player.getInventory();
        inv.setItem(0, OxygenTankItem.filled(ModItems.OXYGEN_TANK.get()));
        inv.setItem(1, new ItemStack(ModItems.EVA_HELMET.get()));
        inv.setItem(2, new ItemStack(ModItems.ALUMINUM_INGOT.get()));
        inv.setItem(3, new ItemStack(ModItems.ALUMINUM_PLATE.get()));
        inv.setItem(4, new ItemStack(ModItems.RAW_BAUXITE.get()));
        inv.setItem(5, new ItemStack(ModItems.BAUXITE_ORE.get()));
        inv.setItem(6, new ItemStack(ModItems.ILMENITE_ORE.get()));
        inv.setItem(7, new ItemStack(ModItems.LUNAR_REGOLITH.get()));
        inv.setItem(8, new ItemStack(ModItems.SOLAR_PANEL.get()));
        inv.selected = 0;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
        player.inventoryMenu.broadcastChanges();
    }
}
