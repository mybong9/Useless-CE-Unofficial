package com.sorrowmist.useless.mixin.mek;

import com.sorrowmist.useless.utils.MekUtils;
import mekanism.api.math.MathUtils;
import mekanism.common.tile.interfaces.IUpgradeTile;
import mekanism.common.util.MekanismUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MekanismUtils.class, remap = false)
public class MekanismUtilsMixin {

    @Inject(method = "getTicks(Lmekanism/common/tile/interfaces/IUpgradeTile;I)I", at = @At("HEAD"), cancellable = true)
    private static void modifyTicks(IUpgradeTile tile, int def, CallbackInfoReturnable<Integer> cir) {
        if (tile.supportsUpgrades()) {
            // Retain the original Forge port's negative-delay encoding for sub-tick work.
            double ticks = def * MekUtils.time(tile);
            cir.setReturnValue(ticks >= 1.0 ? MathUtils.clampToInt(ticks) : -MathUtils.clampToInt(1.0 / ticks));
        }
    }

    @Inject(method = "getEnergyPerTick", at = @At("HEAD"), cancellable = true)
    private static void modifyEnergyPerTick(IUpgradeTile tile, mekanism.api.math.FloatingLong def, CallbackInfoReturnable<mekanism.api.math.FloatingLong> cir) {
        if (tile.supportsUpgrades()) {
            cir.setReturnValue(def.copy().multiply(MekUtils.electricity(tile)));
        }
    }

    @Inject(method = "getMaxEnergy(Lmekanism/common/tile/interfaces/IUpgradeTile;Lmekanism/api/math/FloatingLong;)Lmekanism/api/math/FloatingLong;", at = @At("HEAD"), cancellable = true)
    private static void modifyMaxEnergy(IUpgradeTile tile, mekanism.api.math.FloatingLong def, CallbackInfoReturnable<mekanism.api.math.FloatingLong> cir) {
        if (tile.supportsUpgrades()) {
            cir.setReturnValue(def.copy().multiply(MekUtils.capacity(tile)));
        }
    }
}
