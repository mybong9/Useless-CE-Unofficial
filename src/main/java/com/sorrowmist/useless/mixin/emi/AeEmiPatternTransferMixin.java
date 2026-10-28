package com.sorrowmist.useless.mixin.emi;

import com.sorrowmist.useless.compat.emi.OmniversalEmiPatternRecipe;
import com.sorrowmist.useless.compat.emi.OmniversalEmiTransfer;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

/** Preserve AE2's native EMI transfer for other recipes and terminal implementations. */
@Mixin(targets = "appeng.integration.modules.emi.EmiEncodePatternHandler", remap = false)
public abstract class AeEmiPatternTransferMixin {
    @Inject(method = "supportsRecipe", at = @At("HEAD"), cancellable = true)
    private void uselessMod$supports(EmiRecipe recipe, CallbackInfoReturnable<Boolean> callback) {
        if (recipe instanceof OmniversalEmiPatternRecipe) callback.setReturnValue(true);
    }
    @Inject(method = "canCraft", at = @At("HEAD"), cancellable = true)
    private void uselessMod$canEncode(EmiRecipe recipe, EmiCraftContext<?> context, CallbackInfoReturnable<Boolean> callback) {
        if (recipe instanceof OmniversalEmiPatternRecipe) callback.setReturnValue(OmniversalEmiTransfer.error(recipe, context) == null);
    }
    @Inject(method = "craft", at = @At("HEAD"), cancellable = true)
    private void uselessMod$encode(EmiRecipe recipe, EmiCraftContext<?> context, CallbackInfoReturnable<Boolean> callback) {
        if (recipe instanceof OmniversalEmiPatternRecipe) callback.setReturnValue(OmniversalEmiTransfer.craft(recipe, context));
    }
    @Inject(method = "getTooltip", at = @At("HEAD"), cancellable = true)
    private void uselessMod$tooltip(EmiRecipe recipe, EmiCraftContext<?> context,
                                   CallbackInfoReturnable<List<ClientTooltipComponent>> callback) {
        if (recipe instanceof OmniversalEmiPatternRecipe) callback.setReturnValue(OmniversalEmiTransfer.tooltip(recipe, context));
    }
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void uselessMod$render(EmiRecipe recipe, EmiCraftContext<?> context, List<Widget> widgets,
                                  GuiGraphics graphics, CallbackInfo callback) {
        if (recipe instanceof OmniversalEmiPatternRecipe) callback.cancel();
    }
}
