package com.sorrowmist.useless.content.multiblock;

import com.sorrowmist.useless.content.blocks.multiblock.OmniversalAlloyFurnaceStructure;
import com.sorrowmist.useless.content.blocks.multiblock.UselessCoilBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.LinkedHashMap;
import java.util.List;

/** Standard build: one required module of each type, the other ports filled with jade. */
public final class OmniversalFurnaceMaterials {
    private OmniversalFurnaceMaterials() {}
    public record Requirement(ItemStack stack,int count) {}

    /** Enumerate the same predicate that validates the structure, including data-pack casings. */
    public static List<ItemStack> candidates(OmniversalAlloyFurnaceStructure.Part part) {
        if (part==OmniversalAlloyFurnaceStructure.Part.AIR) return List.of();
        return BuiltInRegistries.BLOCK.stream()
                .filter(b -> b.asItem()!=Items.AIR && OmniversalAlloyFurnaceStructure.matches(part,b.defaultBlockState()))
                .map(b -> new ItemStack(b.asItem())).toList();
    }

    /** Usage lookup includes optional hatches and alternative casings, not just the default bill. */
    public static List<ItemStack> usageItems() {
        var items = new LinkedHashMap<Item,ItemStack>();
        for (var part : OmniversalAlloyFurnaceStructure.Part.values()) {
            for (var stack : candidates(part)) items.putIfAbsent(stack.getItem(),stack);
        }
        for (var required : requirements(1)) items.putIfAbsent(required.stack().getItem(),required.stack());
        return List.copyOf(items.values());
    }

    public static BlockState state(OmniversalAlloyFurnaceStructure.Entry entry,int coilTier) {
        if(coilTier<UselessCoilBlock.MIN_TIER||coilTier>UselessCoilBlock.MAX_TIER)
            throw new IllegalArgumentException("Invalid coil tier: "+coilTier);
        OmniversalAlloyFurnaceStructure.FunctionalPart module=null;
        if(entry.part()==OmniversalAlloyFurnaceStructure.Part.INTERFACE) {
            if(entry.localPos().equals(OmniversalAlloyFurnaceStructure.PREFERRED_PATTERN_ASSEMBLY_POS))
                module=OmniversalAlloyFurnaceStructure.FunctionalPart.PATTERN_ASSEMBLY;
            else if(entry.localPos().equals(OmniversalAlloyFurnaceStructure.PREFERRED_MOLD_HUB_POS))
                module=OmniversalAlloyFurnaceStructure.FunctionalPart.MOLD_HUB;
        }
        return OmniversalFurnaceAutoBuilder.expectedState(entry.part(),coilTier,module,Direction.NORTH);
    }

    public static List<Requirement> requirements(int coilTier) {
        var counts=new LinkedHashMap<Item,Integer>();
        for(var entry:OmniversalAlloyFurnaceStructure.entries()) {
            if(entry.part()==OmniversalAlloyFurnaceStructure.Part.AIR)continue;
            counts.merge(state(entry,coilTier).getBlock().asItem(),1,Integer::sum);
        }
        return counts.entrySet().stream().map(e->new Requirement(new ItemStack(e.getKey()),e.getValue())).toList();
    }
}
