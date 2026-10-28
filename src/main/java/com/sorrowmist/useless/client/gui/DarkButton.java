package com.sorrowmist.useless.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Dark glass button matching the multiblock machine interfaces. */
class DarkButton extends PressableAE2Button {
    DarkButton(int x, int y, int width, int height, Component text, OnPress onPress) {
        super(x, y, width, height, text, onPress);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused();
        StaffGlass.button(graphics, getX(), getY(), getWidth(), getHeight(), active, StaffGlass.aeStyle() ? isVisuallyPressed() : hovered, false);
        var font = Minecraft.getInstance().font;
        var label = getMessage();
        if (font.width(label) > getWidth() - 10) label = Component.literal(
                font.plainSubstrByWidth(label.getString(), Math.max(1, getWidth() - 16)) + "…").withStyle(label.getStyle());
        if (StaffGlass.aeStyle()) {
            OriginalAeStaffStyle.buttonText(graphics, font, label, getX(), getY(), getWidth(), getHeight(), active, isVisuallyPressed());
            return;
        }
        graphics.drawString(font, label, getX() + (getWidth() - font.width(label)) / 2,
                getY() + (getHeight() - font.lineHeight + 1) / 2, active ? StaffGlass.textColor() : StaffGlass.mutedColor(), false);
    }
    /** Uses the native GUI batch so buttons do not change the active shader. */
    static void rounded(GuiGraphics graphics, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        graphics.fill(x, y + r, x + w, y + h - r, color);
        for (int row = 0; row < r; row++) {
            double dy = r - row - 0.5;
            int inset = (int) Math.ceil(r - Math.sqrt(r * r - dy * dy));
            graphics.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
            graphics.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, color);
        }
    }

}