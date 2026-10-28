package com.sorrowmist.useless.mixin.neoeco;

import cn.dancingsnow.neoecoae.api.me.ECOCraftingCPU;
import com.sorrowmist.useless.compat.neoecoae.NeoEcoOutputClaimBridge;
import com.sorrowmist.useless.compat.neoecoae.NeoEcoJobBridge;
import com.sorrowmist.uselesscore.api.crafting.CoreCraftingJobs;
import com.sorrowmist.uselesscore.api.crafting.CoreCraftingOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.crafting.execution.ECOCraftingCPU", remap = false)
public abstract class NeoEcoCpuMixin implements CoreCraftingOutput.Target {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final
    private cn.dancingsnow.neoecoae.multiblock.cluster.NEComputationCluster cluster;

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void useless$compactName(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<
            net.minecraft.network.chat.Component> cir) {
        if (cluster instanceof com.sorrowmist.useless.compat.neoecoae.compact.cluster.CompactComputationCluster) {
            cir.setReturnValue(net.minecraft.network.chat.Component.translatable("gui.useless_mod.compact.c9.cpu"));
        }
    }

    @Override
    public CoreCraftingOutput.Result claimCoreCraftingOutput(CoreCraftingOutput.Request request) {
        return NeoEcoOutputClaimBridge.claim((ECOCraftingCPU) (Object) this, request);
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void useless$saveAttachments(CompoundTag tag, CallbackInfo ci) {
        CoreCraftingJobs.Context context = NeoEcoJobBridge.context((ECOCraftingCPU) (Object) this);
        if (context != null) tag.put("UselessCoreJobAttachments", CoreCraftingJobs.save(context, ((cn.dancingsnow.neoecoae.crafting.execution.ECOCraftingCPU)(Object)this).getLevel().registryAccess()));
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void useless$loadAttachments(CompoundTag tag, CallbackInfo ci) {
        CoreCraftingJobs.Context context = NeoEcoJobBridge.context((ECOCraftingCPU) (Object) this);
        if (context != null) CoreCraftingJobs.load(context, tag.getCompound("UselessCoreJobAttachments"), ((cn.dancingsnow.neoecoae.crafting.execution.ECOCraftingCPU)(Object)this).getLevel().registryAccess());
    }
}
