package com.sorrowmist.useless.client.gui;

import com.sorrowmist.uselesscore.client.ui.UiDraw;
import com.sorrowmist.uselesscore.client.ui.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Uses the exact glass renderer and palette of the wireless logistics interface. */
public final class StaffGlass {
    private StaffGlass() {}
    public static boolean aeStyle() { StaffUiPreferences.load(); return StaffUiPreferences.aeStyle; }
    public static void toggleStyle() {
        StaffUiPreferences.load(); StaffUiPreferences.aeStyle = !StaffUiPreferences.aeStyle; StaffUiPreferences.save();
    }
    public static Component styleLabel() {
        return Component.translatable(aeStyle() ? "gui.useless_mod.staff_ui.glass" : "gui.useless_mod.staff_ui.ae");
    }
    public static int textColor() { return aeStyle() ? OriginalAeStaffStyle.TEXT : UiTheme.DARK.text(); }
    public static int mutedColor() { return aeStyle() ? OriginalAeStaffStyle.MUTED : UiTheme.DARK.muted(); }
    public static void rounded(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        UiDraw.rounded(g, x, y, w, h, aeStyle() ? 0 : radius, color);
    }
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        if (aeStyle()) {
            OriginalAeStaffStyle.panel(g, x, y, w, h);
        } else UiDraw.glass(g, x, y, w, h, 12, UiTheme.DARK);
    }
    public static void card(GuiGraphics g, int x, int y, int w, int h) {
        if (aeStyle()) {
            OriginalAeStaffStyle.card(g, x, y, w, h);
        } else UiDraw.panel(g, x, y, w, h, 8, UiTheme.DARK.inset(), UiTheme.DARK.border());
    }
    public static void button(GuiGraphics g, int x, int y, int w, int h, boolean active, boolean hovered, boolean selected) {
        if (aeStyle()) {
            OriginalAeStaffStyle.button(g, x, y, w, h, active, hovered || selected);
            return;
        }
        var theme = UiTheme.DARK;
        float over = hovered && active ? 1 : 0, chosen = selected ? 1 : 0;
        int fill = UiDraw.mix(theme.surface(), theme.selected(), over * .3f + chosen * .7f);
        int edge = UiDraw.mix(theme.border(), theme.accent(), over * .45f + chosen * .55f);
        int bottom = UiDraw.mix(fill, 0xff000000, .06f);
        if (!active) { fill = UiDraw.alpha(fill, .45f); bottom = UiDraw.alpha(bottom, .45f); edge = UiDraw.alpha(edge, .5f); }
        UiDraw.gradient(g, x, y, w, h, Math.min(10, h * .5f), fill, bottom, edge, .65f,
                active ? 0x16000000 : 0, 1.6f);
    }
}
