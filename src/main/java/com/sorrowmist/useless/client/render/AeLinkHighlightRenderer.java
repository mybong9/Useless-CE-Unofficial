package com.sorrowmist.useless.client.render;




import com.sorrowmist.useless.init.ModNetwork;
import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.items.BeefToolVariants;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import com.sorrowmist.useless.network.AeLinkPreviewRequestPacket;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

/**
 * 手持造化杖且开了 AE 连接模式时，把「工具绑定的访问点」和「它已经连上的机器」画上边框。
 *
 * <p>Forge supplies camera rotation at AFTER_TRANSLUCENT_BLOCKS.
 * Only subtract camera position; do not apply another camera rotation.</p>
 */
@EventBusSubscriber(modid = UselessMod.MODID, value = Dist.CLIENT)
public final class AeLinkHighlightRenderer {
    /** 提示数据变化很慢，没必要每 tick 问服务端。 */
    private static final int REFRESH_INTERVAL = 10;
    private static final double MAX_RENDER_DISTANCE = 128.0;

    private static ResourceLocation dimension;
    private static BlockPos accessPoint;
    private static List<BlockPos> machines = List.of();
    private static int tickCounter = REFRESH_INTERVAL;

    private AeLinkHighlightRenderer() {}

    public static void setLinks(ResourceLocation machineDimension, BlockPos accessPoint,
                                List<BlockPos> machines) {
        AeLinkHighlightRenderer.dimension = machineDimension;
        AeLinkHighlightRenderer.accessPoint = accessPoint.immutable();
        AeLinkHighlightRenderer.machines = List.copyOf(machines);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !shouldRequest(minecraft)) {
            clear();
            return;
        }
        if (++tickCounter < REFRESH_INTERVAL) {
            return;
        }
        tickCounter = 0;
        ModNetwork.CHANNEL.sendToServer(new AeLinkPreviewRequestPacket());
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || dimension == null
                || !dimension.equals(minecraft.level.dimension().location())
                || (accessPoint == null && machines.isEmpty())) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();

        pose.pushPose();
        VertexConsumer lines = buffer.getBuffer(RenderType.lines());

        if (accessPoint != null) {
            // 绑定的访问点用暖色，和「已连接的机器」区分开。
            drawBox(pose, lines, accessPoint, cameraPos, 1.0F, 0.82F, 0.30F, 0.95F);
        }
        for (BlockPos machine : machines) {
            if (Vec3.atCenterOf(machine).distanceToSqr(cameraPos) > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) {
                continue;
            }
            drawBox(pose, lines, machine, cameraPos, 0.35F, 0.78F, 1.0F, 0.9F);
        }

        pose.popPose();
        buffer.endBatch(RenderType.lines());
    }

    private static void drawBox(PoseStack pose, VertexConsumer lines, BlockPos pos, Vec3 cameraPos,
                                float red, float green, float blue, float alpha) {
        AABB box = new AABB(pos).inflate(0.0025).move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        LevelRenderer.renderLineBox(pose, lines, box, red, green, blue, alpha);
    }

    /** 只在手上真的拿着「开着 AE 连接模式、且已经绑定访问点」的造化杖时才要提示数据。 */
    private static boolean shouldRequest(Minecraft minecraft) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = minecraft.player.getItemInHand(hand);
            if (!BeefToolVariants.isBeafTool(stack)) continue;
            if (!CoreItemComponents.getOrDefault(stack, UselessComponents.AeNetworkConnectComponent.get(), false)) continue;
            if (CoreItemComponents.has(stack, UselessComponents.WIRELESS_LINK_TARGET.get())) return true;
        }
        return false;
    }

    /** 清空缓存并将刷新计数器置为上限，使下一次手持当 tick 立即请求一次最新数据。 */
    private static void clear() {
        dimension = null;
        accessPoint = null;
        machines = List.of();
        tickCounter = REFRESH_INTERVAL;
    }
}
