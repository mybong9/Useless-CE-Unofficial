package com.sorrowmist.useless.content.recipe.adapters.mekanism;

import com.sorrowmist.useless.content.recipe.AdapterUtils;
import mekanism.api.math.FloatingLong;

/** Loaded only by the optional Mekanism adapters. */
public final class MekanismRecipeEnergy {
    private MekanismRecipeEnergy() {}
    public static int cost(long energy, long operations) {
        return AdapterUtils.mekanismEnergyCost(energy, operations);
    }
    public static int cost(long energy, long ticks, long operations) {
        return AdapterUtils.mekanismEnergyCost(energy, ticks, operations);
    }
    public static int cost(FloatingLong energy, long ticks, long operations) {
        return AdapterUtils.safeInt(Math.round(energy.doubleValue() * ticks * operations));
    }
}
