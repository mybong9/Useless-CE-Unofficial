package com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae;

import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.core.definitions.AEItems;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Atomic full-recipe encoding for recipes wider than the terminal's visible ghost slots. */
public final class FullRecipeTerminalEncoding {
    private FullRecipeTerminalEncoding() {}

    /** Fills the visible ghost slots without consuming or creating a physical pattern. */
    public static boolean preview(PatternEncodingTermMenu menu, AlloyFurnaceRecipeCatalog.Entry entry, ServerPlayer player) {
        if (!(menu instanceof PatternEncodingTermMenuAccess access)) return false;
        var pattern = OmniversalPatternEncoding.createProcessingPattern(entry.recipe());
        if (pattern.isEmpty()) return false;
        if (!(appeng.api.crafting.PatternDetailsHelper.decodePattern(pattern, player.level())
                instanceof appeng.crafting.pattern.AEProcessingPattern preview)) return false;
        var logic = access.uselessMod$getEncodingLogic();
        logic.setMode(appeng.parts.encoding.EncodingMode.PROCESSING);
        fillPreview(logic.getEncodedInputInv(), preview.getSparseInputs());
        fillPreview(logic.getEncodedOutputInv(), preview.getSparseOutputs());
        logic.saveChanges();
        menu.broadcastChanges();
        return true;
    }

    private static void fillPreview(appeng.util.ConfigInventory inventory, appeng.api.stacks.GenericStack[] stacks) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            inventory.setStack(slot, slot < stacks.length ? stacks[slot] : null);
        }
    }
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
    public static boolean encode(PatternEncodingTermMenu menu, AlloyFurnaceRecipeCatalog.Entry entry, ServerPlayer player) {
        return encode(menu, entry, player, false);
    }

    public static boolean encodeEdited(PatternEncodingTermMenu menu, AlloyFurnaceRecipeCatalog.Entry entry, ServerPlayer player) {
        return encode(menu, entry, player, true);
    }

    private static boolean encode(PatternEncodingTermMenu menu, AlloyFurnaceRecipeCatalog.Entry entry,
                                  ServerPlayer player, boolean useTerminalSlots) {
        if (!(menu instanceof PatternEncodingTermMenuAccess access)) return false;
        var logic = access.uselessMod$getEncodingLogic();
        var output = logic.getEncodedPatternInv();
        var blank = logic.getBlankPatternInv();
        if (!output.getStackInSlot(0).isEmpty()) {
            player.displayClientMessage(Component.translatable("gui.useless_mod.omniversal_pattern.take_encoded_first"), true);
            return false;
        }
        if (!AEItems.BLANK_PATTERN.isSameAs(blank.getStackInSlot(0))) {
            player.displayClientMessage(Component.translatable("gui.useless_mod.omniversal_pattern.insert_blank"), true);
            return false;
        }
        var pattern = useTerminalSlots ? encodeTerminalSlots(logic, entry, player)
                : OmniversalPatternEncoding.reencodeFromRecipe(entry, player);
        if (pattern.isEmpty()) {
            player.displayClientMessage(Component.translatable("gui.useless_mod.omniversal_pattern.encoding_failed"), true);
            return false;
        }
        if (logic instanceof PendingOmniversalPatternHolder holder) {
            holder.uselessMod$setPendingOmniversalRecipe(null);
            holder.uselessMod$setPendingOmniversalSourceId(null);
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
        return true;
    }

    private static ItemStack encodeTerminalSlots(PatternEncodingLogic logic, AlloyFurnaceRecipeCatalog.Entry entry,
                                                  ServerPlayer player) {
        if (logic.getMode() != appeng.parts.encoding.EncodingMode.PROCESSING) return ItemStack.EMPTY;
        var source = OmniversalPatternEncoding.createProcessingPattern(entry.recipe());
        if (source.isEmpty() || !(appeng.api.crafting.PatternDetailsHelper.decodePattern(source, player.level())
                instanceof AEProcessingPattern preview)) return ItemStack.EMPTY;
        var inputs = readEditedSlots(logic.getEncodedInputInv(), preview.getSparseInputs());
        var outputs = readEditedSlots(logic.getEncodedOutputInv(), preview.getSparseOutputs());
        if (!inputs.changed() && !outputs.changed()) {
            return OmniversalPatternEncoding.reencodeFromRecipe(entry, player);
        }
        if (inputs.stacks().isEmpty() || outputs.stacks().isEmpty()) return ItemStack.EMPTY;
        var edited = com.sorrowmist.uselesscore.api.network.codec.CoreAeCodecs.encodeProcessingPattern(
                inputs.stacks(), outputs.stacks());
        if (edited.isEmpty()) return ItemStack.EMPTY;
        edited.getOrCreateTag().putBoolean("useless_mod_full_recipe", true);
        return OmniversalPatternEncoding.encode(edited, entry, player.level());
    }

    private static EditedSlots readEditedSlots(ConfigInventory inventory, GenericStack[] original) {
        List<GenericStack> stacks = new ArrayList<>();
        boolean changed = false;
        for (int slot = 0; slot < Math.max(inventory.size(), original.length); slot++) {
            var before = slot < original.length ? original[slot] : null;
            // A wide recipe can have slots that this terminal cannot display or edit.
            var after = slot < inventory.size() ? inventory.getStack(slot) : before;
            changed |= !Objects.equals(before, after);
            if (after != null) stacks.add(after);
        }
        return new EditedSlots(stacks, changed);
    }

    private record EditedSlots(List<GenericStack> stacks, boolean changed) {}
}
