package com.sorrowmist.useless.event.client;

import com.sorrowmist.useless.client.gui.ModeWheelScreen;
import com.sorrowmist.useless.compat.enderio.EnderIOTravelCompat;
import com.sorrowmist.useless.core.common.KeyBindings;
import com.sorrowmist.useless.utils.UselessItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.client.event.InputEvent;

import java.util.AbstractMap;

/**
 * 模式轮盘处理器，负责处理模式轮盘的显示、隐藏和交互
 */
@EventBusSubscriber(Dist.CLIENT)
public class ModeWheelHandler {

    /**
     * 处理键盘按下事件
     */
    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        // 检查是否是G键按下事件 (GLFW_PRESS = 1)
        if (event.getKey() == KeyBindings.SWITCH_MODE_WHEEL_KEY.get().getKey().getValue()
                && event.getAction() == 1) {
            if (minecraft.screen instanceof ModeWheelScreen) {
                minecraft.setScreen(null);
                return;
            }
            if (minecraft.screen != null) return;

            // 使用工具方法查找目标工具
            ItemStack targetItem = UselessItemUtils.findTargetToolInHands(minecraft.player)
                                                   .map(AbstractMap.SimpleImmutableEntry::getKey)
                                                   .orElse(null);

            if (targetItem != null && !(minecraft.screen instanceof ModeWheelScreen)) {
                // 显示模式轮盘屏幕
                Minecraft.getInstance().setScreen(new ModeWheelScreen(targetItem));
            }
        }
    }

}
