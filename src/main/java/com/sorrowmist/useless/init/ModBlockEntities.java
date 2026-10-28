package com.sorrowmist.useless.init;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.blockentities.AdvancedAlloyFurnaceBlockEntity;
import com.sorrowmist.useless.content.blockentities.OreGeneratorBlockEntity;
import com.sorrowmist.useless.content.blockentities.ThermostatBlockEntity;
import com.sorrowmist.useless.content.blockentities.multiblock.MePatternAssemblyBlockEntity;
import com.sorrowmist.useless.content.blockentities.multiblock.MultiblockAlloyFurnaceCoreBlockEntity;
import com.sorrowmist.useless.content.blockentities.multiblock.OmniversalMoldHubBlockEntity;
import com.sorrowmist.useless.content.blockentities.multiblock.PassiveCraftingHatchBlockEntity;
import com.sorrowmist.useless.compat.draconicevolution.DraconicOpStorageCompat;
import com.sorrowmist.useless.compat.fluxnetworks.FluxNetworksEnergyCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import com.sorrowmist.uselesscore.api.capability.CoreCapabilities;
import com.sorrowmist.uselesscore.api.capability.CoreCapabilityRegistration;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.ModList;


public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, UselessMod.MODID);

    private ModBlockEntities() {}

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
            event.enqueueWork(() -> registerCapabilities(new CoreCapabilityRegistration())));
    }

    private static void registerCapabilities(CoreCapabilityRegistration event) {
        // Forge 1.20.1 exposes block capabilities from each block entity's getCapability override.
        CoreCapabilities.registerBlockProvider(AdvancedAlloyFurnaceBlockEntity.class,
                ForgeCapabilities.ITEM_HANDLER, (blockEntity, side) -> blockEntity.getItemHandler());
        CoreCapabilities.registerBlockProvider(AdvancedAlloyFurnaceBlockEntity.class,
                ForgeCapabilities.ENERGY, (blockEntity, side) -> blockEntity.getEnergyStorage());
        CoreCapabilities.registerBlockProvider(AdvancedAlloyFurnaceBlockEntity.class,
                ForgeCapabilities.FLUID_HANDLER, AdvancedAlloyFurnaceBlockEntity::getCombinedFluidHandler);
        CoreCapabilities.registerBlockProvider(OreGeneratorBlockEntity.class,
                ForgeCapabilities.ITEM_HANDLER, (blockEntity, side) -> blockEntity.getSamples());
        CoreCapabilities.registerBlockProvider(MultiblockAlloyFurnaceCoreBlockEntity.class,
                ForgeCapabilities.ENERGY, (blockEntity, side) -> blockEntity.getEnergyStorage());
        CoreCapabilities.registerBlockProvider(MePatternAssemblyBlockEntity.class,
                ForgeCapabilities.ITEM_HANDLER, (blockEntity, side) -> blockEntity.getPatterns());
        CoreCapabilities.registerBlockProvider(OmniversalMoldHubBlockEntity.class,
                ForgeCapabilities.ITEM_HANDLER, (blockEntity, side) -> blockEntity.getMolds());
        CoreCapabilities.registerBlockProvider(PassiveCraftingHatchBlockEntity.class,
                ForgeCapabilities.ITEM_HANDLER, (blockEntity, side) -> blockEntity.getPatterns());

        if (ModList.get().isLoaded("draconicevolution")) {
            DraconicOpStorageCompat.registerCapabilities(event);
        }
        if (ModList.get().isLoaded("mekanism")) {
            invokeOptionalCapabilityLoader(
                    "com.sorrowmist.useless.compat.mekanism.MekanismCompatLoader", event);
        }
        if (ModList.get().isLoaded("pneumaticcraft")) {
            invokeOptionalCapabilityLoader(
                    "com.sorrowmist.useless.compat.pneumaticcraft.ThermostatHeatCompat", event);
        }
        if (ModList.get().isLoaded(FluxNetworksEnergyCompat.MOD_ID)) {
            FluxNetworksEnergyCompat.registerCapabilities(event);
        }
    }

    /**
     * Keep optional Mekanism classes out of this always-loaded registry class.  In particular, the
     * JVM must not resolve AppMek's capability types on a server that does not install AppMek.
     */
    private static void invokeOptionalCapabilityLoader(String className, CoreCapabilityRegistration event) {
        try {
            Class<?> loader = Class.forName(className, true, ModBlockEntities.class.getClassLoader());
            loader.getMethod("registerCapabilities", CoreCapabilityRegistration.class).invoke(null, event);
        } catch (ReflectiveOperationException | LinkageError exception) {
            UselessMod.LOGGER.error("Failed to register optional capabilities from {}", className, exception);
        }
    }

    public static final RegistryObject<BlockEntityType<OreGeneratorBlockEntity>> ORE_GENERATOR =
            BLOCK_ENTITY_TYPES.register("ore_generator",
                                        () -> BlockEntityType.Builder.of(OreGeneratorBlockEntity::new,
                                                                         ModBlocks.ORE_GENERATOR_BLOCK.get()
                                        ).build(null)
            );


    public static final RegistryObject<BlockEntityType<AdvancedAlloyFurnaceBlockEntity>> ADVANCED_ALLOY_FURNACE =
            BLOCK_ENTITY_TYPES.register("advanced_alloy_furnace",
                                        () -> BlockEntityType.Builder.of(AdvancedAlloyFurnaceBlockEntity::new,
                                                                         ModBlocks.ADVANCED_ALLOY_FURNACE_BLOCK.get()
                                        ).build(null)
            );

    public static final RegistryObject<BlockEntityType<MultiblockAlloyFurnaceCoreBlockEntity>> MULTIBLOCK_ALLOY_FURNACE_CORE =
            BLOCK_ENTITY_TYPES.register("multiblock_alloy_furnace_core",
                    () -> BlockEntityType.Builder.of(MultiblockAlloyFurnaceCoreBlockEntity::new,
                            ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get()).build(null));
    public static final RegistryObject<BlockEntityType<MePatternAssemblyBlockEntity>> ME_PATTERN_ASSEMBLY =
            BLOCK_ENTITY_TYPES.register("me_pattern_assembly",
                    () -> BlockEntityType.Builder.of(MePatternAssemblyBlockEntity::new,
                            ModBlocks.ME_PATTERN_ASSEMBLY.get()).build(null));
    public static final RegistryObject<BlockEntityType<OmniversalMoldHubBlockEntity>> OMNIVERSAL_MOLD_HUB =
            BLOCK_ENTITY_TYPES.register("omniversal_mold_hub",
                    () -> BlockEntityType.Builder.of(OmniversalMoldHubBlockEntity::new,
                            ModBlocks.OMNIVERSAL_MOLD_HUB.get()).build(null));
    public static final RegistryObject<BlockEntityType<PassiveCraftingHatchBlockEntity>> PASSIVE_CRAFTING_HATCH =
            BLOCK_ENTITY_TYPES.register("passive_crafting_hatch",
                    () -> BlockEntityType.Builder.of(PassiveCraftingHatchBlockEntity::new,
                            ModBlocks.PASSIVE_CRAFTING_HATCH.get()).build(null));

    /**
     * 「温度调节器」方块的恒温源状态。
     */
    public static final RegistryObject<BlockEntityType<ThermostatBlockEntity>> TEMPERATURE_REGULATOR =
            BLOCK_ENTITY_TYPES.register("temperature_regulator",
                    () -> BlockEntityType.Builder.of(ThermostatBlockEntity::new,
                            ModBlocks.TEMPERATURE_REGULATOR.get()).build(null));


}
