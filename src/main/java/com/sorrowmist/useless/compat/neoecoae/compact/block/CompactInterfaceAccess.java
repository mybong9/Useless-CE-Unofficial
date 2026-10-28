package com.sorrowmist.useless.compat.neoecoae.compact.block;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.sorrowmist.useless.compat.neoecoae.compact.ui.CompactUiHost;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Opens compact host and interface menus with an explicit mode synchronized to both sides. */
public final class CompactInterfaceAccess {
    public static final BooleanProperty INTERFACE = BooleanProperty.create("useless_interface");
    private static final CompactFactory FACTORY = new CompactFactory();
    private CompactInterfaceAccess() {}

    public static void register() { UIFactory.register(FACTORY); }

    public static boolean requested(BlockState state) {
        return state.hasProperty(INTERFACE) && state.getValue(INTERFACE);
    }

    public static boolean isPlayerCloseEnough(Block block, Level level, BlockPos pos, Player player) {
        return player.level() == level && level.getBlockState(pos).getBlock() == block
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    public static void open(Block block, BlockState state, BlockPos pos, ServerPlayer player) {
        open(pos, player, true);
    }

    public static void openMain(Block block, BlockState state, BlockPos pos, ServerPlayer player) {
        open(pos, player, false);
    }

    private static void open(BlockPos pos, ServerPlayer player, boolean interfaceMode) {
        BlockEntity host = player.level().getBlockEntity(pos);
        if (host instanceof CompactUiHost) FACTORY.openUI(new OpenRequest(host, interfaceMode), player);
    }

    private record OpenRequest(BlockEntity host, boolean interfaceMode) {}

    private static final class CompactFactory extends UIFactory<OpenRequest> {
        private CompactFactory() { super(new ResourceLocation("useless_mod", "compact_machine")); }

        @Override
        protected ModularUI createUITemplate(OpenRequest request, Player player) {
            return request != null && request.host() instanceof CompactUiHost host
                    ? host.createCompactUI(player, request.interfaceMode()) : null;
        }

        @Override
        protected OpenRequest readHolderFromSyncData(FriendlyByteBuf buffer) {
            BlockPos pos = buffer.readBlockPos();
            boolean interfaceMode = buffer.readBoolean();
            Level level = Minecraft.getInstance().level;
            BlockEntity host = level == null ? null : level.getBlockEntity(pos);
            return host instanceof CompactUiHost ? new OpenRequest(host, interfaceMode) : null;
        }

        @Override
        protected void writeHolderToSyncData(FriendlyByteBuf buffer, OpenRequest request) {
            buffer.writeBlockPos(request.host().getBlockPos());
            buffer.writeBoolean(request.interfaceMode());
        }
    }
}