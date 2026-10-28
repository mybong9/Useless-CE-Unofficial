package com.sorrowmist.useless.network;



import com.sorrowmist.useless.init.ModNetwork;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.compat.constructionwand.ConstructionWandLogic;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

public record ConstructionWandPreviewRequestPacket(int requestId, boolean air,
                                                    BlockHitResult hit, InteractionHand hand)
        implements CoreCustomPacketPayload {
    public static final Type<ConstructionWandPreviewRequestPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "construction_wand_preview_request"));
    public static final CoreStreamCodec<FriendlyByteBuf, ConstructionWandPreviewRequestPacket> STREAM_CODEC =
            CoreStreamCodec.of(
                    (buf, packet) -> {
                        buf.writeVarInt(packet.requestId);
                        buf.writeBoolean(packet.air);
                        if (!packet.air) buf.writeBlockHitResult(packet.hit);
                        buf.writeEnum(packet.hand);
                    },
                    buf -> {
                        int requestId = buf.readVarInt();
                        boolean air = buf.readBoolean();
                        BlockHitResult hit = air ? null : buf.readBlockHitResult();
                        return new ConstructionWandPreviewRequestPacket(
                                requestId, air, hit, buf.readEnum(InteractionHand.class));
                    }
            );

    public static ConstructionWandPreviewRequestPacket block(int requestId, BlockHitResult hit,
                                                              InteractionHand hand) {
        return new ConstructionWandPreviewRequestPacket(requestId, false, hit, hand);
    }

    public static ConstructionWandPreviewRequestPacket air(int requestId, InteractionHand hand) {
        return new ConstructionWandPreviewRequestPacket(requestId, true, null, hand);
    }

    public static void handle(ConstructionWandPreviewRequestPacket packet, CorePacketContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ModNetwork.CHANNEL.sendToPlayer(player, new ConstructionWandPreviewPacket(
                    packet.requestId,
                    ConstructionWandLogic.preview(player, packet.hand, packet.hit, packet.air)));
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
