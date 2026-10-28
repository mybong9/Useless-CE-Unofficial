package com.sorrowmist.useless.content.recipe.adapters.ae.extendedae;

import com.glodblock.github.extendedae.common.EPPItemAndBlock;
import com.glodblock.github.extendedae.recipe.CircuitCutterRecipe;
import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.fluid.FluidIngredient;
import com.sorrowmist.uselesscore.api.fluid.SizedFluidIngredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** ExtendedAE 1.20.1 cutter, including the source recipe's fluid input. */
public final class CircuitCutterRecipeAdapter implements IRecipeAdapter<CircuitCutterRecipe> {
    @Override public Class<CircuitCutterRecipe> getRecipeClass() { return CircuitCutterRecipe.class; }
    @Override public ItemStack getMoldItem() { return new ItemStack(EPPItemAndBlock.CIRCUIT_CUTTER); }

    private static List<SizedFluidIngredient> fluidInput(CircuitCutterRecipe recipe) {
        var fluid = recipe.getFluid();
        if (fluid == null || fluid.isEmpty()) return List.of();
        var predicate = (com.glodblock.github.extendedae.recipe.util.FluidIngredient) fluid.getIngredient();
        var choices = predicate.getFluid().stream().map(FluidIngredient::single).toList();
        return List.of(new SizedFluidIngredient(FluidIngredient.anyOf(choices), fluid.getAmount()));
    }

    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<CircuitCutterRecipe> holder, Level level) {
        var source = holder.value();
        var input = source.getInput();
        if (input == null || input.isEmpty() || source.output.isEmpty()) return List.of();
        Ingredient ingredient = (Ingredient) input.getIngredient();
        long energy = (long) AdapterUtils.EXTENDEDAE_ENERGY_MULTIPLIER * AdapterUtils.DEFAULT_PROCESS_TIME
                * AdapterUtils.AE_TO_FE_CONVERSION;
        return List.of(new AdvancedAlloyFurnaceRecipe(AdapterUtils.convertedId(holder.id()),
                List.of(new CountedIngredient(ingredient, input.getAmount())), fluidInput(source),
                List.of(source.output.copy()), List.of(), energy, AdapterUtils.DEFAULT_PROCESS_TIME,
                Ingredient.EMPTY, 0, Ingredient.of(getMoldItem()), AlloyFurnaceMode.NORMAL));
    }

    @Override public List<CoreRecipeHolder<CircuitCutterRecipe>> findMatchingRecipes(Level level,
            Map<Ingredient, Long> items, Map<FluidStack, Long> fluids, ItemStack mold) {
        if (!matchesMold(mold)) return List.of();
        List<CoreRecipeHolder<CircuitCutterRecipe>> result = new ArrayList<>();
        for (var holder : com.sorrowmist.uselesscore.api.recipe.CoreRecipes.ofType(level.getRecipeManager(), CircuitCutterRecipe.TYPE)) {
            var input = holder.value().getInput();
            if (input != null && !input.isEmpty()
                    && AdapterUtils.hasMatchingIngredient(items, (Ingredient) input.getIngredient(), input.getAmount())
                    && AdapterUtils.matchesFluidIngredients(fluids, fluidInput(holder.value()))) result.add(holder);
        }
        return result;
    }
}
