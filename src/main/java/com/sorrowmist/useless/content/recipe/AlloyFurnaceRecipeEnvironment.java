package com.sorrowmist.useless.content.recipe;

import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.CraftingTaskContext;
import net.minecraftforge.fml.ModList;

/** Shared execution gate for physical and AE-driven furnace batches. */
public final class AlloyFurnaceRecipeEnvironment {
    private AlloyFurnaceRecipeEnvironment() {}
    public static CraftingTaskContext.TaskAvailability check(CraftingTaskContext context, AdvancedAlloyFurnaceRecipe recipe) {
        var gt = com.sorrowmist.useless.compat.gtceu.GregTechEnvironment.check(context, recipe);
        if (!gt.available()) return gt;
        if (recipe != null && ModList.get().isLoaded("productivebees"))
            return com.sorrowmist.useless.content.recipe.adapters.productivebees.ProductiveBeesEnvironment.check(context, recipe);
        return CraftingTaskContext.TaskAvailability.ready();
    }
}
