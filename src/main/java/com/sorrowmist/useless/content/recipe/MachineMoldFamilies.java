package com.sorrowmist.useless.content.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class MachineMoldFamilies {
    private static final List<String> TABLES = List.of("basic", "advanced", "elite", "ultimate", "epic", "legendary");
    private static final List<String> AVARITIA_TABLES = List.of("sculk", "nether", "end", "extreme");
    private static final List<String> FACTORIES = List.of("basic", "advanced", "elite", "ultimate", "absolute", "supreme", "cosmic", "infinite");
    private static final Map<String, String> MEK_MACHINES = Map.of(
            "enrichment_chamber", "enriching", "crusher", "crushing", "combiner", "combining",
            "osmium_compressor", "compressing", "metallurgic_infuser", "infusing",
            "purification_chamber", "purifying", "chemical_injection_chamber", "injecting",
            "precision_sawmill", "sawing", "energized_smelter", "smelting");

    private MachineMoldFamilies() {}

    static List<ItemStack> alternatives(ItemStack mold) {
        var id = BuiltInRegistries.ITEM.getKey(mold.getItem());
        List<ItemStack> result = new ArrayList<>();
        result.add(mold);
        if (id.getNamespace().equals("extendedcrafting")) {
            int minimum = TABLES.indexOf(id.getPath().replaceFirst("_(?:auto_)?table$", ""));
            if (minimum >= 0) for (String tier : TABLES.subList(minimum, TABLES.size())) {
                add(result, "extendedcrafting", tier + "_table");
                add(result, "extendedcrafting", tier + "_auto_table");
            }
        }
        if (id.getNamespace().equals("avaritia")) {
            int minimum = AVARITIA_TABLES.indexOf(id.getPath().replaceFirst("_crafting_table$", ""));
            if (minimum >= 0) for (String tier : AVARITIA_TABLES.subList(minimum, AVARITIA_TABLES.size())) {
                add(result, "avaritia", tier + "_crafting_table");
            }
        }
        String factory = id.getNamespace().equals("mekanism") ? MEK_MACHINES.get(id.getPath())
                : id.equals(new ResourceLocation("evolvedmekanism", "alloyer")) ? "alloying" : null;
        int minimumFactory = 0;
        if (factory == null && (id.getNamespace().equals("mekanism") || id.getNamespace().equals("mekanism_extras"))) {
            String path = id.getPath();
            int separator = path.indexOf('_');
            if (separator > 0 && path.endsWith("_factory")) {
                int rank = FACTORIES.indexOf(path.substring(0, separator));
                if (rank >= 0) {
                    minimumFactory = rank;
                    factory = path.substring(separator + 1, path.length() - "_factory".length());
                }
            }
        }
        if (factory != null) for (int i = minimumFactory; i < FACTORIES.size(); i++) {
            add(result, i < 4 ? "mekanism" : "mekanism_extras", FACTORIES.get(i) + "_" + factory + "_factory");
        }
        return result;
    }

    private static void add(List<ItemStack> result, String namespace, String path) {
        var id = new ResourceLocation(namespace, path);
        BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> {
            if (result.stream().noneMatch(stack -> stack.is(item))) result.add(new ItemStack(item));
        });
    }
}
