package com.sorrowmist.useless.network;


import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.uselesscore.api.enums.tool.EnchantMode;
import com.sorrowmist.useless.content.items.EndlessBeafItem;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import com.sorrowmist.useless.utils.UselessItemUtils;
import com.sorrowmist.uselesscore.api.component.CoreItemStackData;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

public class EnchantmentSwitchPacket implements CoreCustomPacketPayload {

    public static final Type<EnchantmentSwitchPacket> TYPE = new Type<>(
            new ResourceLocation(UselessMod.MODID, "enchantment_switch"));
    public static final CoreStreamCodec<FriendlyByteBuf, EnchantmentSwitchPacket> STREAM_CODEC = CoreStreamCodec.of(
            (buf, pkt) -> {
                buf.writeEnum(pkt.mode);
            },
            buf -> new EnchantmentSwitchPacket(buf.readEnum(EnchantMode.class))
    );
    private final EnchantMode mode;

    public EnchantmentSwitchPacket(EnchantMode mode) {
        this.mode = mode;
    }

    public static void handle(EnchantmentSwitchPacket msg, CorePacketContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            var toolEntry = UselessItemUtils.findTargetToolInHands(player);
            if (toolEntry.isEmpty()) return; // 没找到工具直接返回

            var entry = toolEntry.get();
            ItemStack stack = entry.getKey();

            Level level = player.level();

            // 写入组件
            CoreItemComponents.set(stack, UselessComponents.EnchantModeComponent.get(), msg.mode);

            // 切换模型数据
            CoreItemStackData.setCustomModelData(stack, msg.mode == EnchantMode.FORTUNE ? 0 : 1);

            // 更新附魔
            EndlessBeafItem.refreshEnchantments(stack, level);

            // 显式同步物品到客户端
            player.containerMenu.broadcastChanges();
        });
    }

    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
