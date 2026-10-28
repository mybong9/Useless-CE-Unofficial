package com.sorrowmist.useless.content.recipe.adapters.ae.advancedae;

/** Standalone checks for the two binary-incompatible ingredient wrapper shapes. */
public final class AdvancedAERecipeAccessChecks {
    public static void main(String[] args) {
        var oldRecipe = new OldRecipe();
        var newRecipe = new NewRecipe();
        Object oldFluid = AdvancedAERecipeAccess.fluid(oldRecipe);
        Object newFluid = AdvancedAERecipeAccess.fluid(newRecipe);
        check(oldFluid == oldRecipe.fluid && newFluid == newRecipe.fluid, "typed fluid returns");
        check(AdvancedAERecipeAccess.amount(oldFluid) == 1000, "old int amount");
        check(AdvancedAERecipeAccess.amount(newFluid) == 3_000_000_000L, "new long amount");
        check("old fluid".equals(AdvancedAERecipeAccess.ingredient(oldFluid)), "old ingredient");
        check("new fluid".equals(AdvancedAERecipeAccess.ingredient(newFluid)), "new ingredient");
        check(!AdvancedAERecipeAccess.isEmpty(oldFluid) && !AdvancedAERecipeAccess.isEmpty(newFluid), "nonempty requirements");
        check(AdvancedAERecipeAccess.isEmpty(null), "absent fluid");
        check(AdvancedAERecipeAccess.isEmpty(new EmptyStack()), "explicit empty fluid");
        try {
            AdvancedAERecipeAccess.fluid(new Object());
            throw new AssertionError("unsupported API accepted");
        } catch (IllegalStateException expected) {
            check(expected.getCause() instanceof NoSuchMethodException, "unsupported API remains an error");
        }
        System.out.println("AdvancedAE old/new typed fluid access, quantities and empty requirements: PASS");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static final class OldRecipe {
        final OldStack fluid = new OldStack();
        public OldStack getFluid() { return fluid; }
    }
    public static final class NewRecipe {
        final NewStack fluid = new NewStack();
        public NewStack getFluid() { return fluid; }
    }
    public static class OldStack {
        public Object getIngredient() { return "old fluid"; }
        public int getAmount() { return 1000; }
        public boolean isEmpty() { return false; }
    }
    public static final class NewStack {
        public Object getIngredient() { return "new fluid"; }
        public long getAmount() { return 3_000_000_000L; }
        public boolean isEmpty() { return false; }
    }
    public static final class EmptyStack extends OldStack {
        @Override public boolean isEmpty() { return true; }
    }
}
