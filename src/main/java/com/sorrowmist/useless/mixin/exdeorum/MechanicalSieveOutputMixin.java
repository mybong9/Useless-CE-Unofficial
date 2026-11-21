package com.sorrowmist.useless.mixin.exdeorum;

import com.sorrowmist.useless.core.config.ConfigManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "thedarkcolour.exdeorum.blockentity.MechanicalSieveBlockEntity", remap = false)
public abstract class MechanicalSieveOutputMixin {
    @Inject(method = "handleResultItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void useless$insertBulkOutput(ItemStack result, ServerLevel level, RandomSource random,
            CallbackInfoReturnable<Boolean> callback) {
        BlockEntity machine = (BlockEntity) (Object) this;
        if (!(machine.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null)
                instanceof IItemHandlerModifiable inventory) || result.isEmpty()) return;

        int total = result.getCount();
        int remaining = total;
        int end = Math.min(inventory.getSlots(), ConfigManager.mechanicalSieveOutputSlots() + 2);
        for (int slot = 2; slot < end && remaining > 0; slot++) {
            ItemStack existing = inventory.getStackInSlot(slot);
            if (!existing.isEmpty() && !ItemHandlerHelper.canItemStacksStack(result, existing)) continue;
            int limit = ConfigManager.mechanicalSieveOutputStackLimit();
            int amount = Math.min(remaining, Math.max(0, limit - existing.getCount()));
            if (amount == 0) continue;
            inventory.setStackInSlot(slot, result.copyWithCount(existing.getCount() + amount));
            remaining -= amount;
        }
        if (remaining < total) machine.setChanged();
        callback.setReturnValue(remaining < total);
    }
}
