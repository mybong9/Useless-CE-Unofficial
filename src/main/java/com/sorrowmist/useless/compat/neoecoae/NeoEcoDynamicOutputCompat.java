package com.sorrowmist.useless.compat.neoecoae;

import appeng.api.config.Actionable;
import com.sorrowmist.uselesscore.api.crafting.CoreCraftingOutput;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import cn.dancingsnow.neoecoae.api.me.ECOCraftingCPU;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.DynamicPatternCpuStateManager;
import org.jetbrains.annotations.Nullable;

/** Routes dynamic item outputs through P16's atomic output-claim API. */
public final class NeoEcoDynamicOutputCompat {
    private NeoEcoDynamicOutputCompat() {
    }

    public static long claim(@Nullable Object grid, AEKey actualKey, long amount) {
        if (grid == null || !(actualKey instanceof AEItemKey itemKey) || amount <= 0L) {
            return 0L;
        }

        DynamicPatternCpuStateManager manager = DynamicPatternCpuStateManager.INSTANCE;
        DynamicPatternCpuStateManager.EcoCandidate match = null;
        long simulatedAmount = 0L;
        for (var candidate : manager.ecoCandidates(grid, itemKey.getId())) {
            if (!(candidate.cpu() instanceof ECOCraftingCPU cpu)) {
                continue;
            }
            long offered = Math.min(amount, candidate.remainingAmount());
            long preview = route(cpu, candidate, actualKey, offered, Actionable.SIMULATE);
            if (preview <= 0L) {
                continue;
            }
            if (match != null) {
                return 0L;
            }
            match = candidate;
            simulatedAmount = preview;
        }
        if (match == null) {
            return 0L;
        }

        ECOCraftingCPU cpu = (ECOCraftingCPU) match.cpu();
        long committed = route(cpu, match, actualKey, simulatedAmount, Actionable.MODULATE);
        if (committed <= 0L) {
            return 0L;
        }
        manager.claimEcoExact(match.craftingId(), match.exactExpectedKey(), committed);
        return committed;
    }

    private static long route(ECOCraftingCPU cpu, DynamicPatternCpuStateManager.EcoCandidate candidate,
            AEKey actualKey, long amount, Actionable mode) {
        if (actualKey.equals(candidate.exactExpectedKey())
                && cpu instanceof cn.dancingsnow.neoecoae.crafting.execution.ECOCraftingCPU nativeCpu) {
            var logic = nativeCpu.getLogic();
            if (!candidate.craftingId().equals(logic.getProcessingJobId())) {
                return 0L;
            }
            // ECO's normal input buffer reserves feedback for the next cycle before delivering the final output.
            return logic.insert(actualKey, amount, mode);
        }
        CoreCraftingOutput.Result result = CoreCraftingOutput.claim(cpu, new CoreCraftingOutput.Request(
                candidate.craftingId(), candidate.exactExpectedKey(), actualKey, amount, mode));
        return result.accepted() ? result.claimedAmount() : 0L;
    }
}
