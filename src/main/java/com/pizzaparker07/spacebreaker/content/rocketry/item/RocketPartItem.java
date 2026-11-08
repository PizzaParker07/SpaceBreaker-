package com.pizzaparker07.spacebreaker.content.rocketry.item;

import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketPart;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketParts;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Optional;

/** An item that is a rocket part; its stats come from the rocket_part data entry with the same id. */
public class RocketPartItem extends Item {
    public RocketPartItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        HolderLookup.Provider registries = context.registries();
        if (!(registries instanceof RegistryAccess access)) {
            return;
        }
        Optional<RocketPart> found = RocketParts.forStack(access, stack);
        if (found.isEmpty()) {
            return;
        }
        RocketPart p = found.get();
        ChatFormatting grey = ChatFormatting.GRAY;
        tooltip.add(Component.translatable("tooltip.spacebreaker.part.mass", Fmt.kg(p.massKg())).withStyle(grey));
        if (p.propellantKg() > 0) {
            tooltip.add(Component.translatable("tooltip.spacebreaker.part.propellant", Fmt.kg(p.propellantKg()),
                    p.rp1Millibuckets(), p.loxMillibuckets()).withStyle(grey));
            tooltip.add(Component.translatable("tooltip.spacebreaker.part.propellant_methalox",
                    com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.METHALOX.fuelMillibuckets(p.propellantKg()),
                    com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.METHALOX.loxMillibuckets(p.propellantKg())).withStyle(grey));
            tooltip.add(Component.translatable("tooltip.spacebreaker.part.propellant_hydrolox",
                    com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.HYDROLOX.fuelMillibuckets(p.propellantKg()),
                    com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.HYDROLOX.loxMillibuckets(p.propellantKg())).withStyle(grey));
            tooltip.add(Component.translatable("tooltip.spacebreaker.part.propellant_nuclear",
                    com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.NUCLEAR.fuelMillibuckets(p.propellantKg())).withStyle(grey));
            if (p.insulated()) {
                tooltip.add(Component.translatable("tooltip.spacebreaker.part.insulated",
                        Math.round(com.pizzaparker07.spacebreaker.content.rocketry.design.BoilOff.INSULATION * 100)).withStyle(ChatFormatting.GOLD));
            }
            tooltip.add(Component.translatable("tooltip.spacebreaker.part.hydrogen_note").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (p.isEngine()) {
            tooltip.add(Component.translatable("tooltip.spacebreaker.part.thrust", Fmt.num(p.thrustKn())).withStyle(grey));
            tooltip.add(Component.translatable("tooltip.spacebreaker.part.isp", Fmt.num(p.ispSeaLevel()), Fmt.num(p.ispVacuum())).withStyle(grey));
            tooltip.add(Component.translatable("tooltip.spacebreaker.part.burns", p.propellant().displayName()).withStyle(grey));
            if (p.propellant().boilOffPerDay() > 0) {
                tooltip.add(Component.translatable("tooltip.spacebreaker.part.boil_off",
                        Fmt.dec(p.propellant().boilOffPerDay() * 100, 2)).withStyle(grey));
            }
            if (p.propellant() == com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.NUCLEAR) {
                tooltip.add(Component.translatable("tooltip.spacebreaker.part.nuclear").withStyle(ChatFormatting.GOLD));
            }
        }
        if (p.heatShield() > 0) {
            tooltip.add(Component.translatable("tooltip.spacebreaker.part.heat_shield." + Math.min(2, p.heatShield())).withStyle(grey));
        }
        if (p.seats() > 0) {
            tooltip.add(Component.translatable("tooltip.spacebreaker.part.seats", p.seats()).withStyle(grey));
        }
        tooltip.add(Component.translatable("tooltip.spacebreaker.part." + p.type().getSerializedName()).withStyle(ChatFormatting.DARK_GRAY));
        if (p.type() == com.pizzaparker07.spacebreaker.content.rocketry.part.PartType.CARGO) {
            tooltip.add(Component.translatable("tooltip.spacebreaker.part.cargo.route").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
