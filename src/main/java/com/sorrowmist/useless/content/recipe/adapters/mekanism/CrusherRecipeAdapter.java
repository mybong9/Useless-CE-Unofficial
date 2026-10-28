package com.sorrowmist.useless.content.recipe.adapters.mekanism;

import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.CountedIngredient;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CrusherRecipeAdapter implements IRecipeAdapter<ItemStackToItemStackRecipe> {

    @Override
    public Class<ItemStackToItemStackRecipe> getRecipeClass() {
        return ItemStackToItemStackRecipe.class;
    }

    @Override
    @Nullable
    public ItemStack getMoldItem() {
        return new ItemStack(MekanismBlocks.CRUSHER.getBlock());
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<ItemStackToItemStackRecipe> holder, Level level) {
        List<AdvancedAlloyFurnaceRecipe> result = new ArrayList<>();

        if (holder == null) return result;

        ItemStackToItemStackRecipe originalRecipe = holder.value();

        if (!originalRecipe.getType().equals(MekanismRecipeType.CRUSHING.getRecipeType())) {
            return result;
        }

        List<CountedIngredient> countedIngredients = new ArrayList<>();
        addCountedIngredient(countedIngredients, originalRecipe.getInput(), 1);

        if (countedIngredients.isEmpty()) {
            return result;
        }

        List<ItemStack> outputs = originalRecipe.getOutputDefinition();
        if (outputs.isEmpty()) {
            return result;
        }

        result.add(new AdvancedAlloyFurnaceRecipe(
                AdapterUtils.convertedId(holder.id()),
                countedIngredients,
                List.of(),
                outputs.stream().map(ItemStack::copy).toList(),
                List.of(),
                com.sorrowmist.useless.content.recipe.adapters.mekanism.MekanismRecipeEnergy.cost(AdapterUtils.MEKANISM_ENRICHMENT_CHAMBER_ENERGY_PER_TICK, 1),
                AdapterUtils.mekanismProcessTime(1),
                Ingredient.EMPTY,
                0,
                AdapterUtils.toMoldIngredient(getMoldItem()),
                AlloyFurnaceMode.NORMAL
        ));
        return result;
    }

    @Override
    @Nullable
    public List<CoreRecipeHolder<ItemStackToItemStackRecipe>> findMatchingRecipes(Level level, Map<Ingredient, Long> mergedInputs, Map<FluidStack, Long> mergedFluids, @Nullable ItemStack mold) {
        if (level == null || mergedInputs.isEmpty()) {
            return List.of();
        }
        if (mold != null && !mold.isEmpty() && !matchesMold(mold)) {
            return List.of();
        }

        RecipeManager recipeManager = level.getRecipeManager();
        List<CoreRecipeHolder<ItemStackToItemStackRecipe>> recipes = com.sorrowmist.uselesscore.api.recipe.CoreRecipes.ofType(recipeManager, 
                MekanismRecipeType.CRUSHING.getRecipeType()
        );

        List<CoreRecipeHolder<ItemStackToItemStackRecipe>> matches = new java.util.ArrayList<>();
        for (CoreRecipeHolder<ItemStackToItemStackRecipe> holder : recipes) {
            ItemStackToItemStackRecipe recipe = holder.value();
            ItemStackIngredient input = recipe.getInput();
            if (input == null) continue;

            if (matchesIngredient(mergedInputs, input)) {
                matches.add(holder);
            }
        }
        return matches;
    }

    @Nullable
    private static CountedIngredient countedIngredient(ItemStackIngredient input, long multiplier) {
        return MekanismChemicalRecipeSupport.countedItem(input, multiplier);
    }

    private static boolean matchesIngredient(Map<Ingredient, Long> mergedInputs, ItemStackIngredient required) {
        if (required == null) {
            return false;
        }
        CountedIngredient counted = MekanismChemicalRecipeSupport.item(required);
        if (counted == null) return false;
        Map<Ingredient, Long> requiredCounts = new java.util.LinkedHashMap<>();
        AdapterUtils.mergeIngredient(requiredCounts, counted.ingredient(), counted.count());
        return AdapterUtils.matchesRequired(mergedInputs, requiredCounts);
    }

    private static void addCountedIngredient(List<CountedIngredient> countedIngredients, ItemStackIngredient input, long multiplier) {
        CountedIngredient counted = countedIngredient(input, multiplier);
        if (counted == null) {
            return;
        }
        for (int i = 0; i < countedIngredients.size(); i++) {
            CountedIngredient existing = countedIngredients.get(i);
            if (AdapterUtils.areIngredientsEqual(existing.ingredient(), counted.ingredient())) {
                countedIngredients.set(i, new CountedIngredient(existing.ingredient(), existing.count() + counted.count()));
                return;
            }
        }
        countedIngredients.add(counted);
    }
}
