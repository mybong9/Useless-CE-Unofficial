package com.sorrowmist.useless.compat.ae;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.world.entity.player.Player;
import appeng.blockentity.AEBaseBlockEntity;
import net.minecraftforge.fml.ModList;

public final class OptionalRenamer {
    private OptionalRenamer() {}
    public static void open(Player player, AEBaseBlockEntity blockEntity) {
        if (ModList.get().isLoaded("expatternprovider")) Extended.open(player, blockEntity);
    }
    private static final class Extended {
        static void open(Player player, AEBaseBlockEntity blockEntity) {
            MenuOpener.open(com.glodblock.github.extendedae.container.ContainerRenamer.TYPE,
                    player, MenuLocators.forBlockEntity(blockEntity));
        }
    }
}
