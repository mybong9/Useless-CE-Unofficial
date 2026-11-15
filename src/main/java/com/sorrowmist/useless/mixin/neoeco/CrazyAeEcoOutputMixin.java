package com.sorrowmist.useless.mixin.neoeco;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.me.service.CraftingService;
import cn.dancingsnow.neoecoae.platform.mixin.CraftingServiceEarlyBridge;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = CraftingService.class, remap = false)
abstract class CrazyAeEcoOutputMixin {
    @WrapMethod(method = "insertIntoCpus")
    private long useless$routeRemainingOutput(AEKey key, long amount, Actionable mode, Operation<Long> original) {
        // CrazyAE2 returns before ECO's native insertion hook.
        if (amount <= 0 || !((Object) this instanceof CraftingServiceEarlyBridge eco)) {
            return original.call(key, amount, mode);
        }
        long inserted = eco.neoecoae$earlyInsertIntoCpus(0, key, amount, mode);
        return inserted == amount ? inserted : inserted + original.call(key, amount - inserted, mode);
    }
}
