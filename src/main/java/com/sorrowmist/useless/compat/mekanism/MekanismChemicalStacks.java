package com.sorrowmist.useless.compat.mekanism;

import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.merged.BoxedChemicalStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Preserves the gas/infusion/pigment/slurry category required by Mekanism 10.4. */
public final class MekanismChemicalStacks {
    private MekanismChemicalStacks() {}
    public static ChemicalStack<?> copyWithAmount(ChemicalStack<?> stack, long amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative chemical amount");
        if (amount == 0 || stack.isEmpty()) return GasStack.EMPTY;
        ChemicalStack<?> copy = stack.copy();
        copy.setAmount(amount);
        return copy;
    }
    public static boolean sameChemical(ChemicalStack<?> first, ChemicalStack<?> second) {
        return first.getType() == second.getType();
    }
    public static CompoundTag save(ChemicalStack<?> stack, HolderLookup.Provider registries) {
        return BoxedChemicalStack.box(stack).write(new CompoundTag());
    }
    public static ChemicalStack<?> parseOptional(HolderLookup.Provider registries, CompoundTag tag) {
        var boxed = BoxedChemicalStack.read(tag);
        return boxed.isEmpty() ? GasStack.EMPTY : boxed.getChemicalStack();
    }
}
