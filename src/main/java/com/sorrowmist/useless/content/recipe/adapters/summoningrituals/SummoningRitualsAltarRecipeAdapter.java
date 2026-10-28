package com.sorrowmist.useless.content.recipe.adapters.summoningrituals;

import com.almostreliable.summoningrituals.Registration;
import com.almostreliable.summoningrituals.recipe.AltarRecipe;
import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts Summoning Rituals item-output altar recipes into alloy-furnace recipes. */
public final class SummoningRitualsAltarRecipeAdapter implements IRecipeAdapter<AltarRecipe> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation ALTAR_ID = new ResourceLocation(
            "summoningrituals", "altar");

    @Override
    public Class<AltarRecipe> getRecipeClass() {
        return AltarRecipe.class;
    }

    @Override
    public ItemStack getMoldItem() {
        return BuiltInRegistries.ITEM.getOptional(ALTAR_ID)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(
            CoreRecipeHolder<AltarRecipe> holder, Level level) {
        if (holder == null || holder.value() == null) {
            return List.of();
        }

        Converted converted = convertData(holder.value());
        if (converted == null) {
            LOGGER.warn("Skipping unsupported Summoning Rituals altar recipe: {}", holder.id());
            return List.of();
        }

        return List.of(new AdvancedAlloyFurnaceRecipe(
                AdapterUtils.convertedId(holder.id()),
                converted.inputs(),
                List.of(),
                converted.outputs(),
                List.of(),
                AdapterUtils.DEFAULT_ENERGY,
                converted.processTime(),
                Ingredient.EMPTY,
                0,
                AdapterUtils.toMoldIngredient(getMoldItem()),
                AlloyFurnaceMode.NORMAL
        ));
    }

    @Override
    public List<CoreRecipeHolder<AltarRecipe>> findMatchingRecipes(
            Level level, Map<Ingredient, Long> mergedInputs, Map<FluidStack, Long> mergedFluids,
            @Nullable ItemStack mold) {
        if (level == null || !matchesMold(mold) || mergedInputs == null || mergedInputs.isEmpty()) {
            return List.of();
        }

        List<CoreRecipeHolder<AltarRecipe>> matches = new ArrayList<>();
        RecipeManager recipeManager = level.getRecipeManager();
        for (CoreRecipeHolder<AltarRecipe> holder : com.sorrowmist.uselesscore.api.recipe.CoreRecipes.ofType(recipeManager, 
                Registration.ALTAR_RECIPE.type().get())) {
            AltarRecipe source = holder.value();
            Converted converted = source == null ? null : convertData(source);
            if (converted != null && AdapterUtils.matchesRequired(mergedInputs, converted.requirements())) {
                matches.add(holder);
            }
        }
        return matches;
    }

    @Nullable
    private static Converted convertData(@Nullable AltarRecipe source) {
        if (source == null || source.getRecipeTime() <= 0 || source.getCatalyst().isEmpty()
                || !source.getSacrifices().isEmpty()) return null;
        Map<Ingredient, Long> requirements = new LinkedHashMap<>();
        if (!addRequirement(requirements, source.getCatalyst(), 1)) return null;
        for (var input : source.getInputs()) {
            if (input == null || !addRequirement(requirements, input.ingredient(), input.count())) return null;
        }
        List<ItemStack> outputs = new ArrayList<>();
        boolean[] supported = {true};
        source.getOutputs().forEach((type, output, index) -> {
            if (type != com.almostreliable.summoningrituals.recipe.component.RecipeOutputs.OutputType.ITEM
                    || !(output.getOutput() instanceof ItemStack stack) || stack.isEmpty()) {
                supported[0] = false;
            } else outputs.add(stack.copy());
        });
        if (!supported[0] || outputs.isEmpty()) return null;
        var inputs = requirements.entrySet().stream()
                .map(e -> new CountedIngredient(e.getKey(), e.getValue())).toList();
        return new Converted(inputs, requirements, List.copyOf(outputs), source.getRecipeTime());
    }

    private static boolean addRequirement(
            Map<Ingredient, Long> requirements, @Nullable Ingredient ingredient, long count) {
        if (ingredient == null || ingredient.isEmpty() || count <= 0L) {
            return false;
        }
        try {
            AdapterUtils.mergeIngredient(requirements, ingredient, count);
            return true;
        } catch (ArithmeticException exception) {
            return false;
        }
    }

    private record Converted(
            List<CountedIngredient> inputs,
            Map<Ingredient, Long> requirements,
            List<ItemStack> outputs,
            int processTime) {
    }
}
