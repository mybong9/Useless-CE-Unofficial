package com.sorrowmist.useless.content.recipe.adapters.delight.nomadsdelight;

import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.CountedIngredient;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import com.sorrowmist.useless.content.recipe.RecipeSourceIds;
import com.sorrowmist.useless.content.recipe.adapters.delight.DelightRecipeAdapterUtils;
import com.sorrowmist.useless.content.recipe.adapters.delight.DelightSyntheticRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Converts Nomad's Delight's JEI-only butter churn recipes. */
public final class ButterChurnRecipeAdapter implements IRecipeAdapter<DelightSyntheticRecipe> {
    private static final ResourceLocation MOLD_ID =
            new ResourceLocation("nomads_delight", "butter_churn");
    private static final ResourceLocation HORSE_MILK_BUCKET_ID =
            new ResourceLocation("nomads_delight", "horse_milk_bucket");
    private static final ResourceLocation QUMYZ_BUCKET_ID =
            new ResourceLocation("nomads_delight", "qumyz_bucket");
    private static final ResourceLocation CAMEL_MILK_BUCKET_ID =
            new ResourceLocation("nomads_delight", "camel_milk_bucket");
    private static final ResourceLocation SHUBAT_BUCKET_ID =
            new ResourceLocation("nomads_delight", "shubat_bucket");
    private static final ResourceLocation BUTTER_ID =
            new ResourceLocation("nomads_delight", "butter");
    private static final int CHURN_TIME_TICKS = 6000;

    @Override
    public String sourceId() {
        return RecipeSourceIds.NOMADS_DELIGHT;
    }

    @Override
    public Class<DelightSyntheticRecipe> getRecipeClass() {
        return DelightSyntheticRecipe.class;
    }

    @Override
    public ItemStack getMoldItem() {
        return itemStack(MOLD_ID);
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(
            CoreRecipeHolder<DelightSyntheticRecipe> holder, Level level) {
        if (holder == null || holder.value() == null
                || holder.value().convertedRecipe() == null) {
            return List.of();
        }
        return List.of(holder.value().convertedRecipe());
    }

    @Override
    public List<CoreRecipeHolder<DelightSyntheticRecipe>> getGeneratedRecipes(Level level) {
        if (level == null) {
            return List.of();
        }

        List<CoreRecipeHolder<DelightSyntheticRecipe>> generated = new ArrayList<>();
        addRecipe(generated, "horse_milk", HORSE_MILK_BUCKET_ID, QUMYZ_BUCKET_ID);
        addRecipe(generated, "camel_milk", CAMEL_MILK_BUCKET_ID, SHUBAT_BUCKET_ID);
        addRecipe(generated, "milk", ResourceLocation.withDefaultNamespace("milk_bucket"), BUTTER_ID);
        return List.copyOf(generated);
    }

    @Override
    public List<CoreRecipeHolder<DelightSyntheticRecipe>> findMatchingRecipes(
            Level level, Map<Ingredient, Long> mergedInputs,
            Map<FluidStack, Long> mergedFluids, @Nullable ItemStack mold) {
        if (level == null || !matchesMold(mold)
                || mergedInputs == null || mergedInputs.isEmpty()
                || (mergedFluids != null && !mergedFluids.isEmpty())) {
            return List.of();
        }

        List<CoreRecipeHolder<DelightSyntheticRecipe>> matches = new ArrayList<>();
        for (CoreRecipeHolder<DelightSyntheticRecipe> holder : getGeneratedRecipes(level)) {
            AdvancedAlloyFurnaceRecipe recipe = holder.value().convertedRecipe();
            if (recipe != null && DelightRecipeAdapterUtils.matchesItems(
                    recipe.inputs(), mergedInputs, List.of())) {
                matches.add(holder);
            }
        }
        return List.copyOf(matches);
    }

    private static void addRecipe(List<CoreRecipeHolder<DelightSyntheticRecipe>> recipes,
                                  String name, ResourceLocation inputId,
                                  ResourceLocation outputId) {
        ItemStack input = itemStack(inputId);
        ItemStack output = itemStack(outputId);
        if (input.isEmpty() || output.isEmpty()) {
            return;
        }

        ResourceLocation sourceId = new ResourceLocation(
                "nomads_delight", "butter_churn/" + name);
        AdvancedAlloyFurnaceRecipe converted = new AdvancedAlloyFurnaceRecipe(
                AdapterUtils.convertedId(sourceId),
                AdapterUtils.mergeIngredients(List.of(Ingredient.of(input))),
                List.of(),
                List.of(),
                List.of(output),
                List.of(),
                List.of(),
                Math.max(1L, (long) CHURN_TIME_TICKS * AdapterUtils.DEFAULT_ENERGY / 200L),
                CHURN_TIME_TICKS,
                Ingredient.EMPTY,
                0,
                List.of(AdapterUtils.toMoldIngredient(itemStack(MOLD_ID))),
                AlloyFurnaceMode.NORMAL
        );
        recipes.add(new CoreRecipeHolder<>(converted.id(), new DelightSyntheticRecipe(converted)));
    }

    private static ItemStack itemStack(ResourceLocation id) {
        Item item = DelightRecipeAdapterUtils.registeredItem(id);
        return item == null ? ItemStack.EMPTY : item.getDefaultInstance();
    }
}
