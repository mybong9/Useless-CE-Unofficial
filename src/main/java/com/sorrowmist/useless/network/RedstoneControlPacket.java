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
 * 客户端→服务器：循环红石控制模式。
 */
public class RedstoneControlPacket implements CoreCustomPacketPayload {

    public static final Type<RedstoneControlPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "redstone_control"));
    public static final CoreStreamCodec<FriendlyByteBuf, RedstoneControlPacket> STREAM_CODEC = CoreStreamCodec.of(
            (buf, pkt) -> buf.writeBlockPos(pkt.pos),
            buf -> new RedstoneControlPacket(buf.readBlockPos())
    );
    private final BlockPos pos;

    public RedstoneControlPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void handle(RedstoneControlPacket msg, CorePacketContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;

            BlockEntity be = player.level().getBlockEntity(msg.pos);
            if (be instanceof AdvancedAlloyFurnaceBlockEntity furnace) {
                furnace.cycleRedstoneControlMode();
            } else if (be instanceof MultiblockAlloyFurnaceCoreBlockEntity furnace) {
                furnace.cycleRedstoneControlMode();
            }
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
