package com.sorrowmist.useless.network;


import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.items.EndlessBeafItem;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import com.sorrowmist.useless.utils.UselessItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端在魔典预览的五芒星上右键后，将「要摆放哪座仪式」上报给服务端。
 *
 * <p>方块的实际摆放与 AE 扣料全部在服务端完成，客户端仅负责上报预览信息，
 * 以避免客户端伪造摆放结果。</p>
 */
public class RitualSatchelPlacePacket implements CoreCustomPacketPayload {

    public static final Type<RitualSatchelPlacePacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "ritual_satchel_place"));
    public static final CoreStreamCodec<FriendlyByteBuf, RitualSatchelPlacePacket> STREAM_CODEC = CoreStreamCodec.of(
            (buf, pkt) -> {
                buf.writeResourceLocation(pkt.multiblockId);
                buf.writeBlockPos(pkt.anchor);
                buf.writeEnum(pkt.facing);
            },
            buf -> new RitualSatchelPlacePacket(
                    buf.readResourceLocation(), buf.readBlockPos(), buf.readEnum(Rotation.class))
    );

    private final ResourceLocation multiblockId;
    private final BlockPos anchor;
    private final Rotation facing;

    public RitualSatchelPlacePacket(ResourceLocation multiblockId, BlockPos anchor, Rotation facing) {
        this.multiblockId = multiblockId;
        this.anchor = anchor;
        this.facing = facing;
    }

    public static void handle(RitualSatchelPlacePacket msg, CorePacketContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            // 没装 occultism 就没有这套能力，直接丢弃，避免类加载失败
            if (!ModList.get().isLoaded("occultism")) return;

            var toolEntry = UselessItemUtils.findTargetToolInHands(player);
            if (toolEntry.isEmpty()) return;

            ItemStack stack = toolEntry.get().getKey();
            if (!(stack.getItem() instanceof EndlessBeafItem)) return;
            if (!CoreItemComponents.getOrDefault(stack, UselessComponents.BeefRitualSatchelComponent.get(), false)) return;
            if (!(player.level() instanceof ServerLevel serverLevel)) return;

            com.sorrowmist.useless.compat.occultism.RitualSatchelCompat.placeFromAe(
                    serverLevel, player, stack, msg.multiblockId, msg.anchor, msg.facing);

            player.containerMenu.broadcastChanges();
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
