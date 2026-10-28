package com.sorrowmist.useless.client.compat;

/** Optional viewer classes are isolated behind holders that load only after a mod-presence check. */
public final class RecipeViewerClient {
    private RecipeViewerClient() {}

    public static void refreshRecipes(boolean jeiLoaded) {
        if (jeiLoaded) Jei.refresh();
    }

    public static boolean showFurnaceRecipes(boolean emiLoaded, boolean jeiLoaded) {
        if (emiLoaded) {
            Emi.show();
            return true;
        }
        if (jeiLoaded) {
            Jei.show();
            return true;
        }
        return false;
    }

    private static final class Jei {
        static void refresh() { com.sorrowmist.useless.compat.jei.JEIPlugin.refreshAlloyFurnaceRecipes(); }
        static void show() { com.sorrowmist.useless.compat.jei.JEIPlugin.showAdvancedAlloyFurnaceRecipes(); }
    }

    private static final class Emi {
        static void show() { com.sorrowmist.useless.compat.emi.OmniversalEmiPlugin.showFurnaceRecipes(); }
    }
}
