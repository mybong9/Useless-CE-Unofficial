package com.sorrowmist.useless.mixin.exdeorum;

import com.sorrowmist.useless.core.config.ConfigManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        var items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag item = items.getCompound(index);
            int slot = item.getInt("Slot");
            if (slot < 2) continue;
            int count = getStackInSlot(slot).getCount();
            if (count > 64) {
                item.putByte("Count", (byte) 1);
                item.putInt("UselessSieveCount", count);
            }
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        int minimumSize = ConfigManager.mechanicalSieveOutputSlots() + 2;
        if (tag.getInt("Size") < minimumSize) {
            tag = tag.copy();
            tag.putInt("Size", minimumSize);
        }
        super.deserializeNBT(tag);
        var items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag item = items.getCompound(index);
            int slot = item.getInt("Slot");
            int count = item.getInt("UselessSieveCount");
            if (slot >= 2 && slot < getSlots() && count > 64 && count <= 4096) {
                getStackInSlot(slot).setCount(count);
            }
        }
    }
}
