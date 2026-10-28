package com.sorrowmist.useless.mixin.neoeco;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.sorrowmist.useless.compat.neoecoae.compact.shadow.ShadowPatternBusBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Allocate the physical inventory before ECO builds all its slot-indexed caches from its size. */
@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity", remap = false)
public abstract class NeoEcoPatternBusCapacityMixin {
    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE",
            target = "Lcn/dancingsnow/neoecoae/config/NEConfig;getMaxCraftingPatternBusSlotCount()I"), require = 1)
    private int useless$compactPhysicalCapacity(int original) {
        return (Object)this instanceof ShadowPatternBusBlockEntity ? 4096 : original;
    }
}
