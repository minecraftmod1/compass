package com.example.compasshud;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

public final class CompassRenderer {
    private CompassRenderer() {}

    // ======================= EASY SETTINGS =======================
    // Position and size are set in game: press F4.

    /** Letter size multiplier (1.0 = normal font size, scaled with the compass size). */
    public static final float LETTER_SCALE = 0.9f;

    /** Letters orbit at this fraction of the radius. */
    public static final float LETTER_ORBIT = 0.75f;

    /** Shadow offset (down-right) as a fraction of the radius. */
    public static final float SHADOW_OFFSET = 0.03f;

    // Colours as 0xRRGGBB
    public static final int COLOR_ACCENT = 0xF050F8;   // letters, arrow, dot, cross lines
    public static final int COLOR_SHADOW = 0x370F5A;   // drop shadow
    public static final float SHADOW_ALPHA = 0.85f;
    public static final float CROSS_ALPHA = 0.30f;

    // Reference radius used by the colour stops below
    public static final double REF_RADIUS = 65.5;

    // Disk colour stops: { radius (reference px), colour 0xRRGGBB }. Linear blend in between.
    public static final double[][] DISK_STOPS = {
            {0.0,  0xB756DF},
            {27.0, 0xB756DF},   // lighter glow in the middle
            {34.0, 0xA54BCD},   // main body
            {55.0, 0xA54BCD},
            {59.0, 0x8B3CB3},   // darkening
            {60.0, 0x893BB1},   // darkest
            {62.5, 0x8D3DB5},
            {64.0, 0x9743BF},   // rim
            {65.5, 0x9743BF}
    };

    // Shape sizes as fractions of the radius
    public static final double ARROW_APEX = 0.93;
    public static final double ARROW_BASE = 0.83;
    public static final double ARROW_HALF_WIDTH = 0.064;
    public static final double DOT_RADIUS = 0.06;
    public static final double CROSS_HALF_WIDTH = 0.01;   // 0.02R wide in total
    public static final double CROSS_START = 0.10;
    public static final double CROSS_END = 0.66;

    // =============================================================

    // Minecraft yaw of each letter: S=0, W=90, N=180, E=270
    private static final String[] LETTERS = {"S", "W", "N", "E"};
    private static final float[] LETTER_YAW = {0f, 90f, 180f, 270f};

    public static void render(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!CompassConfig.enabled || mc.player == null || mc.options.hideGui) {
            return;
        }

        int size = CompassConfig.size;

        CompassTextures.ensure(mc, size);
        int px = CompassTextures.pixelSize();

        // Keep the compass fully on screen even if the window got smaller.
        int left = Math.max(0, Math.min(CompassConfig.x, g.guiWidth() - size));
        int top = Math.max(0, Math.min(CompassConfig.y, g.guiHeight() - size));

        float yaw = mc.player.getYRot();
        float radius = size / 2f;
        float cx = left + radius;
        float cy = top + radius;

        // 1) Solid disk. Destination is size x size GUI px; the source is the whole texture
        //    (px x px), so it is stretched briefly while the size slider is being dragged.
        g.blit(RenderPipelines.GUI_TEXTURED, CompassTextures.diskId(),
                left, top, 0f, 0f, size, size, px, px, px, px);

        // 2) Cross lines, turning with the dial
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().rotate((float) Math.toRadians(-yaw));
        g.pose().translate(-size / 2f, -size / 2f);
        g.blit(RenderPipelines.GUI_TEXTURED, CompassTextures.crossId(),
                0, 0, 0f, 0f, size, size, px, px, px, px);
        g.pose().popMatrix();

        // 3) Fixed arrow + centre dot (with shadow, baked in the texture)
        g.blit(RenderPipelines.GUI_TEXTURED, CompassTextures.overlayId(),
                left, top, 0f, 0f, size, size, px, px, px, px);

        // 4) Upright letters orbiting the centre
        float ls = LETTER_SCALE * size / 65f;
        float shadow = SHADOW_OFFSET * radius;
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(LETTER_YAW[i] - yaw); // clockwise from top
            float lx = cx + (float) (Math.sin(a) * LETTER_ORBIT * radius);
            float ly = cy - (float) (Math.cos(a) * LETTER_ORBIT * radius);
            Component text = Component.literal(LETTERS[i]).withStyle(ChatFormatting.BOLD);
            drawCentered(g, mc, text, lx + shadow, ly + shadow, ls, 0xFF000000 | COLOR_SHADOW);
            drawCentered(g, mc, text, lx, ly, ls, 0xFF000000 | COLOR_ACCENT);
        }
    }

    private static void drawCentered(GuiGraphics g, Minecraft mc, Component text,
                                     float x, float y, float scale, int argb) {
        float w = mc.font.width(text);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.pose().translate(-w / 2f, -4.5f);
        g.drawString(mc.font, text, 0, 0, argb, false);
        g.pose().popMatrix();
    }
}
