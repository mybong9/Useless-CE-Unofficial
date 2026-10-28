package com.sorrowmist.useless.content.recipe.adapters.industrialforegoing;

import com.buuz135.industrial.config.machine.core.FluidExtractorConfig;
import com.buuz135.industrial.module.ModuleCore;
import com.buuz135.industrial.recipe.FluidExtractorRecipe;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.CountedIngredient;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import com.sorrowmist.useless.content.recipe.RecipeSourceIds;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts block-targeted fluid extractor recipes into deterministic item batches. */
public final class FluidExtractorRecipeAdapter implements IRecipeAdapter<FluidExtractorRecipe> {
    private static final long LATEX_PER_SOURCE_UNIT = 125L;

    @Override
    public String sourceId() {
        return RecipeSourceIds.INDUSTRIAL_FOREGOING;
    }

    @Override
    public Class<FluidExtractorRecipe> getRecipeClass() {
        return FluidExtractorRecipe.class;
    }

    @Override
    public ItemStack getMoldItem() {
        return new ItemStack(ModuleCore.FLUID_EXTRACTOR.getLeft().get());
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(
            CoreRecipeHolder<FluidExtractorRecipe> holder, Level level) {
        if (holder == null || holder.value() == null) return List.of();
        FluidExtractorRecipe source = holder.value();
        Ingredient input = itemInput(source);
        if (input.isEmpty()
                || source.output == null || source.output.isEmpty()
                || source.output.getAmount() <= 0) {
            return List.of();
        }

        FluidStack output = source.output.copy();

        int processTime = IndustrialForegoingRecipeAdapterUtils.positive(
                FluidExtractorConfig.maxProgress);
        return List.of(new AdvancedAlloyFurnaceRecipe(
                AdapterUtils.convertedId(holder.id()),
                List.of(new CountedIngredient(input, 1L)), List.of(), List.of(),
                List.of(), List.of(output), List.of(),
                Math.max(1L, FluidExtractorConfig.powerPerOperation), processTime,
                Ingredient.EMPTY, 0,
                List.of(AdapterUtils.toMoldIngredient(getMoldItem())), AlloyFurnaceMode.NORMAL));
    }

    @Override
    public List<CoreRecipeHolder<FluidExtractorRecipe>> findMatchingRecipes(
            Level level, Map<Ingredient, Long> mergedInputs,
            Map<FluidStack, Long> mergedFluids, @Nullable ItemStack mold) {
        if (level == null || mergedInputs == null || mergedInputs.isEmpty()
                || (mergedFluids != null && !mergedFluids.isEmpty())
                || !matchesMold(mold)) {
            return List.of();
        }

        RecipeType<FluidExtractorRecipe> type = extractorType();
        if (type == null) return List.of();
        RecipeManager recipeManager = level.getRecipeManager();
        List<CoreRecipeHolder<FluidExtractorRecipe>> customMatches = new ArrayList<>();
        List<CoreRecipeHolder<FluidExtractorRecipe>> defaultMatches = new ArrayList<>();
        for (CoreRecipeHolder<FluidExtractorRecipe> holder : com.sorrowmist.uselesscore.api.recipe.CoreRecipes.ofType(recipeManager, type)) {
            FluidExtractorRecipe source = holder.value();
            Ingredient input = source == null ? Ingredient.EMPTY : itemInput(source);
            if (source == null || input.isEmpty()
                    || source.output == null || source.output.isEmpty()) continue;
            Map<Ingredient, Long> required = new LinkedHashMap<>();
            AdapterUtils.mergeIngredient(required, input, 1L);
            if (!AdapterUtils.matchesRequired(mergedInputs, required)) continue;
            if (source.defaultRecipe) defaultMatches.add(holder);
            else customMatches.add(holder);
        }
        return customMatches.isEmpty() ? List.copyOf(defaultMatches) : List.copyOf(customMatches);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static RecipeType<FluidExtractorRecipe> extractorType() {
        if (ModuleCore.FLUID_EXTRACTOR_TYPE == null
                || ModuleCore.FLUID_EXTRACTOR_TYPE.get() == null) return null;
        return (RecipeType<FluidExtractorRecipe>) (RecipeType<?>)
                ModuleCore.FLUID_EXTRACTOR_TYPE.get();
    }

    private static Ingredient itemInput(FluidExtractorRecipe recipe) {
        return recipe.input == null ? Ingredient.EMPTY
                : Ingredient.fromValues(java.util.stream.Stream.of(recipe.input));
    }
}
