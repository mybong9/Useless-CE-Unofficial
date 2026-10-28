package com.sorrowmist.useless.network;


import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.uselesscore.api.enums.tool.ConstructionWandCoreMode;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import com.sorrowmist.useless.content.items.EndlessBeafItem;
import com.sorrowmist.useless.utils.UselessItemUtils;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

public record ConstructionWandCorePacket(ConstructionWandCoreMode mode) implements CoreCustomPacketPayload {
    public static final Type<ConstructionWandCorePacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "construction_wand_core"));
    public static final CoreStreamCodec<FriendlyByteBuf, ConstructionWandCorePacket> STREAM_CODEC = CoreStreamCodec.of(
            (buf, packet) -> buf.writeEnum(packet.mode),
            buf -> new ConstructionWandCorePacket(buf.readEnum(ConstructionWandCoreMode.class))
    );

    public static void handle(ConstructionWandCorePacket message, CorePacketContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            var entry = UselessItemUtils.findTargetToolInHands(player);
            if (entry.isEmpty()) return;

            ItemStack stack = entry.get().getKey();
            if (!(stack.getItem() instanceof EndlessBeafItem)) return;
            CoreItemComponents.set(stack, UselessComponents.ConstructionWandCoreComponent.get(), message.mode);
            player.containerMenu.broadcastChanges();
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
