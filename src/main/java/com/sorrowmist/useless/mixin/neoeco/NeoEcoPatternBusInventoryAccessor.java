package com.sorrowmist.useless.mixin.neoeco;

import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Keeps native interface previews on the compact bus's real aggregate inventory. */
@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity", remap = false)
public interface NeoEcoPatternBusInventoryAccessor {
    @Accessor("inventory")
    appeng.util.inv.AppEngInternalInventory useless$getPhysicalPatternInventory();

    @Mutable
    @Accessor("itemHandler")
    void useless$setPatternItemHandler(IItemHandlerModifiable handler);
}
