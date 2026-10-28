package com.sorrowmist.useless.network;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.client.render.ConstructionWandPreviewRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record ConstructionWandPreviewPacket(int requestId, List<BlockPos> positions)
        implements CoreCustomPacketPayload {
    private static final int MAX_POSITIONS = 4096;
    public static final Type<ConstructionWandPreviewPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "construction_wand_preview"));
    public static final CoreStreamCodec<FriendlyByteBuf, ConstructionWandPreviewPacket> STREAM_CODEC =
            CoreStreamCodec.of(
                    (buf, packet) -> {
                        buf.writeVarInt(packet.requestId);
                        buf.writeVarInt(packet.positions.size());
                        for (BlockPos pos : packet.positions) buf.writeBlockPos(pos);
                    },
                    buf -> {
                        int requestId = buf.readVarInt();
                        int size = buf.readVarInt();
                        if (size < 0 || size > MAX_POSITIONS) {
                            throw new IllegalArgumentException("Invalid construction preview size: " + size);
                        }
                        List<BlockPos> positions = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) positions.add(buf.readBlockPos());
                        return new ConstructionWandPreviewPacket(requestId, positions);
                    }
            );

    public ConstructionWandPreviewPacket {
        if (positions.size() > MAX_POSITIONS) {
            throw new IllegalArgumentException("Construction preview is too large");
        }
        positions = List.copyOf(positions);
    }

    public static void handle(ConstructionWandPreviewPacket packet, CorePacketContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ConstructionWandPreviewRenderer.setPreview(
                    packet.requestId, packet.positions));
        }
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
