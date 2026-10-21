
package com.example.compasshud;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class CompassHudClient implements ClientModInitializer {
    public static final String MOD_ID = "compasshud";

    /** Compass is ON by default. */
    public static boolean enabled = true;

    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.compasshud.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                enabled = !enabled;
                if (client.player != null) {
                    client.player.displayClientMessage(
                            Component.literal("Compass HUD: " + (enabled ? "ON" : "OFF")), true);
                }
            }
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath(MOD_ID, "compass"),
                (graphics, deltaTracker) -> CompassRenderer.render(graphics, deltaTracker));
    }
}
