package com.sorrowmist.useless.api.recipe;

import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeAdapter;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.ItemStack;
import com.sorrowmist.useless.content.recipe.AdapterUtils;

/** Alloy-furnace specialization of the shared Useless Core recipe adapter API. */
public interface IRecipeAdapter<T extends Recipe<?>>
        extends CoreRecipeAdapter<T, AdvancedAlloyFurnaceRecipe> {
    @Override
    default boolean matchesMold(ItemStack mold) {
        ItemStack required = getMoldItem();
        return required == null || required.isEmpty()
                || mold != null && !mold.isEmpty() && AdapterUtils.toMoldIngredient(required).test(mold);
    }
}