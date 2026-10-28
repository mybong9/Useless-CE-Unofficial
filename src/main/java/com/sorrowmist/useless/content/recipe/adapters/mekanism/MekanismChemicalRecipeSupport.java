package com.sorrowmist.useless.content.recipe.adapters.mekanism;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.useless.compat.mekanism.MekanismChemicalCompat;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.CountedIngredient;
import com.sorrowmist.useless.content.recipe.FluidIngredientAllocator;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraftforge.fluids.FluidStack;
import com.sorrowmist.uselesscore.api.fluid.FluidIngredient;
import com.sorrowmist.uselesscore.api.fluid.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Shared conversion and matching helpers for AppMek-backed Mekanism recipes. */
public final class MekanismChemicalRecipeSupport {
    public static final long NON_ITEM_QUANTITY_MULTIPLIER = 1_000L;

    private MekanismChemicalRecipeSupport() {
    }

    @Nullable
    public static GenericStack key(ChemicalStack stack) {
        return MekanismChemicalCompat.toGenericStack(stack);
    }

    static List<GenericStack> keys(ChemicalStackIngredient<?, ?> ingredient) {
        List<GenericStack> result = new ArrayList<>();
        if (ingredient == null || ingredient.hasNoMatchingInstances()) return result;
        for (ChemicalStack representation : ingredient.getRepresentations()) {
            GenericStack key = key(representation);
            if (key != null && key.amount() > 0L && result.stream().noneMatch(existing -> existing.what().equals(key.what()))) {
                result.add(key);
            }
        }
        return result;
    }

    static Ingredient itemIngredient(ItemStackIngredient ingredient) {
        CountedIngredient converted = com.sorrowmist.useless.compat.mekanism.MekanismIngredientConversions.item(ingredient);
        return converted == null ? Ingredient.EMPTY : converted.ingredient();
    }

    @Nullable
    static CountedIngredient item(ItemStackIngredient ingredient) { return countedItem(ingredient, 1L); }

    @Nullable
    static CountedIngredient countedItem(ItemStackIngredient ingredient, long multiplier) {
        if (multiplier <= 0) return null;
        CountedIngredient converted = com.sorrowmist.useless.compat.mekanism.MekanismIngredientConversions.item(ingredient);
        return converted == null ? null : new CountedIngredient(converted.ingredient(),
                saturatingMultiply(converted.count(), multiplier));
    }
    static List<CountedIngredient> items(ItemStackIngredient ingredient) {
        CountedIngredient converted = item(ingredient);
        return converted == null ? List.of() : List.of(converted);
    }

    static boolean matchesItem(Map<Ingredient, Long> inputs, ItemStackIngredient required) {
        CountedIngredient counted = item(required);
        if (counted == null) return false;
        Map<Ingredient, Long> requirements = new LinkedHashMap<>();
        requirements.put(counted.ingredient(), counted.count());
        return AdapterUtils.matchesRequired(inputs, requirements);
    }

    static boolean matchesChemical(Map<AEKey, Long> inputs, ChemicalStackIngredient<?, ?> required) {
        for (GenericStack key : keys(required)) {
            if (inputs.getOrDefault(key.what(), 0L) >= key.amount()) return true;
        }
        return false;
    }

    static boolean matchesFluid(Map<FluidStack, Long> inputs, FluidStackIngredient required) {
        var converted = fluidIngredients(required);
        return !converted.isEmpty() && FluidIngredientAllocator.matches(converted, inputs, 1L);
    }

    public static List<SizedFluidIngredient> fluidIngredients(FluidStackIngredient ingredient) {
        var converted = com.sorrowmist.useless.compat.mekanism.MekanismIngredientConversions.fluid(ingredient);
        return converted == null ? List.of() : List.of(converted);
    }
    private static List<SizedFluidIngredient> scaleFluidIngredients(
            List<SizedFluidIngredient> ingredients) {
        if (ingredients.isEmpty()) return ingredients;
        List<SizedFluidIngredient> scaled = new ArrayList<>(ingredients.size());
        for (SizedFluidIngredient ingredient : ingredients) {
            if (ingredient == null || ingredient.ingredient() == null || ingredient.amount() <= 0) {
                scaled.add(ingredient);
                continue;
            }
            scaled.add(new SizedFluidIngredient(ingredient.ingredient(),
                    saturatingFluidAmount(ingredient.amount())));
        }
        return scaled;
    }

    public static List<FluidStack> fluidRepresentations(FluidStackIngredient ingredient) {
        if (ingredient == null || ingredient.hasNoMatchingInstances()) return List.of();
        List<FluidStack> result = new ArrayList<>();
        for (FluidStack representation : ingredient.getRepresentations()) {
            FluidStack copy = representation.copy();
            // Native representations already carry the amount for their alternative.
            result.add(copy);
        }
        return result;
    }

    public static ResourceLocation variantId(ResourceLocation originalId, String suffix) {
        String clean = suffix == null || suffix.isBlank() ? "converted" : suffix;
        return new ResourceLocation(originalId.getNamespace(),
                originalId.getPath() + "_" + clean.replace('/', '_'));
    }

    public static AdvancedAlloyFurnaceRecipe recipe(ResourceLocation id, List<CountedIngredient> items,
                                             List<?> fluids, List<GenericStack> chemicalInputs,
                                             List<ItemStack> outputs, List<FluidStack> outputFluids,
                                             List<GenericStack> chemicalOutputs, long energy, int processTime,
                                             @Nullable ItemStack mold) {
        List<CountedIngredient> safeItems = items == null ? List.of() : items;
        List<SizedFluidIngredient> safeFluids = toSizedFluidIngredients(fluids);
        List<GenericStack> safeChemicalInputs = chemicalInputs == null ? List.of() : chemicalInputs;
        List<ItemStack> safeOutputs = outputs == null ? List.of() : outputs;
        List<FluidStack> safeOutputFluids = outputFluids == null ? List.of() : outputFluids;
        List<GenericStack> safeChemicalOutputs = chemicalOutputs == null ? List.of() : chemicalOutputs;

        // Mekanism's non-item conversion recipes use the same machine operation for a much
        // larger resource batch. Keep item-bearing conversions unchanged because their item
        // quantities are part of the original recipe's material balance.
        boolean itemFree = safeItems.isEmpty() && safeOutputs.isEmpty();
        if (itemFree) {
            safeFluids = scaleFluidIngredients(safeFluids);
            safeChemicalInputs = scaleGenericStacks(safeChemicalInputs);
            safeOutputFluids = scaleOutputFluidStacks(safeOutputFluids);
            safeChemicalOutputs = scaleGenericStacks(safeChemicalOutputs);
        }

        return new AdvancedAlloyFurnaceRecipe(
                id, safeItems, safeFluids, safeChemicalInputs, safeOutputs, safeOutputFluids, safeChemicalOutputs,
                energy, Math.max(1, processTime), Ingredient.EMPTY, 0,
                AdapterUtils.toMoldIngredient(mold), AlloyFurnaceMode.NORMAL);
    }

    private static List<GenericStack> scaleGenericStacks(List<GenericStack> stacks) {
        if (stacks.isEmpty()) return stacks;
        List<GenericStack> scaled = new ArrayList<>(stacks.size());
        for (GenericStack stack : stacks) {
            if (stack == null || stack.what() == null) {
                scaled.add(stack);
                continue;
            }
            scaled.add(new GenericStack(stack.what(),
                    saturatingMultiply(stack.amount(), NON_ITEM_QUANTITY_MULTIPLIER)));
        }
        return scaled;
    }

    private static List<SizedFluidIngredient> toSizedFluidIngredients(List<?> fluids) {
        if (fluids == null || fluids.isEmpty()) return List.of();
        List<SizedFluidIngredient> result = new ArrayList<>(fluids.size());
        for (Object value : fluids) {
            if (value instanceof SizedFluidIngredient ingredient) {
                if (ingredient.ingredient() != null && !ingredient.ingredient().isEmpty()
                        && ingredient.amount() > 0) {
                    result.add(ingredient);
                }
            } else if (value instanceof FluidStack stack) {
                SizedFluidIngredient converted = AdapterUtils.toSizedFluidIngredient(stack);
                if (converted != null) result.add(converted);
            }
        }
        return List.copyOf(result);
    }

    private static List<FluidStack> scaleOutputFluidStacks(List<FluidStack> stacks) {
        if (stacks.isEmpty()) return stacks;
        List<FluidStack> scaled = new ArrayList<>(stacks.size());
        for (FluidStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                scaled.add(stack);
                continue;
            }
            FluidStack copy = stack.copy();
            copy.setAmount(saturatingFluidAmount(copy.getAmount()));
            scaled.add(copy);
        }
        return scaled;
    }

    private static int saturatingFluidAmount(int amount) {
        if (amount <= 0) return amount;
        long maximum = Integer.MAX_VALUE;
        return amount > maximum / NON_ITEM_QUANTITY_MULTIPLIER
                ? Integer.MAX_VALUE
                : (int) (amount * NON_ITEM_QUANTITY_MULTIPLIER);
    }

    public static long saturatingAdd(long left, long right) {
        return right > 0L && left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    public static long saturatingMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) return 0L;
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    public static CoreRecipeHolder<MekanismSyntheticRecipe> syntheticHolder(
            ResourceLocation id, AdvancedAlloyFurnaceRecipe recipe) {
        return new CoreRecipeHolder<>(id, new MekanismSyntheticRecipe(recipe));
    }

    static boolean matchesConvertedRecipe(AdvancedAlloyFurnaceRecipe recipe,
                                          Map<Ingredient, Long> mergedInputs,
                                          Map<FluidStack, Long> mergedFluids,
                                          Map<AEKey, Long> mergedKeys) {
        if (recipe == null) return false;

        Map<Ingredient, Long> requiredItems = new LinkedHashMap<>();
        for (CountedIngredient input : recipe.inputs()) {
            AdapterUtils.mergeIngredient(requiredItems, input.ingredient(), input.count());
        }
        if (!AdapterUtils.matchesRequired(mergedInputs, requiredItems)) return false;

        if (!FluidIngredientAllocator.matchesLong(recipe.inputFluids(), mergedFluids, 1L)) return false;

        Map<AEKey, Long> requiredKeys = new LinkedHashMap<>();
        for (GenericStack required : recipe.keyInputs()) {
            if (required == null || required.what() == null || required.amount() <= 0L) continue;
            requiredKeys.merge(required.what(), required.amount(), MekanismChemicalRecipeSupport::saturatingAdd);
        }
        for (Map.Entry<AEKey, Long> required : requiredKeys.entrySet()) {
            if (mergedKeys.getOrDefault(required.getKey(), 0L) < required.getValue()) return false;
        }
        return true;
    }

}
