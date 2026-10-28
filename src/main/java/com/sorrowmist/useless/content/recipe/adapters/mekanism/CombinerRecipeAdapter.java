package com.sorrowmist.useless.content.recipe.adapters.mekanism;

import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.CountedIngredient;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.config.MekanismConfig;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.machine.TileEntityCombiner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts Mekanism Combiner recipes to alloy-furnace recipes. */
public final class CombinerRecipeAdapter implements IRecipeAdapter<CombinerRecipe> {
    private static final int PROCESS_TICKS = 200;

    @Override
    public Class<CombinerRecipe> getRecipeClass() {
        return CombinerRecipe.class;
    }

    @Override
    @Nullable
    public ItemStack getMoldItem() {
        return new ItemStack(MekanismBlocks.COMBINER.getBlock());
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(
            CoreRecipeHolder<CombinerRecipe> holder, Level level) {
        if (holder == null || level == null || holder.value() == null) {
            return List.of();
        }

        CombinerRecipe source = holder.value();
        if (source.getType() != MekanismRecipeType.COMBINING.getRecipeType()
                || source.isIncomplete()) {
            return List.of();
        }

        List<CountedIngredient> inputs = countedInputs(source);
        if (inputs.isEmpty()) {
            return List.of();
        }

        List<ItemStack> outputs = source.getOutputDefinition().stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
        if (outputs.isEmpty()) {
            return List.of();
        }

        AdvancedAlloyFurnaceRecipe converted = MekanismChemicalRecipeSupport.recipe(
                AdapterUtils.convertedId(holder.id()),
                inputs,
                List.of(),
                List.of(),
                outputs,
                List.of(),
                List.of(),
                com.sorrowmist.useless.content.recipe.adapters.mekanism.MekanismRecipeEnergy.cost(
                        MekanismConfig.usage.combiner.get(), PROCESS_TICKS, 1L),
                PROCESS_TICKS,
                getMoldItem());
        return List.of(converted);
    }

    @Override
    public List<CoreRecipeHolder<CombinerRecipe>> findMatchingRecipes(
            Level level,
            Map<Ingredient, Long> mergedInputs,
            Map<FluidStack, Long> mergedFluids,
            @Nullable ItemStack mold) {
        if (level == null || mergedInputs == null || mergedInputs.isEmpty()
                || !matchesMold(mold)) {
            return List.of();
        }

        RecipeManager recipeManager = level.getRecipeManager();
        List<CoreRecipeHolder<CombinerRecipe>> matches = new ArrayList<>();
        for (CoreRecipeHolder<CombinerRecipe> holder : com.sorrowmist.uselesscore.api.recipe.CoreRecipes.<CombinerRecipe>ofType(recipeManager, 
                MekanismRecipeType.COMBINING.getRecipeType())) {
            CombinerRecipe recipe = holder.value();
            if (recipe == null || recipe.isIncomplete()) {
                continue;
            }

            List<CountedIngredient> inputs = countedInputs(recipe);
            if (inputs.isEmpty()) {
                continue;
            }

            Map<Ingredient, Long> required = new LinkedHashMap<>();
            for (CountedIngredient input : inputs) {
                AdapterUtils.mergeIngredient(required, input.ingredient(), input.count());
            }
            if (AdapterUtils.matchesRequired(mergedInputs, required)) {
                matches.add(holder);
            }
        }
        return matches;
    }

    private static List<CountedIngredient> countedInputs(CombinerRecipe recipe) {
        CountedIngredient main = countedInput(recipe.getMainInput());
        CountedIngredient extra = countedInput(recipe.getExtraInput());
        if (main == null || extra == null) {
            return List.of();
        }

        List<CountedIngredient> inputs = new ArrayList<>(2);
        addCountedInput(inputs, main);
        addCountedInput(inputs, extra);
        return inputs;
    }

    @Nullable
    private static CountedIngredient countedInput(@Nullable ItemStackIngredient input) {
        return MekanismChemicalRecipeSupport.item(input);
    }

    private static void addCountedInput(List<CountedIngredient> inputs, CountedIngredient input) {
        for (int i = 0; i < inputs.size(); i++) {
            CountedIngredient existing = inputs.get(i);
            if (AdapterUtils.areIngredientsEqual(existing.ingredient(), input.ingredient())) {
                inputs.set(i, new CountedIngredient(existing.ingredient(),
                        MekanismChemicalRecipeSupport.saturatingAdd(existing.count(), input.count())));
                return;
            }
        }
        inputs.add(input);
    }
}
