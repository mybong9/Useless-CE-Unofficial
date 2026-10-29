package com.sorrowmist.useless.content.recipe.adapters.gtceu;

import java.util.List;

/** Exact expected-output batches; the same multiplier applies to inputs, fluids, EU and ticks. */
public record GregTechBatchPlan(long operations) {
    public record Chance(int chance, int maximum, long ticks) {
        public Chance {
            if (maximum <= 0 || chance < 0 || chance > maximum || ticks <= 0)
                throw new IllegalArgumentException("Invalid GT output chance");
        }
    }
    public static GregTechBatchPlan create(List<Chance> chances) {
        long operations = 1;
        for (Chance output : chances) {
            long numerator = Math.multiplyExact((long) output.chance, output.ticks);
            long denominator = output.maximum / gcd(numerator, output.maximum);
            operations = Math.multiplyExact(operations / gcd(operations, denominator), denominator);
            if (operations > Integer.MAX_VALUE) throw new ArithmeticException("GT probability batch too large");
        }
        return new GregTechBatchPlan(operations);
    }
    public long inputFactor(long ticks) { return Math.multiplyExact(operations, ticks); }
    public long outputFactor(Chance chance) {
        long numerator = Math.multiplyExact(inputFactor(chance.ticks), chance.chance);
        if (numerator % chance.maximum != 0) throw new IllegalArgumentException("Chance does not belong to this GT batch");
        return numerator / chance.maximum;
    }
    public long energy(long eut, int ticks, int fePerEU) {
        if (eut < 0 || ticks <= 0 || fePerEU <= 0) throw new IllegalArgumentException("Invalid GT energy");
        return Math.multiplyExact(Math.multiplyExact(Math.multiplyExact(eut, ticks), fePerEU), operations);
    }
    public int duration(int ticks) { return Math.toIntExact(inputFactor(ticks)); }
    private static long gcd(long a, long b) {
        while (b != 0) { long remainder = a % b; a = b; b = remainder; }
        return a;
    }
}
