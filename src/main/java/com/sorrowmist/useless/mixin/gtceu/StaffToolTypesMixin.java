package com.sorrowmist.useless.mixin.gtceu;

import com.sorrowmist.useless.compat.gtceu.GregTechStaffTools;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Set;

@Pseudo
@Mixin(targets = "com.gregtechceu.gtceu.api.item.tool.ToolHelper", remap = false)
public abstract class StaffToolTypesMixin {
    @Inject(method = "getToolTypes", at = @At("HEAD"), cancellable = true, remap = false)
    private static void useless$selectedGtTool(ItemStack stack, CallbackInfoReturnable<Set<?>> result) {
        Set<?> tools = GregTechStaffTools.selectedTools(stack);
        if (tools != null) result.setReturnValue(tools);
    }
}
