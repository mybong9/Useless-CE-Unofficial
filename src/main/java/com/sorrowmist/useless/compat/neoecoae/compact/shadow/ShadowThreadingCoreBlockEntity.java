package com.sorrowmist.useless.compat.neoecoae.compact.shadow;

import appeng.api.orientation.BlockOrientation;
import cn.dancingsnow.neoecoae.api.ECOTier;
import cn.dancingsnow.neoecoae.blocks.entity.computation.ECOComputationThreadingCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.Set;

/**
 * 「影子线程核心」：不落世界的 C9 线程核心，为紧凑 C9 提供真实的 ECO 合成线程。
 *
 * <p>它永远是 C9 等级（每颗 64 线程），内核 {@code spawn()} / {@code deactivate()} 完全走 ECO 原逻辑。</p>
 */
public class ShadowThreadingCoreBlockEntity extends ECOComputationThreadingCoreBlockEntity {
    private Runnable ownerSaveCallback;

    public void setOwnerSaveCallback(Runnable callback) {
        this.ownerSaveCallback = callback;
    }


    public ShadowThreadingCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState, ECOTier.L9);
    }

    private void ensureCapacity(int length) {
        if (getCpus().length >= length) return;
        try {
            var cpus = ECOComputationThreadingCoreBlockEntity.class.getDeclaredField("cpus");
            var deferred = ECOComputationThreadingCoreBlockEntity.class.getDeclaredField("deferredInit");
            cpus.setAccessible(true); deferred.setAccessible(true);
            cpus.set(this, java.util.Arrays.copyOf(getCpus(), length));
            deferred.set(this, java.util.Arrays.copyOf((net.minecraft.nbt.CompoundTag[])deferred.get(this), length));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot grow compact ECO CPU slots", exception);
        }
    }
    @Override public void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("UselessCompactCpuSlots", getCpus().length);
    }
    @Override public void loadTag(net.minecraft.nbt.CompoundTag tag) {
        int length = Math.max(64,tag.getInt("UselessCompactCpuSlots"));
        // Older compact releases saved CPU indices without an explicit array length.
        for (String key : tag.getAllKeys()) if (key.matches("CPU[0-9]+")) {
            try { length=Math.max(length,Math.addExact(Integer.parseInt(key.substring(3)),1)); }
            catch (NumberFormatException | ArithmeticException invalid) { throw new IllegalArgumentException("Invalid compact CPU index",invalid); }
        }
        if (length > 1048576) throw new IllegalArgumentException("Compact CPU array exceeds safe load limit");
        ensureCapacity(length);
        super.loadTag(tag);
    }
    @Override
    public cn.dancingsnow.neoecoae.crafting.execution.ECOCraftingCPU spawn(appeng.api.networking.crafting.ICraftingPlan plan) {
        for (var cpu : getCpus()) if (cpu == null) return super.spawn(plan);
        ensureCapacity(Math.addExact(getCpus().length, 64));
        return super.spawn(plan);
    }

    @Override
    public void updateState(boolean updateExposed) {
    }

    @Override
    public void markForUpdate() {
    }

    @Override
    public void saveChanges() {
        if (ownerSaveCallback != null) ownerSaveCallback.run();
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.noneOf(Direction.class);
    }
}
