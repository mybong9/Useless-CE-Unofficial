package com.sorrowmist.useless.recipe;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/** Exercises the production bridge with JEI, EMI, Forge and Minecraft unavailable to its classloader. */
public final class OptionalRecipeViewerChecks {
    public static void main(String[] args) throws Exception {
        try (var loader = new MissingViewersLoader(Path.of(args[0]).toUri().toURL())) {
            Class<?> bridge = Class.forName("com.sorrowmist.useless.client.compat.RecipeViewerClient", true, loader);
            bridge.getMethod("refreshRecipes", boolean.class).invoke(null, false);
            Object shown = bridge.getMethod("showFurnaceRecipes", boolean.class, boolean.class).invoke(null, false, false);
            check(Boolean.FALSE.equals(shown), "No-viewer button should be a safe no-op");
            check(loader.optionalLoads == 0, "Missing JEI must not be resolved during catalog refresh");

            // Prove the loader would detect the old unconditional call instead of masking it.
            try {
                bridge.getMethod("refreshRecipes", boolean.class).invoke(null, true);
                throw new AssertionError("Present-viewer path should reach the deliberately blocked plugin");
            } catch (InvocationTargetException expected) {
                check(expected.getCause() instanceof LinkageError && loader.optionalLoads > 0,
                        "Optional plugin must load only after the positive presence check");
            }
        }
        System.out.println("Optional recipe viewer checks passed: no-JEI refresh, no-viewer open, guarded plugin loading.");
    }

    private static final class MissingViewersLoader extends URLClassLoader {
        int optionalLoads;
        MissingViewersLoader(URL classes) { super(new URL[]{classes}, ClassLoader.getPlatformClassLoader()); }
        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("mezz.jei.") || name.startsWith("dev.emi.")
                    || name.startsWith("com.sorrowmist.useless.compat.jei.")
                    || name.startsWith("com.sorrowmist.useless.compat.emi.")) {
                optionalLoads++;
                throw new ClassNotFoundException("Optional viewer deliberately absent: " + name);
            }
            return super.loadClass(name, resolve);
        }
    }

    private static void check(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }
}
