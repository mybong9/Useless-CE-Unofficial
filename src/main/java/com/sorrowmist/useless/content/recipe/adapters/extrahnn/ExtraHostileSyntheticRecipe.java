package com.sorrowmist.useless.content.recipe.adapters.extrahnn;

import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class ExtraHostileSyntheticRecipe implements com.sorrowmist.uselesscore.api.recipe.CoreRecipe<Container> {
    private final AdvancedAlloyFurnaceRecipe convertedRecipe;

    public ExtraHostileSyntheticRecipe(AdvancedAlloyFurnaceRecipe convertedRecipe) {
        this.convertedRecipe = convertedRecipe;
    }

    public AdvancedAlloyFurnaceRecipe convertedRecipe() {
        return convertedRecipe;
    }

    @Override
    public boolean matches(Container input, Level level) {
        return convertedRecipe != null && convertedRecipe.matches(input, level);
    }

    @Override
    public ItemStack assemble(Container input, HolderLookup.Provider registries) {
        return convertedRecipe == null ? ItemStack.EMPTY : convertedRecipe.assemble(input, registries);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return convertedRecipe != null && convertedRecipe.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return convertedRecipe == null ? ItemStack.EMPTY : convertedRecipe.getResultItem(registries);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return convertedRecipe == null ? null : convertedRecipe.getSerializer();
    }

    @Override
    public RecipeType<?> getType() {
        return convertedRecipe == null ? null : convertedRecipe.getType();
    }
}
