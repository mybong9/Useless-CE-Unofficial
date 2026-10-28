package com.sorrowmist.useless.content.recipe.adapters.productivebees;

import com.mojang.logging.LogUtils;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.ExpectedOutputScaler;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import cy.jdkdigital.productivebees.compat.jei.ingredients.BeeIngredient;
import cy.jdkdigital.productivebees.common.recipe.AdvancedBeehiveRecipe;
import cy.jdkdigital.productivebees.util.BeeCreator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public final class BeeProduceRecipeAdapter implements IRecipeAdapter<AdvancedBeehiveRecipe> {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public Class<AdvancedBeehiveRecipe> getRecipeClass() {
        return AdvancedBeehiveRecipe.class;
    }

    @Override
    @Nullable
    public ItemStack getMoldItem() {
        return null;
    }

    @Override
    public boolean matchesMold(@Nullable ItemStack mold) {
        return mold != null && !mold.isEmpty() && mold.getItem() instanceof SpawnEggItem;
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<AdvancedBeehiveRecipe> holder, Level level) {
        if (holder == null) {
            return List.of();
        }

        AdvancedBeehiveRecipe source = holder.value();
        BeeIngredient bee = source.ingredient.get();
        if (bee == null) {
            return List.of();
        }

        ItemStack spawnEgg = BeeCreator.getSpawnEgg(bee.getBeeType().toString());
        Optional<ExpectedOutputScaler.ScaledOutputs> scaled = ProductiveBeesAdapterUtils.scaleOutputs(
                source.getRecipeOutputs()
        );
        if (spawnEgg.isEmpty() || scaled.isEmpty() || scaled.get().outputs().isEmpty()) {
            LOGGER.warn("Skipping unsupported Productive Bees produce recipe: {}", holder.id());
            return List.of();
        }

        int operations = scaled.get().operations();
        OptionalInt energy = ExpectedOutputScaler.multiplyToInt(AdapterUtils.DEFAULT_ENERGY, operations);
        OptionalInt processTime = ExpectedOutputScaler.multiplyToInt(AdapterUtils.DEFAULT_PROCESS_TIME, operations);
        if (energy.isEmpty() || processTime.isEmpty()) {
            LOGGER.warn("Skipping overflowing Productive Bees produce recipe: {}", holder.id());
            return List.of();
        }

        AdvancedAlloyFurnaceRecipe converted = new AdvancedAlloyFurnaceRecipe(
                AdapterUtils.convertedId(holder.id()),
                List.of(),
                List.of(new FluidStack(Fluids.WATER, operations)),
                scaled.get().outputs(),
                List.of(),
                energy.getAsInt(),
                processTime.getAsInt(),
                Ingredient.EMPTY,
                0,
                com.sorrowmist.uselesscore.api.recipe.CoreDataComponentIngredient.of(true, spawnEgg),
                AlloyFurnaceMode.NORMAL
        );
        return List.of(converted);
    }

    @Override
    public List<CoreRecipeHolder<AdvancedBeehiveRecipe>> findMatchingRecipes(
            Level level, Map<Ingredient, Long> mergedInputs, Map<FluidStack, Long> mergedFluids,
            @Nullable ItemStack mold) {
        if (level == null || !matchesMold(mold) || mergedFluids.isEmpty()) {
            return List.of();
        }

        RecipeManager recipeManager = level.getRecipeManager();
        List<CoreRecipeHolder<AdvancedBeehiveRecipe>> matches = new ArrayList<>();
        for (CoreRecipeHolder<AdvancedBeehiveRecipe> holder : com.sorrowmist.uselesscore.api.recipe.CoreRecipes.ofType(recipeManager, 
                cy.jdkdigital.productivebees.init.ModRecipeTypes.ADVANCED_BEEHIVE_TYPE.get())) {
            AdvancedBeehiveRecipe source = holder.value();
            BeeIngredient bee = source.ingredient.get();
            if (bee == null) {
                continue;
            }

            ItemStack expectedEgg = BeeCreator.getSpawnEgg(bee.getBeeType().toString());
            if (expectedEgg.isEmpty() || !com.sorrowmist.uselesscore.api.recipe.CoreDataComponentIngredient.of(true, expectedEgg).test(mold)) {
                continue;
            }

            Optional<ExpectedOutputScaler.ScaledOutputs> scaled = ProductiveBeesAdapterUtils.scaleOutputs(
                    source.getRecipeOutputs()
            );
            if (scaled.isEmpty() || scaled.get().outputs().isEmpty()) {
                continue;
            }
            if (availableFluid(mergedFluids, new FluidStack(Fluids.WATER, 1)) >= scaled.get().operations()) {
                matches.add(holder);
            }
        }
        return matches;
    }

    private static long availableFluid(Map<FluidStack, Long> fluids, FluidStack required) {
        long amount = 0;
        for (Map.Entry<FluidStack, Long> entry : fluids.entrySet()) {
            if (com.sorrowmist.uselesscore.api.fluid.CoreFluidStacks.isSameFluidSameComponents(entry.getKey(), required)) {
                amount += entry.getValue();
            }
        }
        return amount;
    }
}
