package com.sorrowmist.useless.compat.neoecoae.compact.shadow;

import appeng.api.orientation.BlockOrientation;
import cn.dancingsnow.neoecoae.blocks.entity.ECOMachineInterfaceBlockEntity;
import cn.dancingsnow.neoecoae.multiblock.calculator.NEClusterCalculator;
import cn.dancingsnow.neoecoae.multiblock.cluster.NECluster;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.EnumSet;
import java.util.Set;

/** An ECO interface attached to the compact host without placing another block in the world. */
public class ShadowInterfaceBlockEntity<C extends NECluster<C>> extends ECOMachineInterfaceBlockEntity<C> {
    private final BlockEntity host;

    public ShadowInterfaceBlockEntity(BlockEntity host, NEClusterCalculator.Factory<C> calculator) {
        super(host.getType(), host.getBlockPos(), host.getBlockState(), calculator);
        this.host = host;
        getMainNode().setFlags();
        getMainNode().setInWorldNode(false);
    }

    public C compactCluster() { return cluster; }

    @Override public void updateState(boolean updateExposed) {}
    @Override public void markForUpdate() {}
    @Override public void saveChanges() { if (host != null) host.setChanged(); }
    public void markAsDirty() { if (host != null) host.setChanged(); }
    public boolean isInvalid() { return host == null || host.isRemoved(); }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.noneOf(Direction.class);
    }
}