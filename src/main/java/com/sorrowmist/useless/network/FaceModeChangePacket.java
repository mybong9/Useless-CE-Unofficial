package com.sorrowmist.useless.network;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.uselesscore.api.enums.FurnaceFace;
import com.sorrowmist.uselesscore.api.enums.FurnaceFaceMode;
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
 * 客户端→服务器：循环指定逻辑面的输入输出模式。
 */
public class FaceModeChangePacket implements CoreCustomPacketPayload {

    public static final Type<FaceModeChangePacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "face_mode_change"));
    public static final CoreStreamCodec<FriendlyByteBuf, FaceModeChangePacket> STREAM_CODEC = CoreStreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.pos);
                buf.writeByte(pkt.faceIndex);
                buf.writeBoolean(pkt.reverse);
            },
            buf -> new FaceModeChangePacket(buf.readBlockPos(), buf.readByte(), buf.readBoolean())
    );
    private final BlockPos pos;
    private final byte faceIndex;
    private final boolean reverse;

    /**
     * @param pos       方块位置
     * @param faceIndex FurnaceFace的ordinal值
     */
    public FaceModeChangePacket(BlockPos pos, int faceIndex) {
        this(pos, faceIndex, false);
    }

    public FaceModeChangePacket(BlockPos pos, int faceIndex, boolean reverse) {
        this.pos = pos;
        this.faceIndex = (byte) faceIndex;
        this.reverse = reverse;
    }

    public static void handle(FaceModeChangePacket msg, CorePacketContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;

            BlockEntity be = player.level().getBlockEntity(msg.pos);
            if (be instanceof AdvancedAlloyFurnaceBlockEntity furnace) {
                FurnaceFace face = FurnaceFace.values()[msg.faceIndex % FurnaceFace.COUNT];
                FurnaceFaceMode newMode = furnace.cycleFaceMode(face, msg.reverse);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
