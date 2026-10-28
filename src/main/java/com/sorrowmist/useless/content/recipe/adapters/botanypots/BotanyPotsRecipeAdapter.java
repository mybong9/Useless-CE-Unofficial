package com.sorrowmist.useless.content.recipe.adapters.botanypots;

import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.fluid.FluidIngredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.darkhax.botanypots.block.BlockBotanyPot;
import net.darkhax.botanypots.data.recipes.crop.BasicCrop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

import java.util.*;

/** Forge 1.20.1 Botany Pots crops, using its native harvest probabilities and roll counts. */
public final class BotanyPotsRecipeAdapter implements IRecipeAdapter<BotanyPotsCropSyntheticRecipe> {
    private record Cached(long generation,List<CoreRecipeHolder<BotanyPotsCropSyntheticRecipe>> recipes) {}
    private final Map<Object,Cached> cache=new WeakHashMap<>();
    @Override public String sourceId() { return RecipeSourceIds.BOTANY_POTS; }
    @Override public Class<BotanyPotsCropSyntheticRecipe> getRecipeClass() { return BotanyPotsCropSyntheticRecipe.class; }
    @Override public ItemStack getMoldItem() { return null; }

    @Override public synchronized List<CoreRecipeHolder<BotanyPotsCropSyntheticRecipe>> getGeneratedRecipes(Level level) {
        if (level==null) return List.of();
        long generation=AlloyFurnaceRecipeCatalog.generation();
        var cached=cache.get(level.getRecipeManager());
        if (cached!=null && cached.generation()==generation) return cached.recipes();
        List<CoreRecipeHolder<BotanyPotsCropSyntheticRecipe>> result = new ArrayList<>();
        Ingredient pots = Ingredient.of(BuiltInRegistries.BLOCK.stream()
                .filter(block -> block instanceof BlockBotanyPot).map(ItemStack::new).filter(s -> !s.isEmpty()));
        if (pots.isEmpty()) return result;
        for (var recipe : level.getRecipeManager().getRecipes()) {
            if (!(recipe instanceof BasicCrop crop) || crop.getSeed().isEmpty()) continue;
            try {
            List<ExpectedOutputScaler.WeightedItemOutput> outputs = new ArrayList<>();
            for (var entry : crop.getResults()) {
                var stack = entry.getItem();
                if (stack.isEmpty()) continue;
                int min = Math.multiplyExact(entry.getMinRolls(), stack.getCount());
                int max = Math.multiplyExact(entry.getMaxRolls(), stack.getCount());
                outputs.add(new ExpectedOutputScaler.WeightedItemOutput(stack, min, max, entry.getChance()));
            }
            var scaled = ExpectedOutputScaler.scale(outputs);
            if (scaled.isEmpty() || scaled.get().outputs().isEmpty()) continue;
            long operations = scaled.get().operations();
            var id = AdapterUtils.convertedId(crop.getId());
            var converted = new AdvancedAlloyFurnaceRecipe(id, List.of(),
                    List.of(new LongSizedFluidIngredient(FluidIngredient.single(Fluids.WATER), 1000L * operations)),
                    List.of(), scaled.get().outputs(), List.of(), List.of(),
                    Math.multiplyExact((long) AdapterUtils.DEFAULT_ENERGY, operations),
                    AdapterUtils.safeInt(Math.multiplyExact((long)Math.max(1,crop.getGrowthTicks()),operations)), Ingredient.EMPTY, 0,
                    List.of(pots, crop.getSeed()), AlloyFurnaceMode.NORMAL);
            result.add(new CoreRecipeHolder<>(id, new BotanyPotsCropSyntheticRecipe(converted)));
            } catch (RuntimeException invalid) {
                com.sorrowmist.useless.UselessMod.LOGGER.warn("Cannot convert Botany Pots crop {}",crop.getId(),invalid);
            }
        }
        var recipes=List.copyOf(result);
        cache.put(level.getRecipeManager(),new Cached(generation,recipes));
        return recipes;
    }

    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(
            CoreRecipeHolder<BotanyPotsCropSyntheticRecipe> holder, Level level) {
        return holder == null ? List.of() : List.of(holder.value().convertedRecipe());
    }
}
