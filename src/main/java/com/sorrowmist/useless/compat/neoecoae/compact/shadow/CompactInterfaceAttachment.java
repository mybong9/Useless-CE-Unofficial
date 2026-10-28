package com.sorrowmist.useless.compat.neoecoae.compact.shadow;

import appeng.api.networking.IManagedGridNode;
import cn.dancingsnow.neoecoae.multiblock.calculator.NEClusterCalculator;
import cn.dancingsnow.neoecoae.multiblock.cluster.NECluster;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Hosts the in-memory ECO interface; LDLib 1 widgets synchronize its menu state directly. */
public final class CompactInterfaceAttachment<C extends NECluster<C>> {
    private static final String PERSIST_KEY = "useless_compact_interface";
    private final BlockEntity host;
    private final NEClusterCalculator.Factory<C> calculator;
    private final ShadowNodeLink nodeLink;
    @Nullable private ShadowInterfaceBlockEntity<C> shadow;
    @Nullable private C joinedCluster;
    @Nullable private appeng.api.networking.IGrid previewGrid;
    private long previewLayout = Long.MIN_VALUE;
    private long previewContents = Long.MIN_VALUE;

    public CompactInterfaceAttachment(BlockEntity host, NEClusterCalculator.Factory<C> calculator) {
        this.host = host;
        this.calculator = calculator;
        this.nodeLink = new ShadowNodeLink(host);
    }

    @Nullable
    public ModularUI createUI(Player player, boolean interfaceMode) {
        if (!interfaceMode) return null;
        ShadowInterfaceBlockEntity<C> target = shadow();
        if (!target.supportsStorageInterfaceUi() && !target.supportsCraftingInterfaceUi() && !target.supportsComputationInterfaceUi()) return null;
        var holder = new cn.dancingsnow.neoecoae.platform.ui.runtime.gui.factory.BlockUIMenuType.BlockUIHolder(player, host.getBlockPos());
        return target.createUI(holder).nativeUI(holder);
    }

    public void attach(ServerLevel level, C cluster, IManagedGridNode node) {
        ShadowInterfaceBlockEntity<C> target = shadow();
        if (target.compactCluster() != cluster) target.updateCluster(cluster);
        if (joinedCluster != cluster) {
            cluster.addBlockEntity(target);
            joinedCluster = cluster;
        }
        nodeLink.attach(target.getMainNode(), level, node);
    }

    public void release() { joinedCluster = null; }

    public void dispose() {
        nodeLink.detach();
        shadow = null;
        joinedCluster = null;
    }

    public void tick() {
        if (shadow == null) return;
        nodeLink.tick();
        refreshCompactPatternMapping();
        shadow.tick();
    }

    /** A shadow interface is not a world machine: native topology broadcasts do not reliably reach it. */
    private void refreshCompactPatternMapping() {
        if (shadow == null || !(host.getLevel() instanceof ServerLevel)
                || !(shadow.compactCluster() instanceof cn.dancingsnow.neoecoae.multiblock.cluster.NECraftingCluster crafting)) return;
        var grid = shadow.getMainNode().getGrid();
        long layout = 1, contents = 1;
        var visible = new java.util.ArrayList<cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity>();
        for (var bus : crafting.getPatternDomainBuses()) {
            var node = bus.getGridNode();
            if (grid == null || node == null || node.getGrid() != grid || bus.isRemoved()) continue;
            visible.add(bus);
            layout = 31 * layout + System.identityHashCode(bus);
            layout = 31 * layout + bus.getPatternSlotCount();
            contents = 31 * contents + bus.getPatternContentRevision();
            contents = 31 * contents + bus.getAuxiliaryRevision();
        }
        boolean layoutChanged = grid != previewGrid || layout != previewLayout;
        if (layoutChanged) {
            // Invalidate even when the domain object/generation did not change across node creation and connection.
            shadow.onPatternBusTopologyChanged(null);
        } else if (contents != previewContents) {
            for (var bus : visible) shadow.onPatternBusInventoryChanged(bus);
        }
        previewGrid = grid;
        previewLayout = layout;
        previewContents = contents;
    }

    public void save(HolderLookup.Provider provider, CompoundTag tag) {
        if (shadow == null) return;
        CompoundTag data = new CompoundTag();
        shadow.saveAdditional(data);
        tag.put(PERSIST_KEY, data);
    }

    public void load(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.contains(PERSIST_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            shadow().load(tag.getCompound(PERSIST_KEY));
        }
    }

    public ShadowInterfaceBlockEntity<C> interfaceTarget() { return shadow(); }

    private ShadowInterfaceBlockEntity<C> shadow() {
        if (shadow == null) shadow = new ShadowInterfaceBlockEntity<>(host, calculator);
        if (shadow.getLevel() != host.getLevel() && host.getLevel() != null) {
            shadow.setLevel(host.getLevel());
        }
        return shadow;
    }
}