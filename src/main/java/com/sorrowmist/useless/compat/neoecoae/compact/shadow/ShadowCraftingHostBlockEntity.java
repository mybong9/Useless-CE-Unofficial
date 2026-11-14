package com.sorrowmist.useless.compat.neoecoae.compact.shadow;

import appeng.api.orientation.BlockOrientation;
import cn.dancingsnow.neoecoae.api.ECOTier;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingSystemBlockEntity;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingWorkerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.Set;

/**
 * 「影子主机」：一个不落在世界里的 F9 合成主机，只用来让 ECO 的
 * {@code CraftingCapabilitySnapshot#isVirtualTopologyEligible()} 认满 8 台满长高能主机。
 *
 * <p>它把等级、建造长度、网络交换模块固定成满配值，其余全部沿用 ECO 原逻辑。</p>
 *
 * <p>紧凑 F9 是「八联合成」的创造级单方块：FT 并行不受单核心容量限制，
 * 因此这里把 ECO 会对外查询、并用于线程数汇总的容量方法全部抬到 64 位/32 位上限。
 * 执行路径仍走 ECO 原生的 worker/parallel core，只是每台 worker 的线程上限不再构成瓶颈。</p>
 */
public class ShadowCraftingHostBlockEntity extends ECOCraftingSystemBlockEntity {

    public ShadowCraftingHostBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState, ECOTier.L9);
        getMainNode().setFlags();
        getMainNode().setInWorldNode(false);
    }

    @Override
    public appeng.api.networking.ticking.TickRateModulation tickingRequest(
            appeng.api.networking.IGridNode node, int ticksSinceLastCall) {
        return node == null ? super.tickingRequest(null, ticksSinceLastCall)
                : appeng.api.networking.ticking.TickRateModulation.IDLE;
    }

    @Override
    public int getMaxBuildLength() {
        // 与 8 联满配 ECO 完全一致：扩张上限 = craftingSystemMaxLength - 4。
        int configured = cn.dancingsnow.neoecoae.config.NEConfig.craftingSystemMaxLength - 4;
        return configured > 0 ? configured : Math.max(1, super.getMaxBuildLength());
    }

    @Override
    public int getSelectedBuildLength() {
        // 紧凑主机永远是「满长」建造：虚拟合成模式判定要求实际 FX 通道数 == 满长值。
        return getMaxBuildLength();
    }

    @Override
    public void setSelectedBuildLength(int length) {
    }

    @Override
    public void updateState(boolean updateExposed) {
    }

    @Override
    public void markForUpdate() {
    }

    @Override
    public void saveChanges() {
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.noneOf(Direction.class);
    }

    @Override
    public boolean hasHighEnergyNetworkSwitch() {
        return true;
    }

    @Override
    public boolean hasNetworkSwitch() {
        return true;
    }

    // ===== 无限 FT 并行 / FX 通道 =====

    @Override
    public int getThreadObjectCapacityForWorker(ECOCraftingWorkerBlockEntity worker) {
        // ECO 的 updateCount() 会对每台 worker 调用这个方法并累加线程数；
        // 返回 int 上限后，8 台主机的 exactThreadCount 至少是 8 * Integer.MAX_VALUE。
        return Integer.MAX_VALUE;
    }

    @Override
    public int getDispatchThreadCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getTotalWorkerThreadCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getPooledParallelism() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getLocalFtParallelCapacity() {
        return Long.MAX_VALUE;
    }


}
