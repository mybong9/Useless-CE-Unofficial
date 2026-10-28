package com.sorrowmist.useless.mixin.botanypots;

import net.darkhax.botanypots.BotanyPotsForgeClient;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BotanyPotsForgeClient.class)
public class BotanyPotsForgeClientMixin {

    private static final boolean DISABLE_POT_RENDERER = true;

    @Inject(
            method = "registerEntityRenderers",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void disablePotRendererRegistration(EntityRenderersEvent.RegisterRenderers event, CallbackInfo ci) {
        if (DISABLE_POT_RENDERER) {
            // 取消注册花盆渲染器
            ci.cancel();
        }
    }
}
