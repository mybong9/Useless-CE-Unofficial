package com.sorrowmist.useless.compat.neoecoae.compact.entity;

import cn.dancingsnow.neoecoae.util.LongFluidTank;
import com.sorrowmist.uselesscore.api.logistics.LongFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 紧凑 F9 对外的流体接口：填充进入冷却液输入仓，抽取来自冷却副产物输出仓。
 *
 * <p>库存直接使用 NeoECOAE 自己的 {@link LongFluidTank}，这样 20.5.7 的冷却控制器、
 * 流体仓以及 {@code getStoredFluidAmount()} 的 long 转换都按原版类型正常工作；
 * F9 只是把 8 台主机的输入/输出池合并成同一个实例。</p>
 *
 * <p>同时实现 Forge 的 {@link IFluidHandler}（单次 int 上限）与 Core 的
 * {@link LongFluidHandler}（long 数量/容量），AE 直连可以按 64 位数量注入/抽取。</p>
 */
public final class CompactF9FluidHandler implements IFluidHandler, LongFluidHandler {

    /**
     * 冷却液池的逻辑容量。取 long 空间的四分之一，远高于 int 上限，又给汇总与序列化
     * 留出余量；NeoECOAE 的 LongFluidTank 内部库存本身就是 long。
     */
    static final long FLEET_CAPACITY = Long.MAX_VALUE / 4L;

    static LongFluidTank createFleetTank(int hosts, Runnable contentsChanged) {
        return new LongFluidTank() {
            @Override
            public int getCapacity() {
                return Integer.MAX_VALUE;
            }

            @Override
            public int getTankCapacity(int index) {
                return Integer.MAX_VALUE;
            }

            @Override
            protected void onContentsChanged() {
                contentsChanged.run();
            }
        };
    }

    private final LongFluidTank input;
    private final LongFluidTank output;

    public CompactF9FluidHandler(LongFluidTank input, LongFluidTank output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return switch (tank) {
            case 0 -> input.getFluid().copy();
            case 1 -> output.getFluid().copy();
            default -> FluidStack.EMPTY;
        };
    }

    @Override
    public int getTankCapacity(int tank) {
        return switch (tank) {
            case 0, 1 -> Integer.MAX_VALUE;
            default -> 0;
        };
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return switch (tank) {
            case 0 -> input.isFluidValid(stack);
            case 1 -> output.isFluidValid(stack);
            default -> false;
        };
    }

    @Override
    public int fill(@NotNull FluidStack resource, @NotNull FluidAction action) {
        return input.fill(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, @NotNull FluidAction action) {
        return output.drain(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action) {
        return output.drain(maxDrain, action);
    }

    // ===== LongFluidHandler：64 位容量与数量 =====

    @Override
    public long amountIn(int tank) {
        return switch (tank) {
            case 0 -> input.getStoredAmount();
            case 1 -> output.getStoredAmount();
            default -> 0L;
        };
    }

    @Override
    public long capacityOf(int tank) {
        return switch (tank) {
            case 0, 1 -> FLEET_CAPACITY;
            default -> 0L;
        };
    }

    @Override
    public long drain(int tank, long amount, boolean simulate) {
        if (tank != 1 || amount <= 0L) {
            return 0L;
        }
        long drained = Math.min(amount, output.getStoredAmount());
        if (!simulate && drained > 0L) {
            output.remove(drained);
        }
        return drained;
    }

    @Override
    public long fill(FluidStack type, long amount, boolean simulate) {
        if (type == null || type.isEmpty() || amount <= 0L) {
            return 0L;
        }
        if (input.getStoredAmount() > 0L && !input.getFluid().isFluidEqual(type)) {
            return 0L;
        }
        long accepted = Math.min(amount, FLEET_CAPACITY - input.getStoredAmount());
        if (accepted <= 0L) {
            return 0L;
        }
        if (!simulate) {
            input.add(type, accepted);
        }
        return accepted;
    }

    /** Player-only recovery view for coolant accidentally left in the input pool. */
    public IFluidHandler inputRecoveryHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() { return 1; }

            @Override
            public @NotNull FluidStack getFluidInTank(int tank) {
                return tank == 0 ? input.getFluid().copy() : FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int tank) {
                return tank == 0 ? Integer.MAX_VALUE : 0;
            }

            @Override
            public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return false; }

            @Override
            public int fill(@NotNull FluidStack resource, @NotNull FluidAction action) { return 0; }

            @Override
            public @NotNull FluidStack drain(@NotNull FluidStack resource, @NotNull FluidAction action) {
                return input.drain(resource, action);
            }

            @Override
            public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action) {
                return input.drain(maxDrain, action);
            }
        };
    }
}
