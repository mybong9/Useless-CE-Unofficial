package com.sorrowmist.useless.network;



import com.sorrowmist.useless.init.ModNetwork;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.data.BeefToolLayout;
import com.sorrowmist.useless.data.BeefToolLayoutManager;
import com.sorrowmist.useless.utils.UselessItemUtils;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

public record BeefToolLayoutRequestPacket() implements CoreCustomPacketPayload {
    public static final Type<BeefToolLayoutRequestPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "beef_tool_layout_request"));
    public static final CoreStreamCodec<FriendlyByteBuf, BeefToolLayoutRequestPacket> STREAM_CODEC =
            CoreStreamCodec.of((buffer, packet) -> {}, buffer -> new BeefToolLayoutRequestPacket());

    public static void handle(BeefToolLayoutRequestPacket packet, CorePacketContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || UselessItemUtils.findTargetToolInHands(player).isEmpty()) {
                return;
            }
            BeefToolLayout layout = BeefToolLayoutManager.loadOrCreate(player);
            ModNetwork.CHANNEL.sendToPlayer(player, new BeefToolLayoutSyncPacket(layout.toJson()));
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
