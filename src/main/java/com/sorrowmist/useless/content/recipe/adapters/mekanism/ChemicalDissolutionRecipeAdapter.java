package com.sorrowmist.useless.content.recipe.adapters.mekanism;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ChemicalDissolutionRecipe;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
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

/** Item + chemical -> chemical adapter for the Chemical Dissolution Chamber. */
public final class ChemicalDissolutionRecipeAdapter implements IRecipeAdapter<ChemicalDissolutionRecipe> {
    private static final long PROCESS_TICKS = 5L * 20L;
    private static final long ENERGY_PER_TICK = 400L;

    @Override
    public Class<ChemicalDissolutionRecipe> getRecipeClass() {
        return ChemicalDissolutionRecipe.class;
    }

    @Override
    public @Nullable ItemStack getMoldItem() {
        return new ItemStack(MekanismBlocks.CHEMICAL_DISSOLUTION_CHAMBER.getBlock());
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<ChemicalDissolutionRecipe> holder, Level level) {
        if (holder == null || level == null || holder.value().getType() != MekanismRecipeType.DISSOLUTION.getRecipeType()) {
            return List.of();
        }
        ChemicalDissolutionRecipe original = holder.value();
        ItemStackIngredient itemInput = original.getItemInput();
        ChemicalStackIngredient<?, ?> chemicalInput = original.getGasInput();
        if (MekanismChemicalRecipeSupport.item(itemInput) == null || chemicalInput == null
                || chemicalInput.hasNoMatchingInstances()) return List.of();

        long multiplier = PROCESS_TICKS;
        List<AdvancedAlloyFurnaceRecipe> result = new ArrayList<>();
        for (ChemicalStack chemical : chemicalInput.getRepresentations()) {
            GenericStack inputKey = MekanismChemicalRecipeSupport.key(com.sorrowmist.useless.compat.mekanism.MekanismChemicalStacks.copyWithAmount(chemical, 
                    MekanismChemicalRecipeSupport.saturatingMultiply(chemical.getAmount(), multiplier)));
            if (inputKey == null) continue;
            for (var boxedOutput : original.getOutputDefinition()) {
                ChemicalStack<?> output = boxedOutput.getChemicalStack();
                GenericStack outputKey = MekanismChemicalRecipeSupport.key(output);
                if (outputKey == null) continue;
                ResourceLocation chemicalId = chemical.getType().getRegistryName();
                result.add(MekanismChemicalRecipeSupport.recipe(
                        MekanismChemicalRecipeSupport.variantId(holder.id(), "dissolution_"
                                + chemicalId.getNamespace() + "_" + chemicalId.getPath()),
                        MekanismChemicalRecipeSupport.items(itemInput), List.of(), List.of(inputKey),
                        List.of(), List.of(), List.of(outputKey),
                        com.sorrowmist.useless.content.recipe.adapters.mekanism.MekanismRecipeEnergy.cost(ENERGY_PER_TICK, PROCESS_TICKS, 1L),
                        AdapterUtils.safeInt(PROCESS_TICKS), getMoldItem()));
            }
        }
        return result;
    }

    @Override
    public List<CoreRecipeHolder<ChemicalDissolutionRecipe>> findMatchingRecipes(
            Level level, Map<Ingredient, Long> mergedInputs, Map<FluidStack, Long> mergedFluids,
            Map<AEKey, Long> mergedKeys, @Nullable ItemStack mold) {
        if (level == null || mergedInputs.isEmpty() || mergedKeys.isEmpty() || !matchesMold(mold)) return List.of();
        RecipeManager manager = level.getRecipeManager();
        List<CoreRecipeHolder<ChemicalDissolutionRecipe>> result = new ArrayList<>();
        for (CoreRecipeHolder<ChemicalDissolutionRecipe> holder : com.sorrowmist.uselesscore.api.recipe.CoreRecipes.<ChemicalDissolutionRecipe>ofType(manager, 
                MekanismRecipeType.DISSOLUTION.getRecipeType())) {
            ChemicalDissolutionRecipe recipe = holder.value();
            if (!MekanismChemicalRecipeSupport.matchesItem(mergedInputs, recipe.getItemInput())) continue;
            long multiplier = PROCESS_TICKS;
            boolean match = false;
            for (ChemicalStack chemical : recipe.getGasInput().getRepresentations()) {
                GenericStack key = MekanismChemicalRecipeSupport.key(com.sorrowmist.useless.compat.mekanism.MekanismChemicalStacks.copyWithAmount(chemical, 
                        MekanismChemicalRecipeSupport.saturatingMultiply(chemical.getAmount(), multiplier)));
                if (key != null && mergedKeys.getOrDefault(key.what(), 0L) >= key.amount()) {
                    match = true;
                    break;
                }
            }
            if (match) result.add(holder);
        }
        return result;
    }
}
