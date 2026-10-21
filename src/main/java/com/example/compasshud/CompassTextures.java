
package com.example.compasshud;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Generates the compass textures in code (no image files).
 * They are rebuilt whenever the GUI scale changes, so the texture is always
 * exactly SIZE * guiScale device pixels and is drawn 1:1. That way the
 * anti-aliased edge is baked in at the exact on-screen resolution and no
 * texture filtering is needed.
 */
public final class CompassTextures {
    private CompassTextures() {}

    private static final int SS = 4; // supersampling: SS x SS samples per pixel

    private static int builtPx = -1;
    private static int generation = 0;
    private static Identifier disk, cross, overlay;

    public static Identifier diskId() { return disk; }
    public static Identifier crossId() { return cross; }
    public static Identifier overlayId() { return overlay; }
    public static int pixelSize() { return builtPx; }

    public static void ensure(Minecraft mc, int sizeGui) {
        int scale = Math.max(1, (int) Math.round(mc.getWindow().getGuiScale()));
        int px = sizeGui * scale;
        if (px == builtPx && disk != null) {
            return;
        }
        release(mc);
        generation++;
        builtPx = px;
        disk = register(mc, "disk_" + generation, buildDisk(px));
        cross = register(mc, "cross_" + generation, buildCross(px));
        overlay = register(mc, "overlay_" + generation, buildOverlay(px));
    }

    private static void release(Minecraft mc) {
        if (disk != null) mc.getTextureManager().release(disk);
        if (cross != null) mc.getTextureManager().release(cross);
        if (overlay != null) mc.getTextureManager().release(overlay);
        disk = cross = overlay = null;
    }

    private static Identifier register(Minecraft mc, String name, NativeImage image) {
        Identifier id = Identifier.fromNamespaceAndPath(CompassHudClient.MOD_ID, name);
        DynamicTexture tex = new DynamicTexture(() -> "compasshud_" + name, image);
        tex.upload();
        mc.getTextureManager().register(id, tex);
        return id;
    }

    // ---------------------------------------------------------------- pixels

    /** NativeImage.setPixelABGR expects 0xAABBGGRR. Red is in the LOW byte. */
    private static int abgr(int rgb, double alpha) {
        int a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255.0);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static int lerpColor(int c0, int c1, double t) {
        int r = (int) Math.round(((c0 >> 16) & 0xFF) * (1 - t) + ((c1 >> 16) & 0xFF) * t);
        int g = (int) Math.round(((c0 >> 8) & 0xFF) * (1 - t) + ((c1 >> 8) & 0xFF) * t);
        int b = (int) Math.round((c0 & 0xFF) * (1 - t) + (c1 & 0xFF) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static int diskColor(double rRef) {
        double[][] s = CompassRenderer.DISK_STOPS;
        if (rRef <= s[0][0]) return (int) s[0][1];
        for (int i = 1; i < s.length; i++) {
            if (rRef <= s[i][0]) {
                double t = (rRef - s[i - 1][0]) / (s[i][0] - s[i - 1][0]);
                return lerpColor((int) s[i - 1][1], (int) s[i][1], t);
            }
        }
        return (int) s[s.length - 1][1];
    }

    // ------------------------------------------------------------------ disk

    private static NativeImage buildDisk(int n) {
        NativeImage img = new NativeImage(n, n, true);
        double half = n / 2.0;
        double R = half - 0.5; // pixel radius of the outer edge
        double k = R / CompassRenderer.REF_RADIUS;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int inside = 0;
                for (int sy = 0; sy < SS; sy++) {
                    for (int sx = 0; sx < SS; sx++) {
                        double dx = x + (sx + 0.5) / SS - half;
                        double dy = y + (sy + 0.5) / SS - half;
                        if (dx * dx + dy * dy <= R * R) inside++;
                    }
                }
                double cov = inside / (double) (SS * SS);
                double dx = x + 0.5 - half;
                double dy = y + 0.5 - half;
                double rRef = Math.min(Math.sqrt(dx * dx + dy * dy) / k, CompassRenderer.REF_RADIUS);
                img.setPixelABGR(x, y, abgr(diskColor(rRef), cov)); // interior alpha = 1.0 (solid)
            }
        }
        return img;
    }

    // ----------------------------------------------------------------- cross

    private static NativeImage buildCross(int n) {
        NativeImage img = new NativeImage(n, n, true);
        double half = n / 2.0;
        double R = half - 0.5;
        double hw = CompassRenderer.CROSS_HALF_WIDTH * R;
        double lo = CompassRenderer.CROSS_START * R;
        double hi = CompassRenderer.CROSS_END * R;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int hit = 0;
                for (int sy = 0; sy < SS; sy++) {
                    for (int sx = 0; sx < SS; sx++) {
                        double ax = Math.abs(x + (sx + 0.5) / SS - half);
                        double ay = Math.abs(y + (sy + 0.5) / SS - half);
                        boolean vert = ax <= hw && ay >= lo && ay <= hi;
                        boolean horz = ay <= hw && ax >= lo && ax <= hi;
                        if (vert || horz) hit++;
                    }
                }
                double cov = hit / (double) (SS * SS);
                img.setPixelABGR(x, y, abgr(CompassRenderer.COLOR_ACCENT, cov * CompassRenderer.CROSS_ALPHA));
            }
        }
        return img;
    }

    // --------------------------------------------------------------- overlay

    /** Fixed arrow + centre dot, with a soft dark-purple drop shadow. */
    private static boolean inShape(double dx, double dy, double R) {
        // centre dot
        if (dx * dx + dy * dy <= sq(CompassRenderer.DOT_RADIUS * R)) return true;
        // arrow: triangle pointing up, y is negative upwards
        double apex = -CompassRenderer.ARROW_APEX * R;
        double base = -CompassRenderer.ARROW_BASE * R;
        if (dy >= apex && dy <= base) {
            double t = (dy - apex) / (base - apex); // 0 at apex, 1 at base
            return Math.abs(dx) <= CompassRenderer.ARROW_HALF_WIDTH * R * t;
        }
        return false;
    }

    private static double sq(double v) { return v * v; }

    private static NativeImage buildOverlay(int n) {
        NativeImage img = new NativeImage(n, n, true);
        double half = n / 2.0;
        double R = half - 0.5;
        double off = CompassRenderer.SHADOW_OFFSET * R;
        int main = CompassRenderer.COLOR_ACCENT;
        int shadow = CompassRenderer.COLOR_SHADOW;

        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int mHit = 0, sHit = 0;
                for (int sy = 0; sy < SS; sy++) {
                    for (int sx = 0; sx < SS; sx++) {
                        double dx = x + (sx + 0.5) / SS - half;
                        double dy = y + (sy + 0.5) / SS - half;
                        if (inShape(dx, dy, R)) mHit++;
                        if (inShape(dx - off, dy - off, R)) sHit++;
                    }
                }
                double am = mHit / (double) (SS * SS);
                double as = sHit / (double) (SS * SS) * CompassRenderer.SHADOW_ALPHA;

                double aOut = am + as * (1 - am);
                int rgb;
                if (aOut <= 0.0001) {
                    rgb = main;
                } else {
                    double wm = am / aOut;
                    rgb = lerpColor(shadow, main, wm);
                }
                img.setPixelABGR(x, y, abgr(rgb, aOut));
            }
        }
        return img;
    }
}
