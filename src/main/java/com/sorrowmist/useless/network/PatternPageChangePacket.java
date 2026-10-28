package com.sorrowmist.useless.network;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.menus.AdvancedAlloyFurnaceMenu;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

public class PatternPageChangePacket implements CoreCustomPacketPayload {

    public static final Type<PatternPageChangePacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "pattern_page_change"));
    public static final CoreStreamCodec<FriendlyByteBuf, PatternPageChangePacket> STREAM_CODEC = CoreStreamCodec.of(
            (buf, pkt) -> buf.writeInt(pkt.page),
            buf -> new PatternPageChangePacket(buf.readInt())
    );
    private final int page;

    public PatternPageChangePacket(int page) {
        this.page = page;
    }

    public static void handle(PatternPageChangePacket msg, CorePacketContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player.containerMenu instanceof AdvancedAlloyFurnaceMenu menu) {
                menu.setPatternPage(msg.page);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
