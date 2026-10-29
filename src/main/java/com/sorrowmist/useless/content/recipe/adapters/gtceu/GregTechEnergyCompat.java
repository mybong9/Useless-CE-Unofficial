package com.sorrowmist.useless.content.recipe.adapters.gtceu;

/** Names differ between GT 1.4.x and modern GT; never silently assume a conversion rate. */
public final class GregTechEnergyCompat {
    private GregTechEnergyCompat() {}
    public static int fePerEU(Object energyConfig) throws ReflectiveOperationException {
        Object value;
        try { value = energyConfig.getClass().getField("feToEuRatio").get(energyConfig); }
        catch (NoSuchFieldException legacy) {
            value = energyConfig.getClass().getField("platformToEuRatio").get(energyConfig);
        }
        int ratio = ((Number) value).intValue();
        if (ratio <= 0) throw new IllegalArgumentException("Invalid GT FE/EU conversion ratio");
        return ratio;
    }
}
