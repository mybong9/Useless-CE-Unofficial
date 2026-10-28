package com.sorrowmist.useless.mixin.ae2;

import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.encoding.PatternEncodingLogic;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.PendingOmniversalPatternHolder;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.PatternEncodingTermMenuAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses the logic owned by the open menu, including terminals with a wrapped target. */
@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingTermMenuMixin implements PatternEncodingTermMenuAccess {
    @Shadow @Final private PatternEncodingLogic encodingLogic;

    @Override
    public PatternEncodingLogic uselessMod$getEncodingLogic() {
        return encodingLogic;
    }

    @Inject(method = "encode()V", at = @At("RETURN"))
    private void uselessMod$finishOmniversalEncoding(CallbackInfo ci) {
        // Some slot implementations suppress inventory callbacks while writing. Recheck
        // once the complete encode operation has finished; the pending pick is consumed
        // by the existing converter, so an earlier successful callback cannot duplicate it.
        if (!encodingLogic.isClientSide()
                && encodingLogic instanceof PendingOmniversalPatternHolder holder) {
            holder.uselessMod$tryConvertPendingOmniversalPattern();
        }
    }
}
