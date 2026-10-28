package com.sorrowmist.useless.compat.neoecoae.compact.shadow;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.orientation.BlockOrientation;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * 「影子样板总线」：不落世界的 F9 智能样板总线。
 *
 * <p>它保留 ECO 原生的样板库存、样板解析与派单逻辑；紧凑 F9 会把它的
 * {@code ICraftingProvider} / {@code IECOPatternStorage} 服务挂到主机的网格节点上，
 * 于是玩家往紧凑方块里插的样板依然由 ECO 自己的总线实现去解析与派单。</p>
 */
public class ShadowPatternBusBlockEntity extends ECOCraftingPatternBusBlockEntity {

    public ShadowPatternBusBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    @Nullable
    public IGrid getGrid() {
        if (cluster != null && cluster.getController() != null) {
            return cluster.getController().getMainNode().getGrid();
        }
        IGridNode node = getGridNode();
        return node == null ? null : node.getGrid();
    }

    @Override
    public void updateState(boolean updateExposed) {
    }

    @Override
    public void markForUpdate() {
    }

    @Override
    public void saveChanges() {
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.noneOf(Direction.class);
    }

    private boolean compactCapacityDirty = true;
    private int compactOccupiedPages;
    @Override public int getPageCount() {
        int configured = com.sorrowmist.useless.core.config.ConfigManager.getCompactF9PatternSlots();
        if (configured == 0) configured = cn.dancingsnow.neoecoae.config.NEConfig.getCraftingPatternBusPages() * SLOTS_PER_PAGE;
        if (compactCapacityDirty) {
            compactCapacityDirty = false;
            compactOccupiedPages = 0;
            var inventory = ((com.sorrowmist.useless.mixin.neoeco.NeoEcoPatternBusInventoryAccessor)this)
                    .useless$getPhysicalPatternInventory();
            if (inventory != null) for(int slot=inventory.size()-1;slot>=0;slot--) {
                if(!inventory.getStackInSlot(slot).isEmpty()) { compactOccupiedPages=slot/63+1; break; }
            }
        }
        return Math.min(66,Math.max((configured+62)/63,compactOccupiedPages));
    }
    @Override public int getPatternSlotCount() {
        // The last page is partial: 66 * 63 is 4158, but physical inventory and caches have 4096 slots.
        return Math.min(4096,super.getPatternSlotCount());
    }
    private Runnable persistenceListener = () -> {};
    public void setPersistenceListener(Runnable listener) { persistenceListener = listener; }
    private boolean compactPatternDirty;
    private int compactBatchDepth;
    @Override public int getPatternContentRevision() { return super.getPatternContentRevision(); }
    @Override public long getAuxiliaryRevision() { return super.getAuxiliaryRevision(); }
    @Override public appeng.api.inventories.InternalInventory getPatternSlotInventory() {
        return super.getPatternSlotInventory();
    }
    @Override public void setPatternDirect(int slot, net.minecraft.world.item.ItemStack stack) {
        super.setPatternDirect(slot,stack);
        compactPatternDirty=true;
        if (persistenceListener != null) persistenceListener.run();
    }
    @Override public void beginPatternBatch() { super.beginPatternBatch(); compactBatchDepth++; }
    @Override public void endPatternBatch() {
        if(compactBatchDepth<=0)throw new IllegalStateException("No pattern batch is active");
        super.endPatternBatch(); compactBatchDepth--; compactPatternDirty=true;
    }
    @Override public void onChangeInventory(appeng.api.inventories.InternalInventory inventory,int slot) {
        // The 20.5 catalog tracks every changed slot, not only the last slot of a batch.
        compactCapacityDirty=true;
        super.onChangeInventory(inventory,slot);
        compactPatternDirty=true;
        if (persistenceListener != null) persistenceListener.run();
    }
    public void flushPendingPatternChanges() {
        if(!compactPatternDirty || compactBatchDepth!=0)return;
        compactPatternDirty=false;
        super.refreshAdvertisedPatterns();
    }
    @Override public void loadTag(net.minecraft.nbt.CompoundTag tag) {
        compactCapacityDirty=true;
        super.loadTag(tag); compactPatternDirty=true; compactCapacityDirty=true;
    }
}
