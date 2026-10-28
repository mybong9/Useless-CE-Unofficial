package com.sorrowmist.useless.network;



import com.sorrowmist.useless.init.ModNetwork;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.core.config.BeefToolProtectionManager;
import com.sorrowmist.useless.core.config.ChainGroupManager;
import com.sorrowmist.useless.data.BeefToolLayout;
import com.sorrowmist.useless.data.BeefToolLayoutManager;
import com.sorrowmist.useless.utils.UselessItemUtils;
import com.sorrowmist.useless.utils.mining.MiningDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

public record BeefToolLayoutUpdatePacket(String json) implements CoreCustomPacketPayload {
    public static final Type<BeefToolLayoutUpdatePacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "beef_tool_layout_update"));
    public static final CoreStreamCodec<FriendlyByteBuf, BeefToolLayoutUpdatePacket> STREAM_CODEC =
            CoreStreamCodec.of(
                    (buffer, packet) -> buffer.writeUtf(packet.json, BeefToolLayout.MAX_TEXT_LENGTH),
                    buffer -> new BeefToolLayoutUpdatePacket(buffer.readUtf(BeefToolLayout.MAX_TEXT_LENGTH)));

    public static void handle(BeefToolLayoutUpdatePacket packet, CorePacketContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (UselessItemUtils.findTargetToolInHands(player).isEmpty()) {
                sendError(player, BeefToolLayout.Error.INVALID_STRUCTURE);
                return;
            }

            try {
                BeefToolLayout layout = BeefToolLayout.fromJson(packet.json);
                BeefToolLayoutManager.validateForSave(layout);
                BeefToolLayoutManager.normalizeForPlayer(player, layout);
                BeefToolLayoutManager.validateForSave(layout);
                BeefToolLayoutManager.save(player, layout);
                // 布局与连锁等价组共用同一份存档：改动后必须丢弃等价组解析缓存，
                // 并清掉 Tab 高亮缓存，否则玩家会看到按旧等价组算出来的连锁范围。
                ChainGroupManager.invalidate(player.getUUID());
                BeefToolProtectionManager.invalidate(player.getUUID());
                MiningDispatcher.clearPlayerCache(player);
                ModNetwork.CHANNEL.sendToPlayer(player, new BeefToolLayoutSyncPacket(layout.toJson()));
            } catch (BeefToolLayout.LayoutException exception) {
                sendError(player, exception.error());
            }
        });
    }

    private static void sendError(ServerPlayer player, BeefToolLayout.Error error) {
        ModNetwork.CHANNEL.sendToPlayer(player, new BeefToolLayoutResultPacket(error));
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
