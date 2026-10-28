package com.sorrowmist.useless.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

class PressableAE2Button extends Button {
    private boolean pressed;

    PressableAE2Button(int x, int y, int width, int height,
                       Component message, Button.OnPress onPress) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) {
            pressed = true;
        }
        return handled;
    }

    /**
     * 触发点击并复位按下状态。
     *
     * <p>按下态若只依赖屏幕的 {@code mouseReleased} 复位，在鼠标移出界面后松开等路径下不会触发，
     * 按钮将停留在按下态。此处以点击回调作为复位兜底，保证任何一次成功点击后都会弹起。</p>
     */
    @Override
    public void onPress() {
        releaseVisualState();
        super.onPress();
    }

    protected boolean isVisuallyPressed() { return pressed; }

    void releaseVisualState() {
        pressed = false;
    }

}
