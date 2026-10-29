package com.sorrowmist.useless.content.recipe.adapters.productivebees;

import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.CraftingTaskContext;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import cy.jdkdigital.productivebees.common.recipe.BeeFishingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/** Fishing bee recipes retain their native biome requirement. */
public final class ProductiveBeesEnvironment {
    private ProductiveBeesEnvironment() {}
    public static CraftingTaskContext.TaskAvailability check(CraftingTaskContext context, AdvancedAlloyFurnaceRecipe recipe) {
        if (!com.sorrowmist.useless.core.config.ConfigManager.isProductiveBeesBiomeRestrictionEnabled())
            return CraftingTaskContext.TaskAvailability.ready();
        var level = context.getLevel();
        String path = recipe.id().getPath();
        if (level == null || !path.endsWith("_converted")) return CraftingTaskContext.TaskAvailability.ready();
        var id = new ResourceLocation(recipe.id().getNamespace(), path.substring(0, path.length() - "_converted".length()));
        var original = level.getRecipeManager().byKey(id).orElse(null);
        if (!(original instanceof BeeFishingRecipe fishing)) return CraftingTaskContext.TaskAvailability.ready();
        boolean allowed = false;
        if (level instanceof ServerLevel server && server.getServer().isSameThread()) {
            try { allowed = BeeFishingRecipe.getBiomeList(fishing, level).contains(level.getBiome(context.getBlockPos()).value()); }
            catch (RuntimeException invalidBiome) {
                com.sorrowmist.useless.UselessMod.LOGGER.debug("Cannot resolve PB fishing biomes for {}", id, invalidBiome);
            }
        }
        if (!allowed)
            return CraftingTaskContext.TaskAvailability.unavailable(
                    "gui.useless_mod.advanced_alloy_furnace.ae_task_status.waiting_conditions", "Productive Bees: biome");
        return CraftingTaskContext.TaskAvailability.ready();
    }
}
