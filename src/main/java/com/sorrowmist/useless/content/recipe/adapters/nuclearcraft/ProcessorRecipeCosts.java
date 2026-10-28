package com.sorrowmist.useless.content.recipe.adapters.nuclearcraft;

/** NuclearCraft floors the time and FE/tick independently before processing. */
public final class ProcessorRecipeCosts {
    private ProcessorRecipeCosts() {}

    public static int ticks(double base, double modifier) {
        double value = base * modifier;
        if (!Double.isFinite(value) || value < 0 || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid process time");
        }
        return Math.max(1, (int) value);
    }

    public static long energy(double base, double modifier, int ticks) {
        double perTick = base * modifier;
        if (ticks <= 0 || !Double.isFinite(perTick) || perTick < 0 || perTick > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid process energy");
        }
        return Math.multiplyExact((long) perTick, ticks);
    }
}
