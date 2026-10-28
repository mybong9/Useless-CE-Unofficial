package com.sorrowmist.useless.client;

import com.sorrowmist.useless.blocks.GlowPlasticBlock;
import com.sorrowmist.uselesscore.api.color.EnumColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT, modid = "useless_mod")
public class ClientSetup {

    @SubscribeEvent
    public static void onItemColor(RegisterColorHandlersEvent.Item event) {
        // 注册所有颜色的物品染色器
        for (EnumColor color : EnumColor.valuesInOrder()) {
            var block = GlowPlasticBlock.GLOW_PLASTIC_BLOCKS.get(color).get();
            event.register((stack, tintIndex) -> {
                // 只对 tintIndex 0 应用颜色染色
                return tintIndex == 0 ? color.getRgb() : 0xFFFFFFFF;
            }, block.asItem());
        }
    }

    @SubscribeEvent
    public static void onBlockColor(RegisterColorHandlersEvent.Block event) {
        // 注册所有颜色的方块染色器
        for (EnumColor color : EnumColor.valuesInOrder()) {
            var block = GlowPlasticBlock.GLOW_PLASTIC_BLOCKS.get(color).get();
            event.register((state, world, pos, tintIndex) -> {
                // 只对 tintIndex 0 应用颜色染色
                return tintIndex == 0 ? color.getRgb() : 0xFFFFFFFF;
            }, block);
        }
    }
}
