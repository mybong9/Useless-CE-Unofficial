package com.sorrowmist.useless.network;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.utils.mining.MiningDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

public class TabKeyPressedPacket implements CoreCustomPacketPayload {

    public static final Type<TabKeyPressedPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "tab_key_pressed"));
    public static final CoreStreamCodec<FriendlyByteBuf, TabKeyPressedPacket> STREAM_CODEC = CoreStreamCodec.of(
            (buf, pkt) -> {
                buf.writeBoolean(pkt.pressed);
            },
            buf -> new TabKeyPressedPacket(buf.readBoolean())
    );
    private final boolean pressed;

    public TabKeyPressedPacket(boolean pressed) {
        this.pressed = pressed;
    }

    public static void handle(TabKeyPressedPacket msg, CorePacketContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() != null) {
                MiningDispatcher.setTabPressed(ctx.player(), msg.pressed);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
