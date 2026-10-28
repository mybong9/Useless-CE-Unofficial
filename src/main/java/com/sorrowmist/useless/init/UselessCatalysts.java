package com.sorrowmist.useless.init;

import com.sorrowmist.useless.core.config.ConfigManager;
import com.sorrowmist.uselesscore.api.crafting.CoreCatalystRegistry;
import com.sorrowmist.uselesscore.api.crafting.CoreCatalystType;
import net.minecraft.resources.ResourceLocation;

/** Registers Useless's built-in catalysts with the shared Core API. */
public final class UselessCatalysts {
    public static final CoreCatalystType NONE = CoreCatalystRegistry.none();
    public static final CoreCatalystType USELESS_INGOT_TIER_1 = tier(1, ModItems.USELESS_INGOT_TIER_1, "一阶无用锭", 2);
    public static final CoreCatalystType USELESS_INGOT_TIER_2 = tier(2, ModItems.USELESS_INGOT_TIER_2, "二阶无用锭", 4);
    public static final CoreCatalystType USELESS_INGOT_TIER_3 = tier(3, ModItems.USELESS_INGOT_TIER_3, "三阶无用锭", 8);
    public static final CoreCatalystType USELESS_INGOT_TIER_4 = tier(4, ModItems.USELESS_INGOT_TIER_4, "四阶无用锭", 16);
    public static final CoreCatalystType USELESS_INGOT_TIER_5 = tier(5, ModItems.USELESS_INGOT_TIER_5, "五阶无用锭", 32);
    public static final CoreCatalystType USELESS_INGOT_TIER_6 = tier(6, ModItems.USELESS_INGOT_TIER_6, "六阶无用锭", 64);
    public static final CoreCatalystType USELESS_INGOT_TIER_7 = tier(7, ModItems.USELESS_INGOT_TIER_7, "七阶无用锭", 128);
    public static final CoreCatalystType USELESS_INGOT_TIER_8 = tier(8, ModItems.USELESS_INGOT_TIER_8, "八阶无用锭", 256);
    public static final CoreCatalystType USELESS_INGOT_TIER_9 = tier(9, ModItems.USELESS_INGOT_TIER_9, "九阶无用锭", 512);
    public static final CoreCatalystType USEFUL_INGOT = CoreCatalystRegistry.register(
            id("useful_ingot"), ModItems.USEFUL_INGOT, Integer.MAX_VALUE, "有用锭",
            () -> Integer.MAX_VALUE, ignored -> 1, true, false);

    private UselessCatalysts() {
    }

    public static void init() {
        // Trigger static registrations before gameplay code begins resolving catalysts.
    }

    public static CoreCatalystType uselessIngotTier(int tier) {
        CoreCatalystType type = CoreCatalystRegistry.byTier(tier);
        return type == null ? NONE : type;
    }

    private static CoreCatalystType tier(int tier, java.util.function.Supplier<? extends net.minecraft.world.item.Item> item,
                                         String displayName, int defaultParallel) {
        return CoreCatalystRegistry.register(id("useless_ingot_tier_" + tier), item, tier, displayName,
                () -> ConfigManager.getAdvancedAlloyFurnaceCatalystParallel(tier),
                baseTime -> Math.max(1, (int) Math.ceil(baseTime
                        * ConfigManager.getAdvancedAlloyFurnaceCatalystTimeMultiplier(tier))),
                false, true);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("useless_mod", path);
    }
}
