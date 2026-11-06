package com.pizzaparker07.spacebreaker.content.research.lab;

import com.pizzaparker07.spacebreaker.content.research.Research;
import com.pizzaparker07.spacebreaker.content.research.ResearchModule;
import com.pizzaparker07.spacebreaker.content.research.ResearchSavedData;
import com.pizzaparker07.spacebreaker.content.research.SampleItem;
import com.pizzaparker07.spacebreaker.content.research.TechDiskItem;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Server-side logic of the Research Lab. Slot 0 = input (disk / sample / record), slot 1 = output.
 * Analysis draws the technology's FE/t every tick; without enough power it pauses (progress is kept).
 */
public class ResearchLabBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int CAPACITY = 20_000;
    public static final int MAX_RECEIVE = 200;

    /** Status codes shown by the GUI. */
    public static final int IDLE = 0, WORKING = 1, NO_POWER = 2, NOTHING_NEW = 3, OUTPUT_FULL = 4, UNKNOWN_TECH = 5,
            NO_RESEARCHER = 6, DONE = 7, ALREADY_KNOWN = 8, MISSING_PREREQS = 9;

    private static final int[] SLOTS_UP = {SLOT_INPUT};
    private static final int[] SLOTS_SIDES = {SLOT_INPUT, SLOT_OUTPUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final Buffer energy = new Buffer();
    private @Nullable UUID researcher;
    private String researcherName = "";
    private boolean shareWithTeam;
    private @Nullable ResourceLocation target;
    private @Nullable ResourceLocation lastResult;
    private int progress;
    private int total;
    private int status = IDLE;
    private int retryCooldown;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> total;
                case 2 -> energy.getEnergyStored();
                case 3 -> status;
                case 4 -> shareWithTeam ? 1 : 0;
                case 5 -> currentDraw();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> total = value;
                case 2 -> energy.setStored(value);
                case 3 -> status = value;
                case 4 -> shareWithTeam = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public ResearchLabBlockEntity(BlockPos pos, BlockState state) {
        super(ResearchModule.RESEARCH_LAB_BE.get(), pos, state);
    }

    // ------------------------------------------------------------------ accessors

    public IEnergyStorage getEnergy() {
        return energy;
    }

    public @Nullable IItemHandler itemHandler(@Nullable Direction side) {
        return side == null ? new InvWrapper(this) : new SidedInvWrapper(this, side);
    }

    public @Nullable ResourceLocation target() {
        return target;
    }

    public @Nullable ResourceLocation lastResult() {
        return lastResult;
    }

    public String researcherName() {
        return researcherName;
    }

    public int status() {
        return status;
    }

    public int progress() {
        return progress;
    }

    public int total() {
        return total;
    }

    public boolean sharesWithTeam() {
        return shareWithTeam;
    }

    public void setShareWithTeam(boolean share) {
        if (shareWithTeam != share) {
            shareWithTeam = share;
            setChanged();
            sync();
        }
    }

    /** The researcher is whoever placed or last opened the lab; it cannot change during an analysis. */
    public void setResearcher(Player player) {
        if (target != null && researcher != null && !researcher.equals(player.getUUID())) {
            return;
        }
        if (!player.getUUID().equals(researcher) || !researcherName.equals(player.getGameProfile().getName())) {
            researcher = player.getUUID();
            researcherName = player.getGameProfile().getName();
            retryCooldown = 0;
            setChanged();
            sync();
        }
    }

    public @Nullable UUID researcher() {
        return researcher;
    }

    public int comparatorSignal() {
        if (target == null || total <= 0) {
            return items.get(SLOT_OUTPUT).isEmpty() ? 0 : 15;
        }
        return 1 + Mth.floor(13F * progress / total);
    }

    private int currentDraw() {
        if (target == null || level == null) {
            return 0;
        }
        return Tech.get(level.registryAccess(), target).map(TechNode::effectiveEnergy).orElse(0);
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResearchLabBlockEntity lab) {
        lab.tick((ServerLevel) level);
        boolean working = lab.status == WORKING;
        if (state.getValue(ResearchLabBlock.WORKING) != working) {
            level.setBlock(pos, state.setValue(ResearchLabBlock.WORKING, working), 3);
        }
    }

    private void tick(ServerLevel level) {
        ItemStack input = items.get(SLOT_INPUT);
        if (target == null) {
            if (input.isEmpty()) {
                if (status != DONE && status != ALREADY_KNOWN) {
                    setStatus(IDLE);
                }
                retryCooldown = 0;
                return;
            }
            if (!items.get(SLOT_OUTPUT).isEmpty()) {
                setStatus(OUTPUT_FULL);
                return;
            }
            if (retryCooldown > 0) {
                retryCooldown--;
                return;
            }
            tryStart(level, input);
            return;
        }
        if (input.isEmpty() || !isResearchable(input)) {
            reset();
            return;
        }
        Optional<TechNode> node = Tech.get(level.registryAccess(), target);
        if (node.isEmpty()) {
            reset();
            return;
        }
        int draw = node.get().effectiveEnergy();
        if (energy.getEnergyStored() >= draw) {
            energy.consume(draw);
            progress++;
            setStatus(WORKING);
            if (progress >= total) {
                finish(level);
            } else if (progress % 20 == 0) {
                setChanged();
            }
        } else {
            setStatus(NO_POWER);
        }
    }

    private void tryStart(ServerLevel level, ItemStack input) {
        retryCooldown = 20;
        ServerPlayer player = researcher == null ? null : level.getServer().getPlayerList().getPlayer(researcher);
        if (player == null) {
            setStatus(NO_RESEARCHER);
            return;
        }
        ResourceLocation candidate = null;
        if (input.getItem() instanceof TechDiskItem) {
            ResourceLocation tech = TechDiskItem.tech(input);
            Optional<TechNode> node = tech == null ? Optional.empty() : Tech.get(level.registryAccess(), tech);
            if (node.isEmpty()) {
                setStatus(UNKNOWN_TECH);
                return;
            }
            if (Tech.knows(player, tech) && !(shareWithTeam && player.getTeam() != null)) {
                // nothing to learn: hand the disk straight back
                lastResult = tech;
                moveToOutput(level, input.copy());
                items.set(SLOT_INPUT, ItemStack.EMPTY);
                setStatus(ALREADY_KNOWN);
                return;
            }
            if (!Tech.prerequisitesMet(player, node.get())) {
                lastResult = tech;
                setStatus(MISSING_PREREQS);
                return;
            }
            candidate = tech;
        } else if (input.getItem() instanceof SampleItem) {
            ResourceLocation kind = SampleItem.kind(input);
            Optional<ResourceLocation> pick = kind == null ? Optional.empty() : Research.pickRevealed(player, kind, level.random);
            if (pick.isEmpty()) {
                setStatus(NOTHING_NEW);
                return;
            }
            candidate = pick.get();
        }
        if (candidate == null) {
            return;
        }
        target = candidate;
        total = Tech.get(level.registryAccess(), candidate).map(TechNode::effectiveResearchTime).orElse(600);
        progress = 0;
        setStatus(WORKING);
        setChanged();
        sync();
    }

    private void finish(ServerLevel level) {
        ResourceLocation tech = target;
        ItemStack input = items.get(SLOT_INPUT);
        if (input.getItem() instanceof TechDiskItem) {
            ItemStack read = input.copy();
            read.set(ResearchModule.ANALYSED.get(), true);
            items.set(SLOT_INPUT, ItemStack.EMPTY);
            moveToOutput(level, read);
        } else {
            input.shrink(1);
        }
        ServerPlayer player = researcher == null ? null : level.getServer().getPlayerList().getPlayer(researcher);
        if (player != null) {
            Research.teach(player, tech, shareWithTeam);
            player.displayClientMessage(Component.translatable("message.spacebreaker.tech_learned",
                    Component.translatable(TechNode.nameKey(tech))), false);
        } else if (researcher != null) {
            ResearchSavedData.get(level.getServer()).addPending(researcher, tech);
        }
        level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.4F);
        lastResult = tech;
        target = null;
        progress = 0;
        total = 0;
        setStatus(DONE);
        setChanged();
        sync();
    }

    private void moveToOutput(ServerLevel level, ItemStack stack) {
        if (items.get(SLOT_OUTPUT).isEmpty()) {
            items.set(SLOT_OUTPUT, stack);
        } else {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, stack);
        }
    }

    private void reset() {
        target = null;
        progress = 0;
        total = 0;
        setStatus(IDLE);
        setChanged();
        sync();
    }

    private void setStatus(int status) {
        if (this.status != status) {
            this.status = status;
            sync();
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public static boolean isResearchable(ItemStack stack) {
        return stack.is(ResearchModule.RESEARCHABLE);
    }

    // ------------------------------------------------------------------ container

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && isResearchable(stack);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (slot == SLOT_INPUT) {
            retryCooldown = 0;
            if (status == DONE || status == ALREADY_KNOWN || status == NOTHING_NEW || status == MISSING_PREREQS) {
                status = IDLE;
            }
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? SLOTS_UP : SLOTS_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.spacebreaker.research_lab");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ResearchLabMenu(id, inventory, this, data);
    }

    // ------------------------------------------------------------------ save / sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        writeShared(tag);
        tag.putInt("energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(2, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        readShared(tag);
        if (tag.contains("energy")) {
            energy.setStored(tag.getInt("energy"));
        }
    }

    private void writeShared(CompoundTag tag) {
        if (researcher != null) {
            tag.putUUID("researcher", researcher);
        }
        tag.putString("researcher_name", researcherName);
        tag.putBoolean("share", shareWithTeam);
        if (target != null) {
            tag.putString("target", target.toString());
        }
        if (lastResult != null) {
            tag.putString("last", lastResult.toString());
        }
        tag.putInt("progress", progress);
        tag.putInt("total", total);
        tag.putInt("status", status);
    }

    private void readShared(CompoundTag tag) {
        researcher = tag.hasUUID("researcher") ? tag.getUUID("researcher") : null;
        researcherName = tag.getString("researcher_name");
        shareWithTeam = tag.getBoolean("share");
        target = tag.contains("target") ? ResourceLocation.tryParse(tag.getString("target")) : null;
        lastResult = tag.contains("last") ? ResourceLocation.tryParse(tag.getString("last")) : null;
        progress = tag.getInt("progress");
        total = tag.getInt("total");
        status = tag.getInt("status");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeShared(tag);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Energy buffer: accepts FE from cables / generators, used only by the lab itself. */
    private static final class Buffer extends EnergyStorage {
        Buffer() {
            super(CAPACITY, MAX_RECEIVE, 0);
        }

        void consume(int amount) {
            energy = Math.max(0, energy - amount);
        }

        void setStored(int amount) {
            energy = Mth.clamp(amount, 0, capacity);
        }
    }
}
