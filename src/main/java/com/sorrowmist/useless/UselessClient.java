package com.sorrowmist.useless;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Registers the Forge configuration screen without declaring a second mod container. */
@Mod.EventBusSubscriber(modid = UselessMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class UselessClient {
    private UselessClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) ->
                        new com.sorrowmist.useless.client.gui.UselessConfigScreen(parent)));
    }
}
