package com.sorrowmist.useless.content.recipe.adapters.malum;

import com.mojang.logging.LogUtils;
import com.sammy.malum.common.recipe.FavorOfTheVoidRecipe;
import com.sammy.malum.registry.common.recipe.RecipeTypeRegistry;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.CountedIngredient;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Converts Malum's Weeping Well item transmutations. */
public final class VoidFavorRecipeAdapter implements IRecipeAdapter<FavorOfTheVoidRecipe> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PROCESS_TIME = 80;

    @Override
    public Class<FavorOfTheVoidRecipe> getRecipeClass() {
        return FavorOfTheVoidRecipe.class;
    }

    @Override
    public ItemStack getMoldItem() {
        return MalumAdapterUtils.item("void_conduit");
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(
            CoreRecipeHolder<FavorOfTheVoidRecipe> holder, Level level) {
        if (holder == null || holder.value() == null) {
            return List.of();
        }

        FavorOfTheVoidRecipe source = holder.value();
        if (source.input == null || source.input.isEmpty()
                || source.output == null || source.output.isEmpty()
                || source.output.getCount() <= 0) {
            LOGGER.warn("Skipping invalid Malum void favor recipe: {}", holder.id());
            return List.of();
        }

        AdvancedAlloyFurnaceRecipe converted = new AdvancedAlloyFurnaceRecipe(
                AdapterUtils.convertedId(holder.id()),
                List.of(new CountedIngredient(source.input, 1L)),
                List.of(),
                List.of(source.output.copy()),
                List.of(),
                AdapterUtils.DEFAULT_ENERGY,
                PROCESS_TIME,
                Ingredient.EMPTY,
                0,
                AdapterUtils.toMoldIngredient(getMoldItem()),
                AlloyFurnaceMode.NORMAL
        );
        return List.of(converted);
    }

    @Override
    public List<CoreRecipeHolder<FavorOfTheVoidRecipe>> findMatchingRecipes(
            Level level, Map<Ingredient, Long> mergedInputs, Map<FluidStack, Long> mergedFluids,
            @Nullable ItemStack mold) {
        if (level == null || !matchesMold(mold) || mergedInputs == null || mergedInputs.isEmpty()) {
            return List.of();
        }

        RecipeManager recipeManager = level.getRecipeManager();
        List<CoreRecipeHolder<FavorOfTheVoidRecipe>> matches = new ArrayList<>();
        for (CoreRecipeHolder<FavorOfTheVoidRecipe> holder : com.sorrowmist.uselesscore.api.recipe.CoreRecipes.ofType(recipeManager, 
                RecipeTypeRegistry.VOID_FAVOR.get())) {
            FavorOfTheVoidRecipe source = holder.value();
            if (source != null && source.input != null && !source.input.isEmpty()
                    && source.output != null && !source.output.isEmpty()
                    && source.output.getCount() > 0
                    && AdapterUtils.matchesRequired(
                    mergedInputs, Map.of(source.input, 1L))) {
                matches.add(holder);
            }
        }
        return matches;
    }
}
