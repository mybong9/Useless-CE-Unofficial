package com.sorrowmist.useless.client.gui;

import com.sorrowmist.uselesscore.client.ui.UiDraw;
import com.sorrowmist.uselesscore.client.ui.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;

/** Dark logistics skin, isolated from other machine screens. */
final class LogisticsStyle {
    static final int TEXT_COLOR=UiTheme.DARK.text(), MUTED_TEXT_COLOR=UiTheme.DARK.muted(),
            SUBTLE_TEXT_COLOR=UiTheme.DARK.muted(), ERROR_TEXT_COLOR=UiTheme.DARK.danger(),
            SLOT_COLOR=UiTheme.DARK.inset(), SLOT_SHADOW_COLOR=UiTheme.DARK.border(),
            HIGHLIGHT_COLOR=UiTheme.DARK.selected();
    static void drawPanel(GuiGraphics g,int x,int y,int w,int h){
        UiDraw.glass(g,x,y,w,h,12,UiTheme.DARK);
    }
    static void drawSlotBackground(GuiGraphics g,int x,int y,Slot slot){UiDraw.slot(g,x+slot.x-1,y+slot.y-1,18,UiTheme.DARK,false);}
}
