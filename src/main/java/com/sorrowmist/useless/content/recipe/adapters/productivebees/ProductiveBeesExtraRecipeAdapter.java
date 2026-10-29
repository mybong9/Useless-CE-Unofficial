package com.sorrowmist.useless.content.recipe.adapters.productivebees;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.fluid.DataComponentFluidIngredient;
import com.sorrowmist.uselesscore.api.fluid.FluidIngredient;
import com.sorrowmist.uselesscore.api.recipe.CoreDataComponentIngredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import cy.jdkdigital.productivebees.ProductiveBeesConfig;
import cy.jdkdigital.productivebees.common.item.HoneyTreat;
import cy.jdkdigital.productivebees.common.recipe.*;
import cy.jdkdigital.productivebees.compat.jei.ingredients.BeeIngredient;
import cy.jdkdigital.productivebees.compat.jei.ingredients.BeeIngredientFactory;
import cy.jdkdigital.productivebees.util.BeeCreator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** Reads Forge PB transformations, preserving encoded bee types and fluid alternatives. */
public final class ProductiveBeesExtraRecipeAdapter implements IRecipeAdapter<ProductiveBeesSyntheticRecipe> {
    private record Cached(long generation, List<CoreRecipeHolder<ProductiveBeesSyntheticRecipe>> recipes) {}
    private final Map<Object, Cached> cache = new WeakHashMap<>();
    @Override public String sourceId() { return RecipeSourceIds.PRODUCTIVE_BEES; }
    @Override public Class<ProductiveBeesSyntheticRecipe> getRecipeClass() { return ProductiveBeesSyntheticRecipe.class; }
    @Override public ItemStack getMoldItem() { return null; }

    @Override public synchronized List<CoreRecipeHolder<ProductiveBeesSyntheticRecipe>> getGeneratedRecipes(Level level) {
        if (level == null) return List.of();
        long generation = AlloyFurnaceRecipeCatalog.generation();
        Cached previous = cache.get(level.getRecipeManager());
        if (previous != null && previous.generation == generation) return previous.recipes;
        List<CoreRecipeHolder<ProductiveBeesSyntheticRecipe>> result = new ArrayList<>();
        for (Recipe<?> source : level.getRecipeManager().getRecipes()) {
            try {
                AdvancedAlloyFurnaceRecipe converted = null;
                if (source instanceof BottlerRecipe bottler) converted = bottler(bottler);
                else if (source instanceof ItemConversionRecipe item) converted = itemConversion(item);
                else if (source instanceof BlockConversionRecipe block) converted = blockConversion(block);
                if (converted != null) add(result, converted);
            } catch (RuntimeException exception) {
                UselessMod.LOGGER.warn("Cannot convert Productive Bees recipe {}", source.getId(), exception);
            }
        }
        // Native PB incubation is hard-coded and only exposes client-side JEI display recipes.
        // Reconstruct its egg + pure type-treat operation from common bee data, without loading Minecraft client classes.
        ItemStack incubator = item("incubator");
        if (!incubator.isEmpty()) for (String bee : BeeIngredientFactory.getOrCreateList().keySet()) {
            try {
                ItemStack egg = BeeCreator.getSpawnEgg(bee), treat = HoneyTreat.getTypeStack(bee, 100);
                if (egg == null || egg.isEmpty() || treat == null || treat.isEmpty()) continue;
                ResourceLocation type = new ResourceLocation(bee);
                ResourceLocation id = new ResourceLocation("productivebees", "useless_incubation/" + type.getNamespace() + "/" + type.getPath());
                int ticks = Math.max(1, ProductiveBeesConfig.GENERAL.incubatorProcessingTime.get());
                add(result, recipe(id, List.of(new CountedIngredient(Ingredient.of(Items.EGG), 1),
                                new CountedIngredient(CoreDataComponentIngredient.of(true, treat), 1)), List.of(),
                        List.of(egg.copy()), List.of(), List.of(Ingredient.of(incubator)), 1, ticks));
            } catch (RuntimeException exception) {
                UselessMod.LOGGER.debug("Cannot convert PB type-treat incubation {}", bee, exception);
            }
        }
        List<CoreRecipeHolder<ProductiveBeesSyntheticRecipe>> recipes = List.copyOf(result);
        cache.put(level.getRecipeManager(), new Cached(generation, recipes));
        return recipes;
    }

    private static AdvancedAlloyFurnaceRecipe bottler(BottlerRecipe source) {
        ItemStack mold = item("bottler");
        if (mold.isEmpty() || source.itemInput.isEmpty() || source.result.isEmpty()) return null;
        int amount = source.fluidInput.getSecond();
        List<FluidIngredient> alternatives = new ArrayList<>();
        for (var fluid : TagOutputRecipe.getAllFluidsFromName(source.fluidInput.getFirst())) {
            if (fluid != Fluids.EMPTY) alternatives.add(DataComponentFluidIngredient.of(true, new FluidStack(fluid, amount)));
        }
        if (amount <= 0 || alternatives.isEmpty()) return null;
        return recipe(source.getId(), List.of(new CountedIngredient(source.itemInput, 1)),
                List.of(new LongSizedFluidIngredient(FluidIngredient.anyOf(alternatives), amount)),
                List.of(source.result.copy()), List.of(), List.of(Ingredient.of(mold)), 1, AdapterUtils.DEFAULT_PROCESS_TIME);
    }

    private static AdvancedAlloyFurnaceRecipe itemConversion(ItemConversionRecipe source) {
        BeeChancePlan plan = BeeChancePlan.inclusive(source.chance);
        Ingredient bees = beeMold(source.getBees());
        if (plan == null || bees.isEmpty() || source.ingredient.isEmpty() || source.output.isEmpty()) return null;
        return recipe(source.getId(), List.of(new CountedIngredient(source.ingredient, plan.successes())), List.of(),
                List.of(source.output.copyWithCount(Math.multiplyExact(source.output.getCount(), plan.successes()))),
                List.of(), List.of(bees), plan.attempts(), AdapterUtils.DEFAULT_PROCESS_TIME);
    }

    private static AdvancedAlloyFurnaceRecipe blockConversion(BlockConversionRecipe source) {
        BeeChancePlan plan = BeeChancePlan.inclusive(source.chance);
        Ingredient bees = beeMold(source.getBees());
        if (plan == null || bees.isEmpty() || source.stateTo == null || source.stateTo.isAir()) return null;
        List<CountedIngredient> items = new ArrayList<>();
        List<LongSizedFluidIngredient> fluids = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();
        List<FluidStack> fluidOutputs = new ArrayList<>();
        if (source.input != null && !source.input.isEmpty()) items.add(new CountedIngredient(source.input, plan.successes()));
        else if (source.stateFrom != null && !source.stateFrom.isAir()) {
            if (!source.stateFrom.equals(source.stateFrom.getBlock().defaultBlockState())) return null;
            ItemStack block = new ItemStack(source.stateFrom.getBlock());
            if (!block.isEmpty()) items.add(new CountedIngredient(Ingredient.of(block), plan.successes()));
            else if (!source.stateFrom.getFluidState().isEmpty()) fluids.add(LongSizedFluidIngredient.from(
                    new FluidStack(source.stateFrom.getFluidState().getType(), Math.multiplyExact(1000, plan.successes()))));
            else return null;
        } else {
            // Match the existing bee-production adapter's small water input for input-free bee production.
            fluids.add(LongSizedFluidIngredient.from(new FluidStack(Fluids.WATER, plan.successes())));
        }
        ItemStack block = new ItemStack(source.stateTo.getBlock());
        if (!block.isEmpty()) outputs.add(block.copyWithCount(plan.successes()));
        else if (!source.stateTo.getFluidState().isEmpty()) fluidOutputs.add(
                new FluidStack(source.stateTo.getFluidState().getType(), Math.multiplyExact(1000, plan.successes())));
        else return null;
        return recipe(source.getId(), items, fluids, outputs, fluidOutputs, List.of(bees), plan.attempts(), AdapterUtils.DEFAULT_PROCESS_TIME);
    }

    private static Ingredient beeMold(List<BeeIngredient> bees) {
        List<Ingredient> choices = new ArrayList<>();
        for (BeeIngredient bee : bees) {
            ItemStack egg = ProductiveBeesAdapterUtils.spawnEgg(bee);
            if (!egg.isEmpty()) choices.add(CoreDataComponentIngredient.of(true, egg));
        }
        return choices.isEmpty() ? Ingredient.EMPTY : net.minecraftforge.common.crafting.CompoundIngredient.of(choices.toArray(Ingredient[]::new));
    }
    private static ItemStack item(String name) { return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("productivebees", name))); }
    private static AdvancedAlloyFurnaceRecipe recipe(ResourceLocation id, List<CountedIngredient> items,
            List<LongSizedFluidIngredient> fluids, List<ItemStack> outputs, List<FluidStack> fluidOutputs,
            List<Ingredient> molds, int attempts, int ticks) {
        return new AdvancedAlloyFurnaceRecipe(AdapterUtils.convertedId(id), items, fluids, List.of(), outputs,
                fluidOutputs, List.of(), Math.multiplyExact((long) AdapterUtils.DEFAULT_ENERGY, attempts),
                Math.multiplyExact(ticks, attempts), Ingredient.EMPTY, 0, molds, AlloyFurnaceMode.NORMAL,
                AdvancedAlloyFurnaceRecipe.NO_EXPLICIT_TIER);
    }
    private static void add(List<CoreRecipeHolder<ProductiveBeesSyntheticRecipe>> result, AdvancedAlloyFurnaceRecipe recipe) {
        result.add(new CoreRecipeHolder<>(recipe.id(), new ProductiveBeesSyntheticRecipe(recipe)));
    }
    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<ProductiveBeesSyntheticRecipe> holder, Level level) {
        return holder == null ? List.of() : List.of(holder.value().convertedRecipe());
    }
    @Override public List<CoreRecipeHolder<ProductiveBeesSyntheticRecipe>> findMatchingRecipes(Level level,
            Map<Ingredient, Long> items, Map<FluidStack, Long> fluids, ItemStack mold) {
        return getGeneratedRecipes(level).stream().filter(holder -> {
            var recipe = holder.value().convertedRecipe();
            if (!AdapterUtils.matchesMold(recipe.mold(), mold)) return false;
            Map<Ingredient, Long> required = new LinkedHashMap<>();
            recipe.inputs().forEach(input -> AdapterUtils.mergeIngredient(required, input.ingredient(), input.count()));
            return AdapterUtils.matchesRequired(items, required) && FluidIngredientAllocator.matchesLong(recipe.inputFluids(), fluids, 1L);
        }).toList();
    }
}
