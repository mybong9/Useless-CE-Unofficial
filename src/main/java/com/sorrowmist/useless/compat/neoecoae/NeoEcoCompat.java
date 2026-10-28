package com.sorrowmist.useless.compat.neoecoae;

import appeng.api.networking.crafting.ICraftingProvider;
import com.sorrowmist.uselesscore.api.crafting.CoreCraftingJobs;
import cn.dancingsnow.neoecoae.api.me.ECOCraftingCPU;
import com.sorrowmist.useless.content.blockentities.AdvancedAlloyFurnaceBlockEntity;
import com.sorrowmist.useless.content.blockentities.multiblock.MePatternAssemblyBlockEntity;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.DynamicPatternCpuStateManager;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.DynamicPatternExecution;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** P16 public-API integration for dynamic Useless Mod outputs. */
public final class NeoEcoCompat {
    public static final ResourceLocation DYNAMIC_OUTPUT_ATTACHMENT =
            new ResourceLocation("useless_mod", "dynamic_component_outputs");

    private static final CoreCraftingJobs.Listener LIFECYCLE = new Lifecycle();
    private static final java.util.function.BiPredicate<CoreCraftingJobs.Context, ICraftingProvider> POLICY = new DispatchPolicy();
    private static boolean initialized;

    private NeoEcoCompat() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        CoreCraftingJobs.registerAttachment(
                DYNAMIC_OUTPUT_ATTACHMENT, NeoEcoDynamicJobAttachment::new);
        CoreCraftingJobs.registerListener(LIFECYCLE);
        CoreCraftingJobs.registerPolicy(POLICY);
        cn.dancingsnow.neoecoae.api.me.lifecycle.ECOCraftingLifecycle.register(
            new cn.dancingsnow.neoecoae.api.me.lifecycle.ECOCraftingLifecycleListener() {
                private CoreCraftingJobs.Context context(cn.dancingsnow.neoecoae.api.me.lifecycle.ECOCraftingJobContext job) {
                    return new CoreCraftingJobs.Context(job.craftingJobId(), job.cpu(),
                        job.cpu() instanceof ECOCraftingCPU cpu ? cpu.getGrid() : null, job.finalOutput());
                }
                @Override public void onJobStarted(cn.dancingsnow.neoecoae.api.me.lifecycle.ECOCraftingJobContext job) {
                    CoreCraftingJobs.start(context(job));
                }
                @Override public void onPatternDispatched(cn.dancingsnow.neoecoae.api.me.lifecycle.ECOCraftingDispatchEvent event) {
                    CoreCraftingJobs.dispatch(new CoreCraftingJobs.Dispatch(context(event.job()),event.pattern(),event.provider(),event.dispatchedCrafts()));
                }
                @Override public void onJobFinished(cn.dancingsnow.neoecoae.api.me.lifecycle.ECOCraftingJobContext job,
                        cn.dancingsnow.neoecoae.api.me.lifecycle.ECOCraftingJobResult result) {
                    CoreCraftingJobs.finish(context(job), switch(result.status()) {
                        case SUCCESS -> CoreCraftingJobs.Result.COMPLETED;
                        case CANCELLED -> CoreCraftingJobs.Result.CANCELLED;
                        case FAILURE -> CoreCraftingJobs.Result.FAILED;
                    });
                }
            });
        cn.dancingsnow.neoecoae.api.me.dispatch.ECOCraftingDispatchPolicyRegistry.register(
            new cn.dancingsnow.neoecoae.api.me.dispatch.ECOCraftingDispatchPolicy() {
                @Override public boolean isProviderAvailable(cn.dancingsnow.neoecoae.api.me.dispatch.ECOCraftingCpuContext context, ICraftingProvider provider) {
                    if (!(context.cpu() instanceof ECOCraftingCPU cpu)) return !provider.isBusy();
                    var job=NeoEcoJobBridge.context(cpu);
                    return job==null ? !provider.isBusy() : CoreCraftingJobs.available(job,provider);
                }
            });
        initialized = true;
    }

    @Nullable
    static Object grid(CoreCraftingJobs.Context context) {
        return context.cpu() instanceof ECOCraftingCPU cpu ? cpu.getGrid() : null;
    }

    private static boolean isUselessProvider(ICraftingProvider provider) {
        return provider instanceof AdvancedAlloyFurnaceBlockEntity
                || provider instanceof MePatternAssemblyBlockEntity;
    }

    private static final class Lifecycle implements CoreCraftingJobs.Listener {
        @Override
        public void onJobStarted(CoreCraftingJobs.Context context) {
            DynamicPatternCpuStateManager.INSTANCE.ensureEcoJob(
                    context.craftingJobId(), context.cpu(), grid(context));
        }

        @Override
        public void onPatternDispatched(CoreCraftingJobs.Dispatch event) {
            DynamicPatternExecution.Resolved resolved =
                    DynamicPatternExecution.resolve(event.pattern());
            if (resolved == null || !isUselessProvider(event.provider())) {
                return;
            }
            long copies = Math.multiplyExact(resolved.copies(), event.dispatchedCrafts());
            DynamicPatternCpuStateManager.INSTANCE.bindEcoDispatch(
                    event.job().craftingJobId(),
                    event.job().cpu(),
                    grid(event.job()),
                    resolved.pattern(),
                    event.job().finalOutput() == null ? null : event.job().finalOutput().what(),
                    copies);
        }

        @Override
        public void onJobFinished(CoreCraftingJobs.Context context, CoreCraftingJobs.Result result) {
            DynamicPatternCpuStateManager.INSTANCE.clearEcoJob(context.craftingJobId());
        }
    }

    private static final class DispatchPolicy implements java.util.function.BiPredicate<CoreCraftingJobs.Context, ICraftingProvider> {
        @Override
        public boolean test(CoreCraftingJobs.Context context, ICraftingProvider provider) {
            if (!isUselessProvider(provider)) {
                return !provider.isBusy();
            }
            Object grid = context.cpu() instanceof ECOCraftingCPU cpu ? cpu.getGrid() : null;
            return !provider.isBusy()
                    && !DynamicPatternCpuStateManager.INSTANCE.hasEcoPendingOnGrid(grid);
        }
    }
}
