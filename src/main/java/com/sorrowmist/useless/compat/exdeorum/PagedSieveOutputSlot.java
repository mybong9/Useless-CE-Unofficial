package com.sorrowmist.useless.compat.exdeorum;

import java.util.function.IntSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

public final class PagedSieveOutputSlot extends SlotItemHandler {
    private final IItemHandlerModifiable inventory;
    private final int firstPageSlot;
    private final IntSupplier page;
    private final Runnable markChanged;

    public PagedSieveOutputSlot(IItemHandlerModifiable inventory, int slot, int x, int y,
            IntSupplier page, Runnable markChanged) {
        super(inventory, slot, x, y);
        this.inventory = inventory;
        this.firstPageSlot = slot;
        this.page = page;
        this.markChanged = markChanged;
    }

    private int backingSlot() {
        return firstPageSlot + page.getAsInt() * MechanicalSievePages.PAGE_SIZE;
    }

    @Override public boolean isActive() { return backingSlot() < inventory.getSlots(); }
    @Override public boolean mayPlace(ItemStack stack) { return false; }
    @Override public boolean mayPickup(Player player) { return isActive() && !getItem().isEmpty(); }
    @Override public ItemStack getItem() {
        return isActive() ? inventory.getStackInSlot(backingSlot()) : ItemStack.EMPTY;
    }
    @Override public int getMaxStackSize() { return isActive() ? inventory.getSlotLimit(backingSlot()) : 0; }
    @Override public int getMaxStackSize(ItemStack stack) { return Math.min(getMaxStackSize(), stack.getMaxStackSize()); }
    @Override public void set(ItemStack stack) {
        if (isActive()) {
            inventory.setStackInSlot(backingSlot(), stack);
            setChanged();
        }
    }
    @Override public ItemStack remove(int amount) {
        if (!isActive()) return ItemStack.EMPTY;
        ItemStack extracted = inventory.extractItem(backingSlot(), amount, false);
        if (!extracted.isEmpty()) setChanged();
        return extracted;
    }
    @Override public void setChanged() { markChanged.run(); }
}
