package com.sorrowmist.useless.content.recipe.adapters.jdtforge;

import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** Forge 1.20.1 compatibility, without linking optional JDT classes at startup. */
public final class JdtForgeRecipeAdapter implements IRecipeAdapter<JdtSyntheticRecipe> {
    private static final String PREFIX = "com.direwolf20.justdirethings.datagen.recipes.";
    private static final Set<String> TYPES = Set.of("GooSpreadRecipe", "GooSpreadRecipeTag", "FluidDropRecipe");
    private record Cached(long generation, List<CoreRecipeHolder<JdtSyntheticRecipe>> recipes) {}
    private final Map<Object, Cached> cache = new WeakHashMap<>();
    @Override public String sourceId() { return "justdirethings"; }
    @Override public Class<JdtSyntheticRecipe> getRecipeClass() { return JdtSyntheticRecipe.class; }
    @Override public ItemStack getMoldItem() { return null; }
    private static Object call(Object recipe, String name) throws ReflectiveOperationException {
        return recipe.getClass().getMethod(name).invoke(recipe);
    }
    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("justdirethings", path)));
    }
    private static Ingredient gooMold(int tier) {
        if (tier < 1 || tier > 4) return Ingredient.EMPTY;
        List<ItemStack> molds = new ArrayList<>();
        for (int i = tier; i <= 4; i++) {
            ItemStack stack = item("gooblock_tier" + i);
            if (!stack.isEmpty()) molds.add(stack);
        }
        return Ingredient.of(molds.stream());
    }
    private static List<LongSizedFluidIngredient> fluidInput(BlockState state) {
        var fluid = state.getFluidState().getType();
        return fluid == Fluids.EMPTY ? List.of() : List.of(LongSizedFluidIngredient.from(new FluidStack(fluid, 1000)));
    }
    private static ItemStack blockOutput(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id.getNamespace().equals("justdirethings")) {
            String path = switch (id.getPath()) {
                case "raw_ferricore_ore" -> "raw_ferricore";
                case "raw_blazegold_ore" -> "raw_blazegold";
                case "raw_celestigem_ore" -> "celestigem";
                case "raw_eclipsealloy_ore" -> "raw_eclipsealloy";
                case "raw_coal_t1_ore" -> "coal_t1";
                case "raw_coal_t2_ore" -> "coal_t2";
                case "raw_coal_t3_ore" -> "coal_t3";
                case "raw_coal_t4_ore" -> "coal_t4";
                default -> null;
            };
            if (path != null) {
                ItemStack output = item(path);
                if (!output.isEmpty()) { output.setCount(4); return output; }
            }
        }
        return new ItemStack(state.getBlock().asItem());
    }
    @Override public synchronized List<CoreRecipeHolder<JdtSyntheticRecipe>> getGeneratedRecipes(Level level) {
        if (level == null) return List.of();
        long generation = AlloyFurnaceRecipeCatalog.generation();
        Cached existing = cache.get(level.getRecipeManager());
        if (existing != null && existing.generation() == generation) return existing.recipes();
        List<CoreRecipeHolder<JdtSyntheticRecipe>> result = new ArrayList<>();
        for (Recipe<?> source : level.getRecipeManager().getRecipes()) {
            String kind = source.getClass().getSimpleName();
            if (!TYPES.contains(kind) || !source.getClass().getName().equals(PREFIX + kind)) continue;
            try {
                List<CountedIngredient> inputs = new ArrayList<>();
                List<LongSizedFluidIngredient> fluids = new ArrayList<>();
                Ingredient mold;
                int ticks;
                if (kind.equals("FluidDropRecipe")) {
                    Item catalyst = (Item) call(source, "getCatalyst");
                    if (catalyst == null || catalyst == Items.AIR) continue;
                    inputs.add(new CountedIngredient(Ingredient.of(catalyst), 1L));
                    fluids.addAll(fluidInput((BlockState) call(source, "getInput")));
                    if (fluids.isEmpty()) continue;
                    mold = Ingredient.of(catalyst);
                    ticks = AdapterUtils.DEFAULT_PROCESS_TIME;
                } else {
                    mold = gooMold(((Number) call(source, "getTierRequirement")).intValue());
                    ticks = Math.max(1, ((Number) call(source, "getCraftingDuration")).intValue());
                    Ingredient input;
                    if (kind.equals("GooSpreadRecipeTag")) {
                        @SuppressWarnings("unchecked")
                        TagKey<Block> tag = (TagKey<Block>) call(source, "getInput");
                        input = Ingredient.of(BuiltInRegistries.BLOCK.getTag(tag).stream()
                                .flatMap(set -> set.stream()).map(holder -> new ItemStack(holder.value().asItem()))
                                .filter(stack -> !stack.isEmpty()));
                    } else {
                        BlockState state = (BlockState) call(source, "getInput");
                        ItemStack stack = new ItemStack(state.getBlock().asItem());
                        input = stack.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stack);
                        if (input.isEmpty()) fluids.addAll(fluidInput(state));
                    }
                    if (!input.isEmpty()) inputs.add(new CountedIngredient(input, 1L));
                }
                if (mold.isEmpty() || inputs.isEmpty() && fluids.isEmpty()) continue;
                BlockState outputState = (BlockState) call(source, "getOutput");
                ItemStack output = kind.equals("FluidDropRecipe") ? ItemStack.EMPTY : blockOutput(outputState);
                List<FluidStack> outputFluids = new ArrayList<>();
                if (output.isEmpty()) {
                    var fluid = outputState.getFluidState().getType();
                    if (fluid == Fluids.EMPTY) continue;
                    outputFluids.add(new FluidStack(fluid, 1000));
                }
                var converted = new AdvancedAlloyFurnaceRecipe(AdapterUtils.convertedId(source.getId()),
                        inputs, fluids, List.of(), output.isEmpty() ? List.of() : List.of(output), outputFluids,
                        List.of(), AdapterUtils.DEFAULT_ENERGY, ticks, Ingredient.EMPTY, 0, List.of(mold), AlloyFurnaceMode.NORMAL);
                result.add(new CoreRecipeHolder<>(converted.id(), new JdtSyntheticRecipe(converted)));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                com.sorrowmist.useless.UselessMod.LOGGER.warn("Cannot convert Just Dire Things recipe {}", source.getId(), exception);
            }
        }
        var recipes = List.copyOf(result);
        cache.put(level.getRecipeManager(), new Cached(generation, recipes));
        return recipes;
    }
    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<JdtSyntheticRecipe> holder, Level level) {
        return holder == null ? List.of() : List.of(holder.value().convertedRecipe());
    }
    @Override public List<CoreRecipeHolder<JdtSyntheticRecipe>> findMatchingRecipes(Level level,
            Map<Ingredient, Long> items, Map<FluidStack, Long> fluids, ItemStack mold) {
        if (level == null || mold == null || mold.isEmpty()) return List.of();
        return getGeneratedRecipes(level).stream().filter(holder -> {
            var recipe = holder.value().convertedRecipe();
            if (!AdapterUtils.matchesMold(recipe.mold(), mold)) return false;
            Map<Ingredient, Long> counts = new LinkedHashMap<>();
            recipe.inputs().forEach(input -> AdapterUtils.mergeIngredient(counts, input.ingredient(), input.count()));
            return AdapterUtils.matchesRequired(items, counts) && FluidIngredientAllocator.matchesLong(recipe.inputFluids(), fluids, 1L);
        }).toList();
    }
}
