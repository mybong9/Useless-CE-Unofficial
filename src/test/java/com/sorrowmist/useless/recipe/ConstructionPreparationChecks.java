package com.sorrowmist.useless.recipe;

import com.sorrowmist.useless.content.multiblock.ConstructionAmounts;
import java.util.LinkedHashMap;
import java.util.Map;

/** Focused arithmetic checks; no Minecraft client or server is started. */
public final class ConstructionPreparationChecks {
    public static void main(String[] args) {
        Map<String, Long> required = Map.of("jade", 2217L, "coil", 128L, "core", 1L);
        check(ConstructionAmounts.matches(required, required, 1), "Full quantities must be accepted");
        check(ConstructionAmounts.matches(required, Map.of("jade", 4434L, "coil", 256L, "core", 2L), 2), "Two complete structures");
        check(!ConstructionAmounts.matches(required, Map.of("jade", 1L, "coil", 128L, "core", 1L), 1), "Truncated counts must fail");
        check(!ConstructionAmounts.matches(required, Map.of("jade", 2217L, "coil", 128L), 1), "Missing core must fail");
        Map<String, Long> extra = new LinkedHashMap<>(required);
        extra.put("other", 1L);
        check(!ConstructionAmounts.matches(required, extra, 1), "Never swallow extra items");
        check(!ConstructionAmounts.matches(required, required, 0), "Zero batch must fail");
        check(!ConstructionAmounts.matches(required, required, -1), "Negative batch must fail");
        check(!ConstructionAmounts.matches(required, required, Long.MAX_VALUE), "Overflow must fail");
        check(!ConstructionAmounts.matches(Map.of(), Map.of(), 1), "Empty recipes must fail");
        System.out.println("Construction preparation: 9 focused quantity checks passed.");
    }

    private static void check(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }
}
