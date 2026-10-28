package com.sorrowmist.useless.content.recipe.adapters.crazyae;

import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import com.sorrowmist.uselesscore.api.recipe.CoreDataComponentIngredient;
import com.sorrowmist.uselesscore.api.fluid.FluidIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** Optional API access for Crazy AE2 Addons' three server recipe types. */
public final class CrazyRecipeAdapter implements IRecipeAdapter<CrazySyntheticRecipe> {
    private record Cached(long generation, List<CoreRecipeHolder<CrazySyntheticRecipe>> recipes) {}
    private final Map<Object, Cached> generated = new WeakHashMap<>();
    private static final String MOD = "crazyae2addons";
    @Override public String sourceId() { return MOD; }
    @Override public Class<CrazySyntheticRecipe> getRecipeClass() { return CrazySyntheticRecipe.class; }
    @Override public ItemStack getMoldItem() { return null; }
    @Override public boolean matchesMold(ItemStack mold) {
        if (mold == null || mold.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(mold.getItem());
        return id.getNamespace().equals(MOD) && Set.of("recipe_fabricator", "research_station", "entropy_cradle_controller").contains(id.getPath());
    }
    private static Object call(Object target, String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }
    private static Object field(Object target, String field) throws ReflectiveOperationException {
        return target.getClass().getField(field).get(target);
    }
    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(MOD, path)));
    }
    private static Object config(String key) throws ReflectiveOperationException {
        Object common = Class.forName("net.oktawia.crazyae2addons.CrazyConfig").getField("COMMON").get(null);
        return ((net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?>) field(common, key)).get();
    }
    private static ItemStack keyedDrive(String key) {
        ItemStack drive = item("data_drive");
        if (!drive.isEmpty()) {
            ListTag keys = new ListTag(); keys.add(StringTag.valueOf(key));
            drive.getOrCreateTag().put("keys", keys);
        }
        return drive;
    }
    @Override public synchronized List<CoreRecipeHolder<CrazySyntheticRecipe>> getGeneratedRecipes(Level level) {
        if (level == null) return List.of();
        long generation = AlloyFurnaceRecipeCatalog.generation();
        Cached cached = generated.get(level.getRecipeManager());
        if (cached != null && cached.generation() == generation) return cached.recipes();
        List<CoreRecipeHolder<CrazySyntheticRecipe>> result = new ArrayList<>();
        for (Recipe<?> source : level.getRecipeManager().getRecipes()) {
            String className = source.getClass().getName();
            if (!className.startsWith("net.oktawia.crazyae2addons.recipes.")) continue;
            try {
                List<CountedIngredient> inputs = new ArrayList<>();
                List<LongSizedFluidIngredient> fluids = new ArrayList<>();
                List<ItemStack> outputs = new ArrayList<>();
                List<FluidStack> fluidOutputs = new ArrayList<>();
                List<Ingredient> molds = new ArrayList<>();
                long energy = 0; int ticks = 10;
                if (className.endsWith(".FabricationRecipe")) {
                    for (Object entry : (List<?>) call(source, "getInputs"))
                        inputs.add(new CountedIngredient((Ingredient) call(entry, "ingredient"), ((Number) call(entry, "count")).longValue()));
                    outputs.add(((ItemStack) call(source, "getOutput")).copy());
                    FluidStack fluid = (FluidStack) call(source, "getFluidInput");
                    if (!fluid.isEmpty()) fluids.add(new LongSizedFluidIngredient(
                            com.sorrowmist.uselesscore.api.fluid.DataComponentFluidIngredient.of(true, fluid), fluid.getAmount()));
                    FluidStack out = (FluidStack) call(source, "getFluidOutput");
                    if (!out.isEmpty()) fluidOutputs.add(out.copy());
                    molds.add(Ingredient.of(item("recipe_fabricator")));
                    String key = (String) call(source, "getRequiredKey");
                    if (Boolean.TRUE.equals(config("ResearchRequired")) && key != null && !key.isBlank())
                        molds.add(CoreDataComponentIngredient.of(false, keyedDrive(key)));
                } else if (className.endsWith(".ResearchRecipe")) {
                    for (Object entry : (List<?>) field(source, "consumables"))
                        inputs.add(new CountedIngredient(Ingredient.of((Item) field(entry, "item")), ((Number) field(entry, "count")).longValue()));
                    ticks = ((Number) field(source, "duration")).intValue();
                    energy = ((Number) call(source, "totalEnergy")).longValue();
                    Object unlock = field(source, "unlock");
                    String key = field(unlock, "key").toString();
                    ItemStack blank = item("data_drive");
                    // The source's unlock.item is a JEI preview, not a physical output.
                    // Research writes keys; require a blank drive and produce that same data.
                    inputs.add(new CountedIngredient(CoreDataComponentIngredient.of(true, blank), 1));
                    outputs.add(keyedDrive(key));
                    molds.add(Ingredient.of(item("research_station")));
                } else if (className.endsWith(".CradleRecipe")) {
                    Object pattern = call(source, "pattern");
                    Map<?, ?> symbols = (Map<?, ?>) call(pattern, "symbolMap");
                    Map<String, Long> counts = new LinkedHashMap<>();
                    for (Object layer : (List<?>) call(pattern, "layers"))
                        for (String[] row : (String[][]) layer)
                            for (String symbol : row) if (!symbol.equals(".")) counts.merge(symbol, 1L, Math::addExact);
                    for (var entry : counts.entrySet()) {
                        List<?> choices = (List<?>) symbols.get(entry.getKey());
                        if (choices == null || choices.isEmpty()) throw new IllegalArgumentException("Missing cradle symbol");
                        List<ItemStack> stacks = new ArrayList<>();
                        for (Object block : choices) {
                            ItemStack stack = new ItemStack((Block) block);
                            if (!stack.isEmpty()) stacks.add(stack);
                        }
                        if (stacks.isEmpty()) throw new IllegalArgumentException("Cradle requires unobtainable block");
                        inputs.add(new CountedIngredient(Ingredient.of(stacks.stream()), entry.getValue()));
                    }
                    outputs.add(new ItemStack((Block) call(source, "resultBlock")));
                    molds.add(Ingredient.of(item("entropy_cradle_controller")));
                    energy = ((Number) config("CradleCost")).longValue(); ticks = 1;
                } else continue;
                if (inputs.isEmpty() || outputs.stream().anyMatch(ItemStack::isEmpty) || ticks <= 0 || energy < 0
                        || molds.stream().anyMatch(Ingredient::isEmpty)) continue;
                AdvancedAlloyFurnaceRecipe converted = new AdvancedAlloyFurnaceRecipe(
                        AdapterUtils.convertedId(source.getId()), inputs, fluids, List.of(), outputs, fluidOutputs,
                        List.of(), energy, ticks, Ingredient.EMPTY, 0, molds, AlloyFurnaceMode.NORMAL, 0);
                result.add(new CoreRecipeHolder<>(converted.id(), new CrazySyntheticRecipe(converted)));
            } catch (ReflectiveOperationException | RuntimeException e) {
                com.sorrowmist.useless.UselessMod.LOGGER.warn("Cannot convert Crazy AE2 recipe {}", source.getId(), e);
            }
        }
        var immutable = List.copyOf(result);
        generated.put(level.getRecipeManager(), new Cached(generation, immutable));
        return immutable;
    }
    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<CrazySyntheticRecipe> holder, Level level) {
        return holder == null ? List.of() : List.of(holder.value().convertedRecipe());
    }
    @Override public List<CoreRecipeHolder<CrazySyntheticRecipe>> findMatchingRecipes(Level level,
            Map<Ingredient, Long> inputs, Map<FluidStack, Long> fluids, ItemStack mold) {
        if (level == null || !matchesMold(mold)) return List.of();
        return getGeneratedRecipes(level).stream().filter(h -> {
            var r = h.value().convertedRecipe();
            // Multi-mold research-gated recipes are dispatched by the multiblock catalog.
            if (r.molds().size() != 1 || !AdapterUtils.matchesMold(r.mold(), mold)) return false;
            Map<Ingredient, Long> required = new LinkedHashMap<>();
            r.inputs().forEach(i -> AdapterUtils.mergeIngredient(required, i.ingredient(), i.count()));
            return AdapterUtils.matchesRequired(inputs, required);
        }).toList();
    }
}
