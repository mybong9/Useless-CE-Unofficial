package com.sorrowmist.useless.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.config.LockCraftingMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderReturnInventory;
import com.sorrowmist.uselesscore.api.ae.CoreConstructionPattern;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.SmartDoublingPatterns;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

/** Return prepared resources through AE's normal, persisted return inventory before completing the job. */
@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class ConstructionPatternProviderMixin {
    @Shadow @Final private IManagedGridNode mainNode;
    @Shadow @Final private IActionSource actionSource;
    @Shadow @Final private List<IPatternDetails> patterns;
    @Shadow @Final private List<GenericStack> sendList;
    @Shadow @Final private PatternProviderReturnInventory returnInv;
    @Shadow public abstract LockCraftingMode getCraftingLockedReason();
    @Shadow public abstract void saveChanges();
    @Shadow private void onPushPatternSuccess(IPatternDetails pattern) { throw new AssertionError(); }

    @Unique private final ArrayDeque<GenericStack> uselessMod$constructionMaterials = new ArrayDeque<>();
    @Unique private GenericStack uselessMod$constructionReceipt;

    @Inject(method = "pushPattern", at = @At("HEAD"), cancellable = true)
    private void uselessMod$prepare(IPatternDetails pattern, KeyCounter[] inputs,
                                   CallbackInfoReturnable<Boolean> callback) {
        var resolved = SmartDoublingPatterns.resolve(pattern);
        if (!(resolved.pattern() instanceof CoreConstructionPattern construction)) return;
        callback.setReturnValue(false);
        if (!mainNode.isActive() || !patterns.contains(pattern) || !sendList.isEmpty()
                || !returnInv.isEmpty() || !uselessMod$constructionMaterials.isEmpty()
                || uselessMod$constructionReceipt != null || getCraftingLockedReason() != LockCraftingMode.NONE
                || !construction.matches(inputs, resolved.operationsPerPush())) return;

        // AE owns and debits the input holders once pushPattern succeeds. Never extract from ME a second time.
        uselessMod$constructionMaterials.addAll(Arrays.asList(construction.materials(resolved.operationsPerPush())));
        uselessMod$constructionReceipt = new GenericStack(
                construction.getPrimaryOutput().what(), resolved.operationsPerPush());
        onPushPatternSuccess(pattern);
        saveChanges();
        mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
        callback.setReturnValue(true);
    }

    @Inject(method = "hasWorkToDo", at = @At("RETURN"), cancellable = true)
    private void uselessMod$pendingWork(CallbackInfoReturnable<Boolean> callback) {
        if (!uselessMod$constructionMaterials.isEmpty() || uselessMod$constructionReceipt != null)
            callback.setReturnValue(true);
    }

    @Inject(method = "doWork", at = @At("HEAD"))
    private void uselessMod$returnMaterials(CallbackInfoReturnable<Boolean> callback) {
        boolean changed = false;
        returnInv.beginBatch();
        try {
            while (!uselessMod$constructionMaterials.isEmpty()) {
                GenericStack stack = uselessMod$constructionMaterials.peekFirst();
                long inserted = returnInv.insert(stack.what(), stack.amount(), Actionable.MODULATE, actionSource);
                if (inserted <= 0) break;
                changed = true;
                uselessMod$constructionMaterials.removeFirst();
                if (inserted < stack.amount()) {
                    uselessMod$constructionMaterials.addFirst(new GenericStack(stack.what(), stack.amount() - inserted));
                    break;
                }
            }
            // A receipt must not reach the crafting CPU while materials are still waiting for ME space.
            if (uselessMod$constructionMaterials.isEmpty() && returnInv.isEmpty()
                    && uselessMod$constructionReceipt != null) {
                GenericStack receipt = uselessMod$constructionReceipt;
                long inserted = returnInv.insert(receipt.what(), receipt.amount(), Actionable.MODULATE, actionSource);
                if (inserted > 0) {
                    changed = true;
                    uselessMod$constructionReceipt = inserted == receipt.amount() ? null
                            : new GenericStack(receipt.what(), receipt.amount() - inserted);
                }
            }
        } finally {
            returnInv.endBatch();
        }
        if (changed) saveChanges();
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void uselessMod$save(CompoundTag tag, CallbackInfo callback) {
        ListTag materials = new ListTag();
        for (var stack : uselessMod$constructionMaterials) materials.add(GenericStack.writeTag(stack));
        tag.put("useless_mod_construction_materials", materials);
        tag.remove("useless_mod_construction_receipt");
        if (uselessMod$constructionReceipt != null)
            tag.put("useless_mod_construction_receipt", GenericStack.writeTag(uselessMod$constructionReceipt));
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void uselessMod$load(CompoundTag tag, CallbackInfo callback) {
        uselessMod$constructionMaterials.clear();
        ListTag materials = tag.getList("useless_mod_construction_materials", Tag.TAG_COMPOUND);
        for (int i = 0; i < materials.size(); i++) {
            GenericStack stack = GenericStack.readTag(materials.getCompound(i));
            if (stack != null && stack.what() instanceof AEItemKey && stack.amount() > 0)
                uselessMod$constructionMaterials.addLast(stack);
        }
        uselessMod$constructionReceipt = tag.contains("useless_mod_construction_receipt", Tag.TAG_COMPOUND)
                ? GenericStack.readTag(tag.getCompound("useless_mod_construction_receipt")) : null;
    }

    @Inject(method = "addDrops", at = @At("TAIL"))
    private void uselessMod$dropPending(List<ItemStack> drops, CallbackInfo callback) {
        for (var stack : uselessMod$constructionMaterials) uselessMod$addStacks(drops, stack);
        if (uselessMod$constructionMaterials.isEmpty() && returnInv.isEmpty()
                && uselessMod$constructionReceipt != null) uselessMod$addStacks(drops, uselessMod$constructionReceipt);
    }

    @Unique private static void uselessMod$addStacks(List<ItemStack> drops, GenericStack stack) {
        if (!(stack.what() instanceof AEItemKey key)) return;
        int max = key.toStack().getMaxStackSize();
        for (long left = stack.amount(); left > 0;) {
            int count = (int) Math.min(left, max);
            drops.add(key.toStack(count));
            left -= count;
        }
    }

    @Inject(method = "clearContent", at = @At("TAIL"))
    private void uselessMod$clear(CallbackInfo callback) {
        uselessMod$constructionMaterials.clear();
        uselessMod$constructionReceipt = null;
    }
}
