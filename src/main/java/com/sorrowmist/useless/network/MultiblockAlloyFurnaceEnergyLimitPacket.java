package com.sorrowmist.useless.network;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.blockentities.multiblock.MultiblockAlloyFurnaceCoreBlockEntity;
import com.sorrowmist.useless.content.menus.MultiblockAlloyFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

public record MultiblockAlloyFurnaceEnergyLimitPacket(
        int containerId, BlockPos pos, long energyLimit) implements CoreCustomPacketPayload {
    public static final Type<MultiblockAlloyFurnaceEnergyLimitPacket> TYPE = new Type<>(
            UselessMod.id("multiblock_alloy_furnace_energy_limit"));
    public static final CoreStreamCodec<FriendlyByteBuf, MultiblockAlloyFurnaceEnergyLimitPacket> STREAM_CODEC =
            CoreStreamCodec.of((buffer, packet) -> {
                buffer.writeVarInt(packet.containerId);
                buffer.writeBlockPos(packet.pos);
                buffer.writeLong(packet.energyLimit);
            }, buffer -> new MultiblockAlloyFurnaceEnergyLimitPacket(
                    buffer.readVarInt(), buffer.readBlockPos(), buffer.readLong()));

    public static void handle(
            MultiblockAlloyFurnaceEnergyLimitPacket packet, CorePacketContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof MultiblockAlloyFurnaceMenu menu)
                    || menu.containerId != packet.containerId
                    || !menu.getBlockPos().equals(packet.pos)
                    || !menu.stillValid(player)
                    || packet.energyLimit < 0L) {
                return;
            }
            MultiblockAlloyFurnaceCoreBlockEntity core = menu.getCore();
            if (core == null || !core.getBlockPos().equals(packet.pos)) return;
            core.setAutomaticEnergyLimit(packet.energyLimit);
            menu.broadcastChanges();
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
