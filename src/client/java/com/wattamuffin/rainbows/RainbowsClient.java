package com.wattamuffin.rainbows;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;

public class RainbowsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RainbowsConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null && !client.isPaused()) {
                RainbowState.tick(client.level);
            } else if (client.level == null) {
                RainbowState.reset();
            }
        });

        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            float alpha = RainbowState.getAlpha(partialTick);
            if (alpha > 0.005f) {
                RainbowRenderer.submit(context, alpha, partialTick);
            }
        });
    }
}