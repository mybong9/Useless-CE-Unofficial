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

public record OreGeneratorOutputTogglePacket(int containerId, BlockPos pos)
        implements CoreCustomPacketPayload {
    public static final Type<OreGeneratorOutputTogglePacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "ore_generator_output_toggle"));
    public static final CoreStreamCodec<FriendlyByteBuf, OreGeneratorOutputTogglePacket> STREAM_CODEC =
            CoreStreamCodec.of((buf, packet) -> {
                buf.writeVarInt(packet.containerId);
                buf.writeBlockPos(packet.pos);
            }, buf -> new OreGeneratorOutputTogglePacket(buf.readVarInt(), buf.readBlockPos()));

    public static void handle(OreGeneratorOutputTogglePacket packet, CorePacketContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof OreGeneratorMenu menu)
                    || menu.containerId != packet.containerId
                    || !menu.getBlockPos().equals(packet.pos)
                    || !menu.stillValid(player)) {
                return;
            }
            OreGeneratorBlockEntity generator = menu.getGenerator();
            if (generator == null) return;
            generator.setOutputToAe(!generator.isOutputToAe());
            menu.broadcastChanges();
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
