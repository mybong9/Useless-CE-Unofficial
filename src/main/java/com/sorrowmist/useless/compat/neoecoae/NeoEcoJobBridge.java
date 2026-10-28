package com.sorrowmist.useless.compat.neoecoae;

import cn.dancingsnow.neoecoae.api.me.ECOCraftingCPU;
import com.sorrowmist.uselesscore.api.crafting.CoreCraftingJobs;
import java.util.UUID;

/** Converts the native ECO 20 CPU state into the shared Core lifecycle context. */
public final class NeoEcoJobBridge {
    private NeoEcoJobBridge() {}

    public static CoreCraftingJobs.Context context(ECOCraftingCPU cpu) {
        if (!(cpu instanceof cn.dancingsnow.neoecoae.crafting.execution.ECOCraftingCPU nativeCpu)) return null;
        UUID id = nativeCpu.getLogic().getProcessingJobId();
        return id == null ? null : new CoreCraftingJobs.Context(id, cpu, cpu.getGrid(), nativeCpu.getLogic().getFinalJobOutput());
    }
}
