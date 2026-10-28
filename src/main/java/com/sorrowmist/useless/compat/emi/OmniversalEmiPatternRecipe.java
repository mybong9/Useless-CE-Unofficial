package com.sorrowmist.useless.compat.emi;

import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.emi.emi.api.recipe.EmiRecipe;

/** Recipes whose full, authoritative pattern is encoded server-side. */
public interface OmniversalEmiPatternRecipe extends EmiRecipe {
    void encode(PatternEncodingTermMenu menu);
    default boolean canEncode() { return !getInputs().isEmpty() && !getOutputs().isEmpty(); }
}
