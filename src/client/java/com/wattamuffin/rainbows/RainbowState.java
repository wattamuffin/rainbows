package com.wattamuffin.rainbows;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

import java.util.Random;

public class RainbowState {
    private static final int TOTAL_DURATION = 2400; // 2 minutes
    private static final int FADE_TICKS = 160;      // 8 seconds fade
    private static final float FADE_STEP = 1.0f / FADE_TICKS;
    private static final float MOONBOW_ALPHA = 0.08f;
    private static final Random RANDOM = new Random();

    private static boolean wasRaining = false;
    private static int remainingTicks = 0;
    private static float prevTimeAlpha = 0f;
    private static float currentTimeAlpha = 0f;

    private static float prevVisibility = 0f;
    private static float currentVisibility = 0f;

    private static boolean moonbow = false;
    private static int lastSkyDarken = -1;
    private static boolean isMorning = true;

    public static void tick(ClientLevel level) {
        prevTimeAlpha = currentTimeAlpha;
        prevVisibility = currentVisibility;

        if (!level.dimensionType().hasSkyLight()) {
            reset();
            return;
        }

        int skyDarken = level.getSkyDarken();
        if (lastSkyDarken != -1 && skyDarken != lastSkyDarken) {
            isMorning = skyDarken < lastSkyDarken;
        }
        lastSkyDarken = skyDarken;

        RainbowsConfig config = RainbowsConfig.get();
        if (config.spawnCondition == RainbowsConfig.SpawnCondition.NEVER) {
            remainingTicks = 0;
            currentTimeAlpha = 0f;
            currentVisibility = 0f;
            moonbow = false;
            return;
        }

        boolean canShowNow;
        if (config.realisticBehavior) {
            canShowNow = skyDarken >= 1 && skyDarken <= 4;
            moonbow = false;
        } else {
            boolean isNight = skyDarken >= 4;
            moonbow = isNight && config.moonbowEnabled;
            canShowNow = !isNight || moonbow;
        }

        // Fade-in Animation
        if (canShowNow) {
            currentVisibility = Math.min(1.0f, currentVisibility + FADE_STEP);
        } else {
            currentVisibility = Math.max(0.0f, currentVisibility - FADE_STEP);
        }

        if (config.spawnCondition == RainbowsConfig.SpawnCondition.ALWAYS) {
            currentTimeAlpha = 1.0f;
            return;
        }

        // AFTER_RAIN Mode
        boolean isRaining = level.isRaining();

        if (wasRaining && !isRaining) {
            if (RANDOM.nextInt(100) < config.chance) {
                remainingTicks = TOTAL_DURATION;
            }
        }
        wasRaining = isRaining;

        if (isRaining && remainingTicks > FADE_TICKS) {
            remainingTicks = FADE_TICKS;
        }

        if (remainingTicks > 0) {
            remainingTicks--;
        }

        float timeAlpha = 0f;
        if (remainingTicks > 0) {
            int elapsed = TOTAL_DURATION - remainingTicks;
            if (elapsed < FADE_TICKS) {
                timeAlpha = elapsed / (float) FADE_TICKS;
            } else if (remainingTicks < FADE_TICKS) {
                timeAlpha = remainingTicks / (float) FADE_TICKS;
            } else {
                timeAlpha = 1.0f;
            }
        }

        currentTimeAlpha = timeAlpha;
    }

    public static float getAlpha(float partialTick) {
        RainbowsConfig config = RainbowsConfig.get();
        if (config.spawnCondition == RainbowsConfig.SpawnCondition.NEVER) {
            return 0f;
        }

        float targetAlpha = moonbow ? MOONBOW_ALPHA : (config.saturation / 100.0f);
        float visibility = Mth.lerp(partialTick, prevVisibility, currentVisibility);

        if (config.spawnCondition == RainbowsConfig.SpawnCondition.ALWAYS) {
            return visibility * targetAlpha;
        }

        float timeAlpha = Mth.lerp(partialTick, prevTimeAlpha, currentTimeAlpha);
        return timeAlpha * visibility * targetAlpha;
    }

    public static boolean isMoonbow() {
        return moonbow;
    }

    public static float getRainbowYaw() {
        if (!RainbowsConfig.get().realisticBehavior) {
            return -45.0f;
        }
        return isMorning ? 90.0f : -90.0f;
    }

    public static void reset() {
        wasRaining = false;
        remainingTicks = 0;
        prevTimeAlpha = 0f;
        currentTimeAlpha = 0f;
        prevVisibility = 0f;
        currentVisibility = 0f;
        moonbow = false;
        lastSkyDarken = -1;
        isMorning = true;
    }
}