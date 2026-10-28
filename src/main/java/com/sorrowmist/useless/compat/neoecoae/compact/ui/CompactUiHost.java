package com.sorrowmist.useless.compat.neoecoae.compact.ui;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.world.entity.player.Player;

/** The explicit menu mode is transmitted by the compact machine's LDLib factory. */
public interface CompactUiHost extends IUIHolder {
    default boolean isRemote() { return ((net.minecraft.world.level.block.entity.BlockEntity)this).getLevel().isClientSide; }
    default boolean isInvalid() { return ((net.minecraft.world.level.block.entity.BlockEntity)this).isRemoved(); }
    default void markAsDirty() { ((net.minecraft.world.level.block.entity.BlockEntity)this).setChanged(); }
    net.minecraft.world.level.block.entity.BlockEntity compactInterfaceTarget();
    ModularUI createUI(Player player);
    ModularUI createCompactUI(Player player, boolean interfaceMode);
}
