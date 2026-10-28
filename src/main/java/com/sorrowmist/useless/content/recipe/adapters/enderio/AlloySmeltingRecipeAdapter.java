package com.sorrowmist.useless.content.recipe.adapters.enderio;
import com.enderio.machines.common.recipe.AlloySmeltingRecipe;
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

/** Converts Ender IO alloy-smelter recipes to the alloy furnace. */
public final class AlloySmeltingRecipeAdapter implements IRecipeAdapter<AlloySmeltingRecipe> {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public Class<AlloySmeltingRecipe> getRecipeClass() {
        return AlloySmeltingRecipe.class;
    }

    @Override
    public ItemStack getMoldItem() {
        return new ItemStack(com.enderio.machines.common.init.MachineBlocks.ALLOY_SMELTER.get());
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(
            CoreRecipeHolder<AlloySmeltingRecipe> holder, Level level) {
        if (holder == null || holder.value() == null) {
            return List.of();
        }
        AlloySmeltingRecipe source = holder.value();
        List<CountedIngredient> inputs = EnderIOAdapterUtils.counted(source.getInputs());
        ItemStack output = EnderIOAdapterUtils.primaryItemOutput(source);
        if (inputs == null || inputs.isEmpty() || output == null || output.isEmpty()
                || output.getCount() <= 0 || source.getBaseEnergyCost() < 0) {
            LOGGER.warn("Skipping invalid Ender IO alloy-smelting recipe: {}", holder.id());
            return List.of();
        }
        return List.of(new AdvancedAlloyFurnaceRecipe(
                AdapterUtils.convertedId(holder.id()), inputs, List.of(), List.of(output.copy()), List.of(),
                source.getBaseEnergyCost(), AdapterUtils.DEFAULT_PROCESS_TIME, Ingredient.EMPTY, 0,
                AdapterUtils.toMoldIngredient(getMoldItem()), AlloyFurnaceMode.NORMAL));
    }

    @Override
    public List<CoreRecipeHolder<AlloySmeltingRecipe>> findMatchingRecipes(
            Level level, Map<Ingredient, Long> mergedInputs,
            Map<FluidStack, Long> mergedFluids, @Nullable ItemStack mold) {
        if (level == null || mergedInputs == null || mergedInputs.isEmpty() || !matchesMold(mold)) {
            return List.of();
        }
        List<CoreRecipeHolder<AlloySmeltingRecipe>> matches = new ArrayList<>();
        RecipeManager manager = level.getRecipeManager();
        for (CoreRecipeHolder<AlloySmeltingRecipe> holder : com.sorrowmist.uselesscore.api.recipe.CoreRecipes.ofType(manager, 
                com.enderio.machines.common.init.MachineRecipes.ALLOY_SMELTING.type().get())) {
            AlloySmeltingRecipe source = holder.value();
            List<CountedIngredient> inputs = EnderIOAdapterUtils.counted(source.getInputs());
            ItemStack output = EnderIOAdapterUtils.primaryItemOutput(source);
            if (inputs != null && output != null && !output.isEmpty()
                    && AdapterUtils.matchesRequired(mergedInputs, EnderIOAdapterUtils.requirements(inputs))) {
                matches.add(holder);
            }
        }
        return matches;
    }
}
