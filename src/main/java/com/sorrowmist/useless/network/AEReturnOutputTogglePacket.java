package com.sorrowmist.useless.network;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.blockentities.AdvancedAlloyFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端→服务器：切换合金炉产物是否回 AE 网络。
 */
public class AEReturnOutputTogglePacket implements CoreCustomPacketPayload {

    public static final Type<AEReturnOutputTogglePacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "ae_return_output_toggle"));
    public static final CoreStreamCodec<FriendlyByteBuf, AEReturnOutputTogglePacket> STREAM_CODEC = CoreStreamCodec.of(
            (buf, pkt) -> buf.writeBlockPos(pkt.pos),
            buf -> new AEReturnOutputTogglePacket(buf.readBlockPos())
    );
    private final BlockPos pos;

    public AEReturnOutputTogglePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void handle(AEReturnOutputTogglePacket msg, CorePacketContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;

            BlockEntity be = player.level().getBlockEntity(msg.pos);
            if (be instanceof AdvancedAlloyFurnaceBlockEntity furnace) {
                furnace.setReturnOutputToAe(!furnace.isReturnOutputToAe());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
