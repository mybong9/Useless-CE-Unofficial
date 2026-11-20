package com.sorrowmist.useless.network;

import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.PatternEncodingTermMenuAccess;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.PendingOmniversalPatternHolder;
import com.sorrowmist.uselesscore.api.recipe.AlloyFurnaceRecipeIdentity;
import com.sorrowmist.useless.content.recipe.RecipeSourceIds;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreByteBufCodecs;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

/**
 * Tells the server which Omniversal Alloy Furnace recipe the player just transferred from JEI into a
 * pattern encoding terminal, so the pattern that terminal encodes next becomes an Omniversal Pattern
 * bound to that exact recipe — mold included.
 *
 * <p>The server either encodes immediately or remembers the selection on this player's menu,
 * according to the JEI direct encoding setting.
 */
public record SelectOmniversalPatternRecipePacket(
        int containerId, ResourceLocation recipeId, String fingerprint, String sourceId) implements CoreCustomPacketPayload {
    public SelectOmniversalPatternRecipePacket {
        sourceId = RecipeSourceIds.normalize(sourceId);
    }

    public static final Type<SelectOmniversalPatternRecipePacket> TYPE =
            new Type<>(UselessMod.id("select_omniversal_pattern_recipe"));
    public static final CoreStreamCodec<FriendlyByteBuf, SelectOmniversalPatternRecipePacket> STREAM_CODEC =
            CoreStreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeVarInt(packet.containerId);
                        CoreByteBufCodecs.RESOURCE_LOCATION.encode(buffer, packet.recipeId);
                        buffer.writeUtf(packet.fingerprint);
                        buffer.writeUtf(packet.sourceId);
                    },
                    buffer -> new SelectOmniversalPatternRecipePacket(
                            buffer.readVarInt(), CoreByteBufCodecs.RESOURCE_LOCATION.decode(buffer),
                            buffer.readUtf(), buffer.readUtf()));

    public static void handle(SelectOmniversalPatternRecipePacket packet, CorePacketContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof PatternEncodingTermMenu menu)
                    || menu.containerId != packet.containerId || !menu.stillValid(player)) return;
            // Read the exact logic used by this menu; getTarget() may expose a wrapper
            // rather than the terminal host and previously discarded the selection silently.
            if (!(menu instanceof PatternEncodingTermMenuAccess access)
                    || !(access.uselessMod$getEncodingLogic() instanceof PendingOmniversalPatternHolder holder)) {
                UselessMod.LOGGER.warn("Cannot record omniversal recipe selection for menu {}",
                        menu.getClass().getName());
                return;
            }

            AlloyFurnaceRecipeIdentity identity;
            try {
                identity = new AlloyFurnaceRecipeIdentity(packet.recipeId, packet.fingerprint);
            } catch (RuntimeException ignored) {
                // A blank fingerprint can only come from a malformed packet; drop it rather than
                // leaving a half-valid pick behind.
                holder.uselessMod$setPendingOmniversalRecipe(null);
                holder.uselessMod$setPendingOmniversalSourceId(null);
                return;
            }
            var selected = com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog.resolve(
                    player.level(), packet.sourceId(), identity);
            if (selected.isEmpty()) {
                access.uselessMod$setDeferredOmniversalRecipe(null, null);
                var candidates = com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog.entries(player.level(), packet.sourceId())
                        .stream().filter(entry -> entry.identity().recipeId().equals(packet.recipeId())).toList();
                if (candidates.size() == 1) selected = java.util.Optional.of(candidates.get(0));
            }
            if (selected.isEmpty()) {
                holder.uselessMod$setPendingOmniversalRecipe(null);
                holder.uselessMod$setPendingOmniversalSourceId(null);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "gui.useless_mod.omniversal_pattern.selection_missing"), true);
                return;
            }
            if (com.sorrowmist.useless.core.config.ConfigManager.isJeiDirectPatternEncodingEnabled()) {
                access.uselessMod$setDeferredOmniversalRecipe(null, null);
                com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.FullRecipeTerminalEncoding
                        .encode(menu, selected.get(), player);
                return;
            }
            access.uselessMod$setDeferredOmniversalRecipe(null, null);
            if (!com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.FullRecipeTerminalEncoding
                    .preview(menu, selected.get(), player)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "gui.useless_mod.omniversal_pattern.encoding_failed"), true);
                return;
            }
            access.uselessMod$setDeferredOmniversalRecipe(selected.get().identity(), selected.get().sourceId());
            UselessMod.LOGGER.info("Omniversal recipe selected: recipe={}, source={}, container={}",
                    packet.recipeId(), packet.sourceId(), packet.containerId());
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "gui.useless_mod.omniversal_pattern.press_encode"), true);
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
