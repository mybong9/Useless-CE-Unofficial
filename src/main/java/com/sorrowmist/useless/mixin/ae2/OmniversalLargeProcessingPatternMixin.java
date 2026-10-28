package com.sorrowmist.useless.mixin.ae2;

import appeng.api.stacks.GenericStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only our server-built full-recipe patterns may exceed AE2's normal slot bounds. */
@Mixin(targets = "appeng.crafting.pattern.ProcessingPatternEncoding", remap = false)
public abstract class OmniversalLargeProcessingPatternMixin {
    @Inject(method = "getProcessingInputs", at = @At("HEAD"), cancellable = true)
    private static void uselessMod$fullInputs(CompoundTag tag, CallbackInfoReturnable<GenericStack[]> cir) {
        if (tag.getBoolean("useless_mod_full_recipe")) cir.setReturnValue(read(tag, "in"));
    }
    @Inject(method = "getProcessingOutputs", at = @At("HEAD"), cancellable = true)
    private static void uselessMod$fullOutputs(CompoundTag tag, CallbackInfoReturnable<GenericStack[]> cir) {
        if (tag.getBoolean("useless_mod_full_recipe")) cir.setReturnValue(read(tag, "out"));
    }
    @org.spongepowered.asm.mixin.Unique
    private static GenericStack[] read(CompoundTag tag, String key) {
        var list = tag.getList(key, Tag.TAG_COMPOUND);
        if (list.isEmpty() || list.size() > 256) throw new IllegalArgumentException("Invalid full-recipe slot count");
        GenericStack[] stacks = new GenericStack[list.size()];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = GenericStack.readTag(list.getCompound(i));
            if (stacks[i] == null || stacks[i].amount() <= 0L) throw new IllegalArgumentException("Invalid full-recipe stack");
        }
        return stacks;
    }
}
