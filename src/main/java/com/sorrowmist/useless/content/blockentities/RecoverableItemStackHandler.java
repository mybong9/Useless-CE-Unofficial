package com.sorrowmist.useless.content.blockentities;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Objects;
import java.util.Collections;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/** Fixed backing storage whose configured overflow becomes withdraw-only recovery storage. */
public class RecoverableItemStackHandler extends ItemStackHandler {
    public static final int MAX_SLOTS = 4096;

    private final IntSupplier activeSlots;
    private final int minimumActiveSlots;
    private final Predicate<ItemStack> validator;
    private final Runnable changeListener;
    private int changeBatchDepth;
    private boolean changePending;
    private boolean highestOccupiedKnown;
    private int highestOccupied = -1;
    private int scannedSlotCount = -1;

    public int getHighestOccupiedSlot() {
        // Slot quick-move can shrink the returned ItemStack in place before a
        // handler notification. Revalidate the cached tail to keep pages accurate.
        if (!highestOccupiedKnown || scannedSlotCount != getSlots()
                || (highestOccupied >= 0 && getStackInSlot(highestOccupied).isEmpty())) {
            highestOccupied = -1;
            for (int slot = getSlots() - 1; slot >= 0; slot--) {
                if (!getStackInSlot(slot).isEmpty()) { highestOccupied = slot; break; }
            }
            scannedSlotCount = getSlots();
            highestOccupiedKnown = true;
        }
        return highestOccupied;
    }

    public RecoverableItemStackHandler(IntSupplier activeSlots, Predicate<ItemStack> validator, Runnable changeListener) {
        this(MAX_SLOTS, 27, activeSlots, validator, changeListener);
    }

    public RecoverableItemStackHandler(int capacity, int minimumActiveSlots, IntSupplier activeSlots,
                                       Predicate<ItemStack> validator, Runnable changeListener) {
        super(validateCapacity(capacity));
        this.activeSlots = Objects.requireNonNull(activeSlots, "activeSlots");
        this.minimumActiveSlots = Math.max(0, Math.min(capacity, minimumActiveSlots));
        this.validator = Objects.requireNonNull(validator, "validator");
        this.changeListener = Objects.requireNonNull(changeListener, "changeListener");
    }

    public int getActiveSlots() {
        return Math.max(minimumActiveSlots, Math.min(getSlots(), activeSlots.getAsInt()));
    }

    public boolean isRecoverySlot(int slot) {
        return slot >= getActiveSlots() && slot < getSlots();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot >= 0 && slot < getActiveSlots() && validator.test(stack);
    }

    protected final boolean isValidItem(ItemStack stack) {
        return validator.test(stack);
    }

    /** Groups several backing-slot writes into one inventory change callback. */
    public void withChangeBatch(Runnable action) {
        changeBatchDepth++;
        try {
            action.run();
        } finally {
            changeBatchDepth--;
            if (changeBatchDepth == 0 && changePending) {
                changePending = false;
                changeListener.run();
            }
        }
    }

    @Override
    protected void onContentsChanged(int slot) {
        highestOccupiedKnown = false;
        if (changeBatchDepth > 0) {
            changePending = true;
        } else {
            changeListener.run();
        }
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        highestOccupiedKnown = false;
        CompoundTag copy = nbt.copy();
        copy.remove("Size");
        Collections.fill(this.stacks, ItemStack.EMPTY);
        super.deserializeNBT(copy);
    }

    private static int validateCapacity(int capacity) {
        if (capacity <= 0 || capacity > MAX_SLOTS) {
            throw new IllegalArgumentException("Capacity must be between 1 and " + MAX_SLOTS);
        }
        return capacity;
    }

    public CompoundTag serializeNBT(HolderLookup.Provider ignored) { return serializeNBT(); }
    public void deserializeNBT(HolderLookup.Provider ignored, CompoundTag tag) { deserializeNBT(tag); }
}
