package com.pizzaparker07.spacebreaker.content.suit.block;

import com.pizzaparker07.spacebreaker.content.suit.ManeuveringUnitItem;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.SuitProtection;
import com.pizzaparker07.spacebreaker.content.suit.SuitTanks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Suit Maintenance Station. Every suited player standing on it or right next to it gets their backpack tanks and
 * Maneuvering Unit topped up (like the Oxygen Filler: the planet's breathable air, else piped oxygen) and their
 * suit pieces slowly mended; a suit piece in its slot is mended too. Runs on FE.
 */
public class SuitStationBlockEntity extends GasMachineBlockEntity {
    /** Breathing ticks pumped per game tick into a wearer's tanks. */
    public static final int FILL_RATE = OxygenFillerBlockEntity.FILL_RATE;
    public static final int FE_PER_FILL_TICK = 8;
    /** One point of wear mended on each damaged piece this often, for this much FE per point. */
    public static final int REPAIR_TICKS = 40;
    public static final int FE_PER_REPAIR = 120;

    public SuitStationBlockEntity(BlockPos pos, BlockState state) {
        super(SuitModule.SUIT_STATION_BE.get(), pos, state, 32000);
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return stack.getItem() instanceof SpaceSuitItem;
    }

    /** Players being serviced: on top of the station or beside it. */
    public AABB serviceArea() {
        return new AABB(worldPosition).inflate(1.5, 0, 1.5).expandTowards(0, 2, 0);
    }

    /** One tick of topping up a wearer's tanks and unit; returns true when it pumped. */
    public boolean fillWearer(Player player, boolean naturalAir) {
        ItemStack torso = SuitTanks.wornTorso(player);
        if (torso.isEmpty() || energy.getEnergyStored() < FE_PER_FILL_TICK) {
            return false;
        }
        int budget = naturalAir ? FILL_RATE : Math.min(FILL_RATE, storedOxygen);
        if (budget <= 0) {
            return false;
        }
        int pumped = 0;
        NonNullList<ItemStack> tanks = SuitTanks.tanks(torso);
        for (int i = 0; i < tanks.size() && pumped < budget; i++) {
            ItemStack t = tanks.get(i);
            if (t.getItem() instanceof OxygenTankItem) {
                ItemStack copy = t.copy();
                pumped += OxygenTankItem.fill(copy, budget - pumped);
                tanks.set(i, copy);
            }
        }
        if (pumped > 0) {
            SuitTanks.setTanks(torso, tanks);
        }
        ItemStack mmu = SuitTanks.maneuveringUnit(torso);
        if (pumped < budget && !mmu.isEmpty()) {
            ItemStack copy = mmu.copy();
            int gas = ManeuveringUnitItem.fill(copy, (budget - pumped) / 2);
            if (gas > 0) {
                SuitTanks.setManeuveringUnit(torso, copy);
                pumped += gas * 2;
            }
        }
        if (pumped <= 0) {
            return false;
        }
        if (!naturalAir) {
            takeStored(pumped);
        }
        energy.consume(FE_PER_FILL_TICK);
        setChanged();
        return true;
    }

    /** Mends one point of wear on a suit piece if there is power; returns true when it did. */
    public boolean mend(ItemStack stack) {
        if (!(stack.getItem() instanceof SpaceSuitItem) || !stack.isDamaged() || energy.getEnergyStored() < FE_PER_REPAIR) {
            return false;
        }
        stack.setDamageValue(stack.getDamageValue() - 1);
        energy.consume(FE_PER_REPAIR);
        setChanged();
        return true;
    }

    /** One service tick for one wearer: tanks every tick, wear every {@link #REPAIR_TICKS}. */
    public boolean service(Player player, boolean naturalAir, long gameTime) {
        boolean worked = fillWearer(player, naturalAir);
        if (gameTime % REPAIR_TICKS == 0) {
            for (EquipmentSlot slot : SuitProtection.SLOTS) {
                worked |= mend(player.getItemBySlot(slot));
            }
        }
        return worked;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SuitStationBlockEntity be) {
        boolean air = OxygenFillerBlockEntity.naturalAir(level);
        boolean working = false;
        for (Player p : level.getEntitiesOfClass(Player.class, be.serviceArea(), p -> !p.isSpectator())) {
            working |= be.service(p, air, level.getGameTime());
        }
        if (level.getGameTime() % REPAIR_TICKS == 0) {
            ItemStack piece = be.getStack().copy();
            if (be.mend(piece)) {
                be.items.setStackInSlot(0, piece);
                working = true;
            }
        }
        // the lamp stays on a little between mending steps so it does not flicker
        if (working || level.getGameTime() % REPAIR_TICKS == 0) {
            if (state.getValue(GasMachineBlock.LIT) != working) {
                level.setBlock(pos, state.setValue(GasMachineBlock.LIT, working), Block.UPDATE_CLIENTS);
            }
        }
    }
}
