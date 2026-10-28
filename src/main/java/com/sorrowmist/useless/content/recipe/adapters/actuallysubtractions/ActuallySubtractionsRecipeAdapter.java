package com.sorrowmist.useless.content.recipe.adapters.actuallysubtractions;

import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.fluid.DataComponentFluidIngredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** Optional 1.20.1 Actually Subtractions recipe access, without a hard dependency. */
public final class ActuallySubtractionsRecipeAdapter implements IRecipeAdapter<ActuallySubtractionsSyntheticRecipe> {
    private static final String MOD = "actuallyadditions";
    private record Cached(long generation, List<CoreRecipeHolder<ActuallySubtractionsSyntheticRecipe>> recipes) {}
    private final Map<Object, Cached> cache = new WeakHashMap<>();
    @Override public String sourceId() { return MOD; }
    @Override public Class<ActuallySubtractionsSyntheticRecipe> getRecipeClass() { return ActuallySubtractionsSyntheticRecipe.class; }
    @Override public ItemStack getMoldItem() { return null; }
    private static ItemStack item(String name) {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(MOD, name)));
    }
    @Override public boolean matchesMold(ItemStack mold) {
        if (mold == null || mold.isEmpty()) return false;
        var id = BuiltInRegistries.ITEM.getKey(mold.getItem());
        return MOD.equals(id.getNamespace()) && Set.of("atomic_reconstructor", "empowerer", "crusher",
                "crusher_double", "canola_press", "fermenting_barrel").contains(id.getPath());
    }
    private static Object call(Object source, String name) throws ReflectiveOperationException {
        return source.getClass().getMethod(name).invoke(source);
    }
    private static long number(Object source, String name) throws ReflectiveOperationException {
        return ((Number) call(source, name)).longValue();
    }
    @Override public synchronized List<CoreRecipeHolder<ActuallySubtractionsSyntheticRecipe>> getGeneratedRecipes(Level level) {
        if (level == null) return List.of();
        long generation = AlloyFurnaceRecipeCatalog.generation();
        Cached cached = cache.get(level.getRecipeManager());
        if (cached != null && cached.generation() == generation) return cached.recipes();
        List<CoreRecipeHolder<ActuallySubtractionsSyntheticRecipe>> result = new ArrayList<>();
        for (Recipe<?> source : level.getRecipeManager().getRecipes()) {
            if (!source.getClass().getName().startsWith("de.ellpeck.actuallyadditions.mod.crafting.")) continue;
            try {
                Map<Ingredient, Long> inputMap = new LinkedHashMap<>();
                List<LongSizedFluidIngredient> fluids = new ArrayList<>();
                List<ItemStack> outputs = new ArrayList<>();
                List<FluidStack> fluidOutputs = new ArrayList<>();
                List<Ingredient> molds = new ArrayList<>();
                long energy = 0; int ticks = AdapterUtils.DEFAULT_PROCESS_TIME;
                String type = source.getClass().getSimpleName();
                switch (type) {
                    case "LaserRecipe" -> {
                        inputMap.put((Ingredient) call(source, "getInput"), 1L);
                        outputs.add(source.getResultItem(level.registryAccess()).copy());
                        energy = number(source, "getEnergy");
                        molds.add(Ingredient.of(item("atomic_reconstructor")));
                    }
                    case "EmpowererRecipe" -> {
                        for (String getter : List.of("getInput", "getStandOne", "getStandTwo", "getStandThree", "getStandFour")) {
                            Ingredient input = (Ingredient) call(source, getter);
                            if (input == null || input.isEmpty()) throw new IllegalArgumentException("Empty empowering input");
                            AdapterUtils.mergeIngredient(inputMap, input, 1L);
                        }
                        outputs.add(((ItemStack) call(source, "getOutput")).copy());
                        energy = Math.multiplyExact(number(source, "getEnergyPerStand"), 4L);
                        ticks = Math.toIntExact(number(source, "getTime"));
                        molds.add(Ingredient.of(item("empowerer")));
                    }
                    case "CrushingRecipe" -> {
                        List<ExpectedOutputScaler.WeightedItemOutput> weighted = new ArrayList<>();
                        for (String suffix : List.of("One", "Two")) {
                            ItemStack output = ((ItemStack) call(source, "getOutput" + suffix)).copy();
                            if (output.isEmpty()) continue;
                            double chance = ((Number) call(source, suffix.equals("One") ? "getFirstChance" : "getSecondChance")).doubleValue();
                            weighted.add(new ExpectedOutputScaler.WeightedItemOutput(output,
                                    output.getCount(), output.getCount(), chance));
                        }
                        var scaled = ExpectedOutputScaler.scale(weighted).orElseThrow();
                        int operations = scaled.operations();
                        inputMap.put((Ingredient) call(source, "getInput"), (long) operations);
                        outputs.addAll(scaled.outputs());
                        ticks = Math.multiplyExact(100, operations);
                        energy = Math.multiplyExact(40L, ticks);
                        molds.add(Ingredient.of(item("crusher"), item("crusher_double")));
                    }
                    case "PressingRecipe" -> {
                        inputMap.put((Ingredient) call(source, "getInput"), 1L);
                        fluidOutputs.add(((FluidStack) call(source, "getOutput")).copy());
                        ticks = 30; energy = 35L * ticks;
                        molds.add(Ingredient.of(item("canola_press")));
                    }
                    case "FermentingRecipe" -> {
                        FluidStack input = (FluidStack) call(source, "getInput");
                        if (input.isEmpty()) continue;
                        fluids.add(new LongSizedFluidIngredient(DataComponentFluidIngredient.of(true, input), input.getAmount()));
                        fluidOutputs.add(((FluidStack) call(source, "getOutput")).copy());
                        ticks = Math.toIntExact(number(source, "getTime"));
                        molds.add(Ingredient.of(item("fermenting_barrel")));
                    }
                    case "ColorChangeRecipe" -> {
                        var field = source.getClass().getDeclaredField("input");
                        if (!field.trySetAccessible()) continue;
                        inputMap.put((Ingredient) field.get(source), 1L);
                        outputs.add(source.getResultItem(level.registryAccess()).copy());
                        energy = 200;
                        molds.add(Ingredient.of(item("atomic_reconstructor")));
                        molds.add(Ingredient.of(item("lens_of_color")));
                    }
                    default -> { continue; } // Fuel, coffee effects and world mining aren't machine transformations.
                }
                if (inputMap.isEmpty() && fluids.isEmpty() || outputs.isEmpty() && fluidOutputs.isEmpty()
                        || ticks <= 0 || energy < 0 || molds.stream().anyMatch(Ingredient::isEmpty)
                        || inputMap.entrySet().stream().anyMatch(e -> e.getKey() == null || e.getKey().isEmpty() || e.getValue() <= 0)
                        || outputs.stream().anyMatch(ItemStack::isEmpty) || fluidOutputs.stream().anyMatch(FluidStack::isEmpty)) continue;
                var inputs = inputMap.entrySet().stream().map(e -> new CountedIngredient(e.getKey(), e.getValue())).toList();
                var converted = new AdvancedAlloyFurnaceRecipe(AdapterUtils.convertedId(source.getId()),
                        inputs, fluids, List.of(), outputs, fluidOutputs, List.of(), energy, ticks,
                        Ingredient.EMPTY, 0, molds, AlloyFurnaceMode.NORMAL, 0);
                result.add(new CoreRecipeHolder<>(converted.id(), new ActuallySubtractionsSyntheticRecipe(converted)));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                com.sorrowmist.useless.UselessMod.LOGGER.warn("Cannot convert Actually Subtractions recipe {}", source.getId(), exception);
            }
        }
        var recipes = List.copyOf(result);
        cache.put(level.getRecipeManager(), new Cached(generation, recipes));
        return recipes;
    }
    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<ActuallySubtractionsSyntheticRecipe> holder, Level level) {
        return holder == null ? List.of() : List.of(holder.value().convertedRecipe());
    }
    @Override public List<CoreRecipeHolder<ActuallySubtractionsSyntheticRecipe>> findMatchingRecipes(Level level,
            Map<Ingredient, Long> inputs, Map<FluidStack, Long> fluids, ItemStack mold) {
        if (level == null || !matchesMold(mold)) return List.of();
        return getGeneratedRecipes(level).stream().filter(holder -> {
            var recipe = holder.value().convertedRecipe();
            if (recipe.molds().size() != 1 || !AdapterUtils.matchesMold(recipe.mold(), mold)) return false;
            Map<Ingredient, Long> required = new LinkedHashMap<>();
            recipe.inputs().forEach(i -> AdapterUtils.mergeIngredient(required, i.ingredient(), i.count()));
            return AdapterUtils.matchesRequired(inputs, required)
                    && FluidIngredientAllocator.matchesLong(recipe.inputFluids(), fluids, 1L);
        }).toList();
    }
}
