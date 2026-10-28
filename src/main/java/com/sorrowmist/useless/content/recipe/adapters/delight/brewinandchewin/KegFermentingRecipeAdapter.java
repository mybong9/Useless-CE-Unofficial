package com.sorrowmist.useless.content.recipe.adapters.delight.brewinandchewin;

import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.CountedIngredient;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import com.sorrowmist.useless.content.recipe.LongSizedFluidIngredient;
import com.sorrowmist.useless.content.recipe.RecipeSourceIds;
import com.sorrowmist.useless.content.recipe.adapters.delight.DelightRecipeAdapterUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import com.sorrowmist.uselesscore.api.fluid.CompoundFluidIngredient;
import com.sorrowmist.uselesscore.api.fluid.FluidIngredient;
import org.jetbrains.annotations.Nullable;
import umpaz.brewinandchewin.common.crafting.KegFermentingRecipe;
import umpaz.brewinandchewin.common.crafting.KegPouringRecipe;
import umpaz.brewinandchewin.common.registry.BnCItems;
import umpaz.brewinandchewin.common.registry.BnCRecipeTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Converts Brewin And Chewin keg fermentation recipes. */
public final class KegFermentingRecipeAdapter implements IRecipeAdapter<KegFermentingRecipe> {
    @Override
    public String sourceId() {
        return RecipeSourceIds.BREWIN_AND_CHEWIN;
    }

    @Override
    public Class<KegFermentingRecipe> getRecipeClass() {
        return KegFermentingRecipe.class;
    }

    @Override
    public ItemStack getMoldItem() {
        return new ItemStack(BnCItems.KEG.get());
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(
            CoreRecipeHolder<KegFermentingRecipe> holder, Level level) {
        if (holder == null || holder.value() == null) {
            return List.of();
        }

        KegFermentingRecipe source = holder.value();
        List<CountedIngredient> inputs = AdapterUtils.mergeIngredients(source.getIngredients());
        List<LongSizedFluidIngredient> inputFluids = fluidInputs(source.getFluidIngredient());
        if (inputs.isEmpty() && inputFluids.isEmpty()) {
            return List.of();
        }

        List<ItemStack> outputs = new ArrayList<>();
        List<FluidStack> outputFluids = List.of();
        if (source.getResultItem() != net.minecraft.world.item.Items.AIR) {
            outputs.add(new ItemStack(source.getResultItem()));
        } else if (source.getResultFluid() != net.minecraft.world.level.material.Fluids.EMPTY) {
            BottledResult bottled = bottledResult(level, new FluidStack(source.getResultFluid(), source.getAmount()));
            if (bottled == null) return List.of();
            inputs = addContainer(inputs, bottled.container(), bottled.operations());
            outputs.add(bottled.output());
        }
        if (outputs.isEmpty() && outputFluids.isEmpty()) {
            return List.of();
        }

        int processTime = Math.max(1, source.getFermentTime());
        long energy = Math.max(1L,
                (long) processTime * AdapterUtils.DEFAULT_ENERGY / 200L);
        return List.of(new AdvancedAlloyFurnaceRecipe(
                AdapterUtils.convertedId(holder.id()),
                inputs,
                inputFluids,
                List.of(),
                outputs,
                outputFluids,
                List.of(),
                energy,
                processTime,
                Ingredient.EMPTY,
                0,
                List.of(AdapterUtils.toMoldIngredient(getMoldItem())),
                AlloyFurnaceMode.NORMAL
        ));
    }

    @Override
    public List<CoreRecipeHolder<KegFermentingRecipe>> findMatchingRecipes(
            Level level, Map<Ingredient, Long> mergedInputs,
            Map<FluidStack, Long> mergedFluids, @Nullable ItemStack mold) {
        if (level == null || !matchesMold(mold)
                || (mergedInputs == null || mergedInputs.isEmpty())
                && (mergedFluids == null || mergedFluids.isEmpty())) {
            return List.of();
        }

        List<CoreRecipeHolder<KegFermentingRecipe>> matches = new ArrayList<>();
        for (CoreRecipeHolder<KegFermentingRecipe> holder : DelightRecipeAdapterUtils.allOf(
                level.getRecipeManager(), KegFermentingRecipe.class)) {
            KegFermentingRecipe source = holder.value();
            List<CountedIngredient> itemRequirements =
                    AdapterUtils.mergeIngredients(source.getIngredients());
            if (source.getResultFluid() != net.minecraft.world.level.material.Fluids.EMPTY) {
                BottledResult bottled = bottledResult(level, new FluidStack(source.getResultFluid(), source.getAmount()));
                if (bottled == null) {
                    continue;
                }
                itemRequirements = addContainer(itemRequirements, bottled.container(), bottled.operations());
            }
            List<LongSizedFluidIngredient> fluidRequirements =
                    fluidInputs(source.getFluidIngredient());
            if (isMatch(itemRequirements, fluidRequirements, mergedInputs, mergedFluids)) {
                matches.add(holder);
            }
        }
        return List.copyOf(matches);
    }

    private static List<CountedIngredient> addContainer(List<CountedIngredient> inputs,
                                                         ItemStack container, int operations) {
        if (container == null || container.isEmpty() || operations <= 0) {
            return List.of();
        }
        Ingredient containerIngredient = Ingredient.of(container.copyWithCount(1));
        List<CountedIngredient> result = new ArrayList<>(inputs);
        for (int i = 0; i < result.size(); i++) {
            CountedIngredient existing = result.get(i);
            if (AdapterUtils.areIngredientsEqual(existing.ingredient(), containerIngredient)) {
                long count;
                try {
                    count = Math.addExact(existing.count(), operations);
                } catch (ArithmeticException exception) {
                    return List.of();
                }
                result.set(i, new CountedIngredient(existing.ingredient(), count));
                return List.copyOf(result);
            }
        }
        result.add(new CountedIngredient(containerIngredient, operations));
        return List.copyOf(result);
    }

    @Nullable
    private static BottledResult bottledResult(Level level, FluidStack fluid) {
        if (level == null || fluid == null || fluid.isEmpty()) {
            return null;
        }
        long available = fluid.getAmount();
        if (available <= 0L) {
            return null;
        }

        return com.sorrowmist.uselesscore.api.recipe.CoreRecipes.ofType(level.getRecipeManager(), BnCRecipeTypes.KEG_POURING.get()).stream()
                .map(CoreRecipeHolder::value)
                .sorted(Comparator.comparing(KegPouringRecipe::isStrict))
                .filter(pouring -> pouring.getRawFluid().isSame(fluid.getFluid()))
                .map(pouring -> {
                    long perContainer = pouring.getAmount();
                    if (perContainer <= 0L || available % perContainer != 0L) {
                        return null;
                    }
                    long operations = available / perContainer;
                    ItemStack container = pouring.getContainer();
                    ItemStack output = pouring.getOutput();
                    if (operations <= 0L || operations > Integer.MAX_VALUE
                            || container == null || container.isEmpty()
                            || output == null || output.isEmpty()) {
                        return null;
                    }
                    long outputCount = (long) output.getCount() * operations;
                    if (outputCount <= 0L || outputCount > Integer.MAX_VALUE) {
                        return null;
                    }
                    return new BottledResult(container.copyWithCount(1),
                            output.copyWithCount((int) outputCount), (int) operations);
                })
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static boolean isMatch(List<CountedIngredient> itemRequirements,
                                   List<LongSizedFluidIngredient> fluidRequirements,
                                   @Nullable Map<Ingredient, Long> mergedInputs,
                                   @Nullable Map<FluidStack, Long> mergedFluids) {
        return DelightRecipeAdapterUtils.matchesItems(
                itemRequirements, mergedInputs, List.of())
                && DelightRecipeAdapterUtils.matchesFluids(fluidRequirements, mergedFluids);
    }

    private static List<LongSizedFluidIngredient> fluidInputs(FluidStack source) {
        return source == null || source.isEmpty() ? List.of() : List.of(LongSizedFluidIngredient.from(source));
    }

    private record BottledResult(ItemStack container, ItemStack output, int operations) {
    }
}
