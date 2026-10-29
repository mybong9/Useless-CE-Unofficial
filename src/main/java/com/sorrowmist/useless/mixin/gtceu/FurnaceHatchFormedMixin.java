package com.sorrowmist.useless.mixin.gtceu;

import com.sorrowmist.useless.compat.gtceu.GregTechParts;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import java.util.List;

@Pseudo
@Mixin(targets = "com.gregtechceu.gtceu.common.machine.multiblock.part.OpticalDataHatchMachine", remap = false)
public abstract class FurnaceHatchFormedMixin {
    // Scope the bridge to data lookup. Other GT code must not assume a native controller
    // exists merely because this receiver belongs to a furnace.
    @WrapOperation(method = "isRecipeAvailable", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/common/machine/multiblock/part/OpticalDataHatchMachine;isFormed()Z", remap = false), remap = false, require = 0)
    private boolean useless$furnaceHatchFormed(@Coerce Object hatch, Operation<Boolean> original) {
        return original.call(hatch) || GregTechParts.isFurnacePart(hatch);
    }
    // Legacy GTM checks an empty controller list instead of calling isFormed.
    // Exactly one of these two version-specific guards exists in supported GTM jars.
    @WrapOperation(method = "isRecipeAvailable", at = @At(value = "INVOKE",
            target = "Ljava/util/List;isEmpty()Z", remap = false), remap = false, require = 0)
    private boolean useless$legacyFurnaceHatchFormed(List<?> controllers, Operation<Boolean> original) {
        return original.call(controllers) && !GregTechParts.isFurnacePart(this);
    }
}
