package com.sorrowmist.useless.content.recipe.adapters.thermal;

import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.fluid.DataComponentFluidIngredient;
import com.sorrowmist.uselesscore.api.fluid.FluidIngredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** Reads Thermal's data recipes, including scripts, without linking optional CoFH classes. */
public final class ThermalMachineRecipeAdapter implements IRecipeAdapter<ThermalSyntheticRecipe> {
    private static final Map<String, String> MACHINES = Map.ofEntries(
            Map.entry("FurnaceRecipe", "machine_furnace"), Map.entry("PulverizerRecipe", "machine_pulverizer"),
            Map.entry("PulverizerRecycleRecipe", "machine_pulverizer"), Map.entry("SawmillRecipe", "machine_sawmill"),
            Map.entry("SmelterRecipe", "machine_smelter"), Map.entry("SmelterRecycleRecipe", "machine_smelter"),
            Map.entry("InsolatorRecipe", "machine_insolator"), Map.entry("CentrifugeRecipe", "machine_centrifuge"),
            Map.entry("PressRecipe", "machine_press"), Map.entry("CrucibleRecipe", "machine_crucible"),
            Map.entry("ChillerRecipe", "machine_chiller"), Map.entry("RefineryRecipe", "machine_refinery"),
            Map.entry("PyrolyzerRecipe", "machine_pyrolyzer"), Map.entry("BrewerRecipe", "machine_brewer"),
            Map.entry("BottlerRecipe", "machine_bottler"), Map.entry("CrystallizerRecipe", "machine_crystallizer"));
    private static final Map<String, String> EXTRA_MACHINES = Map.of(
            "AdvancedRefineryRecipe", "advanced_refinery",
            "ComponentAssemblyRecipe", "component_assembly",
            "EndothermicDehydratorRecipe", "endothermic_dehydrator",
            "FluidMixerRecipe", "fluid_mixer",
            "NitraticIgniterRecipe", "nitratic_igniter");
    private final String namespace;
    private final String recipePackage;
    private final Map<String, String> machines;

    public ThermalMachineRecipeAdapter() { this(false); }

    public ThermalMachineRecipeAdapter(boolean extra) {
        namespace = extra ? "thermal_extra" : "thermal";
        recipePackage = extra ? "mrthomas20121.thermal_extra.recipe." : "cofh.thermal.core.util.recipes.machine.";
        machines = extra ? EXTRA_MACHINES : MACHINES;
    }

    private record Cached(long generation, List<CoreRecipeHolder<ThermalSyntheticRecipe>> recipes) {}
    private final Map<Object, Cached> cache = new WeakHashMap<>();
    @Override public String sourceId() { return namespace; }
    @Override public Class<ThermalSyntheticRecipe> getRecipeClass() { return ThermalSyntheticRecipe.class; }
    @Override public ItemStack getMoldItem() { return null; }
    @Override public boolean matchesMold(ItemStack mold) {
        if (mold == null || mold.isEmpty()) return false;
        var id = BuiltInRegistries.ITEM.getKey(mold.getItem());
        return id.getNamespace().equals(namespace) && machines.containsValue(id.getPath());
    }
    private static Object call(Object object, String method) throws ReflectiveOperationException {
        return object.getClass().getMethod(method).invoke(object);
    }
    @Override public synchronized List<CoreRecipeHolder<ThermalSyntheticRecipe>> getGeneratedRecipes(Level level) {
        if (level == null) return List.of();
        long generation = AlloyFurnaceRecipeCatalog.generation();
        Cached cached = cache.get(level.getRecipeManager());
        if (cached != null && cached.generation() == generation) return cached.recipes();
        List<CoreRecipeHolder<ThermalSyntheticRecipe>> result = new ArrayList<>();
        for (Recipe<?> source : level.getRecipeManager().getRecipes()) {
            if (!source.getClass().getName().startsWith(recipePackage)) continue;
            String machine = machines.get(source.getClass().getSimpleName());
            if (machine == null) continue; // Crafting/fuel declarations are not machine transformations.
            try {
                List<Ingredient> molds = new ArrayList<>();
                ItemStack mold = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(namespace, machine)));
                if (mold.isEmpty()) continue;
                molds.add(Ingredient.of(mold));
                Map<Ingredient, Long> counts = new LinkedHashMap<>();
                for (Object value : (List<?>) call(source, "getInputItems")) {
                    Ingredient ingredient = (Ingredient) value;
                    long count = 1L;
                    if (ingredient.getClass().getName().equals("cofh.lib.util.crafting.IngredientWithCount")) {
                        var field = ingredient.getClass().getDeclaredField("count");
                        if (!field.trySetAccessible()) throw new IllegalArgumentException("Cannot read Thermal input count");
                        count = ((Number) field.get(ingredient)).longValue();
                        // CoFH's JSON retains the wrapped predicate, without its size gate.
                        ingredient = Ingredient.fromJson(ingredient.toJson());
                    }
                    if (ingredient.isEmpty() || count < 0) throw new IllegalArgumentException("Invalid Thermal item input");
                    if (count == 0) molds.add(ingredient); // Press dies and reusable casts.
                    else AdapterUtils.mergeIngredient(counts, ingredient, count);
                }
                List<?> outputItems = (List<?>) call(source, "getOutputItems");
                List<?> chances = (List<?>) call(source, "getOutputItemChances");
                long operations = 1L;
                List<Long> numerators = new ArrayList<>(), denominators = new ArrayList<>();
                for (int i = 0; i < outputItems.size(); i++) {
                    double chance = i < chances.size() ? ((Number) chances.get(i)).doubleValue() : 1.0;
                    // Thermal uses negative chances for outputs unaffected by catalyst
                    // multipliers. BaseMachineRecipe evaluates those using abs(), and
                    // ThermalRecipe fills unspecified chances with -1 (guaranteed).
                    if (!Double.isFinite(chance)) throw new IllegalArgumentException("Invalid Thermal output chance");
                    chance = Math.abs(chance);
                    if (chance > Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid Thermal output chance");
                    long numerator = Math.multiplyExact(Math.round(chance * 1000.0), ((ItemStack) outputItems.get(i)).getCount());
                    long gcd = AdapterUtils.gcd(numerator, 1000L);
                    long denominator = 1000L / gcd;
                    numerators.add(numerator / gcd); denominators.add(denominator);
                    operations = Math.multiplyExact(operations / AdapterUtils.gcd(operations, denominator), denominator);
                }
                List<CountedIngredient> inputs = new ArrayList<>();
                for (var input : counts.entrySet()) inputs.add(new CountedIngredient(input.getKey(), Math.multiplyExact(input.getValue(), operations)));
                List<ItemStack> outputs = new ArrayList<>();
                for (int i = 0; i < outputItems.size(); i++) {
                    long amount = Math.multiplyExact(numerators.get(i), operations / denominators.get(i));
                    if (amount > 0) outputs.add(((ItemStack) outputItems.get(i)).copyWithCount(Math.toIntExact(amount)));
                }
                List<LongSizedFluidIngredient> fluids = new ArrayList<>();
                for (Object value : (List<?>) call(source, "getInputFluids")) {
                    FluidStack[] alternatives = (FluidStack[]) call(value, "getFluids");
                    if (alternatives.length == 0 || alternatives[0].isEmpty()) throw new IllegalArgumentException("Empty Thermal fluid input");
                    int amount = alternatives[0].getAmount();
                    List<FluidIngredient> choices = new ArrayList<>();
                    for (FluidStack alternative : alternatives) {
                        if (alternative.isEmpty() || alternative.getAmount() != amount) throw new IllegalArgumentException("Inconsistent Thermal fluid quantity");
                        choices.add(DataComponentFluidIngredient.of(true, alternative));
                    }
                    fluids.add(new LongSizedFluidIngredient(FluidIngredient.anyOf(choices), Math.multiplyExact(amount, operations)));
                }
                List<FluidStack> fluidOutputs = new ArrayList<>();
                for (Object value : (List<?>) call(source, "getOutputFluids")) {
                    FluidStack output = ((FluidStack) value).copy();
                    output.setAmount(Math.toIntExact(Math.multiplyExact(output.getAmount(), operations)));
                    if (!output.isEmpty()) fluidOutputs.add(output);
                }
                if (inputs.isEmpty() && fluids.isEmpty() || outputs.isEmpty() && fluidOutputs.isEmpty()) continue;
                long energy = Math.multiplyExact(((Number) call(source, "getEnergy")).longValue(), operations);
                int ticks = Math.toIntExact(Math.multiplyExact(AdapterUtils.DEFAULT_PROCESS_TIME, operations));
                if (energy < 0 || ticks <= 0) continue;
                var converted = new AdvancedAlloyFurnaceRecipe(AdapterUtils.convertedId(source.getId()), inputs, fluids,
                        List.of(), outputs, fluidOutputs, List.of(), energy, ticks, Ingredient.EMPTY, 0, molds, AlloyFurnaceMode.NORMAL, 0);
                result.add(new CoreRecipeHolder<>(converted.id(), new ThermalSyntheticRecipe(converted)));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                com.sorrowmist.useless.UselessMod.LOGGER.warn("Cannot convert Thermal machine recipe {}", source.getId(), exception);
            }
        }
        var recipes = List.copyOf(result);
        cache.put(level.getRecipeManager(), new Cached(generation, recipes));
        return recipes;
    }
    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<ThermalSyntheticRecipe> holder, Level level) {
        return holder == null ? List.of() : List.of(holder.value().convertedRecipe());
    }
    @Override public List<CoreRecipeHolder<ThermalSyntheticRecipe>> findMatchingRecipes(Level level,
            Map<Ingredient, Long> items, Map<FluidStack, Long> fluids, ItemStack mold) {
        if (level == null || !matchesMold(mold)) return List.of();
        return getGeneratedRecipes(level).stream().filter(holder -> {
            var recipe = holder.value().convertedRecipe();
            // Additional molds are reusable press dies/casts, checked by the furnace;
            // their presence must not hide the recipe from the machine-mold lookup.
            if (!AdapterUtils.matchesMold(recipe.mold(), mold)) return false;
            Map<Ingredient, Long> counts = new LinkedHashMap<>();
            recipe.inputs().forEach(input -> AdapterUtils.mergeIngredient(counts, input.ingredient(), input.count()));
            return AdapterUtils.matchesRequired(items, counts) && FluidIngredientAllocator.matchesLong(recipe.inputFluids(), fluids, 1L);
        }).toList();
    }
}
