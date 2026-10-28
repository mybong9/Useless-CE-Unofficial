package com.sorrowmist.useless.network;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.blockentities.OreGeneratorBlockEntity;
import com.sorrowmist.useless.content.menus.OreGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

public record OreGeneratorSettingsPacket(int containerId, BlockPos pos, long outputRate)
        implements CoreCustomPacketPayload {
    public static final Type<OreGeneratorSettingsPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "ore_generator_settings"));
    public static final CoreStreamCodec<FriendlyByteBuf, OreGeneratorSettingsPacket> STREAM_CODEC =
            CoreStreamCodec.of((buf, packet) -> {
                buf.writeVarInt(packet.containerId);
                buf.writeBlockPos(packet.pos);
                buf.writeLong(packet.outputRate);
            }, buf -> new OreGeneratorSettingsPacket(
                    buf.readVarInt(), buf.readBlockPos(), buf.readLong()));

    public static void handle(OreGeneratorSettingsPacket packet, CorePacketContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof OreGeneratorMenu menu)
                    || menu.containerId != packet.containerId
                    || !menu.getBlockPos().equals(packet.pos)
                    || !menu.stillValid(player)
                    || packet.outputRate < 1L) {
                return;
            }
            OreGeneratorBlockEntity generator = menu.getGenerator();
            if (generator == null) return;
            generator.setOutputRate(packet.outputRate);
            menu.broadcastChanges();
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
