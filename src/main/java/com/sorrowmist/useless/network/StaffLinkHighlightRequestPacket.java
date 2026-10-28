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
 * 客户端请求「我手上这把造化杖的当前网络里，各锚点分别是什么流向」。
 *
 * <p>不带参数：服务端自己验手持、自己取该玩家（或队伍）当前的网络，客户端无从伪造目标。
 * 手持杖时的世界高亮每 10 tick 问一次，见 {@code StaffLinkHighlightRenderer}。</p>
 */
public record StaffLinkHighlightRequestPacket() implements CoreCustomPacketPayload {
    public static final Type<StaffLinkHighlightRequestPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "staff_link_highlight_request"));
    public static final CoreStreamCodec<FriendlyByteBuf, StaffLinkHighlightRequestPacket> STREAM_CODEC =
            CoreStreamCodec.unit(new StaffLinkHighlightRequestPacket());

    public static void handle(StaffLinkHighlightRequestPacket packet, CorePacketContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                StaffLinkHighlightPacket.answer(player);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
