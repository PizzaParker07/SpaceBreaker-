package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.function.BooleanSupplier;

/**
 * The level-loading screen while a rocket changes dimension: the same closing logic as vanilla's, but it
 * draws the transit view instead of a dirt background and "Loading terrain".
 */
public class TransitScreen extends ReceivingLevelScreen {
    public TransitScreen(BooleanSupplier levelReceived, Reason reason) {
        super(levelReceived, reason);
        TransitView.hold();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF01020A);
        TransitView.render(g, width, height, 1F);
        TransitView.caption(g, width, height, 1F);
    }

    @EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
    static final class Hook {
        private Hook() {
        }

        @SubscribeEvent
        static void opening(ScreenEvent.Opening event) {
            if (event.getNewScreen() == null || event.getNewScreen().getClass() != ReceivingLevelScreen.class) {
                return;
            }
            RocketEntity rocket = RocketHud.ridden();
            if (!TransitView.active()) {
                if (rocket == null) {
                    return; // not a rocket flight: leave other transitions alone
                }
                TransitView.begin(Transit.Kind.TRANSFER, rocket.location(), rocket.destination());
            }
            ReceivingLevelScreen vanilla = (ReceivingLevelScreen) event.getNewScreen();
            try {
                BooleanSupplier received = ObfuscationReflectionHelper.getPrivateValue(ReceivingLevelScreen.class, vanilla, "levelReceived");
                Reason reason = ObfuscationReflectionHelper.getPrivateValue(ReceivingLevelScreen.class, vanilla, "reason");
                if (received != null) {
                    event.setNewScreen(new TransitScreen(received, reason == null ? Reason.OTHER : reason));
                }
            } catch (RuntimeException e) {
                Spacebreaker.LOGGER.debug("Transit view unavailable, using the vanilla loading screen", e);
            }
        }
    }
}
