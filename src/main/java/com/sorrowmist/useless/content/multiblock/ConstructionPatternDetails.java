package com.sorrowmist.useless.content.multiblock;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.sorrowmist.useless.content.items.OmniversalConstructionPatternItem;
import com.sorrowmist.useless.content.items.OmniversalConstructionReceiptItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import java.util.LinkedHashMap;
import java.util.Map;

/** Materials are temporary AE inputs; only a receipt is a craftable output, avoiding recipe cycles. */
public final class ConstructionPatternDetails implements com.sorrowmist.uselesscore.api.ae.CoreConstructionPattern {
    private final AEItemKey definition;
    private final AEItemKey receipt;
    private final Map<AEKey, Long> materials = new LinkedHashMap<>();
    private final IInput[] inputs;

    private ConstructionPatternDetails(AEItemKey definition, ConstructionMaterialBill bill) {
        this.definition = definition;
        this.receipt = AEItemKey.of(OmniversalConstructionReceiptItem.create(bill.tier()));
        for (var row : bill.entries()) {
            var id = new ResourceLocation(row.item());
            if (!BuiltInRegistries.ITEM.containsKey(id)) throw new IllegalArgumentException("Missing building material");
            var item = BuiltInRegistries.ITEM.get(id);
            if (item == Items.AIR) throw new IllegalArgumentException("Invalid building material");
            materials.put(AEItemKey.of(new ItemStack(item)), (long) row.count());
        }
        inputs = materials.entrySet().stream().map(e -> new MaterialInput(e.getKey(), e.getValue()))
                .toArray(IInput[]::new);
    }

    public static ConstructionPatternDetails decode(AEItemKey definition) {
        if (definition == null) return null;
        try {
            return OmniversalConstructionPatternItem.read(definition.toStack())
                    .map(bill -> new ConstructionPatternDetails(definition, bill)).orElse(null);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    public boolean matches(KeyCounter[] holders, long copies) {
        if (holders == null) return false;
        Map<AEKey, Long> supplied = new LinkedHashMap<>();
        try {
            for (var holder : holders) {
                if (holder == null) continue;
                for (var entry : holder) {
                    long amount = entry.getLongValue();
                    if (amount < 0) return false;
                    if (amount > 0) supplied.merge(entry.getKey(), amount, Math::addExact);
                }
            }
            return ConstructionAmounts.matches(materials, supplied, copies);
        } catch (ArithmeticException overflow) {
            return false;
        }
    }

    public GenericStack[] materials(long copies) {
        return materials.entrySet().stream()
                .map(e -> new GenericStack(e.getKey(), Math.multiplyExact(e.getValue(), copies)))
                .toArray(GenericStack[]::new);
    }

    @Override public AEItemKey getDefinition() { return definition; }
    @Override public IInput[] getInputs() { return inputs.clone(); }
    @Override public GenericStack[] getOutputs() { return new GenericStack[]{new GenericStack(receipt, 1)}; }
    @Override public boolean supportsPushInputsToExternalInventory() { return false; }
    @Override public int hashCode() { return definition.hashCode(); }
    @Override public boolean equals(Object other) {
        return other instanceof ConstructionPatternDetails details && definition.equals(details.definition);
    }

    private record MaterialInput(AEKey key, long count) implements IInput {
        @Override public GenericStack[] getPossibleInputs() { return new GenericStack[]{new GenericStack(key, 1)}; }
        @Override public long getMultiplier() { return count; }
        @Override public boolean isValid(AEKey input, Level level) { return key.equals(input); }
        @Override public AEKey getRemainingKey(AEKey input) { return null; }
    }
}
