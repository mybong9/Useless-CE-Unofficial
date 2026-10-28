package com.sorrowmist.useless.compat.emi;

import appeng.core.definitions.AEItems;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.PatternEncodingTermMenuAccess;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class OmniversalEmiTransfer {
    private OmniversalEmiTransfer() {}

    public static Component error(EmiRecipe recipe, EmiCraftContext<?> context) {
        if (!(recipe instanceof OmniversalEmiPatternRecipe ours) || !ours.canEncode()
                || !(context.getScreenHandler() instanceof PatternEncodingTermMenu menu)
                || !(menu instanceof PatternEncodingTermMenuAccess access)
                || context.getType() != EmiCraftContext.Type.FILL_BUTTON)
            return Component.translatable("emi.useless_mod.cannot_encode");
        var logic = access.uselessMod$getEncodingLogic();
        if (!logic.getEncodedPatternInv().getStackInSlot(0).isEmpty())
            return Component.translatable("gui.useless_mod.omniversal_pattern.take_encoded_first");
        if (!AEItems.BLANK_PATTERN.isSameAs(logic.getBlankPatternInv().getStackInSlot(0)))
            return Component.translatable("gui.useless_mod.omniversal_pattern.insert_blank");
        return null;
    }

    public static boolean craft(EmiRecipe recipe, EmiCraftContext<?> context) {
        if (error(recipe, context) != null) return false;
        ((OmniversalEmiPatternRecipe) recipe).encode((PatternEncodingTermMenu) context.getScreenHandler());
        Minecraft.getInstance().setScreen(context.getScreen());
        return true;
    }

    public static List<ClientTooltipComponent> tooltip(EmiRecipe recipe, EmiCraftContext<?> context) {
        Component error = error(recipe, context);
        return error == null ? List.of() : List.of(ClientTooltipComponent.create(error.getVisualOrderText()));
    }
}
