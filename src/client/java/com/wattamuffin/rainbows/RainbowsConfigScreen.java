package com.wattamuffin.rainbows;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class RainbowsConfigScreen extends Screen {
    private final Screen parent;
    private final RainbowsConfig config;
    private AbstractSliderButton chanceSlider;
    private AbstractSliderButton moonbowSaturationSlider;

    public RainbowsConfigScreen(Screen parent) {
        super(Component.literal("Rainbows Settings"));
        this.parent = parent;
        this.config = RainbowsConfig.get();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 102;

        // 1. Spawn Condition (Never / After Rain / Always)
        this.addRenderableWidget(
            CycleButton.<RainbowsConfig.SpawnCondition>builder(
                    mode -> Component.literal(mode.getDisplayName()),
                    () -> config.spawnCondition
                )
                .withValues(RainbowsConfig.SpawnCondition.values())
                .create(
                    centerX - 110, startY, 220, 20,
                    Component.literal("Spawn Condition"),
                    (button, value) -> {
                        config.spawnCondition = value;
                        if (this.chanceSlider != null) {
                            this.chanceSlider.active = (value == RainbowsConfig.SpawnCondition.AFTER_RAIN);
                        }
                    }
                )
        );

        // 2. Chance (0–100%, active only in After Rain mode)
        this.chanceSlider = new AbstractSliderButton(
            centerX - 110, startY + 24, 220, 20,
            Component.literal("Chance: " + config.chance + "%"),
            config.chance / 100.0
        ) {
            @Override
            protected void updateMessage() {
                int val = (int) Math.round(this.value * 100.0);
                this.setMessage(Component.literal("Chance: " + val + "%"));
            }

            @Override
            protected void applyValue() {
                config.chance = (int) Math.round(this.value * 100.0);
            }
        };
        this.chanceSlider.active = (config.spawnCondition == RainbowsConfig.SpawnCondition.AFTER_RAIN);
        this.addRenderableWidget(this.chanceSlider);

        // 3. Saturation (0–100%)
        this.addRenderableWidget(
            new AbstractSliderButton(
                centerX - 110, startY + 48, 220, 20,
                Component.literal("Saturation: " + config.saturation + "%"),
                config.saturation / 100.0
            ) {
                @Override
                protected void updateMessage() {
                    int val = (int) Math.round(this.value * 100.0);
                    this.setMessage(Component.literal("Saturation: " + val + "%"));
                }

                @Override
                protected void applyValue() {
                    config.saturation = (int) Math.round(this.value * 100.0);
                }
            }
        );

        // 4. Detail Level (16x / 32x / 64x)
        this.addRenderableWidget(
            CycleButton.<RainbowsConfig.DetailLevel>builder(
                    level -> Component.literal(level.getDisplayName()),
                    () -> config.detailLevel
                )
                .withValues(RainbowsConfig.DetailLevel.values())
                .create(
                    centerX - 110, startY + 72, 220, 20,
                    Component.literal("Detail Level"),
                    (button, value) -> config.detailLevel = value
                )
        );

        // 5. Moonbow (ON / OFF)
        this.addRenderableWidget(
            CycleButton.onOffBuilder(config.moonbowEnabled)
                .create(
                    centerX - 110, startY + 96, 220, 20,
                    Component.literal("Moonbow"),
                    (button, value) -> {
                        config.moonbowEnabled = value;
                        if (this.moonbowSaturationSlider != null) {
                            this.moonbowSaturationSlider.active = value;
                        }
                    }
                )
        );

        // 6. Moonbow Saturation (0–100% of daytime saturation)
        this.moonbowSaturationSlider = new AbstractSliderButton(
            centerX - 110, startY + 120, 220, 20,
            Component.literal("Moonbow Saturation: " + config.moonbowSaturation + "%"),
            config.moonbowSaturation / 100.0
        ) {
            @Override
            protected void updateMessage() {
                int val = (int) Math.round(this.value * 100.0);
                this.setMessage(Component.literal("Moonbow Saturation: " + val + "%"));
            }

            @Override
            protected void applyValue() {
                config.moonbowSaturation = (int) Math.round(this.value * 100.0);
            }
        };
        this.moonbowSaturationSlider.active = config.moonbowEnabled;
        this.addRenderableWidget(this.moonbowSaturationSlider);

        // 7. Realistic Behavior (ON / OFF)
        this.addRenderableWidget(
            CycleButton.onOffBuilder(config.realisticBehavior)
                .create(
                    centerX - 110, startY + 144, 220, 20,
                    Component.literal("Realistic Behavior"),
                    (button, value) -> config.realisticBehavior = value
                )
        );

        // 8. Done
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(centerX - 100, startY + 176, 200, 20)
                .build()
        );
    }

    @Override
    public void onClose() {
        RainbowsConfig.save();
        super.onClose();
    }
}