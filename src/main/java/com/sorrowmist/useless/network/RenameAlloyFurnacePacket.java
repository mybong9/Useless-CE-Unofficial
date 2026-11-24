package com.sorrowmist.useless.network;

import com.sorrowmist.useless.content.menus.AdvancedAlloyFurnaceMenu;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public record RenameAlloyFurnacePacket(int containerId, String name) implements CoreCustomPacketPayload {
    public static final int MAX_NAME = 48;
    private static final Type<RenameAlloyFurnacePacket> TYPE =
            new Type<>(new ResourceLocation("useless_mod", "rename_alloy_furnace"));
    public static final CoreStreamCodec<FriendlyByteBuf, RenameAlloyFurnacePacket> CODEC = CoreStreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.containerId());
                buf.writeUtf(packet.name(), MAX_NAME);
            }, buf -> new RenameAlloyFurnacePacket(buf.readVarInt(), buf.readUtf(MAX_NAME)));

    @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }

    public static void handle(RenameAlloyFurnacePacket packet, CorePacketContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof AdvancedAlloyFurnaceMenu menu)
                    || menu.containerId != packet.containerId() || player.isSpectator()) return;
            var furnace = menu.getBlockEntity();
            if (furnace == null || furnace.getLevel() != player.level()
                    || player.distanceToSqr(Vec3.atCenterOf(furnace.getBlockPos())) > 64
                    || player.level().getBlockEntity(furnace.getBlockPos()) != furnace) return;
            String name = net.minecraft.SharedConstants.filterText(packet.name()).strip();
            if (name.isEmpty()) return;
            furnace.setName(name);
            furnace.setChanged();
            var state = furnace.getBlockState();
            player.level().sendBlockUpdated(furnace.getBlockPos(), state, state, 2);
        });
    }
}
