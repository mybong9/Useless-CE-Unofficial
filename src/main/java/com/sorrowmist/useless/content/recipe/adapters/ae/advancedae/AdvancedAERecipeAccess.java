package com.sorrowmist.useless.content.recipe.adapters.ae.advancedae;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Bridges IngredientStack's move from AdvancedAE into AE2 Addon Lib. */
final class AdvancedAERecipeAccess {
    private AdvancedAERecipeAccess() {}

    private static final ClassValue<Method> FLUID = new ClassValue<>() {
        @Override protected Method computeValue(Class<?> type) { return method(type, "getFluid"); }
    };
    private static final ClassValue<StackAccess> STACK = new ClassValue<>() {
        @Override protected StackAccess computeValue(Class<?> type) {
            return new StackAccess(method(type, "getIngredient"), method(type, "getAmount"), method(type, "isEmpty"));
        }
    };

    static Object fluid(Object recipe) { return invoke(FLUID.get(recipe.getClass()), recipe); }
    static Object ingredient(Object stack) { return invoke(STACK.get(stack.getClass()).ingredient(), stack); }
    static long amount(Object stack) { return ((Number) invoke(STACK.get(stack.getClass()).amount(), stack)).longValue(); }
    static boolean isEmpty(Object stack) {
        return stack == null || (Boolean) invoke(STACK.get(stack.getClass()).empty(), stack);
    }

    private static Method method(Class<?> type, String name) {
        try { return type.getMethod(name); }
        catch (NoSuchMethodException error) {
            throw new IllegalStateException("Unsupported AdvancedAE recipe API: " + type.getName() + "." + name, error);
        }
    }

    private static Object invoke(Method method, Object receiver) {
        try { return method.invoke(receiver); }
        catch (IllegalAccessException error) {
            throw new IllegalStateException("Cannot access AdvancedAE recipe method " + method, error);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error fatal) throw fatal;
            throw new IllegalStateException("AdvancedAE recipe method failed: " + method, cause);
        }
    }

    private record StackAccess(Method ingredient, Method amount, Method empty) {}
}
