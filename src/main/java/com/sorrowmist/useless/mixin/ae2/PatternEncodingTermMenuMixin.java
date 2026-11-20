package com.sorrowmist.useless.mixin.ae2;

import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.encoding.PatternEncodingLogic;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.PatternEncodingTermMenuAccess;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.FullRecipeTerminalEncoding;
import com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog;
import com.sorrowmist.useless.core.config.ConfigManager;
import com.sorrowmist.useless.compat.extendedae.EcoCraftingPatternUpload;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.sorrowmist.uselesscore.api.recipe.AlloyFurnaceRecipeIdentity;

/** Uses the logic owned by the open menu, including terminals with a wrapped target. */
@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingTermMenuMixin implements PatternEncodingTermMenuAccess {
    @Shadow @Final private PatternEncodingLogic encodingLogic;
    @Unique private AlloyFurnaceRecipeIdentity uselessMod$deferredRecipe;
    @Unique private String uselessMod$deferredSource;

    @Override
    public void uselessMod$setDeferredOmniversalRecipe(AlloyFurnaceRecipeIdentity identity, String source) {
        uselessMod$deferredRecipe = identity;
        uselessMod$deferredSource = source;
    }

    @Inject(method = "clear()V", at = @At("HEAD"))
    private void uselessMod$clearDeferredRecipe(CallbackInfo ci) {
        uselessMod$setDeferredOmniversalRecipe(null, null);
    }

    @Override
    public PatternEncodingLogic uselessMod$getEncodingLogic() {
        return encodingLogic;
    }

    @WrapMethod(method = "encode()V")
    private void uselessMod$encodeFromTerminal(Operation<Void> original) {
        var menu = (PatternEncodingTermMenu) (Object) this;
        if (encodingLogic.isClientSide() || !(menu.getPlayer() instanceof ServerPlayer player)) {
            original.call();
            return;
        }
        if (uselessMod$deferredRecipe != null) {
            var entry = AlloyFurnaceRecipeCatalog.resolve(player.level(),
                    uselessMod$deferredSource, uselessMod$deferredRecipe);
            if (entry.isPresent()) {
                if (FullRecipeTerminalEncoding.encodeEdited(menu, entry.get(), player)) {
                    uselessMod$setDeferredOmniversalRecipe(null, null);
                }
            } else {
                uselessMod$setDeferredOmniversalRecipe(null, null);
                player.displayClientMessage(Component.translatable(
                        "gui.useless_mod.omniversal_pattern.selection_missing"), true);
            }
            return;
        }
        if (!ConfigManager.isEcoCraftingPatternAutoUploadEnabled()) {
            original.call();
            return;
        }
        var output = encodingLogic.getEncodedPatternInv();
        var before = output.getStackInSlot(0);
        original.call();
        var after = output.getStackInSlot(0);
        if (after != before && appeng.core.definitions.AEItems.CRAFTING_PATTERN.isSameAs(after)) {
            EcoCraftingPatternUpload.upload(menu, player);
        }
    }
}
