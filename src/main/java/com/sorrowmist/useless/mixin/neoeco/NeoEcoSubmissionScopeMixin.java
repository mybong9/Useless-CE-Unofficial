package com.sorrowmist.useless.mixin.neoeco;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.sorrowmist.uselesscore.api.crafting.CoreCraftingPlanScopes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
/** Keep the confirmed execution vector while the native 20.5 lifecycle submits a job. */
@Pseudo
@Mixin(targets="cn.dancingsnow.neoecoae.crafting.execution.ECOCraftingCPULogic",remap=false)
public abstract class NeoEcoSubmissionScopeMixin {
    @WrapMethod(method="trySubmitJob")
    private ICraftingSubmitResult useless$confirm(IGrid grid, ICraftingPlan plan, IActionSource source,
            ICraftingRequester requester, Operation<ICraftingSubmitResult> original) {
        try(var ignored=CoreCraftingPlanScopes.confirm(plan)) {
            return original.call(grid,plan,source,requester);
        }
    }
}
