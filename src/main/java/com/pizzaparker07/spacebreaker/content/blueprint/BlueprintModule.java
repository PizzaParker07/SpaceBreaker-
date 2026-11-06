package com.pizzaparker07.spacebreaker.content.blueprint;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Blueprints: the Hologram Projector and the multiblock registry ({@code core.multiblock}). Shows players how
 * every multiblock of the mod is built, layer by layer, with a material list.
 */
public final class BlueprintModule implements SbModule {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Spacebreaker.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Spacebreaker.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Hologram>> HOLOGRAM =
            COMPONENTS.registerComponentType("hologram", b -> b.persistent(Hologram.CODEC).networkSynchronized(Hologram.STREAM_CODEC));
    public static final DeferredItem<HologramProjectorItem> HOLOGRAM_PROJECTOR = ITEMS.registerItem("hologram_projector",
            HologramProjectorItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    @Override
    public String id() {
        return "blueprint";
    }

    @Override
    public void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(Multiblocks::registerRegistry);
        modBus.addListener(BlueprintModule::payloads);
        SbTabs.EQUIPMENT.add(HOLOGRAM_PROJECTOR);
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(BlueprintPayload.TYPE, BlueprintPayload.CODEC, (payload, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = projectorInHand(player);
            if (stack.isEmpty()) {
                return;
            }
            Hologram h = HologramProjectorItem.hologram(stack);
            switch (payload.action()) {
                case BlueprintPayload.SELECT -> {
                    if (Multiblocks.get(player.registryAccess(), payload.blueprint()).isPresent()) {
                        stack.set(HOLOGRAM.get(), Hologram.of(payload.blueprint()));
                    }
                }
                case BlueprintPayload.ROTATE_CW, BlueprintPayload.ROTATE_CCW -> {
                    if (h != null) {
                        stack.set(HOLOGRAM.get(), h.rotated(payload.action() == BlueprintPayload.ROTATE_CW ? 1 : 3));
                    }
                }
                default -> {
                }
            }
        });
    }

    public static ItemStack projectorInHand(net.minecraft.world.entity.player.Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            if (s.is(HOLOGRAM_PROJECTOR.get())) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.item(HOLOGRAM_PROJECTOR, "Hologram Projector", "Hologram Projektörü");
        lang.add("tooltip.spacebreaker.hologram.blueprint", "Projecting: %s", "Yansıtılan: %s");
        lang.add("tooltip.spacebreaker.hologram.hint1", "Use: choose a structure · use on a block: place the projection",
                "Kullan: yapı seç · bloğa kullan: yansıtmayı yerleştir");
        lang.add("tooltip.spacebreaker.hologram.hint2", "Sneak + use: layers · sneak + wheel: turn · sneak + use on a block: remove",
                "Eğil + kullan: katmanlar · eğil + tekerlek: döndür · eğil + bloğa kullan: kaldır");
        lang.add("message.spacebreaker.hologram.all_layers", "Showing all layers", "Tüm katmanlar gösteriliyor");
        lang.add("message.spacebreaker.hologram.layer", "Layer %s of %s", "Katman %s / %s");
        lang.add("message.spacebreaker.hologram.cleared", "Projection removed", "Yansıtma kaldırıldı");
        lang.add("message.spacebreaker.hologram.complete", "Structure complete: %s", "Yapı tamam: %s");
        lang.add("message.spacebreaker.hologram.opened", "Projecting what is missing: %s", "Eksikler yansıtılıyor: %s");
        lang.add("message.spacebreaker.hologram.need_projector", "Carry a Hologram Projector to see how it is built",
                "Nasıl yapıldığını görmek için yanında bir Hologram Projektörü taşı");
        lang.add("gui.spacebreaker.blueprints", "Blueprints", "Planlar");
        lang.add("gui.spacebreaker.blueprints.size", "%s × %s × %s, %s blocks", "%s × %s × %s, %s blok");
        lang.add("gui.spacebreaker.blueprints.empty", "No blueprints loaded", "Yüklü plan yok");
        lang.add("hud.spacebreaker.materials", "Materials", "Malzemeler");
        lang.add("hud.spacebreaker.materials.done", "%s / %s placed", "%s / %s yerleştirildi");
        lang.add("hud.spacebreaker.materials.missing", "%s missing", "%s eksik");
        lang.add("gui.spacebreaker.blueprints.project", "Project", "Yansıt");
        lang.add("gui.spacebreaker.blueprints.materials", "Materials", "Malzemeler");
        lang.add("gui.spacebreaker.blueprints.tech", "Research: %s", "Araştırma: %s");
        lang.add("gui.spacebreaker.blueprints.tech_known", "Researched", "Araştırıldı");
        lang.add("gui.spacebreaker.blueprints.tech_unknown", "Not researched yet (you can still build it)", "Henüz araştırılmadı (yine de kurabilirsin)");
        lang.add("multiblock.spacebreaker.launch_site", "Launch Site", "Fırlatma Alanı");
        lang.add("multiblock.spacebreaker.pressurised_habitat", "Pressurised Habitat", "Basınçlı Yaşam Modülü");
    }

    static Component name(net.minecraft.resources.ResourceLocation id) {
        return Component.translatable("multiblock." + id.getNamespace() + "." + id.getPath());
    }
}
