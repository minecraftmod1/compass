package com.example.compasshud;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class CompassHudClient implements ClientModInitializer {
    public static final String MOD_ID = "compasshud";

    private static KeyMapping settingsKey;

    @Override
    public void onInitializeClient() {
        CompassConfig.load();

        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.compasshud.settings",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (settingsKey.consumeClick()) {
                client.setScreen(new CompassSettingsScreen(client.screen));
            }
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath(MOD_ID, "compass"),
                (graphics, deltaTracker) -> CompassRenderer.render(graphics, deltaTracker));
    }
}
