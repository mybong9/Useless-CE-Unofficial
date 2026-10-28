package com.sorrowmist.useless.network;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.client.network.ClientPacketHandlers;
import com.sorrowmist.useless.data.BeefToolLayout;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

public record BeefToolLayoutSyncPacket(String json) implements CoreCustomPacketPayload {
    public static final Type<BeefToolLayoutSyncPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "beef_tool_layout_sync"));
    public static final CoreStreamCodec<FriendlyByteBuf, BeefToolLayoutSyncPacket> STREAM_CODEC =
            CoreStreamCodec.of(
                    (buffer, packet) -> buffer.writeUtf(packet.json, BeefToolLayout.MAX_TEXT_LENGTH),
                    buffer -> new BeefToolLayoutSyncPacket(buffer.readUtf(BeefToolLayout.MAX_TEXT_LENGTH)));

    public static void handle(BeefToolLayoutSyncPacket packet, CorePacketContext context) {
        // 客户端类型统一收敛到 ClientPacketHandlers，避免专用服务器加载类时解析到客户端类。
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        context.enqueueWork(() -> ClientPacketHandlers.handleBeefToolLayoutSync(packet));
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
