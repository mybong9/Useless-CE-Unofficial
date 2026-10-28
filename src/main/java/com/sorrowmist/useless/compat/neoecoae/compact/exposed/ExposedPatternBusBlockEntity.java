package com.sorrowmist.useless.compat.neoecoae.compact.exposed;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.AEItemKey;
import cn.dancingsnow.neoecoae.api.ECOPatternInsertionResult;
import cn.dancingsnow.neoecoae.blocks.entity.ECOMachineInterfaceBlockEntity;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity;
import cn.dancingsnow.neoecoae.multiblock.cluster.NECraftingCluster;
import com.sorrowmist.useless.compat.neoecoae.compact.NeoEcoCompactRegistry;
import com.sorrowmist.useless.compat.neoecoae.compact.provider.CombinedPatternInventory;
import com.sorrowmist.useless.compat.neoecoae.compact.shadow.ShadowPatternBusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Exposes the compact machine's real pattern inventories to AE2 and ECO terminals. */
public class ExposedPatternBusBlockEntity extends ECOCraftingPatternBusBlockEntity {
    private final CombinedPatternInventory merged = new CombinedPatternInventory();
    private final List<ShadowPatternBusBlockEntity> buses = new ArrayList<>();
    private IManagedGridNode hostNode;
    private int[] revisions = new int[0];
    private int[] sizes = new int[0];
    private int contentRevision;
    private long auxiliaryRevision;
    private InternalInventory terminalView;

    public ExposedPatternBusBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        getMainNode().setFlags();
        getMainNode().setInWorldNode(false);
        // ECO previews read this public handler directly, bypassing the terminal
        // getter. Its slot count and backing inventory must describe the same view.
        ((com.sorrowmist.useless.mixin.neoeco.NeoEcoPatternBusInventoryAccessor) this)
                .useless$setPatternItemHandler((net.minecraftforge.items.IItemHandlerModifiable) merged.toItemHandler());
    }

    public void bind(List<ShadowPatternBusBlockEntity> source, IManagedGridNode hostNode) {
        buses.clear();
        buses.addAll(source);
        merged.bind(buses);
        revisions = new int[buses.size()];
        sizes = new int[buses.size()];
        this.hostNode = hostNode;
        terminalView = null;
        contentRevision++;
        auxiliaryRevision++;
    }

    public void joinPatternDomain(NECraftingCluster owner) {
        owner.addBlockEntity(this);
        updateCluster(owner);
    }

    @Override public void onReady() {}
    @Override public IGrid getGrid() { return getMainNode().getGrid(); }
    @Override public void updateState(boolean updateExposed) {}
    @Override public void markForUpdate() {}
    @Override public void saveChanges() {}
    @Override public boolean isBusy() { return true; }
    @Override public List<IPatternDetails> getAvailablePatterns() { return List.of(); }
    public List<IPatternDetails> getLocalAvailablePatterns() { return List.of(); }
    @Override public InternalInventory getTerminalPatternInventory() {
        if (terminalView == null) terminalView = merged.createTerminalView();
        return terminalView;
    }
    @Override public int getPatternSlotCount() { return merged.size(); }
    public InternalInventory getPatternSlotInventory() { return merged; }
    // This object is a view over multiple physical buses. Its superclass catalog owns only
    // one 504-slot inventory and must never scan the aggregate slot count.
    @Override public void refreshAdvertisedPatterns() {
        for (var bus : buses) bus.refreshAdvertisedPatterns();
        if (hostNode != null) ICraftingProvider.requestUpdate(hostNode);
    }
    @Override public void refreshPatternDetailsForCatalog() {
        for (var bus : buses) bus.refreshPatternDetailsForCatalog();
    }
    @Override public void requestPatternDetailsRefresh() {
        for (var bus : buses) bus.requestPatternDetailsRefresh();
    }
    @Override public void flushScheduledPatternDetails() {
        for (var bus : buses) bus.flushScheduledPatternDetails();
    }
    @Override public IPatternDetails getDecodedPatternDetails(int slot) {
        var bus = merged.busOf(slot);
        return bus == null ? null : bus.getDecodedPatternDetails(merged.localSlotOf(slot));
    }
    @Override public String getPatternSearchKeywords(int slot) {
        var bus = merged.busOf(slot);
        return bus == null ? "" : bus.getPatternSearchKeywords(merged.localSlotOf(slot));
    }
    public int getPatternContentRevision() { return contentRevision; }
    public long getAuxiliaryRevision() { return auxiliaryRevision; }
    @Override public void onChangeInventory(InternalInventory inventory, int slot) {
        // Native preview Slot.setChanged calls this on the aggregate after a real-slot mutation.
        // Never let the superclass's single-bus catalog read the aggregate slot count.
        if (merged == null || buses == null) return;
        if (slot < 0) {
            for (var bus : buses) bus.onChangeInventory(bus.getPatternSlotInventory(), -1);
            return;
        }
        var bus = merged.busOf(slot);
        if (bus != null) bus.onChangeInventory(bus.getPatternSlotInventory(), merged.localSlotOf(slot));
    }
    public void setPatternDirect(int slot, ItemStack stack) { merged.setItemDirect(slot, stack); }
    public void beginPatternBatch() { buses.forEach(ShadowPatternBusBlockEntity::beginPatternBatch); }
    public void endPatternBatch() { buses.forEach(ShadowPatternBusBlockEntity::endPatternBatch); }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        var block = NeoEcoCompactRegistry.COMPACT_F9.get();
        return new PatternContainerGroup(AEItemKey.of(block.asItem()), block.getName(), List.of());
    }

    @Override
    public ECOPatternInsertionResult insertPatternWithResult(ItemStack stack) {
        ECOPatternInsertionResult result = ECOPatternInsertionResult.NO_TARGET;
        for (ShadowPatternBusBlockEntity bus : buses) {
            result = bus.insertPatternWithResult(stack);
            if (result == ECOPatternInsertionResult.INSERTED || result == ECOPatternInsertionResult.ALREADY_PRESENT) break;
        }
        return result;
    }

    public void tick() {
        boolean contentChanged = false;
        boolean layoutChanged = false;
        for (int i = 0; i < buses.size(); i++) {
            ShadowPatternBusBlockEntity bus = buses.get(i);
            bus.flushPendingPatternChanges();
            int revision = bus.getPatternContentRevision();
            int size = bus.getPatternSlotCount();
            contentChanged |= revisions[i] != revision;
            layoutChanged |= sizes[i] != size;
            revisions[i] = revision;
            sizes[i] = size;
        }
        if (!contentChanged && !layoutChanged) return;
        if (contentChanged) contentRevision++;
        if (layoutChanged) {
            auxiliaryRevision++;
            terminalView = null;
        }
        if (hostNode != null) ICraftingProvider.requestUpdate(hostNode);
        IGrid grid = getGrid();
        if (grid != null) {
            for (ECOMachineInterfaceBlockEntity<?> machineInterface :
                    grid.getActiveMachines(ECOMachineInterfaceBlockEntity.class)) {
                invalidatePreview(machineInterface);
            }
        }
    }

    private static void invalidatePreview(ECOMachineInterfaceBlockEntity<?> machineInterface) {
        machineInterface.refreshPatternCatalog();
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.noneOf(Direction.class);
    }
    @Override public ECOPatternInsertionResult insertPatternKnownUnique(ItemStack stack) {
        return insertAcross(bus -> bus.insertPatternKnownUnique(stack));
    }
    @Override public ECOPatternInsertionResult insertPreparedPattern(cn.dancingsnow.neoecoae.api.ECOPreparedPattern pattern) {
        return insertAcross(bus -> bus.insertPreparedPattern(pattern));
    }
    @Override public ECOPatternInsertionResult insertPreparedPatternKnownUnique(cn.dancingsnow.neoecoae.api.ECOPreparedPattern pattern) {
        return insertAcross(bus -> bus.insertPreparedPatternKnownUnique(pattern));
    }
    private ECOPatternInsertionResult insertAcross(java.util.function.Function<ECOCraftingPatternBusBlockEntity,ECOPatternInsertionResult> action) {
        ECOPatternInsertionResult result=ECOPatternInsertionResult.NO_TARGET;
        for(var bus:buses) {
            result=action.apply(bus);
            if(result==ECOPatternInsertionResult.INSERTED || result==ECOPatternInsertionResult.ALREADY_PRESENT)return result;
        }
        return result;
    }

}
