package com.sorrowmist.useless.compat.neoecoae.compact.block;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Allow the compact menu even when vanilla would bypass block use while sneaking. */
@Mod.EventBusSubscriber(modid = "useless_mod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CompactF9Interaction {
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("neoecoae")) return;
        if (event.getEntity().isShiftKeyDown()
                && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof CompactF9Block
                && CompactF9Block.isBackFace(event.getLevel().getBlockState(event.getPos()), event.getFace())) {
            event.setUseBlock(Event.Result.ALLOW);
            event.setUseItem(Event.Result.DENY);
        }
    }
}
