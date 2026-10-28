package com.sorrowmist.useless.compat.kubejs;

import appeng.api.stacks.GenericStack;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.*;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** Preserves the counted-ingredient script shape using KubeJS 6 schema components. */
public final class AdvancedAlloyFurnaceRecipeSchema {
    private static final RecipeComponentBuilder COUNTED_INGREDIENT = RecipeComponent.builder(
            ForgeRecipeComponents.INGREDIENT.key("ingredient"), NumberComponent.LONG.key("count").optional(1L)).inputRole();
    private static final RecipeComponentBuilder SIZED_FLUID_INGREDIENT = RecipeComponent.builder(
            ForgeRecipeComponents.FLUID_INGREDIENT.key("ingredient"), NumberComponent.LONG.key("amount")).inputRole();
    public static final RecipeKey<String> ID = StringComponent.ID.key("id");
    public static final RecipeKey<RecipeComponentBuilderMap[]> INGREDIENTS = COUNTED_INGREDIENT.asArray().key("ingredients").allowEmpty();
    public static final RecipeKey<RecipeComponentBuilderMap[]> INPUT_FLUIDS = SIZED_FLUID_INGREDIENT.asArray().key("input_fluids")
            .optional(new RecipeComponentBuilderMap[0]).allowEmpty();
    public static final RecipeKey<GenericStack[]> KEY_INPUTS = GenericStackRecipeComponent.GENERIC_STACK.asArray().key("key_inputs")
            .optional(new GenericStack[0]).allowEmpty();
    public static final RecipeKey<ItemStack[]> OUTPUTS = ForgeRecipeComponents.ITEM_STACK.asArray().key("outputs")
            .optional(new ItemStack[0]).allowEmpty();
    public static final RecipeKey<FluidStack[]> OUTPUT_FLUIDS = ForgeRecipeComponents.FLUID_STACK.asArray().key("output_fluids")
            .optional(new FluidStack[0]).allowEmpty();
    public static final RecipeKey<GenericStack[]> KEY_OUTPUTS = GenericStackRecipeComponent.GENERIC_STACK.asArray().key("key_outputs")
            .optional(new GenericStack[0]).allowEmpty();
    public static final RecipeKey<InputItem> MOLD = ForgeRecipeComponents.INGREDIENT.key("mold").optional(InputItem.EMPTY).allowEmpty();
    public static final RecipeKey<InputItem[]> MOLDS = ForgeRecipeComponents.INGREDIENT.asArray().key("molds")
            .optional(new InputItem[0]).allowEmpty();
    public static final RecipeKey<Integer> TIER = NumberComponent.ANY_INT.key("tier").optional(AdvancedAlloyFurnaceRecipe.NO_EXPLICIT_TIER);
    public static final RecipeKey<Long> ENERGY = NumberComponent.LONG.key("energy").optional(2000L);
    public static final RecipeKey<Integer> PROCESS_TIME = NumberComponent.INT.key("process_time").optional(200);
    public static final RecipeKey<InputItem> CATALYST = ForgeRecipeComponents.INGREDIENT.key("catalyst").optional(InputItem.EMPTY).allowEmpty();
    private AdvancedAlloyFurnaceRecipeSchema() {}
    public static final RecipeSchema SCHEMA = new RecipeSchema(ID, INGREDIENTS, INPUT_FLUIDS, KEY_INPUTS, OUTPUTS,
            OUTPUT_FLUIDS, KEY_OUTPUTS, MOLD, MOLDS, TIER, ENERGY, PROCESS_TIME, CATALYST)
            .constructor(ID, INGREDIENTS, OUTPUTS)
            .constructor(ID, INGREDIENTS, OUTPUTS, MOLD)
            .constructor(ID, INGREDIENTS, OUTPUTS, MOLD, TIER)
            .constructor(ID, INGREDIENTS, OUTPUTS, MOLD, MOLDS, TIER);
}