package com.sorrowmist.useless.content.recipe.adapters.bloodmagic;

/** Smallest exact batch covering both probabilistic outputs and a consumable tool's lifetime. */
public record ArcBatchPlan(long operations, long outputFactor, long tools, int ticks) {
    public static ArcBatchPlan create(int probabilityOperations, long toolUses) {
        if (probabilityOperations <= 0 || toolUses <= 0) throw new IllegalArgumentException("Positive ARC batch units required");
        long a = probabilityOperations, b = toolUses;
        while (b != 0) { long t = a % b; a = b; b = t; }
        long operations = Math.multiplyExact(probabilityOperations / a, toolUses);
        return new ArcBatchPlan(operations, operations / probabilityOperations, operations / toolUses,
                Math.toIntExact(Math.multiplyExact(100L, operations)));
    }
}
