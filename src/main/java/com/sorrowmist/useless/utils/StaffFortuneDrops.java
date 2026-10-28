package com.sorrowmist.useless.utils;

import com.sorrowmist.useless.content.items.EndlessBeafItem;
import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.uselesscore.api.component.CoreItemStackData;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import com.sorrowmist.uselesscore.api.enums.tool.EnchantMode;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Quantity multiplier shared by both staff variants, applied once at the loot source. */
public final class StaffFortuneDrops {
    public static final int MULTIPLIER = 256;
    private StaffFortuneDrops() { }

    public static boolean enabled(ItemStack tool) {
        return tool.getItem() instanceof EndlessBeafItem
                && CoreItemComponents.getOrDefault(tool, UselessComponents.EnchantModeComponent.get(),
                        EnchantMode.FORTUNE) == EnchantMode.FORTUNE;
    }

    private static final class LootJson {
        private static final com.google.gson.Gson INSTANCE = net.minecraft.world.level.storage.loot.Deserializers
                .createLootTableSerializer().create();
    }
    private static final java.util.Map<net.minecraft.world.level.storage.loot.LootTable, java.util.Set<String>> BONUS_ITEMS
            = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** Restrict the extra quantity to entries whose loot function actually uses Fortune. */
    public static boolean affectsBlockDrop(com.sorrowmist.uselesscore.api.event.CoreBlockDropsEvent event, ItemStack drop) {
        var table = event.getLevel().getServer().getLootData().getLootTable(event.getState().getBlock().getLootTable());
        java.util.Set<String> items = BONUS_ITEMS.get(table);
        if (items == null) {
            java.util.Set<String> collected = new java.util.HashSet<>();
            try {
                collectBonusItems(LootJson.INSTANCE.toJsonTree(table), false, collected);
            } catch (RuntimeException ignored) {
                // Unknown modded loot functions keep their native result, never multiply everything.
            }
            items = java.util.Set.copyOf(collected);
            BONUS_ITEMS.put(table, items);
        }
        return items.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(drop.getItem()).toString());
    }

    static void collectBonusItems(com.google.gson.JsonElement json, boolean inherited, java.util.Set<String> items) {
        if (json.isJsonArray()) {
            for (var child : json.getAsJsonArray()) collectBonusItems(child, inherited, items);
        } else if (json.isJsonObject()) {
            var node = json.getAsJsonObject();
            boolean affected = inherited || hasFortuneBonus(node.get("functions"));
            if (affected && node.has("type") && "minecraft:item".equals(node.get("type").getAsString())
                    && node.has("name")) items.add(node.get("name").getAsString());
            for (String key : java.util.List.of("pools", "entries", "children"))
                if (node.has(key)) collectBonusItems(node.get(key), affected, items);
        }
    }

    private static boolean hasFortuneBonus(com.google.gson.JsonElement json) {
        if (json == null) return false;
        if (json.isJsonArray()) {
            for (var child : json.getAsJsonArray()) if (hasFortuneBonus(child)) return true;
        } else if (json.isJsonObject()) {
            var node = json.getAsJsonObject();
            if (node.has("function") && node.has("enchantment")
                    && "minecraft:apply_bonus".equals(node.get("function").getAsString())
                    && "minecraft:fortune".equals(node.get("enchantment").getAsString())) return true;
            if (node.has("functions") && hasFortuneBonus(node.get("functions"))) return true;
        }
        return false;
    }

    public static List<ItemStack> amplify(ItemStack drop) {
        if (drop.isEmpty()) return List.of();
        if (drop.getMaxStackSize() <= 1 || CoreItemStackData.hasContainerItems(drop)
                || CoreItemStackData.hasBundleItems(drop)) return List.of(drop.copy());
        long remaining = (long) drop.getCount() * MULTIPLIER;
        List<ItemStack> result = new ArrayList<>();
        while (remaining > 0) {
            int count = (int) Math.min(remaining, drop.getMaxStackSize());
            ItemStack copy = drop.copy();
            copy.setCount(count);
            result.add(copy);
            remaining -= count;
        }
        return result;
    }
}
