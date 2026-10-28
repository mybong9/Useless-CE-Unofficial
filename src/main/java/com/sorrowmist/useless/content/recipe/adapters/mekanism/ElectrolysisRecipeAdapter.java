package com.sorrowmist.useless.content.recipe.adapters.mekanism;

import appeng.api.stacks.GenericStack;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ElectrolysisRecipe;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.resources.ResourceLocation;
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

/** Fluid -> two chemical outputs adapter for the Electrolytic Separator. */
public final class ElectrolysisRecipeAdapter implements IRecipeAdapter<ElectrolysisRecipe> {
    private static final long PROCESS_TICKS = AdapterUtils.MEKANISM_BASE_TICKS_REQUIRED;
    private static final long ENERGY_PER_TICK = 200L;

    @Override
    public Class<ElectrolysisRecipe> getRecipeClass() {
        return ElectrolysisRecipe.class;
    }

    @Override
    public @Nullable ItemStack getMoldItem() {
        return new ItemStack(MekanismBlocks.ELECTROLYTIC_SEPARATOR.getBlock());
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<ElectrolysisRecipe> holder, Level level) {
        if (holder == null || level == null || holder.value().getType() != MekanismRecipeType.SEPARATING.getRecipeType()) {
            return List.of();
        }
        ElectrolysisRecipe original = holder.value();
        FluidStackIngredient input = original.getInput();
        if (input == null || input.hasNoMatchingInstances()) return List.of();

        List<AdvancedAlloyFurnaceRecipe> result = new ArrayList<>();
        List<com.sorrowmist.uselesscore.api.fluid.SizedFluidIngredient> fluidInputs =
                MekanismChemicalRecipeSupport.fluidIngredients(input);
        for (ElectrolysisRecipe.ElectrolysisRecipeOutput output : original.getOutputDefinition()) {
            GenericStack left = MekanismChemicalRecipeSupport.key(output.left());
            GenericStack right = MekanismChemicalRecipeSupport.key(output.right());
            if (left == null || right == null) continue;
            result.add(MekanismChemicalRecipeSupport.recipe(
                    MekanismChemicalRecipeSupport.variantId(holder.id(), "separating"),
                    List.of(), fluidInputs, List.of(), List.of(), List.of(), List.of(left, right),
                    com.sorrowmist.useless.content.recipe.adapters.mekanism.MekanismRecipeEnergy.cost(ENERGY_PER_TICK,
                            PROCESS_TICKS, Math.max(1L, Math.round(original.getEnergyMultiplier().doubleValue()))),
                    AdapterUtils.safeInt(PROCESS_TICKS), getMoldItem()));
        }
        return result;
    }

    @Override
    public List<CoreRecipeHolder<ElectrolysisRecipe>> findMatchingRecipes(
            Level level, Map<Ingredient, Long> mergedInputs, Map<FluidStack, Long> mergedFluids,
            Map<appeng.api.stacks.AEKey, Long> mergedKeys, @Nullable ItemStack mold) {
        if (level == null || mergedFluids.isEmpty() || !matchesMold(mold)) return List.of();
        RecipeManager manager = level.getRecipeManager();
        List<CoreRecipeHolder<ElectrolysisRecipe>> result = new ArrayList<>();
        for (CoreRecipeHolder<ElectrolysisRecipe> holder : com.sorrowmist.uselesscore.api.recipe.CoreRecipes.<ElectrolysisRecipe>ofType(manager, 
                MekanismRecipeType.SEPARATING.getRecipeType())) {
            if (MekanismChemicalRecipeSupport.matchesFluid(mergedFluids, holder.value().getInput())) {
                result.add(holder);
            }
        }
        return result;
    }
}
