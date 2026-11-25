package com.wattamuffin.rainbows;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RainbowsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("rainbows.json");

    public enum SpawnCondition {
        NEVER("Never"),
        AFTER_RAIN("After Rain"),
        ALWAYS("Always");

        private final String displayName;

        SpawnCondition(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum DetailLevel {
        X16("16x", 1, false),
        X32("32x", 1, true),
        X64("64x", 2, true);

        private final String displayName;
        private final int scale;
        private final boolean smooth;

        DetailLevel(String displayName, int scale, boolean smooth) {
            this.displayName = displayName;
            this.scale = scale;
            this.smooth = smooth;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getScale() {
            return scale;
        }

        public boolean isSmooth() {
            return smooth;
        }
    }

    public SpawnCondition spawnCondition = SpawnCondition.AFTER_RAIN;
    public int chance = 40; // 0 to 100
    public int saturation = 45; // 0 to 100
    public DetailLevel detailLevel = DetailLevel.X32;
    public boolean moonbowEnabled = true;
    public int moonbowSaturation = 100; // 0 to 100 (% of daytime saturation)
    public boolean realisticBehavior = false;

    private static RainbowsConfig instance;

    public static RainbowsConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                instance = GSON.fromJson(Files.readString(CONFIG_PATH), RainbowsConfig.class);
            } catch (Exception e) {
                instance = new RainbowsConfig();
            }
        }
        if (instance == null) {
            instance = new RainbowsConfig();
        }
        if (instance.spawnCondition == null) {
            instance.spawnCondition = SpawnCondition.AFTER_RAIN;
        }
        if (instance.detailLevel == null) {
            instance.detailLevel = DetailLevel.X16;
        }
        instance.chance = Math.max(0, Math.min(100, instance.chance));
        instance.saturation = Math.max(0, Math.min(100, instance.saturation));
        instance.moonbowSaturation = Math.max(0, Math.min(100, instance.moonbowSaturation));
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(get()));
        } catch (IOException ignored) {
        }
    }
}