package com.velorise.staves.client;

import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import com.velorise.staves.network.AuraSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import java.util.ArrayList;
import java.util.List;

public final class VeloriseStavesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(AuraSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                List<Element> elements = new ArrayList<>();
                for (Element e : Element.values()) {
                    if ((payload.activeMask() & (1 << e.ordinal())) != 0) {
                        elements.add(e);
                    }
                }
                Element reactedAura = (payload.reactedAuraOrdinal() > 0 && payload.reactedAuraOrdinal() <= Element.values().length)
                        ? Element.values()[payload.reactedAuraOrdinal() - 1] : null;
                Element trigger = (payload.triggerElementOrdinal() > 0 && payload.triggerElementOrdinal() <= Element.values().length)
                        ? Element.values()[payload.triggerElementOrdinal() - 1] : null;
                ElementalReaction reaction = (payload.reactionOrdinal() > 0 && payload.reactionOrdinal() <= ElementalReaction.values().length)
                        ? ElementalReaction.values()[payload.reactionOrdinal() - 1] : null;

                ClientAuraTracker.update(payload.entityId(), elements, reactedAura, trigger, reaction, payload.damage(), payload.durationTicks());
            });
        });

        // Register In-Game HUD overlay
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> {
            ClientAuraRenderer.renderPlayerAuraHud(guiGraphics);
        });

        // Register Inventory Screen HUD overlay
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InventoryScreen) {
                ScreenEvents.afterRender(screen).register((scr, guiGraphics, mouseX, mouseY, tickDelta) -> {
                    ClientAuraRenderer.renderPlayerAuraHud(guiGraphics);
                });
            }
        });
    }
}
