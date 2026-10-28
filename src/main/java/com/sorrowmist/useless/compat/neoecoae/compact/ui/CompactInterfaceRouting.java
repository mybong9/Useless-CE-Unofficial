package com.sorrowmist.useless.compat.neoecoae.compact.ui;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIContainer;
import cn.dancingsnow.neoecoae.platform.ui.runtime.gui.factory.BlockUIMenuType;
/** Resolve only an open, nearby compact interface menu; main machine panels retain their own RPC target. */
public final class CompactInterfaceRouting {
    private CompactInterfaceRouting() {}
    public static BlockEntity resolve(BlockEntity original, Player player) {
        if (original instanceof CompactUiHost compact && player != null && !original.isRemoved()
                && player.level() == original.getLevel()
                && player.distanceToSqr(original.getBlockPos().getCenter()) <= 64
                && player.containerMenu instanceof ModularUIContainer menu
                && menu.getModularUI().holder instanceof BlockUIMenuType.BlockUIHolder holder
                && holder.pos.equals(original.getBlockPos())) return compact.compactInterfaceTarget();
        return original;
    }
}
