package com.sorrowmist.useless.network;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.blockentities.AdvancedAlloyFurnaceBlockEntity;
import com.sorrowmist.useless.content.blockentities.multiblock.MultiblockAlloyFurnaceCoreBlockEntity;
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
 * 客户端→服务器：取消合金炉所有当前 AE 合成任务并返还材料。
 */
public class AECancelPacket implements CoreCustomPacketPayload {

    public static final Type<AECancelPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "ae_cancel"));
    public static final CoreStreamCodec<FriendlyByteBuf, AECancelPacket> STREAM_CODEC = CoreStreamCodec.of(
            (buf, pkt) -> buf.writeBlockPos(pkt.pos),
            buf -> new AECancelPacket(buf.readBlockPos())
    );
    private final BlockPos pos;

    public AECancelPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void handle(AECancelPacket msg, CorePacketContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;

            BlockEntity be = player.level().getBlockEntity(msg.pos);
            if (be instanceof AdvancedAlloyFurnaceBlockEntity furnace) {
                furnace.cancelAllAETasks();
            } else if (be instanceof MultiblockAlloyFurnaceCoreBlockEntity furnace) {
                furnace.cancelAllAETasks();
            }
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
