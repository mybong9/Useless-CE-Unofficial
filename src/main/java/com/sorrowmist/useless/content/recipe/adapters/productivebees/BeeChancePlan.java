package com.sorrowmist.useless.content.recipe.adapters.productivebees;

/** Failed conversions keep the bee/block/item; only attempts cost additional time or reagents. */
public record BeeChancePlan(int attempts, int successes) {
    public static BeeChancePlan exclusive(int percent) { return create(percent); }
    public static BeeChancePlan inclusive(int bound) { return create((long) bound + 1); }
    private static BeeChancePlan create(long raw) {
        int percent = (int) Math.max(0, Math.min(100, raw));
        if (percent == 0) return null;
        int a = percent, b = 100;
        while (b != 0) { int remainder = a % b; a = b; b = remainder; }
        return new BeeChancePlan(100 / a, percent / a);
    }
}
