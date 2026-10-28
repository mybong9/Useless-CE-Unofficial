package com.sorrowmist.useless.content.recipe.adapters.pneumaticcraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Retains native StackedIngredient item/tag choices after moving their count into the recipe. */
public final class StackedAssemblyInputJson {
    private StackedAssemblyInputJson() {}

    public static JsonElement withoutCount(JsonElement source) {
        if (source.isJsonArray()) {
            var result = new JsonArray();
            for (JsonElement choice : source.getAsJsonArray()) result.add(withoutCount(choice));
            return result;
        }
        JsonObject value = source.getAsJsonObject();
        JsonObject result = new JsonObject();
        // Native StackedIngredient compares item identity and amount, not stack NBT.
        // Other custom ingredient types never enter this normalizer.
        if (value.has("item") && !value.has("tag")) result.add("item", value.get("item").deepCopy());
        else if (value.has("tag") && !value.has("item")) result.add("tag", value.get("tag").deepCopy());
        else throw new IllegalArgumentException("Unsupported PneumaticCraft stacked ingredient");
        return result;
    }
}
