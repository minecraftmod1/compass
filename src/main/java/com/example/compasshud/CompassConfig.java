package com.example.compasshud;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Holds the compass settings and saves them to config/compasshud.properties. */
public final class CompassConfig {
    private CompassConfig() {}

    public static final int DEFAULT_X = 6;
    public static final int DEFAULT_Y = 6;
    public static final int DEFAULT_SIZE = 65;

    /** Smallest and largest compass diameter the size slider allows (GUI pixels). */
    public static final int MIN_SIZE = 32;
    public static final int MAX_SIZE = 128;

    /** Compass is ON by default. */
    public static boolean enabled = true;

    /** Position of the compass's top-left corner, in GUI pixels from the top-left of the screen. */
    public static int x = DEFAULT_X;
    public static int y = DEFAULT_Y;

    /** Diameter of the compass in GUI pixels. */
    public static int size = DEFAULT_SIZE;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("compasshud.properties");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) {
            return;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(f)) {
            p.load(in);
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true").trim());
            x = Math.max(0, Integer.parseInt(p.getProperty("x", String.valueOf(DEFAULT_X)).trim()));
            y = Math.max(0, Integer.parseInt(p.getProperty("y", String.valueOf(DEFAULT_Y)).trim()));
            size = Math.max(MIN_SIZE, Math.min(MAX_SIZE,
                    Integer.parseInt(p.getProperty("size", String.valueOf(DEFAULT_SIZE)).trim())));
        } catch (IOException | NumberFormatException e) {
            // Bad or unreadable file: keep whatever values we have.
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("enabled", Boolean.toString(enabled));
        p.setProperty("x", Integer.toString(x));
        p.setProperty("y", Integer.toString(y));
        p.setProperty("size", Integer.toString(size));
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "Compass HUD settings (x, y and size are GUI pixels)");
        } catch (IOException e) {
            // Could not save: ignore, the settings just won't persist.
        }
    }
}
