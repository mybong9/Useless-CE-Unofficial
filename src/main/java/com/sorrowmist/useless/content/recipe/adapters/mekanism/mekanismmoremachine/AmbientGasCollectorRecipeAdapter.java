package com.sorrowmist.useless.content.recipe.adapters.mekanism.mekanismmoremachine;

import appeng.api.stacks.GenericStack;
import com.jerry.mekmm.common.config.MoreMachineConfig;
import com.jerry.mekmm.common.registries.MoreMachineBlocks;
import com.jerry.mekmm.common.registries.MoreMachineGas;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.adapters.mekanism.MekanismChemicalRecipeSupport;
import com.sorrowmist.useless.content.recipe.adapters.mekanism.MekanismSyntheticRecipe;
import com.sorrowmist.useless.content.recipe.adapters.mekanism.MekanismSyntheticRecipeAdapter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Exposes Mekanism More Machine's ambient gas collector as a water-fed recipe. */
public final class AmbientGasCollectorRecipeAdapter extends MekanismSyntheticRecipeAdapter {
    private static final int PROCESS_TICKS = 19;
    private static final ResourceLocation RECIPE_ID =
            new ResourceLocation("mekmm", "ambient_gas_collector_water");

    @Override
    public @Nullable ItemStack getMoldItem() {
        return new ItemStack(MoreMachineBlocks.AMBIENT_GAS_COLLECTOR.getBlock());
    }

    @Override
    protected List<CoreRecipeHolder<MekanismSyntheticRecipe>> createGeneratedRecipes(Level level) {
        if (level == null) {
            return List.of();
        }

        int gasAmount = MoreMachineConfig.general.gasCollectAmount.get();
        if (gasAmount <= 0) {
            return List.of();
        }

        GenericStack output = MekanismChemicalRecipeSupport.key(
                new mekanism.api.chemical.gas.GasStack(MoreMachineGas.UNSTABLE_DIMENSIONAL_GAS.getChemical(), gasAmount));
        if (output == null) {
            return List.of();
        }

        AdvancedAlloyFurnaceRecipe converted = MekanismChemicalRecipeSupport.recipe(
                RECIPE_ID,
                List.of(),
                List.of(new FluidStack(Fluids.WATER, gasAmount)),
                List.of(),
                List.of(),
                List.of(),
                List.of(output),
                MekanismChemicalRecipeSupport.saturatingMultiply(
                        Math.round(MoreMachineConfig.usage.ambientGasCollector.get().doubleValue()), PROCESS_TICKS),
                PROCESS_TICKS,
                getMoldItem());
        return List.of(MekanismChemicalRecipeSupport.syntheticHolder(RECIPE_ID, converted));
    }
}
