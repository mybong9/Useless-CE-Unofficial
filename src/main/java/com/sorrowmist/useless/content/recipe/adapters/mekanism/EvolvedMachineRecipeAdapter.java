package com.sorrowmist.useless.content.recipe.adapters.mekanism;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.CountedIngredient;
import com.sorrowmist.useless.api.recipe.IRecipeAdapter;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipes;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Uses Evolved's registered recipes, including datapack additions. */
public final class EvolvedMachineRecipeAdapter implements IRecipeAdapter<MekanismRecipe> {
    private final RecipeType<MekanismRecipe> type;
    private final Class<MekanismRecipe> sourceClass;
    private final ItemStack mold;
    private final List<Method> itemInputs;
    private final Method gasInput;
    private final Method output;

    @SuppressWarnings("unchecked")
    public EvolvedMachineRecipeAdapter(boolean chemixer) throws ReflectiveOperationException {
        String machine = chemixer ? "chemixer" : "alloyer";
        type = (RecipeType<MekanismRecipe>) registeredType(chemixer ? "CHEMIXING" : "ALLOYING");
        sourceClass = (Class<MekanismRecipe>) Class.forName("fr.iglee42.evolvedmekanism.recipes."
                + (chemixer ? "ChemixerRecipe" : "AlloyerRecipe"));
        mold = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("evolvedmekanism", machine)));
        itemInputs = chemixer ? List.of(sourceClass.getMethod("getInputMain"), sourceClass.getMethod("getInputExtra"))
                : List.of(sourceClass.getMethod("getMainInput"), sourceClass.getMethod("getExtraInput"),
                        sourceClass.getMethod("getTertiaryExtraInput"));
        gasInput = chemixer ? sourceClass.getMethod("getInputGas") : null;
        output = sourceClass.getMethod("getOutputDefinition");
    }

    public static RecipeType<?> registeredType(String field) throws ReflectiveOperationException {
        var registry = Class.forName("fr.iglee42.evolvedmekanism.registries.EMRecipeType");
        var handle = (mekanism.common.registration.impl.RecipeTypeRegistryObject<?, ?>)
                registry.getField(field).get(null);
        if (handle == null) throw new NoSuchFieldException("Unregistered Evolved recipe type " + field);
        return handle.getRecipeType();
    }

    @Override public Class<MekanismRecipe> getRecipeClass() { return sourceClass; }
    @Override public ItemStack getMoldItem() { return mold.copy(); }

    @Override
    public List<CoreRecipeHolder<MekanismRecipe>> findMatchingRecipes(Level level, Map<Ingredient, Long> items,
            Map<FluidStack, Long> fluids, Map<AEKey, Long> keys, ItemStack actualMold) {
        if (level == null || type == null || items.isEmpty() || !matchesMold(actualMold)) return List.of();
        return CoreRecipes.ofType(level.getRecipeManager(), type);
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<MekanismRecipe> holder, Level level) {
        MekanismRecipe source = holder.value();
        if (type == null || source.getType() != type || source.isIncomplete() || !sourceClass.isInstance(source)) return List.of();
        try {
            List<CountedIngredient> inputs = new ArrayList<>();
            for (Method method : itemInputs) {
                var converted = MekanismChemicalRecipeSupport.items((ItemStackIngredient) method.invoke(source));
                if (converted.isEmpty()) return List.of();
                inputs.addAll(converted);
            }
            var outputs = ((List<?>) output.invoke(source)).stream().map(ItemStack.class::cast)
                    .filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
            if (outputs.isEmpty()) return List.of();
            if (gasInput == null) return List.of(convert(AdapterUtils.convertedId(holder.id()), inputs, outputs, List.of()));
            var gas = (ChemicalStackIngredient<?, ?>) gasInput.invoke(source);
            List<AdvancedAlloyFurnaceRecipe> recipes = new ArrayList<>();
            for (ChemicalStack<?> stack : gas.getRepresentations()) {
                GenericStack key = MekanismChemicalRecipeSupport.key(stack);
                if (key == null) continue;
                var id = stack.getType().getRegistryName();
                recipes.add(convert(MekanismChemicalRecipeSupport.variantId(holder.id(),
                        id.getNamespace() + "_" + id.getPath()), inputs, outputs, List.of(key)));
            }
            return recipes;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalArgumentException("Cannot read Evolved recipe " + holder.id(), failure);
        }
    }

    private AdvancedAlloyFurnaceRecipe convert(ResourceLocation id, List<CountedIngredient> inputs,
            List<ItemStack> outputs, List<GenericStack> gases) {
        return MekanismChemicalRecipeSupport.recipe(id, inputs, List.of(), gases, outputs, List.of(), List.of(),
                AdapterUtils.DEFAULT_ENERGY, AdapterUtils.MEKANISM_BASE_TICKS_REQUIRED, mold);
    }
}
