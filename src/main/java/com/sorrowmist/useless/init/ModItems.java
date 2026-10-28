package com.sorrowmist.useless.init;


import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.uselesscore.api.rarity.CoreRarities;
import com.sorrowmist.uselesscore.api.enums.tool.ToolTypeMode;
import com.sorrowmist.useless.content.blockentities.AdvancedAlloyFurnaceBlockEntity;
import com.sorrowmist.useless.content.blocks.multiblock.UselessCoilBlock;
import com.sorrowmist.useless.content.items.AE2GiftPackageItem;
import com.sorrowmist.useless.content.items.AdvancedAlloyFurnaceBlockItem;
import com.sorrowmist.useless.content.items.EndlessBeafItem;
import com.sorrowmist.useless.content.items.IngotItem;
import com.sorrowmist.useless.content.items.MultiblockAlloyFurnaceCoreBlockItem;
import com.sorrowmist.useless.content.items.OmniversalPatternConverterItem;
import com.sorrowmist.useless.content.items.RitualBlueprintItem;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.OmniversalPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.crafting.pattern.AEProcessingPattern;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS = com.sorrowmist.uselesscore.api.registry.CoreRegistries.items(UselessMod.MODID);
    // 扳手子类物品注册
    public static final RegistryObject<EndlessBeafItem> ENDLESS_BEAF_WRENCH = ITEMS.register(
            "endless_beaf_wrench",
            () -> new EndlessBeafItem(ToolTypeMode.WRENCH_MODE)
    );
    // 螺丝刀子类物品注册
    public static final RegistryObject<EndlessBeafItem> ENDLESS_BEAF_SCREWDRIVER = ITEMS.register(
            "endless_beaf_screwdriver",
            () -> new EndlessBeafItem(ToolTypeMode.SCREWDRIVER_MODE)
    );
    // 软锤子类物品注册
    public static final RegistryObject<EndlessBeafItem> ENDLESS_BEAF_MALLET = ITEMS.register(
            "endless_beaf_mallet",
            () -> new EndlessBeafItem(ToolTypeMode.MALLET_MODE)
    );
    // 撬棍子类物品注册
    public static final RegistryObject<EndlessBeafItem> ENDLESS_BEAF_CROWBAR = ITEMS.register(
            "endless_beaf_crowbar",
            () -> new EndlessBeafItem(ToolTypeMode.CROWBAR_MODE)
    );
    // 硬锤子类物品注册
    public static final RegistryObject<EndlessBeafItem> ENDLESS_BEAF_HAMMER = ITEMS.register(
            "endless_beaf_hammer",
            () -> new EndlessBeafItem(ToolTypeMode.HAMMER_MODE)
    );
    public static final RegistryObject<EndlessBeafItem> ENDLESS_BEAF_ITEM = ITEMS.register(
            "endless_beaf_item",
            () -> new EndlessBeafItem()
    );
    public static final RegistryObject<EndlessBeafItem> ENDLESS_BEAF_ITEM_NO_WRENCH = ITEMS.register(
            "endless_beaf_item_no_wrench",
            () -> new EndlessBeafItem(ToolTypeMode.NONE_MODE, false)
    );

    static final List<RegistryObject<? extends Item>> CREATIVE_MAIN_TAB_ITEMS = new ArrayList<>();
    public static final RegistryObject<Item> USELESS_INGOT_TIER_1 = registerAndAdd(
            "useless_ingot_tier_1",
            () -> new IngotItem(new Item.Properties().rarity(Rarity.COMMON), 1)
    );
    public static final RegistryObject<Item> USELESS_INGOT_TIER_2 = registerAndAdd(
            "useless_ingot_tier_2",
            () -> new IngotItem(new Item.Properties().rarity(Rarity.COMMON), 2)
    );
    public static final RegistryObject<Item> USELESS_INGOT_TIER_3 = registerAndAdd(
            "useless_ingot_tier_3",
            () -> new IngotItem(new Item.Properties().rarity(Rarity.UNCOMMON), 3)
    );
    public static final RegistryObject<Item> USELESS_INGOT_TIER_4 = registerAndAdd(
            "useless_ingot_tier_4",
            () -> new IngotItem(new Item.Properties().rarity(Rarity.UNCOMMON), 4)
    );
    public static final RegistryObject<Item> USELESS_INGOT_TIER_5 = registerAndAdd(
            "useless_ingot_tier_5",
            () -> new IngotItem(new Item.Properties().rarity(Rarity.RARE), 5)
    );
    public static final RegistryObject<Item> USELESS_INGOT_TIER_6 = registerAndAdd(
            "useless_ingot_tier_6",
            () -> new IngotItem(new Item.Properties().rarity(Rarity.RARE), 6)
    );
    public static final RegistryObject<Item> USELESS_INGOT_TIER_7 = registerAndAdd(
            "useless_ingot_tier_7",
            () -> new IngotItem(new Item.Properties().rarity(Rarity.EPIC), 7)
    );
    public static final RegistryObject<Item> USELESS_INGOT_TIER_8 = registerAndAdd(
            "useless_ingot_tier_8",
            () -> new IngotItem(new Item.Properties().rarity(CoreRarities.MYTHIC), 8)
    );
    public static final RegistryObject<Item> USELESS_INGOT_TIER_9 = registerAndAdd(
            "useless_ingot_tier_9",
            () -> new IngotItem(new Item.Properties().rarity(CoreRarities.LEGENDARY), 9)
    );
    public static final RegistryObject<Item> POSSIBLE_USEFUL_INGOT = registerAndAdd(
            "possible_useful_ingot",
            () -> new Item(new Item.Properties().rarity(CoreRarities.LEGENDARY))
    );
    public static final RegistryObject<Item> USEFUL_INGOT = registerAndAdd(
            "useful_ingot",
            () -> new IngotItem(
                    new Item.Properties().rarity(CoreRarities.LEGENDARY),
                    AdvancedAlloyFurnaceBlockEntity.USEFUL_INGOT_FURNACE_TIER)
    );
    // 金属模具
    public static final RegistryObject<Item> METAL_MOLD_PLATE = registerAndAdd(
            "metal_mold_plate",
            () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> METAL_MOLD_ROD = registerAndAdd(
            "metal_mold_rod",
            () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> METAL_MOLD_GEAR = registerAndAdd(
            "metal_mold_gear",
            () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> METAL_MOLD_WIRE = registerAndAdd(
            "metal_mold_wire",
            () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> METAL_MOLD_BLOCK = registerAndAdd(
            "metal_mold_block",
            () -> new Item(new Item.Properties())
    );
    // 无用锭对应的齿轮物品
    public static final RegistryObject<Item> USELESS_GEAR_TIER_1 = registerAndAdd(
            "useless_gear_tier_1",
            () -> new Item(new Item.Properties().rarity(Rarity.COMMON))
    );
    public static final RegistryObject<Item> USELESS_GEAR_TIER_2 = registerAndAdd(
            "useless_gear_tier_2",
            () -> new Item(new Item.Properties().rarity(Rarity.COMMON))
    );
    public static final RegistryObject<Item> USELESS_GEAR_TIER_3 = registerAndAdd(
            "useless_gear_tier_3",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON))
    );
    public static final RegistryObject<Item> USELESS_GEAR_TIER_4 = registerAndAdd(
            "useless_gear_tier_4",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON))
    );
    public static final RegistryObject<Item> USELESS_GEAR_TIER_5 = registerAndAdd(
            "useless_gear_tier_5",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE))
    );
    public static final RegistryObject<Item> USELESS_GEAR_TIER_6 = registerAndAdd(
            "useless_gear_tier_6",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE))
    );
    public static final RegistryObject<Item> USELESS_GEAR_TIER_7 = registerAndAdd(
            "useless_gear_tier_7",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC))
    );
    public static final RegistryObject<Item> USELESS_GEAR_TIER_8 = registerAndAdd(
            "useless_gear_tier_8",
            () -> new Item(new Item.Properties().rarity(CoreRarities.MYTHIC))
    );
    public static final RegistryObject<Item> USELESS_GEAR_TIER_9 = registerAndAdd(
            "useless_gear_tier_9",
            () -> new Item(new Item.Properties().rarity(CoreRarities.MYTHIC))
    );
    // 无用玻璃方块物品
    public static final RegistryObject<BlockItem> USELESS_GLASS_TIER_1 = registerAndAdd(
            "useless_glass_tier_1",
            () -> new BlockItem(ModBlocks.USELESS_GLASS_TIER_1.get(), new Item.Properties().rarity(Rarity.COMMON))
    );
    public static final RegistryObject<BlockItem> USELESS_GLASS_TIER_2 = registerAndAdd(
            "useless_glass_tier_2",
            () -> new BlockItem(ModBlocks.USELESS_GLASS_TIER_2.get(), new Item.Properties().rarity(Rarity.COMMON))
    );
    public static final RegistryObject<BlockItem> USELESS_GLASS_TIER_3 = registerAndAdd(
            "useless_glass_tier_3",
            () -> new BlockItem(ModBlocks.USELESS_GLASS_TIER_3.get(), new Item.Properties().rarity(Rarity.UNCOMMON))
    );
    public static final RegistryObject<BlockItem> USELESS_GLASS_TIER_4 = registerAndAdd(
            "useless_glass_tier_4",
            () -> new BlockItem(ModBlocks.USELESS_GLASS_TIER_4.get(), new Item.Properties().rarity(Rarity.UNCOMMON))
    );
    public static final RegistryObject<BlockItem> USELESS_GLASS_TIER_5 = registerAndAdd(
            "useless_glass_tier_5",
            () -> new BlockItem(ModBlocks.USELESS_GLASS_TIER_5.get(), new Item.Properties().rarity(Rarity.RARE))
    );
    public static final RegistryObject<BlockItem> USELESS_GLASS_TIER_6 = registerAndAdd(
            "useless_glass_tier_6",
            () -> new BlockItem(ModBlocks.USELESS_GLASS_TIER_6.get(), new Item.Properties().rarity(Rarity.RARE))
    );
    public static final RegistryObject<BlockItem> USELESS_GLASS_TIER_7 = registerAndAdd(
            "useless_glass_tier_7",
            () -> new BlockItem(ModBlocks.USELESS_GLASS_TIER_7.get(), new Item.Properties().rarity(Rarity.EPIC))
    );
    public static final RegistryObject<BlockItem> USELESS_GLASS_TIER_8 = registerAndAdd(
            "useless_glass_tier_8",
            () -> new BlockItem(ModBlocks.USELESS_GLASS_TIER_8.get(), new Item.Properties().rarity(CoreRarities.MYTHIC))
    );
    public static final RegistryObject<BlockItem> USELESS_GLASS_TIER_9 = registerAndAdd(
            "useless_glass_tier_9",
            () -> new BlockItem(ModBlocks.USELESS_GLASS_TIER_9.get(), new Item.Properties().rarity(CoreRarities.LEGENDARY))
    );
    static final RegistryObject<BlockItem> TELEPORT_BLOCK_ITEM = registerAndAdd(
            "teleport_block",
            () -> new BlockItem(ModBlocks.TELEPORT_BLOCK.get(), new Item.Properties())
    );
    static final RegistryObject<BlockItem> TELEPORT_BLOCK_ITEM_2 = registerAndAdd(
            "teleport_block_2",
            () -> new BlockItem(ModBlocks.TELEPORT_BLOCK_2.get(), new Item.Properties())
    );
    static final RegistryObject<BlockItem> TELEPORT_BLOCK_ITEM_3 = registerAndAdd(
            "teleport_block_3",
            () -> new BlockItem(ModBlocks.TELEPORT_BLOCK_3.get(), new Item.Properties())
    );
    // 温度调节器方块物品
    public static final RegistryObject<BlockItem> TEMPERATURE_REGULATOR = registerAndAdd(
            "temperature_regulator",
            () -> new BlockItem(ModBlocks.TEMPERATURE_REGULATOR.get(), new Item.Properties())
    );
    static final RegistryObject<BlockItem> ORE_GENERATOR_BLOCK = registerAndAdd(
            "ore_generator_block",
            () -> new BlockItem(ModBlocks.ORE_GENERATOR_BLOCK.get(), new Item.Properties())
    );
    static final RegistryObject<BlockItem> ADVANCED_ALLOY_FURNACE_BLOCK = registerAndAdd(
            "advanced_alloy_furnace_block",
            () -> new AdvancedAlloyFurnaceBlockItem(ModBlocks.ADVANCED_ALLOY_FURNACE_BLOCK.get(), new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<BlockItem> SUPERVISOR = registerAndAdd(
            "supervisor",
            () -> new BlockItem(ModBlocks.SUPERVISOR.get(), new Item.Properties())
    );
    public static final RegistryObject<BlockItem> MULTIBLOCK_ALLOY_FURNACE_CORE = registerAndAdd(
            "multiblock_alloy_furnace_core",
            () -> new MultiblockAlloyFurnaceCoreBlockItem(
                    ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> ME_PATTERN_ASSEMBLY = registerAndAdd(
            "me_pattern_assembly",
            () -> new BlockItem(ModBlocks.ME_PATTERN_ASSEMBLY.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockItem> OMNIVERSAL_MOLD_HUB = registerAndAdd(
            "omniversal_mold_hub",
            () -> new BlockItem(ModBlocks.OMNIVERSAL_MOLD_HUB.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockItem> PASSIVE_CRAFTING_HATCH = registerAndAdd(
            "passive_crafting_hatch",
            () -> new BlockItem(ModBlocks.PASSIVE_CRAFTING_HATCH.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockItem> OMNIVERSAL_FURNACE_CASING = registerAndAdd(
            "omniversal_furnace_casing",
            () -> new BlockItem(ModBlocks.OMNIVERSAL_FURNACE_CASING.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> OMNIVERSAL_FORGE_FRAME = registerAndAdd(
            "omniversal_forge_frame", () -> new BlockItem(ModBlocks.OMNIVERSAL_FORGE_FRAME.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> OMNIVERSAL_FORGE_TRIM = registerAndAdd(
            "omniversal_forge_trim", () -> new BlockItem(ModBlocks.OMNIVERSAL_FORGE_TRIM.get(),new Item.Properties()));
    public static final RegistryObject<BlockItem> OMNIVERSAL_FORGE_VENT = registerAndAdd(
            "omniversal_forge_vent", () -> new BlockItem(ModBlocks.OMNIVERSAL_FORGE_VENT.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> OMNIVERSAL_FORGE_RESONATOR = registerAndAdd(
            "omniversal_forge_resonator", () -> new BlockItem(ModBlocks.OMNIVERSAL_FORGE_RESONATOR.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> OMNIVERSAL_FORGE_ARMOR = registerAndAdd(
            "omniversal_forge_armor", () -> new BlockItem(ModBlocks.OMNIVERSAL_FORGE_ARMOR.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> OMNIVERSAL_FORGE_COPPER = registerAndAdd(
            "omniversal_forge_copper", () -> new BlockItem(ModBlocks.OMNIVERSAL_FORGE_COPPER.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> TRIGRAM_QIAN = registerAndAdd(
            "omniversal_trigram_qian", () -> new BlockItem(ModBlocks.TRIGRAM_QIAN.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> TRIGRAM_DUI = registerAndAdd(
            "omniversal_trigram_dui", () -> new BlockItem(ModBlocks.TRIGRAM_DUI.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> TRIGRAM_LI = registerAndAdd(
            "omniversal_trigram_li", () -> new BlockItem(ModBlocks.TRIGRAM_LI.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> TRIGRAM_ZHEN = registerAndAdd(
            "omniversal_trigram_zhen", () -> new BlockItem(ModBlocks.TRIGRAM_ZHEN.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> TRIGRAM_KUN = registerAndAdd(
            "omniversal_trigram_kun", () -> new BlockItem(ModBlocks.TRIGRAM_KUN.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> TRIGRAM_GEN = registerAndAdd(
            "omniversal_trigram_gen", () -> new BlockItem(ModBlocks.TRIGRAM_GEN.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> TRIGRAM_KAN = registerAndAdd(
            "omniversal_trigram_kan", () -> new BlockItem(ModBlocks.TRIGRAM_KAN.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> TRIGRAM_XUN = registerAndAdd(
            "omniversal_trigram_xun", () -> new BlockItem(ModBlocks.TRIGRAM_XUN.get(), new Item.Properties()));
    public static final List<RegistryObject<BlockItem>> USELESS_COILS = new ArrayList<>();
    public static final RegistryObject<Item> OMNIVERSAL_CONSTRUCTION_PATTERN = registerAndAdd(
            "omniversal_construction_pattern", com.sorrowmist.useless.content.items.OmniversalConstructionPatternItem::new);
    public static final RegistryObject<Item> OMNIVERSAL_CONSTRUCTION_RECEIPT = registerAndAdd(
            "omniversal_construction_receipt", com.sorrowmist.useless.content.items.OmniversalConstructionReceiptItem::new);
    public static final RegistryObject<Item> OMNIVERSAL_PATTERN = registerAndAdd(
            "omniversal_pattern",
            () -> new com.sorrowmist.uselesscore.api.ae.CoreEncodedPatternItem(
                    new Item.Properties().stacksTo(1), OmniversalPatternDetails::decode));
    public static final RegistryObject<OmniversalPatternConverterItem> OMNIVERSAL_PATTERN_CONVERTER = registerAndAdd(
            "omniversal_pattern_converter",
            () -> new OmniversalPatternConverterItem());
    static {
        for (int tier = UselessCoilBlock.MIN_TIER; tier <= UselessCoilBlock.MAX_TIER; tier++) {
            int capturedTier = tier;
            USELESS_COILS.add(registerAndAdd(UselessCoilBlock.registryName(tier),
                    () -> new BlockItem(ModBlocks.USELESS_COILS.get(capturedTier).get(), new Item.Properties())));
        }
    }

    public static final RegistryObject<AE2GiftPackageItem> AE2_GIFT_PACKAGE;
    public static final RegistryObject<Item> RITUAL_BLUEPRINT;

    static {
        if (ModList.get().isLoaded("ae2")) {
            AE2_GIFT_PACKAGE = ITEMS.register(
                    "ae2_gift_package",
                    () -> new AE2GiftPackageItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(64))
            );
            CREATIVE_MAIN_TAB_ITEMS.add(AE2_GIFT_PACKAGE);
        } else {
            AE2_GIFT_PACKAGE = null;
        }

        if (ModList.get().isLoaded("occultism")) {
            RITUAL_BLUEPRINT = ITEMS.register(
                    "ritual_blueprint",
                    OccultismItems::createRitualBlueprint
            );
            CREATIVE_MAIN_TAB_ITEMS.add(RITUAL_BLUEPRINT);
        } else {
            RITUAL_BLUEPRINT = null;
        }
    }

    private ModItems() {}

    /** Keeps the optional Occultism-backed item class out of ModItems' eager linkage. */
    private static final class OccultismItems {
        private static Item createRitualBlueprint() {
            return new RitualBlueprintItem(new Item.Properties().stacksTo(1));
        }
    }

    private static <T extends Item> RegistryObject<T> registerAndAdd(String name, Supplier<T> supplier) {
        RegistryObject<T> deferred = ITEMS.register(name, supplier);
        CREATIVE_MAIN_TAB_ITEMS.add(deferred);
        return deferred;
    }
}
