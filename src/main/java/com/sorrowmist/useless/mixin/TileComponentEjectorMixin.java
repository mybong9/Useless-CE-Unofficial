package com.sorrowmist.useless.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = mekanism.common.tile.component.TileComponentEjector.class, remap = false)


public class TileComponentEjectorMixin {
    @Shadow(remap = false)
    private int tickDelay;



    @Inject(
            method = "outputItems",
            at = @At("RETURN")
    )
    private void modifyTickDelay(CallbackInfo ci) {
        // 在方法末尾将 tickDelay 设置为 0
        this.tickDelay = 0;
    }

}
