package com.sorrowmist.useless.compat.enderio;

import com.enderio.base.common.handler.TravelHandler;
import com.enderio.api.integration.Integration;
import com.enderio.api.integration.IntegrationManager;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ender IO travel integration. This class is only called after the mod presence check.
 *
 * Registers the native Ender IO 6 integration hook for anchor travel.
 */
public final class EnderIOTravelCompat {
    public static final String MOD_ID = "enderio";
    private static final String ENABLED = "UselessEnderIOTravelEnabled";
    private static boolean registered;

    private EnderIOTravelCompat() {
    }

    public static Item.Properties markAsTravelItem(Item.Properties properties) {
        registerIntegration();
        return properties;
    }

    public static void setTravelItemEnabled(ItemStack stack, boolean enabled) {
        registerIntegration();
        stack.getOrCreateTag().putBoolean(ENABLED, enabled);
    }

    private static synchronized void registerIntegration() {
        if (registered) return;
        IntegrationManager.addIntegration(new Integration() {
            @Override public boolean canBlockTeleport(Player player) {
                return enabled(player.getMainHandItem()) || enabled(player.getOffhandItem());
            }
        });
        registered = true;
    }

    private static boolean enabled(ItemStack stack) {
        return stack.getItem() instanceof com.sorrowmist.useless.content.items.EndlessBeafItem
                && stack.hasTag() && stack.getTag().getBoolean(ENABLED);
    }

    public static InteractionResult tryAnchorTeleport(Level level, Player player) {
        registerIntegration();
        boolean success = TravelHandler.blockTeleport(level, player);
        return success ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
