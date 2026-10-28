package com.sorrowmist.useless.compat.appmek;

import appeng.api.stacks.GenericStack;
import com.sorrowmist.useless.compat.jei.GenericStackJeiIngredientProvider;
import mekanism.api.IMekanismAccess;
import mekanism.api.chemical.ChemicalStack;
import mekanism.client.jei.ChemicalStackRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import me.ramidzkh.mekae2.ae2.MekanismKey;

/** Selects the JEI type for Mekanism 10.4's four chemical domains. */
public final class AppMekJeiChemicalCompat {
    private AppMekJeiChemicalCompat() {}
    public static GenericStackJeiIngredientProvider createProvider() {
        return AppMekJeiChemicalCompat::resolve;
    }
    private static GenericStackJeiIngredientProvider.Ingredient resolve(GenericStack stack) {
        if (!(stack.what() instanceof MekanismKey key)) return null;
        ChemicalStack<?> chemical = key.withAmount(Math.max(1L, stack.amount()));
        if (chemical.isEmpty()) return null;
        var helpers = IMekanismAccess.INSTANCE.jeiHelper();
        IIngredientType<?> type = switch (key.getForm()) {
            case MekanismKey.GAS -> helpers.getGasStackHelper().getIngredientType();
            case MekanismKey.INFUSION -> helpers.getInfusionStackHelper().getIngredientType();
            case MekanismKey.PIGMENT -> helpers.getPigmentStackHelper().getIngredientType();
            case MekanismKey.SLURRY -> helpers.getSlurryStackHelper().getIngredientType();
            default -> null;
        };
        return type == null ? null : new GenericStackJeiIngredientProvider.Ingredient(type, chemical,
                new ChemicalStackRenderer<>(Math.max(1L, chemical.getAmount()), 16, 16));
    }
}