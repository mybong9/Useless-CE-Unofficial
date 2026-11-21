package com.sorrowmist.useless.content.recipe.adapters.integrateddynamics;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.CountedIngredient;
import com.sorrowmist.useless.content.recipe.ExpectedOutputScaler;
import com.sorrowmist.useless.content.recipe.FluidIngredientAllocator;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import com.sorrowmist.useless.content.recipe.LongSizedFluidIngredient;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class IntegratedDynamicsRecipeAdapter implements IRecipeAdapter<Recipe<?>> {
    private static final String MOD_ID = "integrateddynamics";
    private static final String RECIPE_PACKAGE = "org.cyclops.integrateddynamics.core.recipe.type.";

    private enum Machine {
        SQUEEZER("RecipeSqueezer", "squeezer", true, false),
        MECHANICAL_SQUEEZER("RecipeMechanicalSqueezer", "mechanical_squeezer", true, true),
        DRYING_BASIN("RecipeDryingBasin", "drying_basin", false, true),
        MECHANICAL_DRYING_BASIN("RecipeMechanicalDryingBasin", "mechanical_drying_basin", false, true);

        final String type;
        final String item;
        final boolean squeezer;
        final boolean timed;

        Machine(String type, String item, boolean squeezer, boolean timed) {
            this.type = type;
            this.item = item;
            this.squeezer = squeezer;
            this.timed = timed;
        }
    }

    private final Machine machine;
    private final Class<Recipe<?>> recipeClass;

    @SuppressWarnings("unchecked")
    private IntegratedDynamicsRecipeAdapter(Machine machine) throws ClassNotFoundException {
        this.machine = machine;
        this.recipeClass = (Class<Recipe<?>>) Class.forName(RECIPE_PACKAGE + machine.type);
    }

    public static void register(Consumer<IRecipeAdapter<?>> register) {
        for (Machine machine : Machine.values()) {
            try {
                register.accept(new IntegratedDynamicsRecipeAdapter(machine));
            } catch (ClassNotFoundException | LinkageError exception) {
                UselessMod.LOGGER.warn("Integrated Dynamics recipe type unavailable: {}", machine.type, exception);
            }
        }
    }

    @Override public String sourceId() { return MOD_ID; }
    @Override public Class<Recipe<?>> getRecipeClass() { return recipeClass; }
    @Override public ItemStack getMoldItem() {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(MOD_ID, machine.item)));
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<Recipe<?>> holder, Level level) {
        if (holder == null || holder.value().getClass() != recipeClass) return List.of();
        ItemStack mold = getMoldItem();
        if (mold.isEmpty()) return List.of();
        try {
            Recipe<?> recipe = holder.value();
            Ingredient input = (Ingredient) call(recipe, "getInputIngredient");
            FluidStack inputFluid = machine.squeezer ? FluidStack.EMPTY : fluid(recipe, "getInputFluid");
            FluidStack outputFluid = fluid(recipe, "getOutputFluid");
            var batch = itemOutputs(recipe);
            int runs = batch.operations();
            if ((input == null || input.isEmpty()) && inputFluid.isEmpty()) return List.of();
            if (batch.outputs().isEmpty() && outputFluid.isEmpty()) return List.of();

            List<CountedIngredient> inputs = input == null || input.isEmpty()
                    ? List.of() : List.of(new CountedIngredient(input, runs));
            List<LongSizedFluidIngredient> fluids = inputFluid.isEmpty() ? List.of()
                    : List.of(LongSizedFluidIngredient.from(scaled(inputFluid, runs)));
            List<FluidStack> outputs = outputFluid.isEmpty() ? List.of() : List.of(scaled(outputFluid, runs));
            int baseTime = machine.timed ? (int) call(recipe, "getDuration") : AdapterUtils.DEFAULT_PROCESS_TIME;
            int ticks = Math.multiplyExact(Math.max(1, baseTime), runs);
            long energy = Math.multiplyExact((long) AdapterUtils.DEFAULT_ENERGY, runs);
            return List.of(new AdvancedAlloyFurnaceRecipe(AdapterUtils.convertedId(holder.id()),
                    inputs, fluids, List.of(), batch.outputs(), outputs, List.of(), energy, ticks,
                    Ingredient.EMPTY, 0, List.of(Ingredient.of(mold)), AlloyFurnaceMode.NORMAL));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            UselessMod.LOGGER.debug("Cannot convert Integrated Dynamics recipe {}", holder.id(), exception);
            return List.of();
        }
    }

    private ExpectedOutputScaler.ScaledOutputs itemOutputs(Recipe<?> recipe) throws ReflectiveOperationException {
        if (!machine.squeezer) {
            ItemStack output = (ItemStack) call(recipe, "getOutputItemFirst");
            return new ExpectedOutputScaler.ScaledOutputs(1,
                    output == null || output.isEmpty() ? List.of() : List.of(output.copy()));
        }
        var weighted = new ArrayList<ExpectedOutputScaler.WeightedItemOutput>();
        for (Object entry : (Iterable<?>) call(recipe, "getOutputItems")) {
            ItemStack output = (ItemStack) call(entry, "getIngredientFirst");
            if (output == null || output.isEmpty()) continue;
            float chance = (float) call(entry, "getChance");
            weighted.add(new ExpectedOutputScaler.WeightedItemOutput(
                    output, output.getCount(), output.getCount(), chance));
        }
        return ExpectedOutputScaler.scale(weighted)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported output batch size"));
    }

    @Override
    public List<CoreRecipeHolder<Recipe<?>>> findMatchingRecipes(Level level, Map<Ingredient, Long> items,
            Map<FluidStack, Long> fluids, ItemStack mold) {
        if (level == null || !matchesMold(mold)) return List.of();
        var matches = new ArrayList<CoreRecipeHolder<Recipe<?>>>();
        for (Recipe<?> source : level.getRecipeManager().getRecipes()) {
            if (source.getClass() != recipeClass) continue;
            var holder = new CoreRecipeHolder<Recipe<?>>(source.getId(), source);
            for (var converted : convertAll(holder, level)) {
                var required = new java.util.LinkedHashMap<Ingredient, Long>();
                converted.inputs().forEach(input -> AdapterUtils.mergeIngredient(required, input.ingredient(), input.count()));
                if (AdapterUtils.matchesRequired(items, required)
                        && FluidIngredientAllocator.matchesLong(converted.inputFluids(), fluids, 1L)) {
                    matches.add(holder);
                }
            }
        }
        return matches;
    }

    private static Object call(Object recipe, String method) throws ReflectiveOperationException {
        return recipe.getClass().getMethod(method).invoke(recipe);
    }

    private static FluidStack fluid(Object recipe, String method) throws ReflectiveOperationException {
        FluidStack stack = (FluidStack) call(recipe, method);
        return stack == null ? FluidStack.EMPTY : stack;
    }

    private static FluidStack scaled(FluidStack stack, int runs) {
        FluidStack copy = stack.copy();
        copy.setAmount(Math.multiplyExact(stack.getAmount(), runs));
        return copy;
    }
}
