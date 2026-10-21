package com.example.compasshud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Opened with F4: toggle the compass and move it with the X / Y sliders. */
public class CompassSettingsScreen extends Screen {
    private final Screen parent;
    private Button toggleButton;
    private PositionSlider sliderX;
    private PositionSlider sliderY;
    private int panelTop;

    public CompassSettingsScreen(Screen parent) {
        super(Component.literal("Compass HUD Settings"));
        this.parent = parent;
    }

    private int maxX() {
        return Math.max(1, this.width - CompassRenderer.SIZE);
    }

    private int maxY() {
        return Math.max(1, this.height - CompassRenderer.SIZE);
    }

    private static Component toggleLabel() {
        return Component.literal("Compass HUD: " + (CompassConfig.enabled ? "ON" : "OFF"));
    }

    @Override
    protected void init() {
        int w = 200;
        int left = this.width / 2 - w / 2;
        panelTop = this.height / 2 - 48;
        int y = panelTop;

        toggleButton = this.addRenderableWidget(Button.builder(toggleLabel(), button -> {
            CompassConfig.enabled = !CompassConfig.enabled;
            button.setMessage(toggleLabel());
        }).bounds(left, y, w, 20).build());
        y += 24;

        sliderX = this.addRenderableWidget(new PositionSlider(left, y, w, 20, true));
        y += 24;

        sliderY = this.addRenderableWidget(new PositionSlider(left, y, w, 20, false));
        y += 24;

        this.addRenderableWidget(Button.builder(Component.literal("Reset position"), button -> {
            CompassConfig.x = CompassConfig.DEFAULT_X;
            CompassConfig.y = CompassConfig.DEFAULT_Y;
            sliderX.syncFromConfig();
            sliderY.syncFromConfig();
        }).bounds(left, y, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
                .bounds(left + 102, y, 98, 20).build());
    }

    /** No blur or dimming, so you can see the compass clearly while moving it. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, panelTop - 14, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        super.removed();
        CompassConfig.save();
    }

    /** Slider that sets the compass X or Y position in whole GUI pixels. */
    private final class PositionSlider extends AbstractSliderButton {
        private final boolean horizontal;

        PositionSlider(int x, int y, int w, int h, boolean horizontal) {
            super(x, y, w, h, Component.empty(), 0.0);
            this.horizontal = horizontal;
            syncFromConfig();
        }

        private int max() {
            return horizontal ? maxX() : maxY();
        }

        private int current() {
            return (int) Math.round(this.value * max());
        }

        void syncFromConfig() {
            int pos = horizontal ? CompassConfig.x : CompassConfig.y;
            this.value = Math.max(0.0, Math.min(1.0, pos / (double) max()));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal((horizontal ? "X position: " : "Y position: ") + current()));
        }

        @Override
        protected void applyValue() {
            if (horizontal) {
                CompassConfig.x = current();
            } else {
                CompassConfig.y = current();
            }
        }
    }
}
