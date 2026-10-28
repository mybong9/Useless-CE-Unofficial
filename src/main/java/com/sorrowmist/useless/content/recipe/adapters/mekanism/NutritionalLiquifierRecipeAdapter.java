package com.sorrowmist.useless.content.recipe.adapters.mekanism;

import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import mekanism.api.recipes.ItemStackToFluidRecipe;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.machine.TileEntityNutritionalLiquifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Dynamic food -> nutritional paste adapter. */
public final class NutritionalLiquifierRecipeAdapter extends MekanismSyntheticRecipeAdapter {
    private static final long PROCESS_TICKS = 5L * 20L;
    private static final long ENERGY_PER_TICK = 200L;

    @Override
    public @Nullable ItemStack getMoldItem() {
        return new ItemStack(MekanismBlocks.NUTRITIONAL_LIQUIFIER.getBlock());
    }

    @Override
    protected List<CoreRecipeHolder<MekanismSyntheticRecipe>> createGeneratedRecipes(Level level) {
        List<CoreRecipeHolder<MekanismSyntheticRecipe>> result = new ArrayList<>();
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            ItemStack input = entry.getValue().getDefaultInstance();
            var food = input.getFoodProperties(null);
            if (!input.isEdible() || food == null || food.getNutrition() <= 0) continue;
            ItemStackToFluidRecipe recipe = new mekanism.common.recipe.impl.NutritionalLiquifierIRecipe(
                    input.getItem(), mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess.item().from(input, 1),
                    mekanism.common.registries.MekanismFluids.NUTRITIONAL_PASTE.getFluidStack(food.getNutrition() * 50));
            if (recipe == null || recipe.getOutputDefinition().isEmpty()) continue;

            net.minecraftforge.fluids.FluidStack output = recipe.getOutput(input);
            ResourceLocation itemId = entry.getKey().location();
            ResourceLocation id = MekanismChemicalRecipeSupport.variantId(
                    itemId, "nutritional_liquification");
            AdvancedAlloyFurnaceRecipe converted = MekanismChemicalRecipeSupport.recipe(
                    id,
                    MekanismChemicalRecipeSupport.items(recipe.getInput()),
                    List.of(), List.of(), List.of(), List.of(output.copy()), List.of(),
                    com.sorrowmist.useless.content.recipe.adapters.mekanism.MekanismRecipeEnergy.cost(ENERGY_PER_TICK, PROCESS_TICKS, 1L),
                    AdapterUtils.safeInt(PROCESS_TICKS), getMoldItem());
            result.add(MekanismChemicalRecipeSupport.syntheticHolder(id, converted));
        }
        return result;
    }
}
