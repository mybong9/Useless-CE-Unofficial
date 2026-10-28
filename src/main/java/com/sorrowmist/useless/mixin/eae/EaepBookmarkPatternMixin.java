package com.sorrowmist.useless.mixin.eae;

import com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.OmniversalPatternEncoding;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import java.util.Optional;

/** EAEP's bookmark hotkeys bypass JEI's terminal transfer handler entirely. */
@Pseudo
@Mixin(targets = {
        "com.extendedae_plus.network.pattern.CreateCtrlQPatternC2SPacket",
        "com.extendedae_plus.network.pattern.CreateAndUploadPatternC2SPacket"
}, remap = false)
public abstract class EaepBookmarkPatternMixin {
    @Redirect(method = "lambda$handle$0", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/RecipeManager;byKey(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;",
            remap = true), require = 0)
    private static Optional<? extends Recipe<?>> uselessMod$resolveBookmark(
            RecipeManager manager, ResourceLocation id, NetworkEvent.Context context, @Coerce Object packet) {
        ServerPlayer player = context.getSender();
        if (player != null) {
            var entry = AlloyFurnaceRecipeCatalog.resolveJeiDisplay(player.level(), id);
            if (entry.isPresent()) return Optional.of(entry.get().recipe());
        }
        return manager.byKey(id);
    }

    @Inject(method = "createPattern", at = @At("HEAD"), cancellable = true, require = 0)
    private static void uselessMod$encodeOmniversal(Recipe<?> recipe, boolean crafting,
            List<ItemStack> inputs, List<ItemStack> outputs, boolean substitutes, boolean fluidSubstitutes,
            ServerPlayer player, CallbackInfoReturnable<ItemStack> cir) {
        if (player == null || recipe == null) return;
        // Only a canonical entry returned by the directory redirect may use this path.
        // Ignore client-provided output stacks; the server directory is authoritative.
        for (var entry : AlloyFurnaceRecipeCatalog.entries(player.level())) {
            if (entry.recipe() == recipe) {
                cir.setReturnValue(OmniversalPatternEncoding.reencodeFromRecipe(entry, player));
                return;
            }
        }
    }
}
