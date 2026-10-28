package com.sorrowmist.useless.content.recipe.adapters.mekanism;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.api.recipes.PressurizedReactionRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
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

/** Item + fluid + chemical -> item and/or chemical adapter for PRCs. */
public final class PressurizedReactionRecipeAdapter implements IRecipeAdapter<PressurizedReactionRecipe> {
    private static final long BASE_ENERGY_PER_TICK = 5L;

    @Override
    public Class<PressurizedReactionRecipe> getRecipeClass() {
        return PressurizedReactionRecipe.class;
    }

    @Override
    public @Nullable ItemStack getMoldItem() {
        return new ItemStack(MekanismBlocks.PRESSURIZED_REACTION_CHAMBER.getBlock());
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<PressurizedReactionRecipe> holder, Level level) {
        if (holder == null || level == null || holder.value().getType() != MekanismRecipeType.REACTION.getRecipeType()) {
            return List.of();
        }
        PressurizedReactionRecipe original = holder.value();
        ItemStackIngredient itemInput = original.getInputSolid();
        FluidStackIngredient fluidInput = original.getInputFluid();
        ChemicalStackIngredient<?, ?> chemicalInput = original.getInputGas();
        if (MekanismChemicalRecipeSupport.item(itemInput) == null || fluidInput == null || chemicalInput == null
                || fluidInput.hasNoMatchingInstances()
                || chemicalInput.hasNoMatchingInstances()) return List.of();

        List<AdvancedAlloyFurnaceRecipe> result = new ArrayList<>();
        List<com.sorrowmist.uselesscore.api.fluid.SizedFluidIngredient> fluidInputs =
                MekanismChemicalRecipeSupport.fluidIngredients(fluidInput);
        for (ChemicalStack chemical : chemicalInput.getRepresentations()) {
            GenericStack chemicalKey = MekanismChemicalRecipeSupport.key(chemical);
            if (chemicalKey == null) continue;
            for (PressurizedReactionRecipe.PressurizedReactionRecipeOutput output
                    : original.getOutputDefinition()) {
                List<GenericStack> chemicalOutputs = new ArrayList<>();
                if (!output.gas().isEmpty()) {
                    GenericStack outputKey = MekanismChemicalRecipeSupport.key(output.gas());
                    if (outputKey != null) chemicalOutputs.add(outputKey);
                }
                if (output.item().isEmpty() && chemicalOutputs.isEmpty()) continue;

                String suffix = "reaction_" + id(chemical) + "_out";
                result.add(MekanismChemicalRecipeSupport.recipe(
                        MekanismChemicalRecipeSupport.variantId(holder.id(), suffix),
                        MekanismChemicalRecipeSupport.items(itemInput), fluidInputs, List.of(chemicalKey),
                        output.item().isEmpty() ? List.of() : List.of(output.item().copy()), List.of(),
                        chemicalOutputs, reactionEnergy(original), original.getDuration(), getMoldItem()));
            }
        }
        return result;
    }

    @Override
    public List<CoreRecipeHolder<PressurizedReactionRecipe>> findMatchingRecipes(
            Level level, Map<Ingredient, Long> mergedInputs, Map<FluidStack, Long> mergedFluids,
            Map<AEKey, Long> mergedKeys, @Nullable ItemStack mold) {
        if (level == null || mergedInputs.isEmpty() || mergedFluids.isEmpty() || mergedKeys.isEmpty()
                || !matchesMold(mold)) return List.of();
        RecipeManager manager = level.getRecipeManager();
        List<CoreRecipeHolder<PressurizedReactionRecipe>> result = new ArrayList<>();
        for (CoreRecipeHolder<PressurizedReactionRecipe> holder : com.sorrowmist.uselesscore.api.recipe.CoreRecipes.ofType(manager, 
                MekanismRecipeType.REACTION.getRecipeType())) {
            PressurizedReactionRecipe recipe = holder.value();
            if (MekanismChemicalRecipeSupport.matchesItem(mergedInputs, recipe.getInputSolid())
                    && MekanismChemicalRecipeSupport.matchesFluid(mergedFluids, recipe.getInputFluid())
                    && MekanismChemicalRecipeSupport.matchesChemical(mergedKeys, recipe.getInputGas())) {
                result.add(holder);
            }
        }
        return result;
    }

    private static long reactionEnergy(PressurizedReactionRecipe recipe) {
        long energyPerTick = MekanismChemicalRecipeSupport.saturatingAdd(
                BASE_ENERGY_PER_TICK, Math.round(recipe.getEnergyRequired().doubleValue()));
        return MekanismChemicalRecipeSupport.saturatingMultiply(energyPerTick, recipe.getDuration());
    }

    private static String id(ChemicalStack stack) {
        var id = stack.getType().getRegistryName();
        return id.getNamespace() + "_" + id.getPath().replace('/', '_');
    }

    private static String fluidId(FluidStack stack) {
        var id = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(stack.getFluid());
        return id.getNamespace() + "_" + id.getPath().replace('/', '_');
    }
}
