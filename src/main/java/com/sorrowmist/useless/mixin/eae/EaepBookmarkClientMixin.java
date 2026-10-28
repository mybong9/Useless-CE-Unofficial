package com.sorrowmist.useless.mixin.eae;

import com.sorrowmist.useless.compat.extendedae.EaepBookmarkClientBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.extendedae_plus.client.event.CtrlQPatternKeyHandler", remap = false)
public abstract class EaepBookmarkClientMixin {
    @Inject(method = "handleRecipeBookmark(Ljava/lang/Object;ZZ)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void uselessMod$sendFullSelection(Object bookmark, boolean substitutes,
            boolean fluidSubstitutes, CallbackInfo ci) {
        if (EaepBookmarkClientBridge.handle(bookmark, substitutes, fluidSubstitutes)) ci.cancel();
    }
}
