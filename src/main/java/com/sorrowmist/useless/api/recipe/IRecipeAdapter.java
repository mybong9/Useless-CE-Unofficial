package com.sorrowmist.useless.api.recipe;

import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeAdapter;
import net.minecraft.world.item.crafting.Recipe;

/** Alloy-furnace specialization of the shared Useless Core recipe adapter API. */
public interface IRecipeAdapter<T extends Recipe<?>>
        extends CoreRecipeAdapter<T, AdvancedAlloyFurnaceRecipe> {
}