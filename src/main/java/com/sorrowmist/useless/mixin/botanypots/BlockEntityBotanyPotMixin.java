package com.sorrowmist.useless.mixin.botanypots;

import com.sorrowmist.useless.core.config.ConfigManager;
import com.sorrowmist.useless.mixin.accessor.BlockEntityBotanyPotAccessor;
import net.darkhax.botanypots.BotanyPotHelper;
import net.darkhax.botanypots.block.BlockEntityBotanyPot;
import net.darkhax.botanypots.block.inv.BotanyPotContainer;
import net.darkhax.bookshelf.api.Services;
import net.darkhax.bookshelf.api.inventory.IInventoryAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

@SuppressWarnings({"AddedMixinMembersNamePattern"})
@Mixin(BlockEntityBotanyPot.class)
public abstract class BlockEntityBotanyPotMixin {

    @Inject(
            method = "tickPot(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/darkhax/botanypots/block/BlockEntityBotanyPot;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void onTickPot(Level level, BlockPos pos, BlockState state, BlockEntityBotanyPot pot, CallbackInfo ci) {
        // 获取配置的生长倍率
        int multiplier = ConfigManager.getBotanyPotGrowthMultiplier();
        if (multiplier <= 1) return;

        // 检查生长条件
        if (pot.getCrop() == null || pot.getSoil() == null || !pot.areGrowthConditionsMet()) return;

        BotanyPotContainer inv = pot.getInventory();
        int requiredTime = inv.getRequiredGrowthTime();
        if (requiredTime <= 0) requiredTime = 1;

        int actualTime = requiredTime / multiplier;

        BlockEntityBotanyPotAccessor accessor = (BlockEntityBotanyPotAccessor) pot;
        Random rng = accessor.getRng();

        // 正常加速生长模式
        if (actualTime >= 1) {
            int growthThisTick = multiplier - 1;
            int currentGrowth = pot.getGrowthTime();
            int newGrowth = currentGrowth + growthThisTick;
            if (newGrowth >= requiredTime) {
                // 只增长到requiredTime，然后收获重置
                pot.addGrowth(requiredTime - currentGrowth);
                accessor.setDoneGrowing(true);
                if (hasOutputSpace(level, pos, pot)) {
                    performHarvestToContainers(level, pos, pot, rng);
                    pot.resetGrowth();
                }
            } else {
                // 直接增长growthThisTick
                pot.addGrowth(growthThisTick);
            }
            return;
        }

        // 极高速生长模式 - 一次性生成多倍掉落物到容器
        if (!level.isClientSide) {
            int dropsMultiplier = Math.max(1, (int) Math.ceil(multiplier / requiredTime));

            // 检查容器是否有足够空间
            if (hasOutputSpace(level, pos, pot)) {
                // 生成基础掉落物
                List<ItemStack> baseDrops = BotanyPotHelper.generateDrop(rng, level, pos, pot, pot.getCrop());
                if (!baseDrops.isEmpty()) {
                    // 创建多倍掉落物列表
                    performMultipliedHarvest(level, pos, pot, baseDrops, dropsMultiplier);
                }
                pot.resetGrowth();
            }
            // 如果容器没有空间，则停止生长
        }

        ci.cancel();
    }

    /**
     * 检查是否有输出空间（下方容器或自身槽位）
     */
    private static boolean hasOutputSpace(Level level, BlockPos pos, BlockEntityBotanyPot pot) {
        // 先检查自身槽位是否有空间
        if (hasSpaceInSelf(pot)) {
            return true;
        }

        // 如果自身槽位已满，检查下方容器是否有空间接收任意一种自身槽位中的物品
        IInventoryAccess exportTo = Services.INVENTORY_HELPER.getInventory(level, pos.below(), Direction.UP);
        if (exportTo != null) {
            BotanyPotContainer potInventory = (BotanyPotContainer) pot.getInventory();

            // 检查自身槽位中的每种物品是否能在下方容器中找到空间
            for (int potSlot : BotanyPotContainer.STORAGE_SLOT) {
                ItemStack stackInSlot = potInventory.getItem(potSlot);
                if (!stackInSlot.isEmpty()) {
                    // 检查这个物品是否能放入下方容器
                    if (canItemBeInserted(exportTo, stackInSlot)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检查物品是否能被插入到容器中
     */
    private static boolean canItemBeInserted(IInventoryAccess container, ItemStack stack) {
        for (int slot : container.getAvailableSlots()) {
            ItemStack testStack = stack.copy();
            testStack.setCount(1); // 只测试1个物品
            ItemStack remaining = container.insert(slot, testStack, Direction.UP, false);
            if (remaining.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查自身物品栏是否有空间
     */
    private static boolean hasSpaceInSelf(BlockEntityBotanyPot pot) {
        BotanyPotContainer potInventory = pot.getInventory();
        for (int slot : BotanyPotContainer.STORAGE_SLOT) {
            ItemStack stackInSlot = potInventory.getItem(slot);
            if (stackInSlot.isEmpty() || stackInSlot.getCount() < stackInSlot.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将自身槽位的物品输出到下方容器
     */
    private static void exportSelfToBelowContainer(Level level, BlockPos pos, BlockEntityBotanyPot pot) {
        IInventoryAccess exportTo = Services.INVENTORY_HELPER.getInventory(level, pos.below(), Direction.UP);
        if (exportTo == null) return;

        BotanyPotContainer potInventory = (BotanyPotContainer) pot.getInventory();
        boolean exportedAny = false;

        // 遍历自身所有存储槽位
        for (int potSlot : BotanyPotContainer.STORAGE_SLOT) {
            ItemStack stackInSlot = potInventory.getItem(potSlot);
            if (stackInSlot.isEmpty()) continue;

            ItemStack remainingStack = stackInSlot.copy();
            int originalCount = remainingStack.getCount();

            // 尝试输出到下方容器
            for (int exportSlot : exportTo.getAvailableSlots()) {
                if (remainingStack.isEmpty()) break;

                // 使用模拟插入检查是否可以插入
                ItemStack simulatedRemaining = exportTo.insert(exportSlot, remainingStack.copy(), Direction.UP, false);
                if (simulatedRemaining.getCount() < remainingStack.getCount()) {
                    // 可以插入，执行实际插入
                    ItemStack beforeInsert = remainingStack.copy();
                    remainingStack = exportTo.insert(exportSlot, remainingStack, Direction.UP, true);

                    // 检查是否成功插入
                    if (remainingStack.getCount() < beforeInsert.getCount()) {
                        exportedAny = true;
                    }

                    // 如果物品完全输出，跳出循环
                    if (remainingStack.isEmpty()) {
                        break;
                    }
                }
            }

            // 更新自身槽位的物品
            if (remainingStack.getCount() != originalCount) {
                if (remainingStack.isEmpty()) {
                    potInventory.setItem(potSlot, ItemStack.EMPTY);
                } else {
                    potInventory.setItem(potSlot, remainingStack);
                }
            }
        }

        // 如果有物品被输出，标记为脏以保存更改
        if (exportedAny) {
            pot.markDirty();
        }
    }

    /**
     * 执行多倍收获，一次性输出多倍物品
     */
    private static void performMultipliedHarvest(Level level, BlockPos pos, BlockEntityBotanyPot pot, List<ItemStack> baseDrops, int multiplier) {
        // 获取下方容器
        IInventoryAccess exportTo = Services.INVENTORY_HELPER.getInventory(level, pos.below(), Direction.UP);
        BotanyPotContainer potInventory = (BotanyPotContainer) pot.getInventory();

        // 对每个基础掉落物进行倍增
        for (ItemStack baseDrop : baseDrops) {
            if (baseDrop.isEmpty()) continue;

            // 创建多倍堆叠
            ItemStack multipliedDrop = baseDrop.copy();
            multipliedDrop.setCount(baseDrop.getCount() * multiplier);

            ItemStack remainingDrop = multipliedDrop.copy();

            // 优先尝试输出到下方容器
            if (exportTo != null) {
                for (int exportSlot : exportTo.getAvailableSlots()) {
                    if (remainingDrop.isEmpty()) break;

                    ItemStack beforeInsert = remainingDrop.copy();
                    remainingDrop = exportTo.insert(exportSlot, remainingDrop, Direction.UP, false);

                    // 如果成功插入了物品
                    if (remainingDrop.getCount() < beforeInsert.getCount()) {
                        // 实际执行插入操作
                        exportTo.insert(exportSlot, beforeInsert, Direction.UP, true);
                    }
                }
            }

            // 如果下方容器已满，尝试存入自身物品栏
            if (!remainingDrop.isEmpty()) {
                for (int potSlot : BotanyPotContainer.STORAGE_SLOT) {
                    if (remainingDrop.isEmpty()) break;

                    ItemStack stackInSlot = potInventory.getItem(potSlot);

                    // 如果槽位为空或可以合并
                    if (stackInSlot.isEmpty() ||
                            (ItemStack.isSameItemSameTags(stackInSlot, remainingDrop) &&
                                    stackInSlot.getCount() < stackInSlot.getMaxStackSize())) {

                        if (stackInSlot.isEmpty()) {
                            // 空槽位，直接设置物品（但不要超过最大堆叠）
                            int transferAmount = Math.min(remainingDrop.getCount(), remainingDrop.getMaxStackSize());
                            ItemStack toSet = remainingDrop.copy();
                            toSet.setCount(transferAmount);
                            potInventory.setItem(potSlot, toSet);
                            remainingDrop.shrink(transferAmount);
                        } else {
                            // 合并到现有堆栈
                            int spaceLeft = stackInSlot.getMaxStackSize() - stackInSlot.getCount();
                            int toAdd = Math.min(spaceLeft, remainingDrop.getCount());

                            if (toAdd > 0) {
                                stackInSlot.grow(toAdd);
                                potInventory.setItem(potSlot, stackInSlot);
                                remainingDrop.shrink(toAdd);
                            }
                        }
                    }
                }
            }

            // 如果自身物品栏也满了，销毁剩余物品
            if (!remainingDrop.isEmpty()) {
                // 销毁多余物品
            }
        }
    }

    /**
     * 执行单次收获（用于正常模式）
     */
    private static void performHarvestToContainers(Level level, BlockPos pos, BlockEntityBotanyPot pot, Random rng) {
        if (pot.getCrop() == null) return;

        List<ItemStack> drops = BotanyPotHelper.generateDrop(rng, level, pos, pot, pot.getCrop());
        if (drops.isEmpty()) return;

        // 获取下方容器
        IInventoryAccess exportTo = Services.INVENTORY_HELPER.getInventory(level, pos.below(), Direction.UP);
        BotanyPotContainer potInventory = (BotanyPotContainer) pot.getInventory();

        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;

            ItemStack remainingDrop = drop.copy();

            // 优先尝试输出到下方容器
            if (exportTo != null) {
                for (int exportSlot : exportTo.getAvailableSlots()) {
                    if (remainingDrop.isEmpty()) break;

                    ItemStack beforeInsert = remainingDrop.copy();
                    remainingDrop = exportTo.insert(exportSlot, remainingDrop, Direction.UP, false);

                    // 如果成功插入了物品
                    if (remainingDrop.getCount() < beforeInsert.getCount()) {
                        // 实际执行插入操作
                        exportTo.insert(exportSlot, beforeInsert, Direction.UP, true);
                    }
                }
            }

            // 如果下方容器已满，尝试存入自身物品栏
            if (!remainingDrop.isEmpty()) {
                for (int potSlot : BotanyPotContainer.STORAGE_SLOT) {
                    if (remainingDrop.isEmpty()) break;

                    ItemStack stackInSlot = potInventory.getItem(potSlot);

                    // 如果槽位为空或可以合并
                    if (stackInSlot.isEmpty() ||
                            (ItemStack.isSameItemSameTags(stackInSlot, remainingDrop) &&
                                    stackInSlot.getCount() < stackInSlot.getMaxStackSize())) {

                        if (stackInSlot.isEmpty()) {
                            // 空槽位，直接设置物品
                            int transferAmount = Math.min(remainingDrop.getCount(), remainingDrop.getMaxStackSize());
                            ItemStack toSet = remainingDrop.copy();
                            toSet.setCount(transferAmount);
                            potInventory.setItem(potSlot, toSet);
                            remainingDrop.shrink(transferAmount);
                        } else {
                            // 合并到现有堆栈
                            int spaceLeft = stackInSlot.getMaxStackSize() - stackInSlot.getCount();
                            int toAdd = Math.min(spaceLeft, remainingDrop.getCount());

                            if (toAdd > 0) {
                                stackInSlot.grow(toAdd);
                                potInventory.setItem(potSlot, stackInSlot);
                                remainingDrop.shrink(toAdd);
                            }
                        }
                    }
                }
            }

            // 如果自身物品栏也满了，销毁剩余物品
            if (!remainingDrop.isEmpty()) {
                // 销毁多余物品
                break;
            }
        }
    }

    /**
     * 在原有tick逻辑中注入容器检查，防止在容器满时继续生长
     */
    @Inject(
            method = "tickPot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/darkhax/botanypots/block/BlockEntityBotanyPot;areGrowthConditionsMet()Z",
                    shift = At.Shift.AFTER
            ),
            cancellable = true,
            remap = false
    )
    private static void onGrowthCheck(Level level, BlockPos pos, BlockState state, BlockEntityBotanyPot pot, CallbackInfo ci) {
        // 始终尝试将自身槽位的物品输出到下方容器（如果有的话）
        if (!level.isClientSide) {
            exportSelfToBelowContainer(level, pos, pot);
        }

        // 如果作物已经成熟但没有输出空间，停止生长
        if (pot.isCropHarvestable() && !hasOutputSpace(level, pos, pot)) {
            // 停止生长逻辑，防止继续生长和产生更多物品
            ci.cancel();
        }
    }


}
