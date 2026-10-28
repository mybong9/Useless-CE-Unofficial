package com.sorrowmist.useless.network;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.utils.mining.MiningDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

/**
 * 强制破坏按键包
 * 用于客户端通知服务端执行强制破坏
 */
public class ForceBreakKeyPacket implements CoreCustomPacketPayload {

    public static final Type<ForceBreakKeyPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "force_break_key"));

    public static final CoreStreamCodec<FriendlyByteBuf, ForceBreakKeyPacket> STREAM_CODEC = CoreStreamCodec.of(
            (buf, pkt) -> {
                buf.writeBoolean(pkt.tabPressed);
            },
            buf -> new ForceBreakKeyPacket(buf.readBoolean())
    );

    // 是否同时按下了Tab键（连锁模式）
    private final boolean tabPressed;

    public ForceBreakKeyPacket(boolean tabPressed) {
        this.tabPressed = tabPressed;
    }

    public static void handle(ForceBreakKeyPacket msg, CorePacketContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() != null) {
                // 直接执行强制破坏，传入当前是否按下Tab键
                MiningDispatcher.dispatchForceBreak(ctx.player(), msg.tabPressed);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
