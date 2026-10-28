package com.sorrowmist.useless.content.recipe.adapters.enderio;

import com.enderio.machines.common.recipe.SlicingRecipe;
import com.enderio.base.common.init.EIOBlocks;
import com.enderio.base.common.init.EIORecipes;
import com.mojang.logging.LogUtils;
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

/** Converts Ender IO Slice and Splice recipes to the alloy furnace. */
public final class SlicingRecipeAdapter implements IRecipeAdapter<SlicingRecipe> {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public Class<SlicingRecipe> getRecipeClass() {
        return SlicingRecipe.class;
    }

    @Override
    public ItemStack getMoldItem() {
        return new ItemStack(com.enderio.machines.common.init.MachineBlocks.SLICE_AND_SPLICE.get());
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(
            CoreRecipeHolder<SlicingRecipe> holder, Level level) {
        if (holder == null || holder.value() == null) {
            return List.of();
        }
        SlicingRecipe source = holder.value();
        List<CountedIngredient> inputs = counted(source.getInputs());
        ItemStack output = EnderIOAdapterUtils.primaryItemOutput(source);
        if (inputs == null || inputs.isEmpty() || output == null || output.isEmpty()
                || output.getCount() <= 0 || source.getBaseEnergyCost() < 0) {
            LOGGER.warn("Skipping invalid Ender IO slicing recipe: {}", holder.id());
            return List.of();
        }
        return List.of(new AdvancedAlloyFurnaceRecipe(
                AdapterUtils.convertedId(holder.id()), inputs, List.of(), List.of(output.copy()), List.of(),
                source.getBaseEnergyCost(), AdapterUtils.DEFAULT_PROCESS_TIME, Ingredient.EMPTY, 0,
                AdapterUtils.toMoldIngredient(getMoldItem()), AlloyFurnaceMode.NORMAL));
    }

    @Override
    public List<CoreRecipeHolder<SlicingRecipe>> findMatchingRecipes(
            Level level, Map<Ingredient, Long> mergedInputs,
            Map<FluidStack, Long> mergedFluids, @Nullable ItemStack mold) {
        if (level == null || mergedInputs == null || mergedInputs.isEmpty() || !matchesMold(mold)) {
            return List.of();
        }
        List<CoreRecipeHolder<SlicingRecipe>> matches = new ArrayList<>();
        RecipeManager manager = level.getRecipeManager();
        for (CoreRecipeHolder<SlicingRecipe> holder : com.sorrowmist.uselesscore.api.recipe.CoreRecipes.ofType(manager, com.enderio.machines.common.init.MachineRecipes.SLICING.type().get())) {
            SlicingRecipe source = holder.value();
            List<CountedIngredient> inputs = counted(source.getInputs());
            if (inputs != null && !EnderIOAdapterUtils.primaryItemOutput(source).isEmpty()
                    && AdapterUtils.matchesRequired(mergedInputs, EnderIOAdapterUtils.requirements(inputs))) {
                matches.add(holder);
            }
        }
        return matches;
    }

    private static List<CountedIngredient> counted(List<Ingredient> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        return source.stream()
                .filter(ingredient -> ingredient != null && !ingredient.isEmpty())
                .collect(java.util.stream.Collectors.groupingBy(
                        ingredient -> ingredient,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new CountedIngredient(entry.getKey(), entry.getValue()))
                .toList();
    }
}
