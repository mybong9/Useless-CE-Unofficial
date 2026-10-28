package com.sorrowmist.useless.network;


import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.uselesscore.api.enums.tool.ToolTypeMode;
import com.sorrowmist.useless.content.items.BeefToolVariants;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import com.sorrowmist.useless.init.ModItems;
import com.sorrowmist.useless.utils.UselessItemUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import org.jetbrains.annotations.NotNull;

public record ToolTypeModeSwitchPacket(ToolTypeMode mode) implements CoreCustomPacketPayload {
    public static final CoreStreamCodec<FriendlyByteBuf, ToolTypeModeSwitchPacket> STREAM_CODEC =
            CoreStreamCodec.of(
                    (buf, packet) -> buf.writeEnum(packet.mode),
                    buf -> new ToolTypeModeSwitchPacket(buf.readEnum(ToolTypeMode.class))
            );

    public static final Type<ToolTypeModeSwitchPacket> TYPE =
            new Type<>(new ResourceLocation(UselessMod.MODID, "tool_type_mode_switch"));

    public static void handle(ToolTypeModeSwitchPacket msg, CorePacketContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            var toolEntry = UselessItemUtils.findTargetToolInHands(player);
            if (toolEntry.isEmpty()) return; // 没找到工具直接返回

            var entry = toolEntry.get();
            ItemStack targetItem = entry.getKey();
            InteractionHand targetHand = entry.getValue();

            // 1. 创建新物品实例
            ItemStack newStack = switch (msg.mode) {
                case NONE_MODE, WRENCH_MODE, SCREWDRIVER_MODE, MALLET_MODE, CROWBAR_MODE, HAMMER_MODE ->
                        BeefToolVariants.createForToolMode(targetItem, msg.mode);
                case OMNITOOL_MODE -> {
                    ResourceLocation omnitoolId = new ResourceLocation("omnitools", "omni_wrench");
                    Item toolItem = BuiltInRegistries.ITEM.get(omnitoolId);
                    if (toolItem != Items.AIR) {
                        yield new ItemStack(toolItem);
                    } else {
                        yield new ItemStack(ModItems.ENDLESS_BEAF_WRENCH.get());
                    }
                }
            };

            // 2. 复制原有物品的所有NBT数据到新实例
            com.sorrowmist.uselesscore.api.component.CoreItemStackData.copyData(targetItem, newStack);

            // 3. 覆盖状态：最后设置当前的工具模式组件
            if (!newStack.isEmpty()) {
                CoreItemComponents.set(newStack, UselessComponents.CurrentToolTypeComponent.get(), msg.mode);
                player.setItemInHand(targetHand, newStack);

                // 显式同步物品到客户端
                player.containerMenu.broadcastChanges();
            }
        });
    }
    @Override
    public @NotNull Type<? extends CoreCustomPacketPayload> type() {
        return TYPE;
    }
}
