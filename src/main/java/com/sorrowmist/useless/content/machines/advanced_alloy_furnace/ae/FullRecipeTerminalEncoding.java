package com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae;

import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.core.definitions.AEItems;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Atomic full-recipe encoding for recipes wider than the terminal's visible ghost slots. */
public final class FullRecipeTerminalEncoding {
    private FullRecipeTerminalEncoding() {}
    public static boolean required(PatternEncodingTermMenu menu, AlloyFurnaceRecipeCatalog.Entry entry) {
        // Every selected omniversal recipe is encoded from the authoritative server
        // catalog. Ghost slots cannot preserve arbitrary counts, catalysts or custom
        // predicates and optional terminals may replace their normal encoding path.
        return true;
    }
    public static boolean required(PatternEncodingTermMenu menu, AdvancedAlloyFurnaceRecipe recipe) {
        return recipe.inputs().stream().anyMatch(input -> input.count() > 64)
                || recipe.outputs().stream().anyMatch(output -> output.getCount() > 64)
                || recipe.inputs().size() + recipe.inputFluids().size() + recipe.keyInputs().size() > menu.getProcessingInputSlots().length
                || recipe.outputs().size() + recipe.outputFluids().size() + recipe.keyOutputs().size() > menu.getProcessingOutputSlots().length;
    }
    public static void encode(PatternEncodingTermMenu menu, AlloyFurnaceRecipeCatalog.Entry entry, ServerPlayer player) {
        if (!(menu instanceof PatternEncodingTermMenuAccess access)) return;
        var logic = access.uselessMod$getEncodingLogic();
        if (logic instanceof PendingOmniversalPatternHolder holder) {
            // A failed full-recipe selection must not leave a different recipe pending.
            holder.uselessMod$setPendingOmniversalRecipe(null);
            holder.uselessMod$setPendingOmniversalSourceId(null);
        }
        var output = logic.getEncodedPatternInv();
        var blank = logic.getBlankPatternInv();
        if (!output.getStackInSlot(0).isEmpty()) {
            player.displayClientMessage(Component.translatable("gui.useless_mod.omniversal_pattern.take_encoded_first"), true);
            return;
        }
        if (!AEItems.BLANK_PATTERN.isSameAs(blank.getStackInSlot(0))) {
            player.displayClientMessage(Component.translatable("gui.useless_mod.omniversal_pattern.insert_blank"), true);
            return;
        }
        var pattern = OmniversalPatternEncoding.reencodeFromRecipe(entry, player);
        if (pattern.isEmpty()) {
            player.displayClientMessage(Component.translatable("gui.useless_mod.omniversal_pattern.encoding_failed"), true);
            return;
        }
        blank.extractItem(0, 1, false);
        output.setItemDirect(0, pattern);
        if (com.sorrowmist.useless.core.config.ConfigManager.isOmniversalPatternAutoUploadEnabled()) {
            var result=OmniversalPatternUploader.upload(menu,pattern,player);
            if (result.outcome()==OmniversalPatternUploader.Outcome.UPLOADED) {
                output.setItemDirect(0,net.minecraft.world.item.ItemStack.EMPTY);
                PatternUploadNotice.notifyUploaded(player,result.target());
            } else if (result.outcome()==OmniversalPatternUploader.Outcome.DUPLICATE) {
                output.setItemDirect(0,net.minecraft.world.item.ItemStack.EMPTY);
                var remaining=blank.addItems(AEItems.BLANK_PATTERN.stack(1));
                if (!remaining.isEmpty()) player.getInventory().placeItemBackInInventory(remaining,false);
                PatternUploadNotice.notifyDuplicate(player);
            }
        }
        logic.saveChanges();
        menu.broadcastChanges();
    }
}
