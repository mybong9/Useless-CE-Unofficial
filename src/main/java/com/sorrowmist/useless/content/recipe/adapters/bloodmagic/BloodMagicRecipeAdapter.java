package com.sorrowmist.useless.content.recipe.adapters.bloodmagic;

import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import wayoftime.bloodmagic.recipe.*;

import java.util.*;

/** Native Blood Magic 3.3 recipe access; ingredient alternatives and NBT are retained. */
public final class BloodMagicRecipeAdapter<T extends Recipe<?>> implements IRecipeAdapter<T> {
    private final Class<T> type;
    private final String mold;

    public BloodMagicRecipeAdapter(Class<T> type, String mold) { this.type = type; this.mold = mold; }
    @Override public Class<T> getRecipeClass() { return type; }
    @Override public String sourceId() { return RecipeSourceIds.BLOOD_MAGIC; }
    @Override public ItemStack getMoldItem() {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("bloodmagic", mold));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<T> holder, Level level) {
        List<Ingredient> ingredients;
        ItemStack output;
        long energy = 0;
        int ticks = 100;
        int tier = 0;
        T recipe = holder.value();
        if (recipe instanceof RecipeBloodAltar altar) {
            ingredients = List.of(altar.getInput()); output = altar.getOutput();
            energy = 2L * altar.getSyphon();
            ticks = Math.max(1, (int) Math.min(Integer.MAX_VALUE,
                    ((long) altar.getSyphon() + Math.max(1, altar.getConsumeRate()) - 1)
                            / Math.max(1, altar.getConsumeRate())));
            tier = altar.getMinimumTier();
        } else if (recipe instanceof RecipeTartaricForge forge) {
            ingredients = forge.getInput(); output = forge.getOutput();
            energy = (long) Math.ceil(forge.getSoulDrain() * 2);
        } else if (recipe instanceof RecipeAlchemyTable table) {
            ingredients = table.getInput(); output = table.getOutput();
            energy = 2L * table.getSyphon(); ticks = Math.max(1, table.getTicks());
            tier = table.getMinimumTier();
        } else if (recipe instanceof RecipeAlchemyArray array) {
            ingredients = List.of(array.getBaseInput(), array.getAddedInput()); output = array.getOutput();
        } else return List.of();
        ItemStack machine = getMoldItem();
        if (machine.isEmpty() || output.isEmpty() || ingredients.isEmpty()
                || ingredients.stream().anyMatch(Ingredient::isEmpty)) return List.of();
        // As in the original 1.20.1 converter, life essence/souls become FE at 2 FE per unit.
        return List.of(new AdvancedAlloyFurnaceRecipe(AdapterUtils.convertedId(holder.id()),
                ingredients.stream().map(i -> new CountedIngredient(i, 1)).toList(), List.of(), List.of(),
                List.of(output.copy()), List.of(), List.of(), Math.max(0, energy), ticks,
                Ingredient.EMPTY, 0, List.of(Ingredient.of(machine)), AlloyFurnaceMode.NORMAL,
                Math.max(0, Math.min(AdvancedAlloyFurnaceRecipe.MAX_TIER, tier))));
    }

    @Override public List<CoreRecipeHolder<T>> findMatchingRecipes(Level level, Map<Ingredient, Long> items,
            Map<FluidStack, Long> fluids, ItemStack mold) {
        if (!matchesMold(mold)) return List.of();
        List<CoreRecipeHolder<T>> matches = new ArrayList<>();
        for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
            if (!type.isInstance(recipe)) continue;
            var holder = new CoreRecipeHolder<>(recipe.getId(), type.cast(recipe));
            for (var converted : convertAll(holder, level)) {
                Map<Ingredient, Long> required = new LinkedHashMap<>();
                converted.inputs().forEach(i -> required.merge(i.ingredient(), i.count(), Long::sum));
                if (AdapterUtils.matchesRequired(items, required)) { matches.add(holder); break; }
            }
        }
        return matches;
    }
}
