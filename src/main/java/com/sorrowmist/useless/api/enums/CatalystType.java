package com.sorrowmist.useless.api.enums;

import com.sorrowmist.useless.init.UselessCatalysts;
import com.sorrowmist.uselesscore.api.crafting.CoreCatalystRegistry;
import com.sorrowmist.uselesscore.api.crafting.CoreCatalystType;
import net.minecraft.world.item.ItemStack;

/**
 * @deprecated Use {@link CoreCatalystType} and {@link CoreCatalystRegistry}; retained as a source
 * compatibility facade for addons compiled against the older Useless API.
 */
@Deprecated
public enum CatalystType {
    NONE(UselessCatalysts.NONE),
    USELESS_INGOT_TIER_1(UselessCatalysts.USELESS_INGOT_TIER_1),
    USELESS_INGOT_TIER_2(UselessCatalysts.USELESS_INGOT_TIER_2),
    USELESS_INGOT_TIER_3(UselessCatalysts.USELESS_INGOT_TIER_3),
    USELESS_INGOT_TIER_4(UselessCatalysts.USELESS_INGOT_TIER_4),
    USELESS_INGOT_TIER_5(UselessCatalysts.USELESS_INGOT_TIER_5),
    USELESS_INGOT_TIER_6(UselessCatalysts.USELESS_INGOT_TIER_6),
    USELESS_INGOT_TIER_7(UselessCatalysts.USELESS_INGOT_TIER_7),
    USELESS_INGOT_TIER_8(UselessCatalysts.USELESS_INGOT_TIER_8),
    USELESS_INGOT_TIER_9(UselessCatalysts.USELESS_INGOT_TIER_9),
    USEFUL_INGOT(UselessCatalysts.USEFUL_INGOT);

    private final CoreCatalystType delegate;

    CatalystType(CoreCatalystType delegate) {
        this.delegate = delegate;
    }

    public static CatalystType fromStack(ItemStack stack) {
        CoreCatalystType type = CoreCatalystRegistry.fromStack(stack);
        for (CatalystType value : values()) if (value.delegate == type) return value;
        return NONE;
    }

    public static CatalystType uselessIngotTier(int tier) {
        for (CatalystType value : values()) {
            if (value.delegate.isUselessIngotTier() && value.delegate.getTier() == tier) return value;
        }
        return NONE;
    }

    public int getTier() { return delegate.getTier(); }
    public String getDisplayName() { return delegate.getDisplayName(); }
    public int getNormalRecipeParallel() { return delegate.getNormalRecipeParallel(); }
    public boolean isInfiniteParallel() { return delegate.isInfiniteParallel(); }
    public boolean isUsefulIngot() { return delegate.isUsefulIngot(); }
    public boolean isValidCatalyst() { return delegate.isValidCatalyst(); }
    public boolean isUselessIngotTier() { return delegate.isUselessIngotTier(); }
    public int calculateProcessTime(int baseTime) { return delegate.calculateProcessTime(baseTime); }

    public CoreCatalystType coreType() { return delegate; }
}
