package com.sorrowmist.useless.content.recipe.adapters.hostilenetworks;

import java.util.List;

/** Canonical model checks shared by EHNN templates and whole-tier upgrade calculations. */
public final class ModelBoundaryRules {
    private ModelBoundaryRules() {}
    public static boolean sameFour(String expected, List<String> ids) {
        return expected != null && ids != null && ids.size() == 4 && ids.stream().allMatch(expected::equals);
    }
    public static long steps(long span, int perStep) {
        return span <= 0 || perStep <= 0 ? 0 : 1 + (span - 1) / perStep;
    }
}
