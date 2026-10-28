package com.sorrowmist.useless.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** 1.21 MachineScreenStyle / AE2Button artwork backported to the 1.20.1 blit API. */
public final class OriginalAeStaffStyle {
    public static final int PANEL = 0xFFCBCCD4, HIGHLIGHT = 0xFFF2F2F2;
    public static final int SLOT = 0xFFADB0C4, SLOT_SHADOW = 0xFF9A9FB4;
    public static final int TEXT = 0xFF413F54, MUTED = 0xFF878FA5, SUBTLE = 0xFF6D7287;
    private static final ResourceLocation BACKGROUND = texture("background");
    private static final ResourceLocation NORMAL = texture("button"), PRESSED = texture("button_highlighted"), DISABLED = texture("button_disabled");
    private static ResourceLocation texture(String name) {
        return new ResourceLocation("useless_mod", "textures/gui/staff_ae/" + name + ".png");
    }
    private OriginalAeStaffStyle() {}
    public static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        if (w < 8 || h < 8) return;
        // Same 4px corners and 248px repeatable body as 1.21 BackgroundGenerator.
        graphics.blitNineSliced(BACKGROUND, x, y, w, h, 4, 256, 256, 0, 0);
    }
    public static void card(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, HIGHLIGHT);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
    }
    public static void button(GuiGraphics graphics, int x, int y, int w, int h, boolean active, boolean down) {
        if (w <= 0 || h <= 0) return;
        var texture = !active ? DISABLED : down ? PRESSED : NORMAL;
        int bx = Math.min(3, w / 2), by = Math.min(3, h / 2);
        int[] dx = {x, x + bx, x + w - bx}, dy = {y, y + by, y + h - by};
        int[] dw = {bx, w - bx * 2, bx}, dh = {by, h - by * 2, by};
        int[] sx = {0, bx, 200 - bx}, sy = {0, by, 20 - by};
        int[] sw = {bx, 200 - bx * 2, bx}, sh = {by, 20 - by * 2, by};
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            if (dw[col] > 0 && dh[row] > 0)
                graphics.blit(texture, dx[col], dy[row], dw[col], dh[row],
                        (float) sx[col], (float) sy[row], sw[col], sh[row], 200, 20);
        }
    }
    public static void buttonText(GuiGraphics graphics, Font font, Component label,
                                  int x, int y, int w, int h, boolean active, boolean selected) {
        int centerY = y + (h - font.lineHeight + 1) / 2;
        int textX = x + (w - font.width(label)) / 2;
        int color = !active ? TEXT : selected ? 0xFF517497 : HIGHLIGHT;
        // Match original AE2Button's explicit one-pixel baseline relief.
        graphics.drawString(font, label, textX, centerY - (!active ? -1 : selected ? 0 : 1), color, false);
    }
}
