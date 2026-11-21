package com.sorrowmist.useless.compat.exdeorum;

import com.sorrowmist.useless.UselessMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UselessMod.MODID, value = Dist.CLIENT)
public final class MechanicalSieveScreenPages {
    private MechanicalSieveScreenPages() {}

    @SubscribeEvent
    public static void addButtons(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || !(screen.getMenu() instanceof MechanicalSievePages pages)) return;
        int center = screen.getGuiLeft() + 123;
        int y = Math.max(0, screen.getGuiTop() - 18);
        event.addListener(Button.builder(Component.literal("<"), button ->
                requestPage(screen, pages, MechanicalSievePages.PREVIOUS, -1))
                .bounds(center - 47, y, 20, 16).build());
        event.addListener(Button.builder(Component.literal(">"), button ->
                requestPage(screen, pages, MechanicalSievePages.NEXT, 1))
                .bounds(center + 27, y, 20, 16).build());
    }

    @SubscribeEvent
    public static void drawPage(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || !(screen.getMenu() instanceof MechanicalSievePages pages)) return;
        event.getGuiGraphics().drawCenteredString(Minecraft.getInstance().font,
                (pages.useless$page() + 1) + " / " + pages.useless$pageCount(),
                screen.getGuiLeft() + 123, Math.max(0, screen.getGuiTop() - 18) + 4, 0xFFFFFF);
    }

    private static void requestPage(AbstractContainerScreen<?> screen, MechanicalSievePages pages, int button, int delta) {
        int target = pages.useless$page() + delta;
        var gameMode = Minecraft.getInstance().gameMode;
        if (gameMode != null && target >= 0 && target < pages.useless$pageCount()) {
            gameMode.handleInventoryButtonClick(screen.getMenu().containerId, button);
        }
    }
}
