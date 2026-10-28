package com.sorrowmist.useless.compat.kubejs;

import appeng.api.stacks.GenericStack;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.sorrowmist.uselesscore.api.network.codec.CoreAeCodecs;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.util.JsonIO;

public final class GenericStackRecipeComponent implements RecipeComponent<GenericStack> {
    public static final GenericStackRecipeComponent GENERIC_STACK = new GenericStackRecipeComponent();
    private GenericStackRecipeComponent() {}
    @Override public Class<?> componentClass() { return GenericStack.class; }
    @Override public String componentType() { return "generic_stack"; }
    @Override public GenericStack read(RecipeJS recipe, Object from) {
        if (from instanceof GenericStack stack) return stack;
        return CoreAeCodecs.GENERIC_STACK.parse(JsonOps.INSTANCE, JsonIO.of(from))
                .getOrThrow(false, message -> { throw new IllegalArgumentException(message); });
    }
    @Override public JsonElement write(RecipeJS recipe, GenericStack value) {
        return CoreAeCodecs.GENERIC_STACK.encodeStart(JsonOps.INSTANCE, value)
                .getOrThrow(false, message -> { throw new IllegalArgumentException(message); });
    }
    @Override public boolean hasPriority(RecipeJS recipe, Object from) {
        return from instanceof java.util.Map<?, ?> || from instanceof JsonElement || from instanceof GenericStack;
    }
    @Override public String checkEmpty(dev.latvian.mods.kubejs.recipe.RecipeKey<GenericStack> key, GenericStack value) {
        return value == null || value.amount() <= 0 ? "Generic stack amount must be positive" : "";
    }
}