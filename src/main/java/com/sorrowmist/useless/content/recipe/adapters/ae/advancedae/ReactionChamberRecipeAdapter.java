package com.sorrowmist.useless.content.recipe.adapters.ae.advancedae;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.CountedIngredient;
import com.sorrowmist.useless.content.recipe.FluidIngredientAllocator;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import com.sorrowmist.uselesscore.api.fluid.SizedFluidIngredient;
import net.pedroksl.advanced_ae.recipes.ReactionChamberRecipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AdvancedAE 反应仓配方适配器
 */
public class ReactionChamberRecipeAdapter implements IRecipeAdapter<ReactionChamberRecipe> {

    @Override
    public Class<ReactionChamberRecipe> getRecipeClass() {
        return ReactionChamberRecipe.class;
    }

    @Override
    @Nullable
    public ItemStack getMoldItem() {
        // AAEBlockDefinition changed to LibBlockDefinition; its field descriptor is
        // binary-incompatible even though REACTION_CHAMBER kept the same name.
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("advanced_ae", "reaction_chamber"));
        if (item == null || item == Items.AIR) {
            throw new IllegalStateException("AdvancedAE reaction chamber item is unavailable");
        }
        return new ItemStack(item);
    }

    @Override
    @Nullable
    public AdvancedAlloyFurnaceRecipe convert(CoreRecipeHolder<ReactionChamberRecipe> holder, Level level) {
        if (holder == null) return null;

        ReactionChamberRecipe originalRecipe = holder.value();
        ResourceLocation originalId = holder.id();

        // 获取输入材料 - 使用全类名避免与 glodium 的 IngredientStack 冲突
        List<CountedIngredient> countedIngredients = new ArrayList<>();
        List<?> inputs = originalRecipe.getInputs();

        for (Object inputStack : inputs) {
            if (!AdvancedAERecipeAccess.isEmpty(inputStack)) {
                Ingredient ingredient = (Ingredient) AdvancedAERecipeAccess.ingredient(inputStack);
                long count = AdvancedAERecipeAccess.amount(inputStack);
                if (ingredient != null && !ingredient.isEmpty()) {
                    countedIngredients.add(new CountedIngredient(ingredient, count));
                }
            }
        }

        if (countedIngredients.isEmpty()) {
            return null;
        }

        // 获取输出物品或流体
        List<ItemStack> outputs = new ArrayList<>();
        List<FluidStack> outputFluids = new ArrayList<>();

        var output = originalRecipe.output;
        if (output != null && output.what() != null) {
            if (output.what() instanceof AEItemKey key) {
                outputs.add(key.toStack((int) output.amount()));
            } else if (output.what() instanceof AEFluidKey key) {
                outputFluids.add(key.toStack((int) output.amount()));
            }
        }

        // 获取流体输入（如果有）- 使用全类名
        List<SizedFluidIngredient> inputFluids = new ArrayList<>();
        Object fluidInput = AdvancedAERecipeAccess.fluid(originalRecipe);
        if (!AdvancedAERecipeAccess.isEmpty(fluidInput)) {
            var fluidIngredient = AdvancedAERecipeAccess.ingredient(fluidInput);
            long amount = AdvancedAERecipeAccess.amount(fluidInput);
            if (!(fluidIngredient instanceof Fluid fluid) || fluid == net.minecraft.world.level.material.Fluids.EMPTY
                    || amount <= 0 || amount > Integer.MAX_VALUE) return null;
            inputFluids.add(new SizedFluidIngredient(com.sorrowmist.uselesscore.api.fluid.FluidIngredient.single(fluid), (int) amount));
        }

        // 能量消耗：AdvancedAE 配方字段为 FE
        int energyCost = originalRecipe.getEnergy();
        // 处理时间
        int processTime = AdapterUtils.ADVANCEDAE_REACTION_CHAMBER_PROCESS_TIME;

        // 创建反应仓模具要求
        Ingredient moldIngredient = Ingredient.of(getMoldItem());

        return new AdvancedAlloyFurnaceRecipe(
                AdapterUtils.convertedId(originalId),
                countedIngredients,
                inputFluids,
                outputs,
                outputFluids,
                energyCost,
                processTime,
                Ingredient.EMPTY,    // 无催化剂
                0,
                moldIngredient,      // 反应仓作为模具
                AlloyFurnaceMode.NORMAL
        );
    }

    @Override
    @Nullable
    public List<CoreRecipeHolder<ReactionChamberRecipe>> findMatchingRecipes(Level level, Map<Ingredient, Long> mergedInputs, Map<FluidStack, Long> mergedFluids, @Nullable ItemStack mold) {
        if (level == null || mergedInputs.isEmpty()) {
            return List.of();
        }

        RecipeManager recipeManager = level.getRecipeManager();
        List<CoreRecipeHolder<ReactionChamberRecipe>> recipes = com.sorrowmist.uselesscore.api.recipe.CoreRecipes.ofType(recipeManager, ReactionChamberRecipe.TYPE);

        List<CoreRecipeHolder<ReactionChamberRecipe>> matches = new java.util.ArrayList<>();
        for (CoreRecipeHolder<ReactionChamberRecipe> holder : recipes) {
            ReactionChamberRecipe recipe = holder.value();
            List<?> recipeInputs = recipe.getInputs();

            Map<Ingredient, Long> requiredInputs = new LinkedHashMap<>();
            for (Object inputStack : recipeInputs) {
                if (AdvancedAERecipeAccess.isEmpty(inputStack)) continue;

                Ingredient ingredient = (Ingredient) AdvancedAERecipeAccess.ingredient(inputStack);
                AdapterUtils.mergeIngredient(requiredInputs, ingredient, AdvancedAERecipeAccess.amount(inputStack));
            }

            if (AdapterUtils.matchesRequired(mergedInputs, requiredInputs)
                    && !recipeInputs.isEmpty() && matchesFluidInput(mergedFluids, AdvancedAERecipeAccess.fluid(recipe))) {
                matches.add(holder);
            }
        }
        return matches;
    }

    private boolean matchesFluidInput(Map<FluidStack, Long> mergedFluids, Object fluidInput) {
        if (AdvancedAERecipeAccess.isEmpty(fluidInput)) {
            return true;
        }

        var fluidIngredient = AdvancedAERecipeAccess.ingredient(fluidInput);
        long amount = AdvancedAERecipeAccess.amount(fluidInput);
        if (!(fluidIngredient instanceof Fluid fluid) || fluid == net.minecraft.world.level.material.Fluids.EMPTY
                || amount <= 0 || amount > Integer.MAX_VALUE) {
            return false;
        }
        return FluidIngredientAllocator.matches(
                List.of(new SizedFluidIngredient(com.sorrowmist.uselesscore.api.fluid.FluidIngredient.single(fluid), (int) amount)),
                mergedFluids, 1L);
    }
}
