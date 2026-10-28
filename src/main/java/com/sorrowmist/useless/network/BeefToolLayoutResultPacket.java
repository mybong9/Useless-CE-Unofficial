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

public record BeefToolLayoutResultPacket(BeefToolLayout.Error error) implements CoreCustomPacketPayload {
    public static final Type<BeefToolLayoutResultPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "beef_tool_layout_result"));
    public static final CoreStreamCodec<FriendlyByteBuf, BeefToolLayoutResultPacket> STREAM_CODEC =
            CoreStreamCodec.of(
                    (buffer, packet) -> buffer.writeEnum(packet.error),
                    buffer -> new BeefToolLayoutResultPacket(buffer.readEnum(BeefToolLayout.Error.class)));

    public static void handle(BeefToolLayoutResultPacket packet, CorePacketContext context) {
        // 客户端类型统一收敛到 ClientPacketHandlers，避免专用服务器加载类时解析到客户端类。
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        context.enqueueWork(() -> ClientPacketHandlers.handleBeefToolLayoutResult(packet));
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
