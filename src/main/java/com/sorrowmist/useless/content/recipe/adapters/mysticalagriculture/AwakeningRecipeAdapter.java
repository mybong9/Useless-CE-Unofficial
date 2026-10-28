package com.sorrowmist.useless.content.recipe.adapters.mysticalagriculture;

import com.blakebr0.mysticalagriculture.api.crafting.IAwakeningRecipe;
import com.blakebr0.mysticalagriculture.init.ModRecipeTypes;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.CountedIngredient;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mystical Agriculture 觉醒祭坛配方适配器
 * <p>
 * 将觉醒祭坛配方转换为高级合金熔炉配方
 * <p>
 * 处理逻辑：
 * - 祭坛物品(input) + 基座物品(ingredients) + 精华(essences，含数量) → 普通输入（合并相同物品，全部消耗）
 * - 产物(result) → 输出
 * - mysticalagriculture:awakening_altar → 模具（不消耗）
 */
public class AwakeningRecipeAdapter implements IRecipeAdapter<IAwakeningRecipe> {

    @Override
    public Class<IAwakeningRecipe> getRecipeClass() {
        return IAwakeningRecipe.class;
    }

    @Nullable
    @Override
    public ItemStack getMoldItem() {
        return new ItemStack(
                BuiltInRegistries.ITEM.get(
                        new ResourceLocation("mysticalagriculture", "awakening_altar")));
    }

    @Override
    public boolean matchesMold(@Nullable ItemStack mold) {
        return mold != null
                && BuiltInRegistries.ITEM.getKey(mold.getItem()).equals(
                        new ResourceLocation("mysticalagriculture", "awakening_altar"));
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<IAwakeningRecipe> holder, Level level) {
        if (holder == null) return List.of();

        IAwakeningRecipe recipe = holder.value();
        ResourceLocation originalId = holder.id();

        List<Ingredient> allIngredients = recipe.getIngredients();
        Ingredient altarIngredient = allIngredients.isEmpty() ? Ingredient.EMPTY : allIngredients.get(0);
        List<ItemStack> essences = recipe.getEssences();
        ItemStack output = recipe.getResultItem(null);

        if (output.isEmpty()) {
            return List.of();
        }

        // 合并所有输入（祭坛物品 + 基座物品 + 精华）
        Map<Ingredient, Long> ingredientCounts = new LinkedHashMap<>();

        if (!altarIngredient.isEmpty()) {
            AdapterUtils.mergeIngredient(ingredientCounts, altarIngredient, 1L);
        }

        // Forge 1.20.1: center at 0, pedestal inputs at 2/4/6/8.
        // Odd slots are essence display placeholders, counted separately below.
        for (int i = 2; i < allIngredients.size(); i += 2) {
            Ingredient ing = allIngredients.get(i);
            if (!ing.isEmpty()) {
                AdapterUtils.mergeIngredient(ingredientCounts, ing, 1L);
            }
        }

        // 精华物品（附带数量）
        for (ItemStack essence : essences) {
            if (!essence.isEmpty()) {
                AdapterUtils.mergeIngredient(ingredientCounts, Ingredient.of(essence), (long) essence.getCount());
            }
        }

        if (ingredientCounts.isEmpty()) {
            return List.of();
        }

        List<CountedIngredient> countedIngredients = new ArrayList<>();
        for (Map.Entry<Ingredient, Long> entry : ingredientCounts.entrySet()) {
            countedIngredients.add(new CountedIngredient(entry.getKey(), entry.getValue()));
        }

        int processTime = AdapterUtils.DEFAULT_PROCESS_TIME + (allIngredients.size() / 2 + essences.size()) * 15;

        AdvancedAlloyFurnaceRecipe convertedRecipe = new AdvancedAlloyFurnaceRecipe(
                AdapterUtils.convertedId(originalId),
                countedIngredients,
                List.of(),
                List.of(output.copy()),
                List.of(),
                5000,
                processTime,
                Ingredient.EMPTY,
                0,
                AdapterUtils.toMoldIngredient(getMoldItem()),
                AlloyFurnaceMode.NORMAL
        );

        return List.of(convertedRecipe);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public List<CoreRecipeHolder<IAwakeningRecipe>> findMatchingRecipes(Level level, Map<Ingredient, Long> mergedInputs, Map<FluidStack, Long> mergedFluids, @Nullable ItemStack mold) {
        if (level == null || mergedInputs.isEmpty()) {
            return List.of();
        }

        if (!matchesMold(mold)) {
            return List.of();
        }

        RecipeManager recipeManager = level.getRecipeManager();
        List<CoreRecipeHolder<IAwakeningRecipe>> recipes = (List<CoreRecipeHolder<IAwakeningRecipe>>) (List<?>) com.sorrowmist.uselesscore.api.recipe.CoreRecipes.ofType(recipeManager, 
                ModRecipeTypes.AWAKENING.get()
        );

        List<CoreRecipeHolder<IAwakeningRecipe>> matches = new java.util.ArrayList<>();
        for (CoreRecipeHolder<IAwakeningRecipe> holder : recipes) {
            IAwakeningRecipe recipe = holder.value();

            List<Ingredient> allIngredients = recipe.getIngredients();
            Ingredient altarIngredient = allIngredients.isEmpty() ? Ingredient.EMPTY : allIngredients.get(0);
            List<ItemStack> essences = recipe.getEssences();
            ItemStack output = recipe.getResultItem(null);

            if (output.isEmpty()) continue;

            // 合并所有需要的配料
            Map<Ingredient, Long> requiredCounts = new LinkedHashMap<>();
            if (!altarIngredient.isEmpty()) {
                AdapterUtils.mergeIngredient(requiredCounts, altarIngredient, 1L);
            }
            for (int i = 2; i < allIngredients.size(); i += 2) {
                Ingredient ing = allIngredients.get(i);
                if (!ing.isEmpty()) {
                    AdapterUtils.mergeIngredient(requiredCounts, ing, 1L);
                }
            }
            for (ItemStack essence : essences) {
                if (!essence.isEmpty()) {
                    AdapterUtils.mergeIngredient(requiredCounts, Ingredient.of(essence), (long) essence.getCount());
                }
            }

            if (requiredCounts.isEmpty()) continue;

            if (AdapterUtils.matchesRequired(mergedInputs, requiredCounts)) {
                matches.add(holder);
            }
        }
        return matches;
    }
}
