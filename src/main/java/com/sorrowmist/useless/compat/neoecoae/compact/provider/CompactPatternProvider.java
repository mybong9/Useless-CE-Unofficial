package com.sorrowmist.useless.compat.neoecoae.compact.provider;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import cn.dancingsnow.neoecoae.api.ECOPatternInsertionResult;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingSystemBlockEntity;
import cn.dancingsnow.neoecoae.crafting.execution.fastpath.ECOBatchCraftingRequest;
import cn.dancingsnow.neoecoae.crafting.execution.fastpath.ECOExtractedPatternExecution;
import com.sorrowmist.useless.compat.neoecoae.compact.entity.CompactF9BlockEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** A native ECO 20 pattern-bus service aggregating the compact host's shadow buses. */
public final class CompactPatternProvider extends ECOCraftingPatternBusBlockEntity {
    private final CompactF9BlockEntity host;
    private final List<ECOCraftingPatternBusBlockEntity> buses = new ArrayList<>();
    private final Map<BatchFastPathOffer, ECOCraftingPatternBusBlockEntity> offerOwners = new WeakHashMap<>();

    public CompactPatternProvider(CompactF9BlockEntity host) {
        super(host.getType(), host.getBlockPos(), host.getBlockState());
        this.host = host;
        getMainNode().setFlags();
        getMainNode().setInWorldNode(false);
    }

    public void bind(List<? extends ECOCraftingPatternBusBlockEntity> source) {
        buses.clear();
        buses.addAll(source);
        offerOwners.clear();
        if (host.getLevel() != null) setLevel(host.getLevel());
    }

    public boolean isEmpty() { return buses.isEmpty(); }
    public List<ECOCraftingPatternBusBlockEntity> buses() { return List.copyOf(buses); }
    @Override public void onReady() {}
    @Override public IGrid getGrid() { return host.getMainNode().getGrid(); }
    @Override public ECOCraftingSystemBlockEntity getCraftingController() { return host; }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        Set<IPatternDetails> union = new LinkedHashSet<>();
        for (ECOCraftingPatternBusBlockEntity bus : buses) union.addAll(bus.getAvailablePatterns());
        return List.copyOf(union);
    }

    public List<IPatternDetails> getLocalAvailablePatterns() { return getAvailablePatterns(); }

    @Override
    public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputs) {
        return pushPattern(pattern, inputs, null);
    }

    @Override
    public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputs, UUID jobId) {
        for (ECOCraftingPatternBusBlockEntity bus : buses) {
            if (bus.pushPattern(pattern, inputs, jobId)) return true;
        }
        return false;
    }

    @Override
    public boolean pushPattern(ECOExtractedPatternExecution execution, UUID jobId) {
        for (ECOCraftingPatternBusBlockEntity bus : buses) {
            if (bus.pushPattern(execution, jobId)) return true;
        }
        return false;
    }

    @Override
    public boolean isBusy() {
        return buses.isEmpty() || buses.stream().allMatch(ECOCraftingPatternBusBlockEntity::isBusy);
    }

    @Override
    public Set<AEKey> getEmitableItems() {
        Set<AEKey> union = new LinkedHashSet<>();
        for (ECOCraftingPatternBusBlockEntity bus : buses) union.addAll(bus.getEmitableItems());
        return union;
    }

    @Override
    public int getAvailableThreadSlots() {
        long count = 0;
        for (ECOCraftingPatternBusBlockEntity bus : buses) count += bus.getAvailableThreadSlots();
        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    @Override
    public BatchFastPathOffer findBatchFastPathOffer(ECOExtractedPatternExecution execution, int maximum) {
        BatchFastPathOffer best = null;
        ECOCraftingPatternBusBlockEntity owner = null;
        for (var bus : buses) {
            var candidate = bus.findBatchFastPathOffer(execution, maximum);
            if (candidate != null && (best == null || candidate.maxBatchSize() > best.maxBatchSize())) {
                best=candidate; owner=bus;
            }
        }
        if(best!=null) offerOwners.put(best,owner);
        return best;
    }

    @Override
    public boolean pushBatch(ECOBatchCraftingRequest request, BatchFastPathOffer offer) {
        ECOCraftingPatternBusBlockEntity owner = offerOwners.remove(offer);
        return owner != null && owner.pushBatch(request, offer);
    }

    @Override
    public boolean recoverJobToNetwork(UUID jobId, MEStorage storage) {
        boolean complete = true;
        for (ECOCraftingPatternBusBlockEntity bus : buses) complete &= bus.recoverJobToNetwork(jobId, storage);
        return complete;
    }

    @Override
    public ECOPatternInsertionResult insertPatternWithResult(ItemStack stack) {
        if (stack.isEmpty() || buses.isEmpty()) return ECOPatternInsertionResult.NO_TARGET;
        ECOPatternInsertionResult last = ECOPatternInsertionResult.NO_TARGET;
        for (ECOCraftingPatternBusBlockEntity bus : buses) {
            last = bus.insertPatternWithResult(stack);
            if (last == ECOPatternInsertionResult.INSERTED || last == ECOPatternInsertionResult.ALREADY_PRESENT) return last;
        }
        return last;
    }

    @Override public boolean insertPattern(ItemStack stack) { return insertPatternWithResult(stack) == ECOPatternInsertionResult.INSERTED; }
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