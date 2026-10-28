package com.sorrowmist.useless.content.machines.advanced_alloy_furnace.catalyst;

import com.sorrowmist.uselesscore.api.crafting.CoreCatalystType;
import com.sorrowmist.uselesscore.api.crafting.CoreCatalystRegistry;
import com.sorrowmist.useless.init.UselessCatalysts;

/**
 * 催化剂在某个具体配方上下文中的运行结果。
 * 该对象用于把催化剂静态信息和配方目标无用锭等级整合成统一执行参数。
 */
public record ResolvedCatalystEffect(
        CoreCatalystType catalystType,
        long catalystParallel,
        long recipeParallel,
        int processTime,
        boolean energyMultipliesWithParallel,
        int energyDivisor,
        boolean uselessIngotRecipe,
        int targetUselessIngotTier
) {
}
