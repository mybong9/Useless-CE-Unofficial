package com.sorrowmist.useless.network;



import com.sorrowmist.useless.init.ModNetwork;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.menus.StaffLinkMenu;
import com.sorrowmist.useless.content.stafflink.StaffLinkEngine;
import com.sorrowmist.useless.content.stafflink.StaffLinkRoute;
import com.sorrowmist.useless.world.stafflink.StaffLinkManager;
import com.sorrowmist.useless.world.stafflink.StaffLinkNetwork;
import com.sorrowmist.useless.world.stafflink.StaffLinkSavedData;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端请求解绑一个锚点。
 *
 * <p>服务端只认「当前打开的那个物流界面所属的网络」，因此包体里不带网络 ID——
 * 玩家无法用它去动别人的网络。</p>
 */
public record StaffLinkDetachPacket(GlobalPos anchor) implements CoreCustomPacketPayload {

    public static final Type<StaffLinkDetachPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "staff_link_detach"));
    public static final CoreStreamCodec<FriendlyByteBuf, StaffLinkDetachPacket> STREAM_CODEC =
            CoreStreamCodec.of(
                    (buffer, packet) -> StaffLinkRoute.writeAnchor(buffer, packet.anchor()),
                    buffer -> new StaffLinkDetachPacket(StaffLinkRoute.readAnchor(buffer)));

    public static void handle(StaffLinkDetachPacket packet, CorePacketContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof StaffLinkMenu menu)) {
                return;
            }
            // 越权校验：界面所属的网络必须挂在他（或他队伍）名下。
            if (!StaffLinkManager.canAccess(player.server, player, menu.getNetworkId())) {
                return;
            }
            StaffLinkNetwork network = StaffLinkManager.networkById(player.server, menu.getNetworkId());
            if (network == null || !network.isBound(packet.anchor())) {
                return;
            }
            network.detach(packet.anchor());
            StaffLinkSavedData data = StaffLinkSavedData.get(player.server);
            data.markDirty();
            if (network.isEmpty()) {
                // 锚点清空 = 这张网络没有意义了：连登记一起删掉，界面随后自动关闭。
                data.remove(network.id());
                return;
            }
            StaffLinkEngine.wake(network.id());
            ModNetwork.CHANNEL.sendToPlayer(player, StaffLinkSyncPacket.of(
                    player.server, StaffLinkManager.ownerIdOf(player), network));
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
