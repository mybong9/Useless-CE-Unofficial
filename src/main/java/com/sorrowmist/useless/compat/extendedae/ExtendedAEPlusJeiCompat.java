package com.sorrowmist.useless.compat.extendedae;


public final class ExtendedAEPlusJeiCompat {
    private ExtendedAEPlusJeiCompat() {
    }
    public static void presetAlloyFurnaceSearchKey() {
        try {
            Class.forName("com.extendedae_plus.util.uploadPattern.RecipeTypeNameConfig")
                    .getMethod("setLastProcessingName", String.class)
                    .invoke(null, "Omni Alloy Furnace");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // This convenience hook is absent from older Forge 1.20.1 releases.
        }
    }
}
