package com.sorrowmist.useless.network;

import appeng.menu.me.items.PatternEncodingTermMenu;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.FullRecipeTerminalEncoding;
import com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.recipe.AlloyFurnaceRecipeIdentity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** EMI requests an exact recipe; all quantities, fluid keys and output NBT are derived by the server. */
public record EncodeOmniversalPatternPacket(SelectOmniversalPatternRecipePacket selection)
        implements CoreCustomPacketPayload {
    public static final Type<EncodeOmniversalPatternPacket> TYPE = new Type<>(UselessMod.id("encode_omniversal_pattern"));
    public static final CoreStreamCodec<FriendlyByteBuf, EncodeOmniversalPatternPacket> STREAM_CODEC = CoreStreamCodec.of(
            (buf, packet) -> SelectOmniversalPatternRecipePacket.STREAM_CODEC.encode(buf, packet.selection),
            buf -> new EncodeOmniversalPatternPacket(SelectOmniversalPatternRecipePacket.STREAM_CODEC.decode(buf)));

    public static void handle(EncodeOmniversalPatternPacket packet, CorePacketContext context) {
        context.enqueueWork(() -> {
            var pick = packet.selection;
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof PatternEncodingTermMenu menu)
                    || menu.containerId != pick.containerId() || !menu.stillValid(player)) return;
            AlloyFurnaceRecipeIdentity identity;
            try { identity = new AlloyFurnaceRecipeIdentity(pick.recipeId(), pick.fingerprint()); }
            catch (IllegalArgumentException invalid) { return; }
            var entry = AlloyFurnaceRecipeCatalog.resolve(player.level(), pick.sourceId(), identity);
            if (entry.isEmpty()) {
                var candidates = AlloyFurnaceRecipeCatalog.entries(player.level(), pick.sourceId()).stream()
                        .filter(candidate -> candidate.identity().recipeId().equals(pick.recipeId())).toList();
                if (candidates.size() == 1) entry = java.util.Optional.of(candidates.get(0));
            }
            if (entry.isPresent()) FullRecipeTerminalEncoding.encode(menu, entry.get(), player);
            else player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "gui.useless_mod.omniversal_pattern.selection_missing"), true);
        });
    }
    @Override public Type<EncodeOmniversalPatternPacket> type() { return TYPE; }
}
