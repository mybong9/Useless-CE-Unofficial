package com.sorrowmist.useless.network;

import com.sorrowmist.useless.UselessMod;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端请求「我手上这把造化杖绑定的访问点，已经连了哪些机器」。
 *
 * <p>不带参数：服务端自己从玩家手里找工具、读绑定坐标，客户端无从伪造目标。</p>
 */
public record AeLinkPreviewRequestPacket() implements CoreCustomPacketPayload {
    public static final Type<AeLinkPreviewRequestPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "ae_link_preview_request"));
    public static final CoreStreamCodec<FriendlyByteBuf, AeLinkPreviewRequestPacket> STREAM_CODEC =
            CoreStreamCodec.unit(new AeLinkPreviewRequestPacket());

    public static void handle(AeLinkPreviewRequestPacket packet, CorePacketContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                AeLinkPreviewPacket.answer(player);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
