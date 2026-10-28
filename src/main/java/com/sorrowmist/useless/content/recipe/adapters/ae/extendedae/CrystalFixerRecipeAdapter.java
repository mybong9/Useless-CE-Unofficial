package com.sorrowmist.useless.content.recipe.adapters.ae.extendedae;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.glodblock.github.extendedae.common.EPPItemAndBlock;
import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipe;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;

/** Converts the hard-coded repair stages used by ExtendedAE's 1.20.1 TileCrystalFixer. */
public final class CrystalFixerRecipeAdapter implements IRecipeAdapter<CrystalFixerRecipeAdapter.Repair> {
    @Override public Class<Repair> getRecipeClass() { return Repair.class; }
    @Override public ItemStack getMoldItem() { return new ItemStack(EPPItemAndBlock.CRYSTAL_FIXER); }

    @Override public List<CoreRecipeHolder<Repair>> getGeneratedRecipes(Level level) {
        var stages = List.of(AEBlocks.QUARTZ_BLOCK.block(), AEBlocks.DAMAGED_BUDDING_QUARTZ.block(),
                AEBlocks.CHIPPED_BUDDING_QUARTZ.block(), AEBlocks.FLAWED_BUDDING_QUARTZ.block());
        List<CoreRecipeHolder<Repair>> result = new ArrayList<>();
        for (int i = 0; i < stages.size() - 1; i++) {
            var id = new ResourceLocation("useless_mod", "converted/extendedae/crystal_fixer/step_" + i);
            // Average 2 growth per work step, 100 growth per repair, 10% fuel use per step.
            var converted = new AdvancedAlloyFurnaceRecipe(id,
                    List.of(new CountedIngredient(Ingredient.of(stages.get(i)), 1),
                            new CountedIngredient(Ingredient.of(AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED.asItem()), 5)),
                    List.of(), List.of(new ItemStack(stages.get(i + 1))), List.of(),
                    50L * 50L * AdapterUtils.AE_TO_FE_CONVERSION, 50, Ingredient.EMPTY, 0,
                    Ingredient.of(getMoldItem()), AlloyFurnaceMode.NORMAL);
            result.add(new CoreRecipeHolder<>(id, new Repair(converted)));
        }
        return result;
    }

    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<Repair> holder, Level level) {
        return List.of(holder.value().recipe());
    }

    public record Repair(AdvancedAlloyFurnaceRecipe recipe) implements CoreRecipe<Container> {
        @Override public ResourceLocation getId() { return recipe.id(); }
        @Override public boolean matches(Container input, Level level) { return false; }
        @Override public boolean canCraftInDimensions(int width, int height) { return false; }
        @Override public ItemStack assemble(Container input, HolderLookup.Provider registries) { return ItemStack.EMPTY; }
        @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return recipe.outputs().get(0).copy(); }
        @Override public RecipeSerializer<?> getSerializer() { return null; }
        @Override public RecipeType<?> getType() { return null; }
    }
}
