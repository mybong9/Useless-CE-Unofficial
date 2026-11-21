package com.sorrowmist.useless.mixin.exdeorum;

import com.sorrowmist.useless.core.config.ConfigManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo
@Mixin(targets = "thedarkcolour.exdeorum.blockentity.MechanicalSieveBlockEntity$ItemHandler", remap = false)
public abstract class MechanicalSieveInventoryMixin extends ItemStackHandler {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 22), require = 1)
    private static int useless$expandInventory(int original) {
        return ConfigManager.mechanicalSieveOutputSlots() + 2;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        int minimumSize = ConfigManager.mechanicalSieveOutputSlots() + 2;
        if (tag.getInt("Size") < minimumSize) {
            tag = tag.copy();
            tag.putInt("Size", minimumSize);
        }
        super.deserializeNBT(tag);
    }
}
