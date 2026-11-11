package com.sorrowmist.useless.content.recipe.adapters.embers;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
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

public final class EmbersRecipeAdapter implements IRecipeAdapter<Recipe<?>> {
    private enum Machine {
        MELTING("MeltingRecipe", "melter"),
        MIXING("MixingRecipe", "mixer_centrifuge"),
        STAMPING("StampingRecipe", "stamp_base"),
        BOILING("BoilingRecipe", "mini_boiler");

        final String recipeClass;
        final String mold;
        Machine(String recipeClass, String mold) {
            this.recipeClass = recipeClass;
            this.mold = mold;
        }
    }

    private final Machine machine;
    private final Class<Recipe<?>> recipeClass;

    @SuppressWarnings("unchecked")
    private EmbersRecipeAdapter(Machine machine) throws ClassNotFoundException {
        this.machine = machine;
        recipeClass = (Class<Recipe<?>>) Class.forName("com.rekindled.embers.recipe." + machine.recipeClass);
    }

    public static void register(java.util.function.Consumer<IRecipeAdapter<?>> register) {
        for (Machine machine : Machine.values()) {
            try {
                register.accept(new EmbersRecipeAdapter(machine));
            } catch (ClassNotFoundException | LinkageError unavailable) {
                UselessMod.LOGGER.warn("Embers recipe API unavailable for {}", machine.recipeClass);
            }
        }
    }

    @Override public String sourceId() { return "embers"; }
    @Override public Class<Recipe<?>> getRecipeClass() { return recipeClass; }
    @Override public ItemStack getMoldItem() { return item(machine.mold); }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<Recipe<?>> holder, Level level) {
        if (holder == null || holder.value().getClass() != recipeClass || getMoldItem().isEmpty()) return List.of();
        try {
            var recipe = holder.value();
            var inputs = new ArrayList<CountedIngredient>();
            var fluids = new ArrayList<LongSizedFluidIngredient>();
            var molds = new ArrayList<Ingredient>();
            molds.add(Ingredient.of(getMoldItem()));
            var fluidOutputs = new ArrayList<FluidStack>();
            var itemOutputs = new ArrayList<ItemStack>();
            switch (machine) {
                case MELTING -> inputs.add(itemInput(recipe));
                case MIXING -> {
                    for (Object input : (List<?>) call(recipe, "getDisplayInputFluids")) fluids.add(fluidInput(input));
                }
                case BOILING -> fluids.add(fluidInput(call(recipe, "getDisplayInput")));
                case STAMPING -> {
                    var input = (Ingredient) call(recipe, "getDisplayInput");
                    if (!input.isEmpty()) inputs.add(new CountedIngredient(input, 1));
                    var stamp = (Ingredient) call(recipe, "getDisplayStamp");
                    var stamper = item("stamper");
                    if (stamp.isEmpty() || stamper.isEmpty()) return List.of();
                    molds.add(Ingredient.of(stamper));
                    molds.add(stamp);
                    var fluid = call(recipe, "getDisplayInputFluid");
                    if (fluid != fluid.getClass().getField("EMPTY").get(null)) fluids.add(fluidInput(fluid));
                    ItemStack output = (ItemStack) call(recipe, "getResultItem");
                    if (output.isEmpty()) return List.of();
                    itemOutputs.add(output.copy());
                }
            }
            if (machine != Machine.STAMPING) {
                FluidStack output = (FluidStack) call(recipe, "getDisplayOutput");
                if (output.isEmpty()) return List.of();
                fluidOutputs.add(output.copy());
            }
            if (inputs.isEmpty() && fluids.isEmpty()) return List.of();
            ResourceLocation id = AdapterUtils.convertedId(holder.id());
            var regular = converted(id, inputs, fluids, itemOutputs, fluidOutputs, molds);
            if (machine == Machine.MELTING) {
                var bonus = (FluidStack) call(recipe, "getBonus");
                var separator = item("geologic_separator");
                if (!bonus.isEmpty() && !separator.isEmpty()) {
                    return List.of(regular, converted(id.withSuffix("_separator"), inputs, fluids,
                            itemOutputs, List.of(fluidOutputs.get(0), bonus.copy()),
                            List.of(molds.get(0), Ingredient.of(separator))));
                }
            }
            return List.of(regular);
        } catch (ReflectiveOperationException | RuntimeException unsupported) {
            UselessMod.LOGGER.debug("Cannot convert Embers recipe {}", holder.id(), unsupported);
            return List.of();
        }
    }

    @Override
    public List<CoreRecipeHolder<Recipe<?>>> findMatchingRecipes(Level level, Map<Ingredient, Long> items,
            Map<FluidStack, Long> fluids, ItemStack mold) {
        if (level == null || !matchesMold(mold)) return List.of();
        var matches = new ArrayList<CoreRecipeHolder<Recipe<?>>>();
        for (Recipe<?> source : level.getRecipeManager().getRecipes()) {
            if (source.getClass() != recipeClass) continue;
            var holder = new CoreRecipeHolder<Recipe<?>>(source.getId(), source);
            if (convertAll(holder, level).stream().anyMatch(recipe -> {
                Map<Ingredient, Long> required = new LinkedHashMap<>();
                recipe.inputs().forEach(input -> AdapterUtils.mergeIngredient(required, input.ingredient(), input.count()));
                return AdapterUtils.matchesRequired(items, required)
                        && FluidIngredientAllocator.matchesLong(recipe.inputFluids(), fluids, 1L);
            })) matches.add(holder);
        }
        return matches;
    }

    private static CountedIngredient itemInput(Recipe<?> recipe) throws ReflectiveOperationException {
        Ingredient input = (Ingredient) call(recipe, "getDisplayInput");
        if (input.isEmpty()) throw new IllegalArgumentException("Empty Embers item input");
        return new CountedIngredient(input, 1);
    }

    private static LongSizedFluidIngredient fluidInput(Object input) throws ReflectiveOperationException {
        var variants = new ArrayList<FluidIngredient>();
        int amount = 0;
        for (Object candidate : (List<?>) call(input, "getFluids")) {
            FluidStack stack = (FluidStack) candidate;
            if (stack.isEmpty()) continue;
            if (amount != 0 && amount != stack.getAmount()) {
                throw new IllegalArgumentException("Embers fluid alternatives require different amounts");
            }
            amount = stack.getAmount();
            variants.add(LongSizedFluidIngredient.from(stack).ingredient());
        }
        if (variants.isEmpty()) throw new IllegalArgumentException("Empty Embers fluid input");
        return new LongSizedFluidIngredient(FluidIngredient.anyOf(variants), amount);
    }

    private static AdvancedAlloyFurnaceRecipe converted(ResourceLocation id, List<CountedIngredient> items,
            List<LongSizedFluidIngredient> fluids, List<ItemStack> outputs, List<FluidStack> fluidOutputs,
            List<Ingredient> molds) {
        return new AdvancedAlloyFurnaceRecipe(id, List.copyOf(items), List.copyOf(fluids), List.of(),
                List.copyOf(outputs), List.copyOf(fluidOutputs), List.of(), AdapterUtils.DEFAULT_ENERGY,
                AdapterUtils.DEFAULT_PROCESS_TIME, Ingredient.EMPTY, 0, List.copyOf(molds), AlloyFurnaceMode.NORMAL);
    }

    private static Object call(Object value, String method) throws ReflectiveOperationException {
        var getter = value.getClass().getMethod(method);
        if (!getter.canAccess(value)) getter.setAccessible(true);
        return getter.invoke(value);
    }

    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("embers", path)));
    }
}
