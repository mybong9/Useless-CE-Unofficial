package com.sorrowmist.useless.mixin.neoeco;

import cn.dancingsnow.neoecoae.multiblock.cluster.NECluster;
import com.sorrowmist.useless.compat.neoecoae.compact.cluster.CompactComputationCluster;
import com.sorrowmist.useless.compat.neoecoae.compact.cluster.CompactCraftingCluster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Compact fleets own their network lifecycle and must not join physical frequency partitions. */
@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.multiblock.network.NELogicalNetworkManager", remap = false)
public abstract class CompactLogicalNetworkMixin {
    @Inject(method = {"attach", "refresh", "detach", "detachBeforeDestroy", "clearAssociation"},
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void useless$keepCompactFleet(NECluster<?> cluster, CallbackInfo ci) {
        if (cluster instanceof CompactCraftingCluster || cluster instanceof CompactComputationCluster) {
            ci.cancel();
        }
    }
}
