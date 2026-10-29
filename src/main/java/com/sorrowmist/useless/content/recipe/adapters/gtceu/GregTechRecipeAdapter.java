package com.sorrowmist.useless.content.recipe.adapters.gtceu;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.fluid.DataComponentFluidIngredient;
import com.sorrowmist.uselesscore.api.fluid.FluidIngredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

import java.lang.reflect.Field;
import java.util.*;

/** Optional GTM bridge. All GT-specific linkage stays behind reflective, checked boundaries. */
public final class GregTechRecipeAdapter implements IRecipeAdapter<GregTechSyntheticRecipe> {
    private record Cached(long generation, List<CoreRecipeHolder<GregTechSyntheticRecipe>> recipes) {}
    private record Machine(ItemStack stack, int tier) {}
    private record Part(String capability, Object value, int chance, int maximum, long ticks) {}
    private final Map<Object, Cached> cache = new WeakHashMap<>();

    @Override public String sourceId() { return "gtceu"; }
    @Override public Class<GregTechSyntheticRecipe> getRecipeClass() { return GregTechSyntheticRecipe.class; }
    @Override public ItemStack getMoldItem() { return null; }

    @Override public synchronized List<CoreRecipeHolder<GregTechSyntheticRecipe>> getGeneratedRecipes(Level level) {
        if (level == null) return List.of();
        long generation = AlloyFurnaceRecipeCatalog.generation();
        Cached previous = cache.get(level.getRecipeManager());
        if (previous != null && previous.generation == generation) return previous.recipes;
        List<CoreRecipeHolder<GregTechSyntheticRecipe>> result = new ArrayList<>();
        int skipped = 0;
        try {
            Map<Object, List<Machine>> machines = machineMolds();
            int fePerEU = fePerEU();
            long[] voltages = (long[]) gtValuesClass().getField("V").get(null);
            for (Recipe<?> source : level.getRecipeManager().getRecipes()) {
                if (!source.getClass().getName().equals("com.gregtechceu.gtceu.api.recipe.GTRecipe")) continue;
                try {
                    AdvancedAlloyFurnaceRecipe converted = convertSource(source, machines, voltages, fePerEU);
                    if (converted == null) { skipped++; continue; }
                    result.add(new CoreRecipeHolder<>(converted.id(), new GregTechSyntheticRecipe(converted)));
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    skipped++;
                    UselessMod.LOGGER.debug("Unsupported GTM recipe {}", source.getId(), exception);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            UselessMod.LOGGER.warn("Cannot read GTM recipe API; GTM conversion disabled for this recipe generation", exception);
        }
        List<CoreRecipeHolder<GregTechSyntheticRecipe>> recipes = List.copyOf(result);
        cache.put(level.getRecipeManager(), new Cached(generation, recipes));
        UselessMod.LOGGER.info("GTM alloy recipes: {} converted; {} with unsupported conditions or contents skipped", result.size(), skipped);
        return recipes;
    }

    private static AdvancedAlloyFurnaceRecipe convertSource(Recipe<?> source,
            Map<Object, List<Machine>> machines, long[] voltages, int fePerEU) throws ReflectiveOperationException {
        // Supported conditions are retained on the native recipe and checked at execution time.
        if (((Collection<?>) field(source, "conditions")).stream().anyMatch(condition ->
                    !com.sorrowmist.useless.compat.gtceu.GregTechEnvironment.supports(condition))
                || !((Collection<?>) field(source, "ingredientActions")).isEmpty()
                || !((CompoundTag) field(source, "data")).isEmpty()
                || optionalField(source, "outputModifier") != null) return null;
        Object independent = Class.forName("com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic").getField("OR").get(null);
        for (var mapping : Map.of("inputChanceLogics", "inputs", "outputChanceLogics", "outputs",
                "tickInputChanceLogics", "tickInputs", "tickOutputChanceLogics", "tickOutputs").entrySet()) {
            Map<?, ?> contents = (Map<?, ?>) field(source, mapping.getValue());
            for (var entry : ((Map<?, ?>) field(source, mapping.getKey())).entrySet()) {
                List<?> values = (List<?>) contents.get(entry.getKey());
                if (values == null || entry.getValue() == independent) continue;
                for (Object value : values) {
                    long chance = number(fieldOrAccessor(value, "chance"));
                    long maximum = number(fieldOrAccessor(value, "maxChance"));
                    if (chance > 0 && chance < maximum) return null;
                }
            }
        }
        int duration = ((Number) field(source, "duration")).intValue();
        if (duration <= 0) return null;
        List<Part> inputs = parts(source, "inputs", 1);
        inputs.addAll(parts(source, "tickInputs", duration));
        List<Part> outputs = parts(source, "outputs", 1);
        outputs.addAll(parts(source, "tickOutputs", duration));
        if (inputs.stream().anyMatch(p -> !Set.of("item", "fluid", "eu").contains(p.capability))
                || outputs.stream().anyMatch(p -> !Set.of("item", "fluid").contains(p.capability))) return null;
        long eut = 0, voltage = 0;
        for (Part part : inputs) {
            if (!part.capability.equals("eu")) continue;
            if (part.ticks != duration || part.chance != part.maximum) return null;
            long v, a;
            if (part.value instanceof Number energy) { v = energy.longValue(); a = 1; }
            else { v = number(call(part.value, "voltage")); a = number(call(part.value, "amperage")); }
            if (v < 0 || a < 0) return null;
            voltage = Math.max(voltage, v);
            eut = Math.addExact(eut, Math.multiplyExact(v, a));
        }
        int minTier = 0;
        while (minTier < voltages.length && voltage > voltages[minTier]) minTier++;
        List<Machine> candidates = machines.get(source.getType());
        if (candidates == null) return null;
        final int requiredTier = minTier;
        ItemStack[] machineChoices = candidates.stream().filter(m -> m.tier < 0 || m.tier >= requiredTier)
                .map(Machine::stack).toArray(ItemStack[]::new);
        if (machineChoices.length == 0) return null;
        List<Ingredient> molds = new ArrayList<>();
        molds.add(Ingredient.of(machineChoices));
        GregTechBatchPlan batch = GregTechBatchPlan.create(outputs.stream()
                .map(part -> new GregTechBatchPlan.Chance(part.chance, part.maximum, part.ticks)).toList());
        List<CountedIngredient> items = new ArrayList<>();
        List<LongSizedFluidIngredient> fluids = new ArrayList<>();
        for (Part input : inputs) {
            if (input.capability.equals("eu")) continue;
            if (input.chance != 0 && input.chance != input.maximum) return null;
            long factor = batch.inputFactor(input.ticks);
            if (input.capability.equals("item")) {
                Ingredient ingredient = (Ingredient) input.value;
                long amount = itemAmount(ingredient);
                if (amount <= 0 || ingredient.isEmpty()) return null;
                ingredient = unsized(ingredient);
                if (input.chance == 0) molds.add((Ingredient) input.value);
                else items.add(new CountedIngredient(ingredient, Math.multiplyExact(amount, factor)));
            } else {
                // Reusable fluids cannot be represented by the furnace's item mold slots.
                if (input.chance == 0) return null;
                FluidStack[] stacks = fluidStacks(input.value);
                int amount = fluidAmount(input.value);
                if (stacks.length == 0 || amount <= 0) return null;
                List<FluidIngredient> choices = new ArrayList<>();
                for (FluidStack stack : stacks) {
                    if (stack.isEmpty() || stack.getAmount() != amount) return null;
                    choices.add(DataComponentFluidIngredient.of(true, stack));
                }
                fluids.add(new LongSizedFluidIngredient(FluidIngredient.anyOf(choices), Math.multiplyExact(amount, factor)));
            }
        }
        List<ItemStack> itemOutputs = new ArrayList<>();
        List<FluidStack> fluidOutputs = new ArrayList<>();
        for (Part output : outputs) {
            if (output.chance == 0) continue;
            long factor = batch.outputFactor(new GregTechBatchPlan.Chance(output.chance, output.maximum, output.ticks));
            if (output.capability.equals("item")) {
                Ingredient ingredient = (Ingredient) output.value;
                ItemStack[] choices = ingredient.getItems();
                if (choices.length != 1) return null; // Ambiguous/tag outputs are not a deterministic product.
                itemOutputs.add(choices[0].copyWithCount(Math.toIntExact(Math.multiplyExact(itemAmount(ingredient), factor))));
            } else {
                FluidStack[] choices = fluidStacks(output.value);
                if (choices.length != 1) return null;
                FluidStack stack = choices[0].copy();
                stack.setAmount(Math.toIntExact(Math.multiplyExact(fluidAmount(output.value), factor)));
                fluidOutputs.add(stack);
            }
        }
        if (items.isEmpty() && fluids.isEmpty() || itemOutputs.isEmpty() && fluidOutputs.isEmpty()) return null;
        long energy = batch.energy(eut, duration, fePerEU);
        int time = batch.duration(duration);
        return new AdvancedAlloyFurnaceRecipe(AdapterUtils.convertedId(source.getId()), items, fluids, List.of(),
                itemOutputs, fluidOutputs, List.of(), energy, time, Ingredient.EMPTY, 0, molds, AlloyFurnaceMode.NORMAL, 0);
    }

    private static List<Part> parts(Object recipe, String mapName, long ticks) throws ReflectiveOperationException {
        Map<?, ?> map = (Map<?, ?>) field(recipe, mapName);
        List<Part> result = new ArrayList<>();
        for (var entry : map.entrySet()) {
            String name = (String) field(entry.getKey(), "name");
            for (Object content : (List<?>) entry.getValue()) {
                int chance = ((Number) fieldOrAccessor(content, "chance")).intValue();
                int maximum = ((Number) fieldOrAccessor(content, "maxChance")).intValue();
                if (maximum <= 0 || chance < 0 || chance > maximum) throw new IllegalArgumentException("Invalid GT chance");
                result.add(new Part(name, fieldOrAccessor(content, "content"), chance, maximum, ticks));
            }
        }
        return result;
    }

    private static Map<Object, List<Machine>> machineMolds() throws ReflectiveOperationException {
        Object registry = Class.forName("com.gregtechceu.gtceu.api.registry.GTRegistries").getField("MACHINES").get(null);
        Map<Object, List<Machine>> result = new IdentityHashMap<>();
        for (Object definition : (Collection<?>) call(registry, "values")) {
            try {
                // GT/GTL also register non-processing machines (hatches, storage, etc.).
                // Their recipe type array may be null, and is not a catalog-wide failure.
                Object[] types = (Object[]) call(definition, "getRecipeTypes");
                if (types == null || types.length == 0) continue;
                ItemStack stack = (ItemStack) call(definition, "asStack");
                if (stack == null || stack.isEmpty()) continue;
                int tier = ((Number) call(definition, "getTier")).intValue();
                for (Object type : types) {
                    if (type != null) result.computeIfAbsent(type, ignored -> new ArrayList<>()).add(new Machine(stack, tier));
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                UselessMod.LOGGER.warn("Cannot read GTM machine mold {}; skipping this machine only", definition, exception);
            }
        }
        return result;
    }

    private static Class<?> gtValuesClass() throws ClassNotFoundException {
        try { return Class.forName("com.gregtechceu.gtceu.api.GTValues"); }
        catch (ClassNotFoundException legacy) { return Class.forName("com.gregtechceu.gtceu.GTValues"); }
    }

    private static int fePerEU() throws ReflectiveOperationException {
        Object holder = Class.forName("com.gregtechceu.gtceu.config.ConfigHolder").getField("INSTANCE").get(null);
        int ratio = ((Number) field(field(field(holder, "compat"), "energy"), "feToEuRatio")).intValue();
        if (ratio <= 0) throw new IllegalArgumentException("Invalid GT FE/EU conversion ratio");
        return ratio;
    }

    private static long itemAmount(Ingredient ingredient) throws ReflectiveOperationException {
        String name = ingredient.getClass().getSimpleName();
        if (name.equals("IntProviderIngredient")) throw new IllegalArgumentException("Random-sized GT ingredient");
        if (name.equals("SizedIngredient")) return number(call(ingredient, "getAmount"));
        ItemStack[] choices = ingredient.getItems();
        return choices.length == 0 ? 0 : choices[0].getCount();
    }
    private static Ingredient unsized(Ingredient ingredient) throws ReflectiveOperationException {
        return ingredient.getClass().getSimpleName().equals("SizedIngredient") ? (Ingredient) call(ingredient, "getInner") : ingredient;
    }
    private static FluidStack[] fluidStacks(Object ingredient) throws ReflectiveOperationException {
        if (ingredient.getClass().getSimpleName().equals("IntProviderFluidIngredient")) throw new IllegalArgumentException("Random-sized GT fluid");
        return (FluidStack[]) call(ingredient, "getStacks");
    }
    private static int fluidAmount(Object ingredient) throws ReflectiveOperationException { return ((Number) call(ingredient, "getAmount")).intValue(); }
    private static long number(Object value) { return ((Number) value).longValue(); }
    private static Object optionalField(Object target, String name) throws ReflectiveOperationException {
        try { return field(target, name); } catch (NoSuchFieldException absent) { return null; }
    }
    private static Object field(Object target, String name) throws ReflectiveOperationException { return target.getClass().getField(name).get(target); }
    private static Object fieldOrAccessor(Object target, String name) throws ReflectiveOperationException {
        try { Field field = target.getClass().getField(name); return field.get(target); }
        catch (NoSuchFieldException absent) { return call(target, name); }
    }
    private static Object call(Object target, String name) throws ReflectiveOperationException { return target.getClass().getMethod(name).invoke(target); }

    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<GregTechSyntheticRecipe> holder, Level level) {
        return holder == null ? List.of() : List.of(holder.value().convertedRecipe());
    }
    @Override public List<CoreRecipeHolder<GregTechSyntheticRecipe>> findMatchingRecipes(Level level,
            Map<Ingredient, Long> items, Map<FluidStack, Long> fluids, ItemStack mold) {
        if (level == null) return List.of();
        return getGeneratedRecipes(level).stream().filter(holder -> {
            var recipe = holder.value().convertedRecipe();
            if (!AdapterUtils.matchesMold(recipe.mold(), mold)) return false;
            Map<Ingredient, Long> required = new LinkedHashMap<>();
            recipe.inputs().forEach(input -> AdapterUtils.mergeIngredient(required, input.ingredient(), input.count()));
            return AdapterUtils.matchesRequired(items, required) && FluidIngredientAllocator.matchesLong(recipe.inputFluids(), fluids, 1L);
        }).toList();
    }
}
