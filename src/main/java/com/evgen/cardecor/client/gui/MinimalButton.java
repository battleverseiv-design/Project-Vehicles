package com.evgen.cardecor.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Minimalist Project Zomboid dark tactical button.
 * - Flat semi-transparent dark slate background (no vanilla stone bevel).
 * - Subtle 1px border with smooth gold/white hover highlight.
 * - Crisp typography without muddy shadows.
 * - Muted button click sound (no loud vanilla wooden click).
 */
public class MinimalButton extends AbstractButton {

    private static final int BG_IDLE     = 0xD0101622;
    private static final int BG_HOVER    = 0xEA1C2738;
    private static final int BG_DISABLED = 0x800B0E14;

    private static final int BORDER_IDLE     = 0x802D3C4E;
    private static final int BORDER_HOVER    = 0xFFE5A93C; // Gold accent
    private static final int BORDER_DISABLED = 0x401D2633;

    private static final int TEXT_IDLE     = 0xFFE2E8F0;
    private static final int TEXT_HOVER    = 0xFFFFFFFF;
    private static final int TEXT_DISABLED = 0xFF5A6978;

    private final Consumer<MinimalButton> action;

    public MinimalButton(int x, int y, int width, int height, Component message, Consumer<MinimalButton> action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    @Override
    public void onPress() {
        if (this.action != null) {
            this.action.accept(this);
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        // Muted per user request: no standard button click sound
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        boolean hovered = this.isHoveredOrFocused();

        int bg = !this.active ? BG_DISABLED : (hovered ? BG_HOVER : BG_IDLE);
        int border = !this.active ? BORDER_DISABLED : (hovered ? BORDER_HOVER : BORDER_IDLE);
        int text = !this.active ? TEXT_DISABLED : (hovered ? TEXT_HOVER : TEXT_IDLE);

        // Sleek flat background
        g.fill(x, y, x + w, y + h, bg);

        // 1px crisp outline
        g.renderOutline(x, y, w, h, border);

        // Crisp centered text
        Font font = Minecraft.getInstance().font;
        g.drawCenteredString(font, getMessage(), x + w / 2, y + (h - 8) / 2, text);
    }

    public static Builder builder(Component message, Consumer<MinimalButton> action) {
        return new Builder(message, action);
    }

    public static class Builder {
        private final Component message;
        private final Consumer<MinimalButton> action;
        private Tooltip tooltip;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;

        public Builder(Component message, Consumer<MinimalButton> action) {
            this.message = message;
            this.action = action;
        }

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder tooltip(Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public MinimalButton build() {
            MinimalButton btn = new MinimalButton(this.x, this.y, this.width, this.height, this.message, this.action);
            btn.setTooltip(this.tooltip);
            return btn;
        }
    }
}
