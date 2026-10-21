package com.example.compasshud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Opened with F4: toggle the compass, change its size and move it with sliders. */
public class CompassSettingsScreen extends Screen {
    private enum Kind { SIZE, X, Y }

    private final Screen parent;
    private ConfigSlider sliderSize;
    private ConfigSlider sliderX;
    private ConfigSlider sliderY;
    private int panelTop;

    public CompassSettingsScreen(Screen parent) {
        super(Component.literal("Compass HUD Settings"));
        this.parent = parent;
    }

    /** Furthest right the compass can be placed at its current size. */
    private int maxPosX() {
        return Math.max(1, this.width - CompassConfig.size);
    }

    /** Furthest down the compass can be placed at its current size. */
    private int maxPosY() {
        return Math.max(1, this.height - CompassConfig.size);
    }

    private static Component toggleLabel() {
        return Component.literal("Compass HUD: " + (CompassConfig.enabled ? "ON" : "OFF"));
    }

    @Override
    protected void init() {
        int w = 200;
        int left = this.width / 2 - w / 2;
        panelTop = this.height / 2 - 60;
        int y = panelTop;

        this.addRenderableWidget(Button.builder(toggleLabel(), button -> {
            CompassConfig.enabled = !CompassConfig.enabled;
            button.setMessage(toggleLabel());
        }).bounds(left, y, w, 20).build());
        y += 24;

        sliderSize = this.addRenderableWidget(new ConfigSlider(left, y, w, 20, Kind.SIZE));
        y += 24;

        sliderX = this.addRenderableWidget(new ConfigSlider(left, y, w, 20, Kind.X));
        y += 24;

        sliderY = this.addRenderableWidget(new ConfigSlider(left, y, w, 20, Kind.Y));
        y += 24;

        this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
            CompassConfig.size = CompassConfig.DEFAULT_SIZE;
            CompassConfig.x = CompassConfig.DEFAULT_X;
            CompassConfig.y = CompassConfig.DEFAULT_Y;
            sliderSize.syncFromConfig();
            sliderX.syncFromConfig();
            sliderY.syncFromConfig();
        }).bounds(left, y, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
                .bounds(left + 102, y, 98, 20).build());
    }

    /** No blur or dimming, so you can see the compass clearly while adjusting it. */
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

    /** One slider class for size, X position and Y position (all in whole GUI pixels). */
    private final class ConfigSlider extends AbstractSliderButton {
        private final Kind kind;

        ConfigSlider(int x, int y, int w, int h, Kind kind) {
            super(x, y, w, h, Component.empty(), 0.0);
            this.kind = kind;
            syncFromConfig();
        }

        private int rangeMin() {
            return kind == Kind.SIZE ? CompassConfig.MIN_SIZE : 0;
        }

        private int rangeMax() {
            return switch (kind) {
                case SIZE -> CompassConfig.MAX_SIZE;
                case X -> maxPosX();
                case Y -> maxPosY();
            };
        }

        private int currentValue() {
            return rangeMin() + (int) Math.round(this.value * (rangeMax() - rangeMin()));
        }

        void syncFromConfig() {
            int pos = switch (kind) {
                case SIZE -> CompassConfig.size;
                case X -> CompassConfig.x;
                case Y -> CompassConfig.y;
            };
            int range = Math.max(1, rangeMax() - rangeMin());
            this.value = Math.max(0.0, Math.min(1.0, (pos - rangeMin()) / (double) range));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (this.kind == null) {
                return;
            }
            String label = switch (kind) {
                case SIZE -> "Size: ";
                case X -> "X position: ";
                case Y -> "Y position: ";
            };
            this.setMessage(Component.literal(label + currentValue()));
        }

        @Override
        protected void applyValue() {
            int v = currentValue();
            switch (kind) {
                case SIZE -> {
                    CompassConfig.size = v;
                    // A bigger compass has less room: keep it fully on screen.
                    CompassConfig.x = Math.min(CompassConfig.x, maxPosX());
                    CompassConfig.y = Math.min(CompassConfig.y, maxPosY());
                    if (sliderX != null) {
                        sliderX.syncFromConfig();
                    }
                    if (sliderY != null) {
                        sliderY.syncFromConfig();
                    }
                }
                case X -> CompassConfig.x = v;
                case Y -> CompassConfig.y = v;
            }
        }
    }
}
