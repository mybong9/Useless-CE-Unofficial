package com.sorrowmist.useless.mixin;

import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.AEItemFilters;
import appeng.util.inv.filter.IAEItemFilter;
import com.glodblock.github.extendedae.common.tileentities.matrix.TileAssemblerMatrixPattern;
import com.sorrowmist.useless.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileAssemblerMatrixPattern.class)
public class TileAssemblerMatrixPatternMixin {

    private AppEngInternalInventory modifiedPatternInventory;

    /**
     * 在构造函数完成后替换patternInventory
     */
    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void onConstructed(BlockPos pos, BlockState blockState, CallbackInfo ci) {
        TileAssemblerMatrixPattern self = (TileAssemblerMatrixPattern) (Object) this;

        // 创建新的库存实例
        int newSize = 36 * ConfigManager.getMatrixPatternCount();
        this.modifiedPatternInventory = new AppEngInternalInventory(self, newSize, 1);

        // 使用反射替换原字段
        try {
            java.lang.reflect.Field field = TileAssemblerMatrixPattern.class.getDeclaredField("patternInventory");
            field.setAccessible(true);
            field.set(self, this.modifiedPatternInventory);

            // 也需要重新设置过滤器
            java.lang.reflect.Method setFilterMethod = AppEngInternalInventory.class.getDeclaredMethod("setFilter", IAEItemFilter.class);
            setFilterMethod.setAccessible(true);

            // 重新创建过滤器
            java.lang.reflect.Field filterField = TileAssemblerMatrixPattern.class.getDeclaredField("exposedInventory");
            filterField.setAccessible(true);

            // 重新创建exposedInventory
            FilteredInternalInventory newExposedInventory = new FilteredInternalInventory(
                    this.modifiedPatternInventory,
                    AEItemFilters.INSERT_ONLY
            );
            filterField.set(self, newExposedInventory);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
