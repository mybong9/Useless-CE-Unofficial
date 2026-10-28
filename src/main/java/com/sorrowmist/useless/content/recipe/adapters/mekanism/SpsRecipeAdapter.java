package com.sorrowmist.useless.content.recipe.adapters.mekanism;

import appeng.api.stacks.GenericStack;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.Mekanism;
import mekanism.common.config.MekanismConfig;
import mekanism.common.registries.MekanismGases;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Dynamic SPS recipe-viewer adapter. */
public final class SpsRecipeAdapter extends MekanismSyntheticRecipeAdapter {
    private static final long PROCESS_TICKS = AdapterUtils.MEKANISM_BASE_TICKS_REQUIRED;

    @Override
    public @Nullable ItemStack getMoldItem() {
        return new ItemStack(MekanismBlocks.SPS_CASING.getBlock());
    }

    @Override
    protected List<CoreRecipeHolder<MekanismSyntheticRecipe>> createGeneratedRecipes(Level level) {
        List<CoreRecipeHolder<MekanismSyntheticRecipe>> result = new ArrayList<>();
        // The SPS has no common recipe type. Mirror Mekanism's recipe-viewer source
        // from common classes so dedicated servers never load client-only classes.
        var input = IngredientCreatorAccess.gas().from(new mekanism.api.chemical.gas.GasStack(
                MekanismGases.POLONIUM.getChemical(), MekanismConfig.general.spsInputPerAntimatter.get()));
        GenericStack outputKey = MekanismChemicalRecipeSupport.key(new mekanism.api.chemical.gas.GasStack(MekanismGases.ANTIMATTER.getChemical(), 1));
        if (outputKey == null) return result;

        for (var concreteInput : input.getRepresentations()) {
            GenericStack inputKey = MekanismChemicalRecipeSupport.key(concreteInput);
            if (inputKey == null) continue;
            ResourceLocation id = MekanismChemicalRecipeSupport.variantId(
                    Mekanism.rl("antimatter"), "sps_" + id(concreteInput));
            AdvancedAlloyFurnaceRecipe converted = MekanismChemicalRecipeSupport.recipe(
                    id, List.of(), List.of(), List.of(inputKey), List.of(), List.of(), List.of(outputKey),
                    MekanismChemicalRecipeSupport.saturatingMultiply(
                            Math.round(MekanismConfig.general.spsEnergyPerInput.get().doubleValue()), concreteInput.getAmount()),
                    AdapterUtils.safeInt(PROCESS_TICKS), getMoldItem());
            result.add(MekanismChemicalRecipeSupport.syntheticHolder(id, converted));
        }
        return result;
    }

    private static String id(mekanism.api.chemical.ChemicalStack stack) {
        var id = stack.getType().getRegistryName();
        return id.getNamespace() + "_" + id.getPath().replace('/', '_');
    }
}
