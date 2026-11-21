package com.sorrowmist.useless.mixin.exdeorum;

import com.sorrowmist.useless.compat.exdeorum.MechanicalSievePages;
import com.sorrowmist.useless.compat.exdeorum.PagedSieveOutputSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "thedarkcolour.exdeorum.menu.MechanicalSieveMenu", remap = false)
public abstract class MechanicalSieveMenuMixin extends AbstractContainerMenu implements MechanicalSievePages {
    @Unique private final DataSlot useless$outputPage = DataSlot.standalone();
    @Unique private final DataSlot useless$outputPages = DataSlot.standalone();
    @Unique private final int[] useless$visibleCounts = new int[PAGE_SIZE];

    protected MechanicalSieveMenuMixin() {
        super(null, 0);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lthedarkcolour/exdeorum/blockentity/MechanicalSieveBlockEntity;)V",
            at = @At("RETURN"), require = 1)
    private void useless$paginateOutputs(int id, Inventory playerInventory, @Coerce BlockEntity machine, CallbackInfo ci) {
        addDataSlot(useless$outputPage);
        addDataSlot(useless$outputPages);
        for (int index = 0; index < PAGE_SIZE; index++) {
            int visibleSlot = index;
            addDataSlot(new DataSlot() {
                @Override public int get() {
                    Slot slot = getSlot(visibleSlot + 2);
                    return slot instanceof PagedSieveOutputSlot output
                            ? output.storedCount() : useless$visibleCounts[visibleSlot];
                }
                @Override public void set(int value) { useless$visibleCounts[visibleSlot] = value; }
            });
        }
        if (playerInventory.player.level().isClientSide) return;
        if (!(getSlot(2) instanceof SlotItemHandler output)
                || !(output.getItemHandler() instanceof IItemHandlerModifiable inventory)) return;

        useless$outputPages.set(Math.max(1, (inventory.getSlots() - 2 + PAGE_SIZE - 1) / PAGE_SIZE));
        for (int index = 2; index < 2 + PAGE_SIZE; index++) {
            var original = getSlot(index);
            Slot replacement = new PagedSieveOutputSlot(
                    inventory, index, original.x, original.y, useless$outputPage::get, machine::setChanged);
            replacement.index = index;
            slots.set(index, replacement);
        }
    }

    @Inject(method = {"quickMoveStack", "m_7648_"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void useless$quickMoveBulkOutput(Player player, int index, CallbackInfoReturnable<ItemStack> callback) {
        if (index < 2 || index >= 2 + PAGE_SIZE || !(getSlot(index) instanceof PagedSieveOutputSlot output)) return;
        ItemStack original = output.getItem().copy();
        ItemStack remaining = original.copy();
        if (original.isEmpty() || !moveItemStackTo(remaining, 2 + PAGE_SIZE, slots.size(), true)) {
            callback.setReturnValue(ItemStack.EMPTY);
            return;
        }
        output.remove(original.getCount() - remaining.getCount());
        callback.setReturnValue(original);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != PREVIOUS && id != NEXT) return super.clickMenuButton(player, id);
        int target = useless$page() + (id == PREVIOUS ? -1 : 1);
        if (!stillValid(player) || target < 0 || target >= useless$pageCount()) return false;
        useless$outputPage.set(target);
        broadcastFullState();
        return true;
    }

    @Override public int useless$page() { return useless$outputPage.get(); }
    @Override public int useless$pageCount() { return Math.max(1, useless$outputPages.get()); }
    @Override public int useless$storedCount(int visibleSlot) { return useless$visibleCounts[visibleSlot]; }
}
