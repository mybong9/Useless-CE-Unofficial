package com.sorrowmist.useless.compat.mekanism;

import com.google.gson.JsonElement;
import com.sorrowmist.useless.content.recipe.CountedIngredient;
import com.sorrowmist.uselesscore.api.fluid.DataComponentFluidIngredient;
import com.sorrowmist.uselesscore.api.fluid.FluidIngredient;
import com.sorrowmist.uselesscore.api.fluid.SizedFluidIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CompoundIngredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;

/** Converts Mekanism's public JSON ingredient form without expanding tags to fixed item lists. */
public final class MekanismIngredientConversions {
    private MekanismIngredientConversions() {}
    public static CountedIngredient item(ItemStackIngredient source) {
        return source == null || source.hasNoMatchingInstances() ? null : item(source.serialize());
    }
    private static CountedIngredient item(JsonElement json) {
        if (json.isJsonArray()) {
            var alternatives = new ArrayList<Ingredient>();
            long amount = -1;
            for (JsonElement child : json.getAsJsonArray()) {
                CountedIngredient converted = item(child);
                if (converted == null) return null;
                if (amount != -1 && amount != converted.count()) {
                    throw new IllegalArgumentException("Mekanism item alternatives require distinct quantities");
                }
                amount = converted.count();
                alternatives.add(converted.ingredient());
            }
            return alternatives.isEmpty() ? null : new CountedIngredient(
                    CompoundIngredient.of(alternatives.toArray(Ingredient[]::new)), amount);
        }
        var object = json.getAsJsonObject();
        long amount = object.has("amount") ? object.get("amount").getAsLong() : 1;
        Ingredient ingredient = Ingredient.fromJson(object.get("ingredient"));
        return amount <= 0 || ingredient.isEmpty() ? null : new CountedIngredient(ingredient, amount);
    }
    public static SizedFluidIngredient fluid(FluidStackIngredient source) {
        return source == null || source.hasNoMatchingInstances() ? null : fluid(source.serialize());
    }
    private static SizedFluidIngredient fluid(JsonElement json) {
        if (json.isJsonArray()) {
            var alternatives = new ArrayList<FluidIngredient>();
            int amount = -1;
            for (JsonElement child : json.getAsJsonArray()) {
                SizedFluidIngredient converted = fluid(child);
                if (converted == null) return null;
                if (amount != -1 && amount != converted.amount()) {
                    throw new IllegalArgumentException("Mekanism fluid alternatives require distinct quantities");
                }
                amount = converted.amount();
                alternatives.add(converted.ingredient());
            }
            return alternatives.isEmpty() ? null : new SizedFluidIngredient(FluidIngredient.anyOf(alternatives), amount);
        }
        var object = json.getAsJsonObject();
        int amount = object.get("amount").getAsInt();
        if (amount <= 0) return null;
        if (object.has("tag")) {
            return new SizedFluidIngredient(FluidIngredient.tag(TagKey.create(Registries.FLUID,
                    new ResourceLocation(object.get("tag").getAsString()))), amount);
        }
        var id = new ResourceLocation(object.get("fluid").getAsString());
        var fluid = ForgeRegistries.FLUIDS.getValue(id);
        if (fluid == null) throw new IllegalArgumentException("Unknown Mekanism fluid: " + id);
        FluidStack stack = new FluidStack(fluid, amount);
        if (object.has("nbt")) {
            try { stack.setTag(TagParser.parseTag(object.get("nbt").getAsString())); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
                throw new IllegalArgumentException("Invalid Mekanism fluid NBT", exception);
            }
        }
        return new SizedFluidIngredient(DataComponentFluidIngredient.of(true, stack), amount);
    }
}
