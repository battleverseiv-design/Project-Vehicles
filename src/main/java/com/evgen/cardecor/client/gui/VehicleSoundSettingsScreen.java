package com.evgen.cardecor.client.gui;

import com.evgen.cardecor.client.VehicleSoundConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class VehicleSoundSettingsScreen extends Screen {

    private final Screen parentScreen;

    public VehicleSoundSettingsScreen(Screen parentScreen) {
        super(Component.literal("Звуки транспорта"));
        this.parentScreen = parentScreen;
        VehicleSoundConfig.load();
    }

    public VehicleSoundSettingsScreen() {
        this(null);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int colWidth = 150;
        int gap = 10;
        int leftColX = centerX - colWidth - (gap / 2);
        int rightColX = centerX + (gap / 2);
        int startY = 48;
        int rowH = 24;

        // COLUMN 1 (Left)
        addRenderableWidget(new SoundSlider(leftColX, startY + rowH * 0, colWidth, 20, "Общая громкость", VehicleSoundConfig.masterVolume, 2.0F, v -> {
            VehicleSoundConfig.masterVolume = v;
        }));

        addRenderableWidget(new SoundSlider(leftColX, startY + rowH * 1, colWidth, 20, "Звук двигателя", VehicleSoundConfig.engineVolume, 2.0F, v -> {
            VehicleSoundConfig.engineVolume = v;
        }));

        addRenderableWidget(new SoundSlider(leftColX, startY + rowH * 2, colWidth, 20, "Звук в салоне", VehicleSoundConfig.insideVolume, 1.5F, v -> {
            VehicleSoundConfig.insideVolume = v;
        }));

        addRenderableWidget(new SoundSlider(leftColX, startY + rowH * 3, colWidth, 20, "Стартер зажигания", VehicleSoundConfig.starterVolume, 2.0F, v -> {
            VehicleSoundConfig.starterVolume = v;
        }));

        addRenderableWidget(new SoundSlider(leftColX, startY + rowH * 4, colWidth, 20, "Переключение передач", VehicleSoundConfig.gearShiftVolume, 2.0F, v -> {
            VehicleSoundConfig.gearShiftVolume = v;
        }));

        // COLUMN 2 (Right)
        addRenderableWidget(new SoundSlider(rightColX, startY + rowH * 0, colWidth, 20, "Качение шин", VehicleSoundConfig.tireRollVolume, 2.0F, v -> {
            VehicleSoundConfig.tireRollVolume = v;
        }));

        addRenderableWidget(new SoundSlider(rightColX, startY + rowH * 1, colWidth, 20, "Тормоза и занос", VehicleSoundConfig.brakeVolume, 2.0F, v -> {
            VehicleSoundConfig.brakeVolume = v;
        }));

        addRenderableWidget(new SoundSlider(rightColX, startY + rowH * 2, colWidth, 20, "Клаксон / Гудок", VehicleSoundConfig.hornVolume, 2.0F, v -> {
            VehicleSoundConfig.hornVolume = v;
        }));

        addRenderableWidget(new SoundSlider(rightColX, startY + rowH * 3, colWidth, 20, "Взрывы шин и удары", VehicleSoundConfig.impactVolume, 2.0F, v -> {
            VehicleSoundConfig.impactVolume = v;
        }));

        addRenderableWidget(new SoundSlider(rightColX, startY + rowH * 4, colWidth, 20, "Эффект Доплера", VehicleSoundConfig.dopplerIntensity, 1.5F, v -> {
            VehicleSoundConfig.dopplerIntensity = v;
        }));

        // Bottom Action Buttons (Standard Vanilla Layout)
        int btnY = this.height - 32;

        addRenderableWidget(MinimalButton.builder(Component.literal("По умолчанию"), btn -> {
            VehicleSoundConfig.resetDefaults();
            this.rebuildWidgets();
        }).bounds(leftColX, btnY, colWidth, 20).build());

        addRenderableWidget(MinimalButton.builder(CommonComponents.GUI_DONE, btn -> {
            VehicleSoundConfig.save();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parentScreen);
            }
        }).bounds(rightColX, btnY, colWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderDirtBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        VehicleSoundConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    private static class SoundSlider extends AbstractSliderButton {
        private final String label;
        private final float maxVal;
        private final Consumer<Float> setter;

        public SoundSlider(int x, int y, int width, int height, String label, float currentVal, float maxVal, Consumer<Float> setter) {
            super(x, y, width, height, Component.literal(label + ": " + (int)(currentVal * 100) + "%"), (double) (currentVal / maxVal));
            this.label = label;
            this.maxVal = maxVal;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float actualVal = (float) (this.value * maxVal);
            if (actualVal <= 0.005F) {
                this.setMessage(Component.literal(this.label + ": ВЫКЛ"));
            } else {
                this.setMessage(Component.literal(this.label + ": " + (int)(actualVal * 100) + "%"));
            }
        }

        @Override
        protected void applyValue() {
            float actualVal = (float) (this.value * maxVal);
            this.setter.accept(actualVal);
            VehicleSoundConfig.save();
        }
    }
}
