package com.sorrowmist.useless.compat.neoecoae.compact.ui;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Forge 1.20.1 LDLib overview for a single-block compact machine. */
public final class CompactSingleBlockUI {
    private CompactSingleBlockUI() {}

    public static ModularUI create(IUIHolder holder, Player player, Component title, Component... lines) {
        int inventoryY = 36 + lines.length * 15;
        ModularUI ui = new ModularUI(320, inventoryY + 86, holder, player)
                .background(new ColorRectTexture(0xFFE5E3EF));
        ui.widget(new LabelWidget(10, 9, title).setTextColor(0xFF3F3D52).setDropShadow(false));
        for (int i = 0; i < lines.length; i++) {
            ui.widget(new LabelWidget(10, 27 + i * 15, lines[i])
                    .setTextColor(0xFF555266).setDropShadow(false));
        }
        for (int slot = 0; slot < 36; slot++) {
            int row = slot < 9 ? 3 : (slot / 9 - 1);
            ui.widget(new SlotWidget(player.getInventory(), slot, 79 + slot % 9 * 18,
                    inventoryY + row * 18 + (slot < 9 ? 4 : 0))
                    .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                    .setLocationInfo(true, slot < 9));
        }
        return ui;
    }
}