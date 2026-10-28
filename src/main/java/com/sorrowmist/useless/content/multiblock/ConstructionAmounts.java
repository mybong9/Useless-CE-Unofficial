package com.sorrowmist.useless.content.multiblock;

import java.util.Map;

/** Exact validation before a provider accepts ownership of a material preparation batch. */
public final class ConstructionAmounts {
    private ConstructionAmounts() {}

    public static <K> boolean matches(Map<K, Long> required, Map<K, Long> supplied, long copies) {
        return com.sorrowmist.uselesscore.api.crafting.CoreMaterialAmounts.matches(required, supplied, copies);
    }
}
