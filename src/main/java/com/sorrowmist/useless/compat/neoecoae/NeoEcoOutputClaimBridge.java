package com.sorrowmist.useless.compat.neoecoae;
import cn.dancingsnow.neoecoae.api.me.ECOCraftingCPU;
import cn.dancingsnow.neoecoae.api.me.output.ECOCraftingOutputClaimRequest;
import com.sorrowmist.uselesscore.api.crafting.CoreCraftingOutput;
/** Use ECO's atomic public claim API instead of mutating its internal job inventories. */
public final class NeoEcoOutputClaimBridge {
    private NeoEcoOutputClaimBridge() {}
    public static CoreCraftingOutput.Result claim(ECOCraftingCPU cpu, CoreCraftingOutput.Request request) {
        var sink=cpu.getOutputClaimSink();
        if(sink==null)return CoreCraftingOutput.Result.REJECTED;
        var result=sink.claimCraftingOutput(new ECOCraftingOutputClaimRequest(request.craftingJobId(),
            request.expectedKey(),request.actualKey(),request.amount(),request.mode()));
        return new CoreCraftingOutput.Result(result.accepted(),result.claimedAmount());
    }
}
