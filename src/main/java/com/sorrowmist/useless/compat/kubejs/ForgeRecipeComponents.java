package com.sorrowmist.useless.compat.kubejs;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.sorrowmist.uselesscore.api.fluid.*;
import com.sorrowmist.uselesscore.api.recipe.CoreItemStackCodecs;
import dev.latvian.mods.kubejs.fluid.FluidStackJS;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.*;
import dev.latvian.mods.kubejs.recipe.component.*;
import dev.latvian.mods.kubejs.util.JsonIO;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import java.util.ArrayList;

/** KubeJS 6 script wrappers that serialize through the shared Forge recipe codecs. */
final class ForgeRecipeComponents {
    private ForgeRecipeComponents() {}
    static final RecipeComponent<InputItem> INGREDIENT = new RecipeComponent<>() {
        @Override public Class<?> componentClass() { return InputItem.class; }
        @Override public ComponentRole role() { return ComponentRole.INPUT; }
        @Override public InputItem read(RecipeJS recipe, Object from) { return ItemComponents.INPUT.read(recipe, from); }
        @Override public JsonElement write(RecipeJS recipe, InputItem value) { return value.ingredient.toJson(); }
        @Override public boolean isInput(RecipeJS recipe, InputItem value, ReplacementMatch match) {
            return ItemComponents.INPUT.isInput(recipe, value, match);
        }
        @Override public InputItem replaceInput(RecipeJS recipe, InputItem value, ReplacementMatch match, InputReplacement replacement) {
            return ItemComponents.INPUT.replaceInput(recipe, value, match, replacement);
        }
    };
    static final RecipeComponent<ItemStack> ITEM_STACK = new CodecComponent<>(ItemStack.class,
            CoreItemStackCodecs.STACK, ComponentRole.OUTPUT) {
        @Override public ItemStack read(RecipeJS recipe, Object from) {
            if (from instanceof ItemStack stack) return stack.copy();
            JsonElement json = JsonIO.of(from);
            if (json.isJsonObject() && json.getAsJsonObject().has("id")) return decode(json);
            return OutputItem.of(from).item.copy();
        }
        @Override public boolean isOutput(RecipeJS recipe, ItemStack value, ReplacementMatch match) {
            return ItemComponents.OUTPUT.isOutput(recipe, OutputItem.of(value, 1D), match);
        }
        @Override public ItemStack replaceOutput(RecipeJS recipe, ItemStack value, ReplacementMatch match, OutputReplacement replacement) {
            return ItemComponents.OUTPUT.replaceOutput(recipe, OutputItem.of(value, 1D), match, replacement).item.copy();
        }
    };
    static final RecipeComponent<FluidStack> FLUID_STACK = new CodecComponent<>(FluidStack.class,
            CoreFluidStackCodecs.STACK, ComponentRole.OUTPUT) {
        @Override public FluidStack read(RecipeJS recipe, Object from) {
            if (from instanceof FluidStack stack) return stack.copy();
            JsonElement json = JsonIO.of(from);
            if (json.isJsonObject() && (json.getAsJsonObject().has("id") || json.getAsJsonObject().has("FluidName"))) return decode(json);
            FluidStackJS wrapped = FluidStackJS.of(from);
            FluidStack stack = new FluidStack(wrapped.getFluid(), Math.toIntExact(wrapped.getAmount()));
            if (wrapped.getNbt() != null) stack.setTag(wrapped.getNbt().copy());
            return stack;
        }
        @Override public boolean isOutput(RecipeJS recipe, FluidStack value, ReplacementMatch match) {
            return FluidComponents.OUTPUT.isOutput(recipe, wrap(value), match);
        }
        @Override public FluidStack replaceOutput(RecipeJS recipe, FluidStack value, ReplacementMatch match, OutputReplacement replacement) {
            return read(recipe, FluidComponents.OUTPUT.replaceOutput(recipe, wrap(value), match, replacement));
        }
        private FluidStackJS wrap(FluidStack stack) {
            return FluidStackJS.of(net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(stack.getFluid()).toString(),
                    stack.getAmount(), stack.getTag());
        }
    };
    static final RecipeComponent<FluidIngredient> FLUID_INGREDIENT = new CodecComponent<>(FluidIngredient.class,
            FluidIngredient.CODEC, ComponentRole.INPUT) {
        @Override public FluidIngredient read(RecipeJS recipe, Object from) {
            if (from instanceof FluidIngredient ingredient) return ingredient;
            JsonElement json = JsonIO.of(from);
            var decoded = FluidIngredient.CODEC.parse(JsonOps.INSTANCE, json).result();
            if (decoded.isPresent()) return decoded.get();
            if (json.isJsonArray()) {
                var children = new ArrayList<FluidIngredient>();
                for (JsonElement child : json.getAsJsonArray()) children.add(read(recipe, child));
                return FluidIngredient.anyOf(children);
            }
            String tag = json.isJsonPrimitive() && json.getAsString().startsWith("#")
                    ? json.getAsString().substring(1)
                    : json.isJsonObject() && json.getAsJsonObject().has("tag")
                        ? json.getAsJsonObject().get("tag").getAsString() : null;
            if (tag != null) return FluidIngredient.tag(TagKey.create(Registries.FLUID, new ResourceLocation(tag)));
            FluidStack stack = FLUID_STACK.read(recipe, from);
            return stack.hasTag() ? DataComponentFluidIngredient.of(true, stack) : FluidIngredient.single(stack);
        }
    };
    private abstract static class CodecComponent<T> implements RecipeComponent<T> {
        private final Class<T> type;
        private final Codec<T> codec;
        private final ComponentRole role;
        CodecComponent(Class<T> type, Codec<T> codec, ComponentRole role) { this.type = type; this.codec = codec; this.role = role; }
        @Override public Class<?> componentClass() { return type; }
        @Override public ComponentRole role() { return role; }
        @Override public JsonElement write(RecipeJS recipe, T value) {
            return codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow(false,
                    message -> { throw new IllegalArgumentException(message); });
        }
        protected T decode(JsonElement json) {
            return codec.parse(JsonOps.INSTANCE, json).getOrThrow(false,
                    message -> { throw new IllegalArgumentException(message); });
        }
    }
}