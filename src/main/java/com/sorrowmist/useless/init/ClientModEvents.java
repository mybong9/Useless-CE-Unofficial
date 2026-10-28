package com.sorrowmist.useless.init;

import com.sorrowmist.useless.UselessMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Forge lifecycle events use the mod bus; runtime client events use the Forge bus. */
@Mod.EventBusSubscriber(modid = UselessMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    private ClientModEvents() {}
    @SubscribeEvent public static void screens(FMLClientSetupEvent event) { ClientSetup.registerScreens(event); }
    @SubscribeEvent public static void geometry(ModelEvent.RegisterGeometryLoaders event) { ClientSetup.registerGeometryLoaders(event); }
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) { ClientSetup.modifyBakedModels(event); }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) { ClientSetup.onItemColor(event); }
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) { ClientSetup.onBlockColor(event); }
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) {
        com.sorrowmist.useless.event.client.ClientEventBusSubscriber.registerKeyBindings(event);
    }
}
