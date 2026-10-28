package com.sorrowmist.useless.mixin.ae2lt;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Avoids a long division for each positive saturated task-count multiplication. */
@Pseudo
@Mixin(targets = "com.moakiee.ae2lt.crafting.timewheel.Ae2LtTimeWheelCraftingCpuLogic", remap = false)
public abstract class TimeWheelCpuArithmeticMixin {
    @Redirect(method = "*", at = @At(value = "INVOKE", target =
            "Lcom/moakiee/ae2lt/crafting/timewheel/Ae2LtTimeWheelCraftingCpuLogic;multiplySaturated(JJ)J",
            remap = false), require = 0, remap = false)
    private static long useless$multiplySaturated(long left, long right) {
        if (left <= 0L || right <= 0L) return 0L;
        long product = left * right;
        if ((left | right) >>> 31 == 0L) return product;
        return product >= 0L && Math.multiplyHigh(left, right) == 0L ? product : Long.MAX_VALUE;
    }
}
