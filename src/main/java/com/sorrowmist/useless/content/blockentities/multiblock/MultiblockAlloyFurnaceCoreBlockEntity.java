package com.sorrowmist.useless.content.blockentities.multiblock;



import com.sorrowmist.useless.init.ModNetwork;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnits;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import com.sorrowmist.uselesscore.api.crafting.bigint.cpu.AlloyFurnaceBigIntegerCpuBinding;
import com.sorrowmist.uselesscore.api.enums.RedstoneControlMode;
import com.sorrowmist.useless.compat.AppFluxCompat;
import com.sorrowmist.useless.content.blocks.multiblock.MultiblockAlloyFurnaceCoreBlock;
import com.sorrowmist.useless.content.blocks.multiblock.MultiblockFurnaceActivity;
import com.sorrowmist.useless.content.blocks.multiblock.OmniversalAlloyFurnaceStructure;
import com.sorrowmist.useless.content.blocks.multiblock.UselessCoilBlock;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.AdvancedAlloyFurnaceAeManager;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.AlloyFurnaceAeHost;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.CraftingAeOutputTarget;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.GridOutputTarget;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.CraftingTaskContext;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.OmniversalBigIntegerTarget;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.OmniversalPatternDetails;
import com.sorrowmist.useless.compat.neoecoae.NeoEcoDynamicOutputCompat;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.SmartDoublingPatterns;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.catalyst.ResolvedCatalystEffect;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.parallel.AlloyFurnaceParallelCalculator;
import com.sorrowmist.useless.content.multiblock.OmniversalCoilStats;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog;
import com.sorrowmist.useless.core.config.AlloyFurnaceTierRules;
import com.sorrowmist.useless.core.config.ConfigManager;
import com.sorrowmist.uselesscore.api.component.MultiblockRecoveryData;
import com.sorrowmist.useless.energy.EnergyManager;
import com.sorrowmist.useless.energy.IEnergyManager;
import com.sorrowmist.useless.init.ModBlockEntities;
import com.sorrowmist.useless.network.AlloyFurnaceErrorHighlightPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.sorrowmist.useless.content.menus.MultiblockAlloyFurnaceMenu;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/** Controller, energy store and persistent task owner for the multiblock furnace. */
public final class MultiblockAlloyFurnaceCoreBlockEntity extends BlockEntity implements AlloyFurnaceAeHost, MenuProvider {
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        // Includes the eight stations, exterior seals and the completion beam (top y=101).
        return new net.minecraft.world.phys.AABB(getBlockPos()).inflate(38, 0, 38).expandTowards(0, 103, 0).expandTowards(0, -3, 0);
    }
    private static final int SAFETY_REVALIDATION_TICKS = 40;
    public static final int MENU_DATA_COUNT = 11;
    private static final ItemStackHandler EMPTY_ITEMS = new ItemStackHandler(0);
    private static final FluidTank[] EMPTY_TANKS = new FluidTank[0];

    private final IEnergyManager energy = EnergyManager.builder()
            .capacity(1L).maxReceive(1L).maxExtract(0L).onChange(this::setChanged).build();
    private final AdvancedAlloyFurnaceAeManager aeManager = new AdvancedAlloyFurnaceAeManager(this);
    private boolean structureDirty = true;
    private boolean formed;
    private int coilTier;
    @Nullable
    private BlockPos patternAssemblyPos;
    @Nullable
    private BlockPos moldHubPos;
    @Nullable
    private BlockPos passiveHatchPos;
    private List<BlockPos> assemblyPositions=List.of(),moldHubPositions=List.of(),passiveHatchPositions=List.of();
    private List<BlockPos> gregTechParts = List.of();
    public List<BlockPos> getGregTechParts() { return formed ? gregTechParts : List.of(); }
    private long combinedMoldRevision=Long.MIN_VALUE;
    private java.util.Map<Integer,ItemStack> combinedMolds=java.util.Map.of();
    private com.sorrowmist.useless.content.recipe.MoldMatcher.PreparedMolds combinedMoldMatcher;
    private int validationTimer;
    private long structureGeneration;
    private boolean deferredTasksLoaded;
    private long recipeCatalogGeneration = -1L;
    private boolean unloading;
    /** 对外公开 API 的目标视图缓存：视图很轻（核心引用 + 一个路由字符串），一个实例即可。 */
    @Nullable
    private OmniversalBigIntegerTarget bigIntegerTarget;
    private boolean coilActivitySynchronized;
    private RedstoneControlMode redstoneControlMode = RedstoneControlMode.DISABLED;
    private long automaticEnergyLimit = Long.MAX_VALUE;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            long stored = energy.getEnergyStoredLong();
            long capacity = energy.getMaxEnergyStoredLong();
            long energyLimit = getAutomaticEnergyLimit();
            return switch (index) {
                case 0 -> formed ? 1 : 0;
                case 1 -> coilTier;
                case 2 -> (int) stored;
                case 3 -> (int) (stored >>> 32);
                case 4 -> (int) capacity;
                case 5 -> (int) (capacity >>> 32);
                case 6 -> aeManager.getActiveAETaskCount();
                case 7 -> redstoneControlMode.ordinal();
                case 8 -> getMaxAETaskCount();
                case 9 -> (int) energyLimit;
                case 10 -> (int) (energyLimit >>> 32);
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {}
        @Override public int getCount() { return MENU_DATA_COUNT; }
    };

    public MultiblockAlloyFurnaceCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MULTIBLOCK_ALLOY_FURNACE_CORE.get(), pos, state);
    }

    private boolean dismantling;
    public void beginDismantling() { cancelAllTasksForRemoval(); dismantling=true; formed=false; setChanged(); }
    public void endDismantling() { dismantling=false; requestStructureValidation(); }

    public void serverTick() {
        if (dismantling || unloading || isRemoved() || level == null || level.isClientSide) return;
        if (structureDirty || ++validationTimer >= SAFETY_REVALIDATION_TICKS) {
            validationTimer = 0;
            validateStructure();
        }
        if (!deferredTasksLoaded) {
            // 目录未就绪时保持未加载状态，下一 tick 重试；否则完好样板会被误判失效并退还材料。
            deferredTasksLoaded = aeManager.loadDeferredTasks();
        }
        long currentCatalogGeneration = AlloyFurnaceRecipeCatalog.generation();
        if (recipeCatalogGeneration != currentCatalogGeneration) {
            recipeCatalogGeneration = currentCatalogGeneration;
            updatePatterns();
        }
        // The assembly node may have mounted before the controller was linked
        // or while the AE grid was still booting.  Retry the provider refresh
        // after every validation/load transition without rebuilding patterns.
        aeManager.tickPatternRefresh();
        for (var assembly : getAssemblies()) assembly.refreshProviderIfReady();
        boolean progressed = false;
        if (isTaskExecutionEnabled()) {
            drawEnergyFromAeNetwork();
            aeManager.flushAEBatches();
            progressed = aeManager.tickAETasks();
            aeManager.tickUnreturnedInputs();
        }
        // 合成样板的产物必须在 AE2 记录完本批预期产物之后才能回网，所以不受上面的开关影响：
        // 即使任务执行被关闭，已经装配好的产物也必须继续投递。
        aeManager.tickQueuedCraftingOutputs();
        boolean hasWork=aeManager.hasWork();
        for (var hatch : getPassiveHatches()) {
            progressed |= hatch.serverTickFromController(this);
            hasWork |= hatch.hasWork();
        }
        updateDisplayedActivity(progressed, hasWork);
    }

    private void updateDisplayedActivity(boolean progressed, boolean hasWork) {
        MePatternAssemblyBlockEntity assembly = getAssembly();
        boolean aeConnected = formed && assembly != null && assembly.getMainNode().isActive()
                && assembly.getMainNode().getNode() != null
                && !assembly.getMainNode().getNode().getConnections().isEmpty();
        MultiblockFurnaceActivity activity =
                MultiblockFurnaceActivity.resolve(formed, aeConnected, hasWork);
        BlockState state = getBlockState();
        boolean activityChanged =
                state.getValue(MultiblockAlloyFurnaceCoreBlock.ACTIVITY) != activity;
        if (activityChanged) {
            level.setBlock(worldPosition,
                    state.setValue(MultiblockAlloyFurnaceCoreBlock.ACTIVITY, activity),
                    Block.UPDATE_CLIENTS);
        }
        if (activityChanged || !coilActivitySynchronized) {
            setCoilsActive(activity == MultiblockFurnaceActivity.RUN);
            coilActivitySynchronized = true;
        }
    }

    public void requestStructureValidation() {
        if (unloading || isRemoved()) return;
        structureDirty = true;
    }

    public void validateStructure() {
        if (unloading || isRemoved() || level == null) return;
        structureDirty = false;
        coilActivitySynchronized = false;
        Direction facing = getBlockState().getValue(MultiblockAlloyFurnaceCoreBlock.FACING);
        OmniversalAlloyFurnaceStructure.ValidationResult result =
                OmniversalAlloyFurnaceStructure.validate(level, worldPosition, facing);
        int validatedCoilTier = result.valid() ? result.coilTier() : 0;
        BlockPos validatedAssemblyPos = result.valid() ? result.patternAssemblyPos() : null;
        BlockPos validatedMoldHubPos = result.valid() ? result.moldHubPos() : null;
        BlockPos validatedHatchPos = result.valid() ? result.passiveHatchPos() : null;
        boolean stateChanged = formed != result.valid()
                || coilTier != validatedCoilTier
                || !java.util.Objects.equals(patternAssemblyPos, validatedAssemblyPos)
                || !java.util.Objects.equals(moldHubPos, validatedMoldHubPos)
                || !java.util.Objects.equals(passiveHatchPos, validatedHatchPos)
                || !assemblyPositions.equals(result.valid()?result.assemblies():List.of())
                || !moldHubPositions.equals(result.valid()?result.moldHubs():List.of())
                || !passiveHatchPositions.equals(result.valid()?result.passiveHatches():List.of())
                || !gregTechParts.equals(result.valid() ? result.gregTechParts() : List.of());
        formed = result.valid();
        coilTier = validatedCoilTier;
        patternAssemblyPos = validatedAssemblyPos == null ? null : validatedAssemblyPos.immutable();
        moldHubPos = validatedMoldHubPos == null ? null : validatedMoldHubPos.immutable();
        passiveHatchPos = validatedHatchPos == null ? null : validatedHatchPos.immutable();
        assemblyPositions=result.valid()?result.assemblies():List.of();
        moldHubPositions=result.valid()?result.moldHubs():List.of();
        passiveHatchPositions=result.valid()?result.passiveHatches():List.of();
        gregTechParts = result.valid() ? result.gregTechParts() : List.of();
        com.sorrowmist.useless.compat.gtceu.GregTechParts.bind(this, gregTechParts);
        if (stateChanged) {
            structureGeneration++;
            if (formed) {
                OmniversalCoilStats stats = OmniversalCoilStats.forTier(coilTier);
                energy.setMaxEnergyStored(stats.energyCapacity());
                energy.setMaxReceive(stats.maxReceive());
            }
            BlockState state = getBlockState();
            if (state.getValue(MultiblockAlloyFurnaceCoreBlock.FORMED) != formed) {
                level.setBlock(worldPosition, state.setValue(MultiblockAlloyFurnaceCoreBlock.FORMED, formed), 3);
            }
            setChanged();
        }
        linkFunctionalParts();
        synchronizeFormedTextures();
        if (stateChanged) updatePatterns();
        broadcastStructureError(result);
    }

    private void synchronizeFormedTextures() {
        for (var entry : OmniversalAlloyFurnaceStructure.entries()) {
            BlockPos pos = entry.worldPos(worldPosition,
                    getBlockState().getValue(MultiblockAlloyFurnaceCoreBlock.FACING));
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            var property = com.sorrowmist.useless.content.blocks.multiblock.OmniversalForgePartBlock.FORMED;
            if (state.hasProperty(property) && state.getValue(property) != formed) {
                level.setBlock(pos, state.setValue(property, formed), Block.UPDATE_CLIENTS);
            }
        }
    }

    private long completionPulseUntil;
    private long lastCompletionEventTick = Long.MIN_VALUE;

    @Override
    public void onCraftingCompleted() {
        if (level == null || level.isClientSide || !formed) return;
        // Parallel tasks finishing in one server tick share one visual event.
        if (lastCompletionEventTick == level.getGameTime()) return;
        lastCompletionEventTick = level.getGameTime();
        level.blockEvent(worldPosition, getBlockState().getBlock(), 71, 0);
    }

    @Override
    public boolean triggerEvent(int id, int parameter) {
        if (id == 71) {
            if (level != null && level.isClientSide) completionPulseUntil = level.getGameTime() + 60;
            return true;
        }
        return super.triggerEvent(id, parameter);
    }

    public float getCompletionPulse(float partialTick) {
        return level == null ? 0 : net.minecraft.util.Mth.clamp(
                (completionPulseUntil - level.getGameTime() - partialTick) / 60F, 0F, 1F);
    }

    /**
     * 把「当前该修的那一个方块」下发给追踪本区块的客户端：从下往上取世界坐标 Y 最小的 mismatch，
     * 同 Y 时保留 ENTRIES 遍历中先出现的（遍历顺序本身即由低到高）。
     *
     * <p>成型时下发 {@code null} 清掉残留高亮；验证本身只在结构变化或每 40 tick 触发，天然节流。</p>
     */
    private void broadcastStructureError(OmniversalAlloyFurnaceStructure.ValidationResult result) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockPos lowestError = null;
        if (!formed) {
            for (OmniversalAlloyFurnaceStructure.Mismatch mismatch : result.mismatches()) {
                BlockPos pos = mismatch.worldPos();
                if (lowestError == null || pos.getY() < lowestError.getY()) {
                    lowestError = pos;
                }
            }
        }
        ModNetwork.CHANNEL.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(worldPosition),
                new AlloyFurnaceErrorHighlightPacket(worldPosition, lowestError));
    }

    private void linkFunctionalParts() {
        if (unloading || isRemoved() || level == null) return;
        Direction facing = getBlockState().getValue(MultiblockAlloyFurnaceCoreBlock.FACING);
        for (OmniversalAlloyFurnaceStructure.Entry entry : OmniversalAlloyFurnaceStructure.entries()) {
            if (entry.part() != OmniversalAlloyFurnaceStructure.Part.INTERFACE) continue;
            BlockPos pos = entry.worldPos(worldPosition, facing);
            if (!level.isLoaded(pos)) continue;
            BlockEntity part = level.getBlockEntity(pos);
            if (part instanceof MePatternAssemblyBlockEntity assembly) {
                if (formed && assemblyPositions.contains(pos)) {
                    assembly.linkController(worldPosition, structureGeneration);
                } else if (assembly.isLinkedToController(worldPosition)) {
                    assembly.linkController(null, structureGeneration);
                }
            } else if (part instanceof OmniversalMoldHubBlockEntity hub) {
                hub.linkController(formed && moldHubPositions.contains(pos)
                        ? worldPosition : null, structureGeneration);
            } else if (part instanceof PassiveCraftingHatchBlockEntity hatch) {
                if (formed && passiveHatchPositions.contains(pos)) {
                    hatch.linkController(worldPosition, structureGeneration);
                } else if (hatch.isLinkedToController(worldPosition)) {
                    // Keep our own raw association while invalid so removal can still
                    // recover buffered materials, without stealing another core's hatch.
                    hatch.linkController(worldPosition, structureGeneration);
                }
            }
        }
    }

    private <T> List<T> loadedParts(List<BlockPos> positions,Class<T> type) {
        if (unloading || isRemoved() || level==null) return List.of();
        List<T> result=new ArrayList<>();
        for (BlockPos pos:positions) {
            if (!level.isLoaded(pos)) continue;
            var part=level.getBlockEntity(pos);
            if (type.isInstance(part)) result.add(type.cast(part));
        }
        return result;
    }

    private List<MePatternAssemblyBlockEntity> getAssemblies() {
        return loadedParts(assemblyPositions,MePatternAssemblyBlockEntity.class);
    }
    private List<PassiveCraftingHatchBlockEntity> getPassiveHatches() {
        return formed?loadedParts(passiveHatchPositions,PassiveCraftingHatchBlockEntity.class):List.of();
    }
    public List<OmniversalMoldHubBlockEntity> getLinkedMoldHubs() {
        return formed?loadedParts(moldHubPositions,OmniversalMoldHubBlockEntity.class):List.of();
    }
    @Nullable
    private MePatternAssemblyBlockEntity getAssembly() {
        var assemblies=getAssemblies();
        for (var assembly:assemblies) if (assembly.getMainNode().isActive()) return assembly;
        return assemblies.isEmpty()?null:assemblies.get(0);
    }
    @Nullable
    public OmniversalMoldHubBlockEntity getLinkedMoldHub() {
        var hubs=getLinkedMoldHubs();return hubs.isEmpty()?null:hubs.get(0);
    }
    public boolean isAssemblyNetworkEligible(MePatternAssemblyBlockEntity assembly) {
        var grid=getAeGrid();return grid!=null && assembly.getMainNode().getGrid()==grid;
    }
    public java.util.Map<Integer,ItemStack> getAvailableMoldMap() {
        var hubs=getLinkedMoldHubs();
        long revision=31*structureGeneration+AlloyFurnaceRecipeCatalog.generation();
        for (var hub:hubs) revision=31*revision+hub.getMoldStorageRevision()+hub.getMolds().getActiveSlots();
        if (combinedMoldMatcher==null || revision!=combinedMoldRevision) {
            var available=new java.util.LinkedHashMap<Integer,ItemStack>();
            int offset=0;
            for (var hub:hubs) {
                var molds=hub.getMolds();
                for(int slot=0;slot<molds.getActiveSlots();slot++) {
                    var stack=molds.getStackInSlot(slot);
                    if(!stack.isEmpty()) available.put(offset+slot,stack.copy());
                }
                offset+=molds.getActiveSlots();
            }
            combinedMolds=java.util.Collections.unmodifiableMap(available);
            combinedMoldMatcher=com.sorrowmist.useless.content.recipe.MoldMatcher.prepare(combinedMolds);
            combinedMoldRevision=revision;
        }
        return combinedMolds;
    }
    public boolean containsMolds(List<net.minecraft.world.item.crafting.Ingredient> requirements) {
        getAvailableMoldMap();return combinedMoldMatcher.matches(requirements);
    }

    public void onAeGridChanged() {
        if (unloading || isRemoved()) return;
        setChanged();
        for (var assembly:getAssemblies()) assembly.requestProviderRefresh();
    }

    public void updatePatterns() {
        if (dismantling || unloading || isRemoved() || level == null || level.isClientSide) return;
        aeManager.updatePatterns();
        for (var assembly:getAssemblies()) assembly.requestProviderRefresh();
    }

    public List<IPatternDetails> getAvailablePatterns() {
        return aeManager.getAvailablePatterns();
    }

    @Override
    public void onPatternsRebuilt() {
        for (var assembly:getAssemblies()) assembly.requestProviderRefresh();
    }

    public int getPatternPriority() {
        return aeManager.getPatternPriority();
    }

    public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputs) {
        return aeManager.pushPattern(pattern, inputs);
    }

    /** 数据能源原生 bigint 批次入口（单位原型 + 可能超过 long 的次数）。 */
    public boolean pushBigIntegerCraftingPattern(IPatternDetails pattern, BigInteger count,
                                                 KeyCounter[] unitPrototype) {
        return aeManager.pushBigIntegerCraftingPattern(pattern, count, unitPrototype);
    }

    public boolean isBusy() {
        return aeManager.isBusy();
    }

    public void cancelAllAETasks() {
        aeManager.cancelAllTasks();
    }

    /** Cancels every task owned by this structure before recovery data is captured. */
    public void cancelAllTasksForRemoval() {
        aeManager.cancelAllTasks();
        if (level == null) return;
        setCoilsActive(false);
        coilActivitySynchronized = true;
        Direction facing = getBlockState().getValue(MultiblockAlloyFurnaceCoreBlock.FACING);
        for (OmniversalAlloyFurnaceStructure.Entry entry : OmniversalAlloyFurnaceStructure.entries()) {
            if (entry.part() != OmniversalAlloyFurnaceStructure.Part.INTERFACE) continue;
            BlockPos pos = entry.worldPos(worldPosition, facing);
            if (!level.isLoaded(pos)) continue;
            if (level.getBlockEntity(pos) instanceof PassiveCraftingHatchBlockEntity hatch) {
                hatch.prepareForControllerRemoval(this);
            }
        }
    }

    private void setCoilsActive(boolean active) {
        if (level == null || level.isClientSide) return;
        Direction facing = getBlockState().getValue(MultiblockAlloyFurnaceCoreBlock.FACING);
        for (OmniversalAlloyFurnaceStructure.Entry entry
                : OmniversalAlloyFurnaceStructure.entries()) {
            if (entry.part() != OmniversalAlloyFurnaceStructure.Part.COIL) continue;
            BlockPos pos = entry.worldPos(worldPosition, facing);
            if (!level.isLoaded(pos)) continue;
            BlockState coilState = level.getBlockState(pos);
            if (!(coilState.getBlock() instanceof UselessCoilBlock)
                    || coilState.getValue(UselessCoilBlock.ACTIVE) == active) {
                continue;
            }
            level.setBlock(pos, coilState.setValue(UselessCoilBlock.ACTIVE, active),
                    Block.UPDATE_CLIENTS);
        }
    }

    public MultiblockRecoveryData createRecoveryData() {
        return new MultiblockRecoveryData(
                MultiblockRecoveryData.CURRENT_VERSION,
                energy.getEnergyStoredLong(),
                aeManager.getUnreturnedInputsSnapshot(),
                automaticEnergyLimit);
    }

    public void restoreRecoveryData(MultiblockRecoveryData data) {
        if (data == null) return;
        energy.setMaxEnergyStored(Math.max(energy.getMaxEnergyStoredLong(), data.energy()));
        energy.setEnergyStored(data.energy());
        aeManager.addUnreturnedInputs(data.contents());
        automaticEnergyLimit = data.automaticEnergyLimit();
        setChanged();
    }

    public Collection<AdvancedAlloyFurnaceAeManager.AETaskProgress> getAETaskProgressList() {
        return aeManager.getAETaskProgressList();
    }

    public void updateClientTaskProgress(List<com.sorrowmist.useless.network.AETaskProgressPacket.TaskProgressData> tasks) {
        aeManager.updateClientTaskProgress(tasks);
    }

    public boolean isFormed() {
        return formed;
    }

    public int getCoilTier() {
        return coilTier;
    }

    @Override
    public int getMachineTier() {
        return coilTier;
    }

    public long getStructureGeneration() {
        return structureGeneration;
    }

    public long getPassiveCraftingMaxParallel() {
        return formed ? OmniversalCoilStats.forTier(coilTier).singleTaskParallel() : 1;
    }

    @Override
    public long getCraftingPatternCapacity() {
        return Math.max(1L, getPassiveCraftingMaxParallel());
    }

    public boolean isPatternAssemblyLinked(BlockPos pos, long generation) {
        return formed && assemblyPositions.contains(pos)
                && structureGeneration == generation;
    }

    public boolean isMoldHubLinked(BlockPos pos, long generation) {
        return formed && moldHubPositions.contains(pos)
                && structureGeneration == generation;
    }

    public boolean isPassiveHatchLinked(BlockPos pos, long generation) {
        return formed && passiveHatchPositions.contains(pos)
                && structureGeneration == generation;
    }

    public RedstoneControlMode getRedstoneControlMode() {
        return redstoneControlMode;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.useless_mod.multiblock_alloy_furnace_core");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MultiblockAlloyFurnaceMenu(containerId, inventory, worldPosition);
    }

    public void sendAETaskProgressToPlayer(ServerPlayer player) {
        aeManager.sendAETaskProgressToPlayer(player);
    }

    public void cycleRedstoneControlMode() {
        redstoneControlMode = redstoneControlMode.next();
        setChanged();
    }

    @Override
    public @Nullable IManagedGridNode getMainNode() {
        MePatternAssemblyBlockEntity assembly = getAssembly();
        return assembly == null ? null : assembly.getMainNode();
    }

    @Override
    public @Nullable IGrid getAeGrid() {
        MePatternAssemblyBlockEntity assembly = getAssembly();
        return assembly == null ? null : assembly.getMainNode().getGrid();
    }

    @Override
    public int getMaxAETaskCount() {
        return formed ? OmniversalCoilStats.forTier(coilTier).threads() : 0;
    }

    public int getRemainingAETaskCount(boolean craftingPattern) {
        return aeManager.getRemainingAETaskCount(craftingPattern);
    }

    /**
     * 对外公开 API（{@code api.crafting.bigint}）的提交入口。
     *
     * <p>与数据能源适配器走同一条折叠路径，区别只是多带一个 CPU 回执绑定；{@code cpuBinding} 为
     * {@code null} 时产物只切段写回 ME 网络。万象样板与 AE2 合成样板都由这一个入口处理
     * （合成样板不收能量）。</p>
     */
    public boolean pushBigIntegerBatch(IPatternDetails pattern, BigInteger count,
                                       KeyCounter[] unitPrototype,
                                       @Nullable AlloyFurnaceBigIntegerCpuBinding cpuBinding) {
        return aeManager.pushBigIntegerBatch(pattern, count, unitPrototype, cpuBinding);
    }

    /**
     * 对外公开 API 的目标视图：第三方 CPU 通过
     * {@code AlloyFurnaceBigIntegerApi.findTargets(grid)} 拿到的就是这个对象。
     *
     * <p>视图本身很轻（只持有本核心与一个路由字符串），实例缓存在核心上即可；机器未成形或正在卸载时
     * 返回 {@code null}，这样发现流程不会拿到一台已经没了的机器。</p>
     */
    @Nullable
    public OmniversalBigIntegerTarget bigIntegerTarget() {
        if (!formed || unloading) {
            return null;
        }
        if (bigIntegerTarget == null) {
            bigIntegerTarget = new OmniversalBigIntegerTarget(
                    this, "useless_mod:multiblock_alloy_furnace@" + worldPosition.asLong());
        }
        return bigIntegerTarget;
    }

    @Override
    public Iterable<ItemStack> getPatternStacks() {
        if (!formed) return List.of();
        List<ItemStack> result=new ArrayList<>();
        for(var assembly:getAssemblies()) {
            var patterns=assembly.getPatterns();
            for(int slot=0;slot<patterns.getActiveSlots();slot++) result.add(patterns.getStackInSlot(slot));
        }
        return result;
    }

    @Override
    public boolean canPublishPatterns() {
        return formed;
    }

    @Override
    public boolean acceptsPattern(IPatternDetails pattern) {
        IPatternDetails original = SmartDoublingPatterns.unwrap(pattern);
        // 万象样板交给合金炉加工；AE2 合成样板由样板总成在虚拟工作台上自执行。
        return original instanceof OmniversalPatternDetails
                || original instanceof IMolecularAssemblerSupportedPattern;
    }

    @Override
    public AdvancedAlloyFurnaceRecipe resolveTaskRecipe(
            IPatternDetails pattern, List<ItemStack> items, List<FluidStack> fluids,
            List<GenericStack> keys, long operations) {
        IPatternDetails original = SmartDoublingPatterns.unwrap(pattern);
        return original instanceof OmniversalPatternDetails omniversal ? omniversal.recipe() : null;
    }

    @Override
    public boolean isTaskRecipeAvailable(AdvancedAlloyFurnaceRecipe recipe) {
        return getTaskAvailability(recipe).available();
    }

    @Override
    public CraftingTaskContext.TaskAvailability getTaskAvailability(AdvancedAlloyFurnaceRecipe recipe) {
        var gt = com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeEnvironment.check(this, recipe);
        if (!gt.available()) return gt;
        if (!com.sorrowmist.useless.content.recipe.adapters.voidminers.VoidMiningRecipeAdapter
                .allowsDimension(recipe, getLevel())) {
            return CraftingTaskContext.TaskAvailability.unavailable(
                    "gui.useless_mod.advanced_alloy_furnace.ae_task_status.waiting_conditions", "");
        }
        if (!formed) {
            return CraftingTaskContext.TaskAvailability.unavailable(
                    "gui.useless_mod.advanced_alloy_furnace.ae_task_status.waiting_structure", "");
        }
        if (recipe == null) {
            return CraftingTaskContext.TaskAvailability.unavailable(
                    "gui.useless_mod.advanced_alloy_furnace.ae_task_status.waiting_recipe", "");
        }
        int requiredTier = AlloyFurnaceTierRules.requiredTier(recipe.id(), recipe.tier());
        if (coilTier < requiredTier) {
            return CraftingTaskContext.TaskAvailability.unavailable(
                    "gui.useless_mod.advanced_alloy_furnace.ae_task_status.waiting_tier", "");
        }
        if (recipe.molds().isEmpty()) {
            return CraftingTaskContext.TaskAvailability.ready();
        }
        if (getLinkedMoldHubs().isEmpty()) {
            return CraftingTaskContext.TaskAvailability.unavailable(
                    "gui.useless_mod.advanced_alloy_furnace.ae_task_status.waiting_mold_hub", "");
        }
        return containsMolds(recipe.molds())
                ? CraftingTaskContext.TaskAvailability.ready()
                : CraftingTaskContext.TaskAvailability.unavailable(
                        "gui.useless_mod.advanced_alloy_furnace.ae_task_status.waiting_missing_mold",
                        CraftingTaskContext.describeRequiredMolds(recipe.molds()));
    }

    @Override
    public ResolvedCatalystEffect resolveTaskEffect(AdvancedAlloyFurnaceRecipe recipe) {
        return OmniversalCoilStats.forTier(Math.max(1, coilTier)).resolveEffect(recipe);
    }

    @Override
    public long getTaskParallel(AdvancedAlloyFurnaceRecipe recipe, ResolvedCatalystEffect effect) {
        return AlloyFurnaceParallelCalculator.calculateAeTaskParallel(recipe, effect);
    }

    @Override
    public boolean supportsLongAeAmounts() {
        return true;
    }

    /**
     * 多方块支持把万象样板折叠成 BigInteger 批次（一次解析配方、产物 ×count、按 count 收能量）。
     *
     * <p>单方块高级合金炉不覆写该方法 ⇒ 保持 {@code false}，其万象样板继续走长版 counted 路径。</p>
     */
    @Override
    public boolean supportsBigIntegerRecipeBatches() {
        return true;
    }

    /**
     * 本 tick 允许单批产生的产物分段数（AIMD 控制器，转发给 AE 管理器）。
     *
     * <p>用于解决超大量合成过程中交付不连续的问题：单批规模由实测反馈探测得出，
     * 收敛至「一批约在一到两个 tick 内交付完成」，而非使用固定值估算。</p>
     */
    @Override
    public long outputSegmentBudget() {
        return aeManager.outputSegmentBudget();
    }

    @Override
    public boolean isTaskExecutionEnabled() {
        return formed && level != null
                && redstoneControlMode.shouldRun(level.hasNeighborSignal(worldPosition));
    }

    @Override
    public int getInputSlotsStart() { return 0; }
    @Override
    public int getInputSlotsCount() { return 0; }
    @Override
    public int getOutputSlotsStart() { return 0; }
    @Override
    public int getOutputSlotsCount() { return 0; }
    @Override
    public int getCatalystSlot() { return 0; }
    @Override
    public int getMoldSlot() { return 0; }
    @Override
    public int getFluidTankCount() { return 0; }
    @Override
    public Level getLevel() { return level; }
    @Override
    public ItemStackHandler getItemHandler() { return EMPTY_ITEMS; }
    @Override
    public IEnergyManager getEnergyManager() { return energy; }
    public IEnergyManager getEnergyStorage() { return energy; }

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction side) {
        net.minecraftforge.common.util.LazyOptional<T> provided =
                com.sorrowmist.uselesscore.api.capability.CoreCapabilities
                        .getRegisteredBlockProvider(this, capability, side);
        return provided.isPresent() ? provided : super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        com.sorrowmist.uselesscore.api.capability.CoreCapabilities.invalidateBlockProviders(this);
    }
    public long getAutomaticEnergyLimit() {
        return clampAutomaticEnergyLimit(automaticEnergyLimit, energy.getMaxEnergyStoredLong());
    }

    public void setAutomaticEnergyLimit(long limit) {
        if (limit < 0L) return;
        long clampedLimit = clampAutomaticEnergyLimit(limit, energy.getMaxEnergyStoredLong());
        if (automaticEnergyLimit == clampedLimit) return;
        automaticEnergyLimit = clampedLimit;
        setChanged();
    }
    @Override
    public void markChanged() { setChanged(); }
    @Override
    public void sendAETaskProgressToClients() { aeManager.sendAETaskProgressToClients(); }
    @Override
    public int getCatalystMaxParallel() { return getMaxAETaskCount(); }
    @Override
    public boolean isReturnOutputToAe() { return true; }
    @Override
    public void stashUnreturnedInput(AEKey key, long amount) { aeManager.stashUnreturnedInput(key, amount); }
    @Override
    public ConcurrentHashMap<Integer, AdvancedAlloyFurnaceAeManager.AETaskProgress> getAETaskProgressMap() {
        return aeManager.getAETaskProgressMap();
    }
    @Override
    public AtomicInteger getTotalAEMaxProgressAtomic() { return aeManager.getTotalAEMaxProgressAtomic(); }
    @Override
    public AtomicInteger getTotalAEProgressAtomic() { return aeManager.getTotalAEProgressAtomic(); }
    @Override
    public ReentrantLock getCraftingLock() { return aeManager.getCraftingLock(); }
    @Override
    public FluidTank[] getInputFluidTanks() { return EMPTY_TANKS; }
    @Override
    public FluidTank[] getOutputFluidTanks() { return EMPTY_TANKS; }

    @Override
    public void handleUnreturnedItem(ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        if (key != null) stashUnreturnedInput(key, stack.getCount());
    }

    @Override
    public void handleUnreturnedFluid(FluidStack stack) {
        AEFluidKey key = AEFluidKey.of(stack);
        if (key != null) stashUnreturnedInput(key, stack.getAmount());
    }

    @Override
    public long tryOutputToAE(ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        return key == null ? 0L : tryOutputKeyToAE(key, stack.getCount());
    }

    @Override
    public long tryOutputFluidToAE(FluidStack stack) {
        AEFluidKey key = AEFluidKey.of(stack);
        return key == null ? 0L : tryOutputKeyToAE(key, stack.getAmount());
    }

    @Override
    public long tryOutputKeyToAE(AEKey key, long amount) {
        CraftingAeOutputTarget target = resolveAeOutputTarget();
        return target == null ? 0L : target.insert(key, amount);
    }

    /**
     * 解析一次 ME 网络写入目标，供一次「产物回网」刷写 pass 内复用。
     *
     * <p>此前每写一个分段都重新解析一次（{@link #getAeNetworkAccess()} 中 2 次方块实体查询 +
     * 2 次分配，再加 {@link #getAeGrid()} 又一次装配体查询）。可持续吞吐直接由「每 tick 可插入次数」
     * 决定，一 tick 可能插入数千次 ⇒ 该解析成本直接影响吞吐，必须在 pass 外仅执行一次。</p>
     */
    @Override
    public @Nullable CraftingAeOutputTarget resolveAeOutputTarget() {
        AeNetworkAccess access = getAeNetworkAccess();
        if (access == null) {
            return null;
        }
        MEStorage storage = access.storage();
        IActionSource source = access.source();
        // 网格也一并解析一次（getAeGrid() 内部又是一次装配体查询）。
        // 返回的 GridOutputTarget 支持「认领提升」：刷写 pass 会把 neoecoae 的逐段认领
        // 提到逐键一次（见 CraftingAeOutputTarget#supportsClaimHoisting）。
        return new GridOutputTarget(getAeGrid(), storage, source);
    }

    @Nullable
    AeNetworkAccess getAeNetworkAccess() {
        if (!formed) return null;
        MePatternAssemblyBlockEntity assembly = getAssembly();
        if (assembly == null || !assembly.getMainNode().isActive()) return null;
        IGrid grid = assembly.getMainNode().getGrid();
        if (grid == null) return null;
        var storageService = grid.getStorageService();
        return new AeNetworkAccess(storageService, IActionSource.ofMachine(assembly));
    }

    record AeNetworkAccess(IStorageService storageService, IActionSource source) {
        MEStorage storage() {
            return storageService.getInventory();
        }

        KeyCounter cachedInventory() {
            return storageService.getCachedInventory();
        }
    }

    private void drawEnergyFromAeNetwork() {
        boolean drawAppflux = AppFluxCompat.isLoaded() && ConfigManager.isFurnaceDrawAppfluxEnergyEnabled();
        boolean drawAe = ConfigManager.isFurnaceDrawAeEnergyEnabled();
        if (!drawAppflux && !drawAe) return;
        MePatternAssemblyBlockEntity assembly = getAssembly();
        if (assembly == null || !assembly.getMainNode().isActive()) return;
        long wanted = calculateAutomaticEnergyRequest(
                energy.getEnergyStoredLong(), energy.getMaxEnergyStoredLong(),
                energy.getMaxReceiveLong(), automaticEnergyLimit);
        IGrid grid = assembly.getMainNode().getGrid();
        if (wanted <= 0 || grid == null) return;
        IActionSource source = IActionSource.ofMachine(assembly);
        if (drawAppflux) {
            long received = AppFluxCompat.extractFe(grid, wanted, source);
            if (received > 0) {
                energy.modifyEnergy(received);
                wanted -= received;
            }
        }
        if (drawAe && wanted > 0) {
            IEnergyService energyService = grid.getEnergyService();
            double extracted = energyService.extractAEPower(
                    PowerUnits.FE.convertTo(PowerUnits.AE, wanted), Actionable.MODULATE, PowerMultiplier.ONE);
            long received = Math.min(wanted,
                    (long) Math.floor(PowerUnits.AE.convertTo(PowerUnits.FE, extracted)));
            if (received > 0) energy.modifyEnergy(received);
        }
    }

    static long calculateAutomaticEnergyRequest(
            long stored, long capacity, long maxReceive, long configuredLimit) {
        long effectiveLimit = clampAutomaticEnergyLimit(configuredLimit, capacity);
        if (stored >= effectiveLimit) return 0L;
        return Math.min(Math.max(0L, maxReceive), effectiveLimit - Math.max(0L, stored));
    }

    static long clampAutomaticEnergyLimit(long requested, long capacity) {
        return Math.min(Math.max(0L, requested), Math.max(0L, capacity));
    }

    private static List<BlockPos> readPartPositions(CompoundTag tag,String key,BlockPos legacy) {
        if(tag.contains(key)) return java.util.Arrays.stream(tag.getLongArray(key)).mapToObj(BlockPos::of).toList();
        return legacy==null?List.of():List.of(legacy);
    }

    @Override
    public void load(CompoundTag tag) {
        net.minecraft.core.HolderLookup.Provider registries = com.sorrowmist.uselesscore.api.registry.CoreRegistries.lookup(this);
        super.load(tag);
        energy.deserializeNBT(tag.getCompound("Energy"));
        formed = tag.getBoolean("Formed");
        coilTier = tag.getInt("CoilTier");
        structureGeneration = tag.getLong("StructureGeneration");
        patternAssemblyPos = tag.contains("PatternAssembly")
                ? BlockPos.of(tag.getLong("PatternAssembly")) : null;
        moldHubPos = tag.contains("MoldHub") ? BlockPos.of(tag.getLong("MoldHub")) : null;
        passiveHatchPos = tag.contains("PassiveHatch")
                ? BlockPos.of(tag.getLong("PassiveHatch")) : null;
        assemblyPositions=readPartPositions(tag,"Assemblies",patternAssemblyPos);
        moldHubPositions=readPartPositions(tag,"MoldHubs",moldHubPos);
        passiveHatchPositions=readPartPositions(tag,"PassiveHatches",passiveHatchPos);
        redstoneControlMode = RedstoneControlMode.byIndex(tag.getInt("RedstoneControlMode"));
        automaticEnergyLimit = tag.contains("AutomaticEnergyLimit", Tag.TAG_ANY_NUMERIC)
                ? Math.max(0L, tag.getLong("AutomaticEnergyLimit"))
                : Long.MAX_VALUE;
        aeManager.setPatternPriority(tag.getInt("PatternPriority"));
        aeManager.readTasksTag(tag);
        structureDirty = true;
        deferredTasksLoaded = false;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        unloading = false;
    }

    @Override
    public void onChunkUnloaded() {
        com.sorrowmist.useless.compat.gtceu.GregTechParts.release(this);
        unloading = true;
        super.onChunkUnloaded();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        net.minecraft.core.HolderLookup.Provider registries = com.sorrowmist.uselesscore.api.registry.CoreRegistries.lookup(this);
        super.saveAdditional(tag);
        tag.putLongArray("Assemblies",assemblyPositions.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLongArray("MoldHubs",moldHubPositions.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLongArray("PassiveHatches",passiveHatchPositions.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putInt("DataVersion", 1);
        tag.put("Energy", energy.serializeNBT());
        tag.putBoolean("Formed", formed);
        tag.putInt("CoilTier", coilTier);
        tag.putLong("StructureGeneration", structureGeneration);
        if (patternAssemblyPos != null) tag.putLong("PatternAssembly", patternAssemblyPos.asLong());
        if (moldHubPos != null) tag.putLong("MoldHub", moldHubPos.asLong());
        if (passiveHatchPos != null) tag.putLong("PassiveHatch", passiveHatchPos.asLong());
        tag.putInt("RedstoneControlMode", redstoneControlMode.ordinal());
        tag.putLong("AutomaticEnergyLimit", automaticEnergyLimit);
        tag.putInt("PatternPriority", aeManager.getPatternPriority());
        CompoundTag tasks = new CompoundTag();
        aeManager.saveTasks(tasks, registries);
        tag.put("AeTasks", tasks);
    }

    @Override
    public void setRemoved() {
        com.sorrowmist.useless.compat.gtceu.GregTechParts.release(this);
        unloading = true;
        super.setRemoved();
        aeManager.shutdown();
    }
}
