package com.sorrowmist.useless.mixin.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingTreeProcess;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.CraftingCalculation;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.DynamicPatternPlanning;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftingTreeProcess.class, remap = false)
public abstract class CraftingTreeProcessMixin {
    @Shadow(remap = false)
    @Final
    IPatternDetails details;

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "NEW",
                    target = "appeng/crafting/CraftingTreeNode"),
            remap = false)
    private CraftingTreeNode uselessMod$preferDeclaredDynamicInput(ICraftingService service,CraftingCalculation calculation,
            AEKey key,long amount,CraftingTreeProcess parent,int slot) {
        AEKey preferred = DynamicPatternPlanning.preferDeclaredCraftableInput(
                details,
                slot,
                key,
                service);
        return new CraftingTreeNode(service,calculation,preferred,amount,parent,slot);
    }
}
