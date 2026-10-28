package com.sorrowmist.useless.network;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.blockentities.multiblock.PassiveCraftingHatchBlockEntity;
import com.sorrowmist.useless.content.menus.PassiveCraftingHatchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

public record PassiveCraftingSettingsPacket(
        int containerId, BlockPos pos, int intervalTicks, long multiplier)
        implements CoreCustomPacketPayload {
    public static final Type<PassiveCraftingSettingsPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "passive_crafting_settings"));
    public static final CoreStreamCodec<FriendlyByteBuf, PassiveCraftingSettingsPacket> STREAM_CODEC =
            CoreStreamCodec.of((buf, packet) -> {
                buf.writeVarInt(packet.containerId);
                buf.writeBlockPos(packet.pos);
                buf.writeVarInt(packet.intervalTicks);
                buf.writeVarLong(packet.multiplier);
            }, buf -> new PassiveCraftingSettingsPacket(
                    buf.readVarInt(), buf.readBlockPos(), buf.readVarInt(), buf.readVarLong()));

    public static void handle(PassiveCraftingSettingsPacket packet, CorePacketContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof PassiveCraftingHatchMenu menu)
                    || menu.containerId != packet.containerId
                    || !menu.getBlockPos().equals(packet.pos)
                    || !menu.stillValid(player)) {
                return;
            }
            PassiveCraftingHatchBlockEntity hatch = menu.getHatch();
            if (hatch == null || hatch.getBlockPos().distSqr(packet.pos) != 0.0D
                    || packet.intervalTicks < PassiveCraftingHatchBlockEntity.MIN_INTERVAL_TICKS
                    || packet.intervalTicks > PassiveCraftingHatchBlockEntity.MAX_INTERVAL_TICKS
                    || packet.multiplier < 1
                    || packet.multiplier > hatch.getCurrentMaxParallel()) {
                return;
            }
            hatch.applySettings(packet.intervalTicks, packet.multiplier);
            menu.broadcastChanges();
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
