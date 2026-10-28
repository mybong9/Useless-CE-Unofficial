package com.sorrowmist.useless.compat.neoecoae;

import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.DynamicPatternCpuStateManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import com.sorrowmist.uselesscore.api.crafting.CoreCraftingJobs;
import appeng.api.stacks.AEKey;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Persists dynamic-output reservations through NeoECOAE's public job attachment API. */
public final class NeoEcoDynamicJobAttachment implements CoreCraftingJobs.Attachment {
    private final UUID craftingJobId;
    private final Object cpu;
    private final Object grid;
    private final Map<AEKey, Set<AEKey>> actualKeys = new LinkedHashMap<>();

    public NeoEcoDynamicJobAttachment(CoreCraftingJobs.Context context) {
        this.craftingJobId = context.craftingJobId();
        this.cpu = context.cpu();
        this.grid = NeoEcoCompat.grid(context);
        DynamicPatternCpuStateManager.INSTANCE.ensureEcoJob(craftingJobId, cpu, grid);
    }

    @Override
    public ResourceLocation id() {
        return NeoEcoCompat.DYNAMIC_OUTPUT_ATTACHMENT;
    }

    @Override
    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = DynamicPatternCpuStateManager.INSTANCE.writeEcoJob(craftingJobId, registries);
        if (tag == null) tag = new CompoundTag();
        ListTag entries = new ListTag();
        actualKeys.forEach((expected, actual) -> {
            CompoundTag entry = new CompoundTag();
            entry.put("Expected", expected.toTagGeneric());
            ListTag variants = new ListTag();
            actual.forEach(key -> variants.add(key.toTagGeneric()));
            entry.put("Actual", variants);
            entries.add(entry);
        });
        tag.put("CoreActualKeys", entries);
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        actualKeys.clear();
        ListTag entries = tag.getList("CoreActualKeys", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            AEKey expected = AEKey.fromTagGeneric(entry.getCompound("Expected"));
            if (expected == null) continue;
            ListTag variants = entry.getList("Actual", Tag.TAG_COMPOUND);
            for (int j = 0; j < variants.size(); j++) {
                AEKey actual = AEKey.fromTagGeneric(variants.getCompound(j));
                if (actual != null) recordActualKey(expected, actual);
            }
        }
        if (tag.isEmpty()) {
            DynamicPatternCpuStateManager.INSTANCE.ensureEcoJob(craftingJobId, cpu, grid);
        } else {
            DynamicPatternCpuStateManager.INSTANCE.readEcoJob(
                    craftingJobId, cpu, grid, tag, registries);
        }
    }

    @Override
    public void clear(CoreCraftingJobs.Result result) {
        actualKeys.clear();
        DynamicPatternCpuStateManager.INSTANCE.clearEcoJob(craftingJobId);
    }

    public void recordActualKey(AEKey expected, AEKey actual) {
        actualKeys.computeIfAbsent(expected, ignored -> new LinkedHashSet<>()).add(actual);
    }

    public Set<AEKey> inventoryKeys(AEKey expected, AEKey current) {
        Set<AEKey> keys = new LinkedHashSet<>(actualKeys.getOrDefault(expected, Set.of()));
        keys.add(expected);
        keys.add(current);
        return keys;
    }
}
