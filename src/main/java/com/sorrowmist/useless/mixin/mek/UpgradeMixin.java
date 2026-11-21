package com.sorrowmist.useless.mixin.mek;

import com.sorrowmist.useless.core.config.ConfigManager;
import mekanism.api.Upgrade;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Upgrade.class, remap = false)
public abstract class UpgradeMixin {
    @Inject(method = "getMax", at = @At("HEAD"), cancellable = true)
    private void useless$readServerUpgradeLimit(CallbackInfoReturnable<Integer> callback) {
        Upgrade upgrade = (Upgrade) (Object) this;
        if (upgrade == Upgrade.SPEED || upgrade == Upgrade.ENERGY) {
            callback.setReturnValue(ConfigManager.getMaxUpgrade());
        }
    }

    @Redirect(method = "buildMap", at = @At(value = "FIELD",
            target = "Lmekanism/api/Upgrade;maxStack:I", opcode = Opcodes.GETFIELD))
    private static int useless$readSavedUpgradeLimit(Upgrade upgrade) {
        return upgrade.getMax();
    }
}
