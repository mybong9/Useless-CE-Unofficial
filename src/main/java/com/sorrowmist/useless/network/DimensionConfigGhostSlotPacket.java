package com.sorrowmist.useless.network;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.menus.DimensionConfigMenu;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

public record DimensionConfigGhostSlotPacket(int containerId, int slot, ResourceLocation blockId)
        implements CoreCustomPacketPayload {
    public static final Type<DimensionConfigGhostSlotPacket> TYPE = new Type<>(
            UselessMod.id("dimension_config_ghost_slot"));
    public static final CoreStreamCodec<FriendlyByteBuf, DimensionConfigGhostSlotPacket> STREAM_CODEC =
            CoreStreamCodec.of((buffer, packet) -> {
                buffer.writeVarInt(packet.containerId);
                buffer.writeVarInt(packet.slot);
                buffer.writeResourceLocation(packet.blockId);
            }, buffer -> new DimensionConfigGhostSlotPacket(
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readResourceLocation()));

    public static void handle(DimensionConfigGhostSlotPacket packet, CorePacketContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof DimensionConfigMenu menu)
                    || menu.containerId != packet.containerId
                    || !menu.stillValid(player)) {
                return;
            }
            menu.setGhostBlockId(packet.slot, packet.blockId);
            menu.broadcastChanges();
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
