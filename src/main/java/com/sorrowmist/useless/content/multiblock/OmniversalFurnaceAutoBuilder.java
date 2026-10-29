package com.sorrowmist.useless.content.multiblock;

import appeng.api.config.Actionable;
import com.sorrowmist.useless.compat.AE2Compat;
import com.sorrowmist.useless.content.blockentities.multiblock.MePatternAssemblyBlockEntity;
import com.sorrowmist.useless.content.blockentities.multiblock.MultiblockAlloyFurnaceCoreBlockEntity;
import com.sorrowmist.useless.content.blockentities.multiblock.PassiveCraftingHatchBlockEntity;
import com.sorrowmist.useless.content.blockentities.multiblock.OmniversalMoldHubBlockEntity;
import com.sorrowmist.useless.content.blocks.multiblock.MultiblockAlloyFurnaceCoreBlock;
import com.sorrowmist.useless.content.blocks.multiblock.DirectionalMultiblockPartBlock;
import com.sorrowmist.useless.content.blocks.multiblock.OmniversalAlloyFurnaceStructure;
import com.sorrowmist.useless.content.blocks.multiblock.UselessCoilBlock;
import com.sorrowmist.useless.init.ModBlocks;
import com.sorrowmist.useless.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OmniversalFurnaceAutoBuilder {
    private OmniversalFurnaceAutoBuilder() {
    }

    public record Result(boolean success, Component message) {
    }

    public static Result build(ServerPlayer player, ItemStack tool, BlockPos corePos) {
        ServerLevel level = player.serverLevel();
        BlockState coreState = level.getBlockState(corePos);
        if (!coreState.is(ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get())) {
            return failure("gui.useless_mod.multiblock_builder.invalid_core");
        }
        var access = materialAccess(player, tool, corePos);
        Direction facing = coreState.getValue(MultiblockAlloyFurnaceCoreBlock.FACING);

        for (var entry : OmniversalAlloyFurnaceStructure.entries()) {
            if (!level.isLoaded(entry.worldPos(corePos, facing))) {
                return new Result(false, Component.translatable("gui.useless_mod.multiblock_builder.unloaded"));
            }
        }
        int existingTier = 0;
        for (OmniversalAlloyFurnaceStructure.Entry entry : OmniversalAlloyFurnaceStructure.entries()) {
            if (entry.part() != OmniversalAlloyFurnaceStructure.Part.COIL) continue;
            Block block = level.getBlockState(entry.worldPos(corePos, facing)).getBlock();
            if (block instanceof UselessCoilBlock coil) {
                if (existingTier == 0) existingTier = coil.tier();
                else if (existingTier != coil.tier()) {
                    return failure("gui.useless_mod.multiblock_builder.mixed_coils");
                }
            }
        }

        int coilTier = existingTier;
        if (coilTier == 0) coilTier = coilTier(player.getOffhandItem());
        if (coilTier == 0 && player.getAbilities().instabuild) coilTier = UselessCoilBlock.MAX_TIER;
        if (coilTier == 0) {
            int fallbackTier = UselessCoilBlock.MIN_TIER;
            long bestAvailable = 0;
            for (int candidate = UselessCoilBlock.MAX_TIER;
                 candidate >= UselessCoilBlock.MIN_TIER; candidate--) {
                int missing = missingCoils(level, corePos, facing, candidate);
                ItemStack coil = new ItemStack(ModBlocks.USELESS_COILS.get(candidate).get(), missing);
                long available = extract(access, coil, Actionable.SIMULATE)
                        + countInventory(player, coil.getItem());
                if (available > bestAvailable) {
                    bestAvailable = available;
                    fallbackTier = candidate;
                }
                if (available >= missing) {
                    coilTier = candidate;
                    break;
                }
            }
            // Continue to the complete deficit report even when no coil tier has enough stock.
            if (coilTier == 0) coilTier = fallbackTier;
        }

        FunctionalLayout functionalLayout = resolveFunctionalLayout(level, corePos, facing);
        if (functionalLayout == null) {
            return failure("gui.useless_mod.multiblock_builder.invalid_functional_parts");
        }

        List<Placement> placements = new ArrayList<>();
        Map<Item, Integer> requirements = new LinkedHashMap<>();
        Map<Item, Long> availableMaterials = new LinkedHashMap<>();
        for (OmniversalAlloyFurnaceStructure.Entry entry : OmniversalAlloyFurnaceStructure.entries()) {
            if (entry.part() == OmniversalAlloyFurnaceStructure.Part.CORE) continue;
            BlockPos pos = entry.worldPos(corePos, facing);
            if (!level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)
                    || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, tool)) {
                return failure("gui.useless_mod.multiblock_builder.no_permission");
            }
            BlockState current = level.getBlockState(pos);
            if ((entry.part() == OmniversalAlloyFurnaceStructure.Part.CASING
                    || entry.part() == OmniversalAlloyFurnaceStructure.Part.INTERFACE)
                    && com.sorrowmist.useless.compat.gtceu.GregTechParts.accepts(level, pos)
                    && !com.sorrowmist.useless.compat.gtceu.GregTechParts.claimedByOther(level, pos, corePos)) continue;
            OmniversalAlloyFurnaceStructure.FunctionalPart functionalPart =
                    functionalLayout.parts().get(pos);
            BlockState expected = expectedState(entry.part(), coilTier, functionalPart, facing);
            if (matches(entry.part(), current, coilTier, functionalPart)) continue;
            if (level.getBlockEntity(pos) != null || !canReplaceForBuild(current, functionalPart)) {
                return failure("gui.useless_mod.multiblock_builder.blocked", pos.toShortString());
            }
            // The validator accepts tagged casing alternatives; construction must accept them too.
            if (functionalPart == null && (entry.part() == OmniversalAlloyFurnaceStructure.Part.CASING
                    || entry.part() == OmniversalAlloyFurnaceStructure.Part.INTERFACE)) {
                for (ItemStack candidate : OmniversalFurnaceMaterials.candidates(entry.part())) {
                    if (!(candidate.getItem() instanceof BlockItem blockItem)
                            || OmniversalAlloyFurnaceStructure.functionalPart(blockItem.getBlock().defaultBlockState()) != null) continue;
                    long available = availableMaterials.computeIfAbsent(candidate.getItem(), item ->
                            extract(access, new ItemStack(item, Integer.MAX_VALUE), Actionable.SIMULATE)
                                    + countInventory(player, item));
                    if (available > requirements.getOrDefault(candidate.getItem(), 0)) {
                        expected = blockItem.getBlock().defaultBlockState();
                        break;
                    }
                }
            }
            placements.add(new Placement(pos.immutable(), current, expected));
            if (!expected.isAir()) {
                requirements.merge(expected.getBlock().asItem(), 1, Integer::sum);
            }
        }

        if (!player.getAbilities().instabuild) {
            Component missing = missingMaterials(player, access, requirements);
            if (missing != null) return new Result(false, missing);
        }

        MaterialTransaction materials = new MaterialTransaction(player, access);
        if (!player.getAbilities().instabuild && !materials.commit(requirements)) {
            materials.rollback();
            return failure("gui.useless_mod.multiblock_builder.commit_failed");
        }

        List<Placement> completed = new ArrayList<>();
        for (Placement placement : placements) {
            if (!level.setBlock(placement.pos, placement.expected, 3)) {
                for (int index = completed.size() - 1; index >= 0; index--) {
                    Placement previous = completed.get(index);
                    level.setBlock(previous.pos, previous.original, 3);
                }
                materials.rollback();
                return failure("gui.useless_mod.multiblock_builder.commit_failed");
            }
            completed.add(placement);
        }

        if (level.getBlockEntity(corePos) instanceof MultiblockAlloyFurnaceCoreBlockEntity core) {
            core.requestStructureValidation();
            core.validateStructure();
        }
        return new Result(true, Component.translatable(
                "gui.useless_mod.multiblock_builder.success", coilTier));
    }

    /** Server-thread transaction: reserve inputs, replace every coil, then return the old coils. */
    public static Result replaceCoils(ServerPlayer player, ItemStack tool, BlockPos corePos) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(corePos);
        if (!state.is(ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get())
                || !state.getValue(MultiblockAlloyFurnaceCoreBlock.FORMED)
                || !(player.getOffhandItem().getItem() instanceof BlockItem item)
                || !(item.getBlock() instanceof UselessCoilBlock replacement)) return failure("gui.useless_mod.multiblock_builder.invalid_core");
        var access = materialAccess(player, tool, corePos);
        if (access == null) return new Result(false, Component.literal("替换线圈需要可用的 AE 网络"));
        Direction facing = state.getValue(MultiblockAlloyFurnaceCoreBlock.FACING);
        List<Placement> changes = new ArrayList<>();
        Map<Item, Integer> recovered = new LinkedHashMap<>();
        for (var entry : OmniversalAlloyFurnaceStructure.entries()) {
            if (entry.part() != OmniversalAlloyFurnaceStructure.Part.COIL) continue;
            BlockPos pos = entry.worldPos(corePos, facing);
            if (!level.isLoaded(pos)) return failure("gui.useless_mod.multiblock_builder.unloaded");
            BlockState old = level.getBlockState(pos);
            if (!(old.getBlock() instanceof UselessCoilBlock)) return failure("gui.useless_mod.multiblock_builder.mixed_coils");
            if (old.getBlock() == replacement) continue;
            if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,Direction.UP,tool)
                    || net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos) == -1)
                return failure("gui.useless_mod.multiblock_builder.no_permission");
            changes.add(new Placement(pos.immutable(), old, replacement.defaultBlockState()));
            recovered.merge(old.getBlock().asItem(),1,Integer::sum);
        }
        if (changes.isEmpty()) return new Result(true, Component.literal("当前已使用该线圈"));
        for (var entry : recovered.entrySet()) {
            ItemStack stack = new ItemStack(entry.getKey(),entry.getValue());
            if (insert(access,stack,Actionable.SIMULATE) < entry.getValue())
                return new Result(false,Component.literal("AE 网络没有足够空间回收旧线圈"));
        }
        Map<Item,Integer> requirements = Map.of(item,changes.size());
        Component missing = missingMaterials(player,access,requirements);
        if (missing != null) return new Result(false,missing);
        var materials = new MaterialTransaction(player,access);
        if (!materials.commit(requirements)) {
            materials.rollback(); return failure("gui.useless_mod.multiblock_builder.commit_failed");
        }
        List<Placement> completed = new ArrayList<>();
        for (var change : changes) {
            if (!level.setBlock(change.pos,change.expected,2)) {
                for (var previous : completed) level.setBlock(previous.pos,previous.original,2);
                materials.rollback(); return failure("gui.useless_mod.multiblock_builder.commit_failed");
            }
            completed.add(change);
        }
        for (var entry : recovered.entrySet()) {
            ItemStack stack = new ItemStack(entry.getKey(),entry.getValue());
            int remainder = entry.getValue() - (int)insert(access,stack,Actionable.MODULATE);
            // A storage listener can change capacity after simulation; never discard returned materials.
            if (remainder > 0) player.getInventory().placeItemBackInInventory(new ItemStack(entry.getKey(),remainder));
        }
        for (var change : changes) level.updateNeighborsAt(change.pos,change.expected.getBlock());
        if (level.getBlockEntity(corePos) instanceof MultiblockAlloyFurnaceCoreBlockEntity core) {
            core.requestStructureValidation(); core.validateStructure();
        }
        return new Result(true,Component.literal("已替换 " + changes.size() + " 个线圈，旧线圈已回收"));
    }

    private static FunctionalLayout resolveFunctionalLayout(
            ServerLevel level, BlockPos corePos, Direction facing) {
        Map<BlockPos, OmniversalAlloyFurnaceStructure.FunctionalPart> parts = new LinkedHashMap<>();
        for (OmniversalAlloyFurnaceStructure.Entry entry : OmniversalAlloyFurnaceStructure.entries()) {
            if (entry.part() != OmniversalAlloyFurnaceStructure.Part.INTERFACE) continue;
            BlockPos pos = entry.worldPos(corePos, facing);
            OmniversalAlloyFurnaceStructure.FunctionalPart functionalPart =
                    OmniversalAlloyFurnaceStructure.functionalPart(level.getBlockState(pos));
            if (functionalPart == null) continue;
            if (isExclusivePartClaimedByAnotherCore(level, corePos, pos, functionalPart)) return null;
            parts.put(pos.immutable(), functionalPart);
        }

        if (!parts.containsValue(OmniversalAlloyFurnaceStructure.FunctionalPart.PATTERN_ASSEMBLY)) {
            BlockPos pos = chooseFunctionalPosition(level, corePos, facing, parts,
                    OmniversalAlloyFurnaceStructure.PREFERRED_PATTERN_ASSEMBLY_POS);
            if (pos == null) return null;
            parts.put(pos, OmniversalAlloyFurnaceStructure.FunctionalPart.PATTERN_ASSEMBLY);
        }
        if (!parts.containsValue(OmniversalAlloyFurnaceStructure.FunctionalPart.MOLD_HUB)) {
            BlockPos pos = chooseFunctionalPosition(level, corePos, facing, parts,
                    OmniversalAlloyFurnaceStructure.PREFERRED_MOLD_HUB_POS);
            if (pos == null) return null;
            parts.put(pos, OmniversalAlloyFurnaceStructure.FunctionalPart.MOLD_HUB);
        }
        return new FunctionalLayout(Map.copyOf(parts));
    }

    private static boolean isExclusivePartClaimedByAnotherCore(
            ServerLevel level, BlockPos corePos, BlockPos partPos,
            OmniversalAlloyFurnaceStructure.FunctionalPart functionalPart) {
        return switch (functionalPart) {
            case PATTERN_ASSEMBLY -> level.getBlockEntity(partPos)
                    instanceof MePatternAssemblyBlockEntity assembly
                    && assembly.isClaimedByOtherController(corePos);
            case PASSIVE_HATCH -> level.getBlockEntity(partPos)
                    instanceof PassiveCraftingHatchBlockEntity hatch
                    && hatch.isClaimedByOtherController(corePos);
            case MOLD_HUB -> level.getBlockEntity(partPos) instanceof OmniversalMoldHubBlockEntity hub && hub.isClaimedByOtherController(corePos);
        };
    }

    private static BlockPos chooseFunctionalPosition(
            ServerLevel level, BlockPos corePos, Direction facing,
            Map<BlockPos, OmniversalAlloyFurnaceStructure.FunctionalPart> assigned,
            BlockPos preferredLocalPos) {
        for (OmniversalAlloyFurnaceStructure.Entry entry : OmniversalAlloyFurnaceStructure.entries()) {
            if (entry.part() != OmniversalAlloyFurnaceStructure.Part.INTERFACE
                    || !entry.localPos().equals(preferredLocalPos)) continue;
            BlockPos pos = entry.worldPos(corePos, facing);
            if (!assigned.containsKey(pos) && isAvailableFunctionalPosition(level, pos)) {
                return pos.immutable();
            }
        }
        for (OmniversalAlloyFurnaceStructure.Entry entry : OmniversalAlloyFurnaceStructure.entries()) {
            if (entry.part() != OmniversalAlloyFurnaceStructure.Part.INTERFACE) continue;
            BlockPos pos = entry.worldPos(corePos, facing);
            if (!assigned.containsKey(pos) && isAvailableFunctionalPosition(level, pos)) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static boolean isAvailableFunctionalPosition(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) != null) return false;
        BlockState state = level.getBlockState(pos);
        return state.canBeReplaced() || state.isAir() || state.is(ModTags.OMNIVERSAL_FURNACE_CASINGS) || state.is(ModBlocks.OMNIVERSAL_FORGE_FRAME.get());
    }

    private static Component missingMaterials(ServerPlayer player, com.sorrowmist.uselesscore.api.ae.CoreLinkedGrid.Access access, Map<Item, Integer> requirements) {
        var message = Component.translatable("gui.useless_mod.multiblock_builder.missing_materials");
        boolean missing = false;
        for (var entry : requirements.entrySet()) {
            ItemStack requested = new ItemStack(entry.getKey(), entry.getValue());
            long available = extract(access, requested, Actionable.SIMULATE)
                    + countInventory(player, entry.getKey());
            if (available < entry.getValue()) {
                message.append("\n").append(requested.getHoverName()).append(" × " + (entry.getValue() - available));
                missing = true;
            }
        }
        return missing ? message : null;
    }

    private static int missingCoils(ServerLevel level, BlockPos corePos, Direction facing, int tier) {
        int missing = 0;
        for (var entry : OmniversalAlloyFurnaceStructure.entries()) {
            if (entry.part() == OmniversalAlloyFurnaceStructure.Part.COIL
                    && !matches(entry.part(), level.getBlockState(entry.worldPos(corePos, facing)), tier, null)) {
                missing++;
            }
        }
        return missing;
    }

    private static int coilTier(ItemStack stack) {
        var bill=com.sorrowmist.useless.content.items.OmniversalConstructionPatternItem.read(stack);
        if(bill.isPresent())return bill.get().tier();
        int receiptTier=com.sorrowmist.useless.content.items.OmniversalConstructionReceiptItem.tier(stack);
        if(receiptTier!=0)return receiptTier;
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof UselessCoilBlock coil ? coil.tier() : 0;
    }

    private static boolean matches(OmniversalAlloyFurnaceStructure.Part part, BlockState state, int coilTier,
                                   OmniversalAlloyFurnaceStructure.FunctionalPart functionalPart) {
        if (functionalPart != null) {
            return OmniversalAlloyFurnaceStructure.functionalPart(state) == functionalPart;
        }
        return switch (part) {
            case CORE -> state.is(ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get());
            case CASING -> state.is(ModTags.OMNIVERSAL_FURNACE_CASINGS)
                    && OmniversalAlloyFurnaceStructure.functionalPart(state) == null;
            case COIL -> state.getBlock() instanceof UselessCoilBlock coil && coil.tier() == coilTier;
            case COPPER -> state.is(ModBlocks.OMNIVERSAL_FORGE_COPPER.get());
            case TRIGRAM_QIAN -> state.is(ModBlocks.TRIGRAM_QIAN.get());
            case TRIGRAM_DUI -> state.is(ModBlocks.TRIGRAM_DUI.get());
            case TRIGRAM_LI -> state.is(ModBlocks.TRIGRAM_LI.get());
            case TRIGRAM_ZHEN -> state.is(ModBlocks.TRIGRAM_ZHEN.get());
            case TRIGRAM_KUN -> state.is(ModBlocks.TRIGRAM_KUN.get());
            case TRIGRAM_GEN -> state.is(ModBlocks.TRIGRAM_GEN.get());
            case TRIGRAM_KAN -> state.is(ModBlocks.TRIGRAM_KAN.get());
            case TRIGRAM_XUN -> state.is(ModBlocks.TRIGRAM_XUN.get());
            case FRAME -> state.is(ModBlocks.OMNIVERSAL_FORGE_FRAME.get());
            case TRIM -> state.is(ModBlocks.OMNIVERSAL_FORGE_TRIM.get());
            case VENT -> state.is(ModBlocks.OMNIVERSAL_FORGE_VENT.get());
            case RESONATOR -> state.is(ModBlocks.OMNIVERSAL_FORGE_RESONATOR.get());
            case INTERFACE -> state.is(ModBlocks.OMNIVERSAL_FORGE_FRAME.get()) || state.is(ModTags.OMNIVERSAL_FURNACE_CASINGS) && OmniversalAlloyFurnaceStructure.functionalPart(state)==null;
            case AIR -> state.isAir();
        };
    }

    private static boolean canReplaceForBuild(BlockState current,
                                              OmniversalAlloyFurnaceStructure.FunctionalPart functionalPart) {
        return current.canBeReplaced() || current.isAir()
                || functionalPart != null && (current.is(ModTags.OMNIVERSAL_FURNACE_CASINGS) || current.is(ModBlocks.OMNIVERSAL_FORGE_FRAME.get()));
    }

    public static BlockState expectedState(OmniversalAlloyFurnaceStructure.Part part, int coilTier,
                                    OmniversalAlloyFurnaceStructure.FunctionalPart functionalPart,
                                    Direction facing) {
        if (functionalPart != null) {
            return (switch (functionalPart) {
                case PATTERN_ASSEMBLY -> ModBlocks.ME_PATTERN_ASSEMBLY.get().defaultBlockState();
                case MOLD_HUB -> ModBlocks.OMNIVERSAL_MOLD_HUB.get().defaultBlockState();
                case PASSIVE_HATCH -> ModBlocks.PASSIVE_CRAFTING_HATCH.get().defaultBlockState();
            }).setValue(DirectionalMultiblockPartBlock.FACING, facing);
        }
        return switch (part) {
            case CASING -> ModBlocks.OMNIVERSAL_FORGE_ARMOR.get().defaultBlockState();
            case COIL -> ModBlocks.USELESS_COILS.get(coilTier).get().defaultBlockState();
            case COPPER -> ModBlocks.OMNIVERSAL_FORGE_COPPER.get().defaultBlockState();
            case TRIGRAM_QIAN -> ModBlocks.TRIGRAM_QIAN.get().defaultBlockState();
            case TRIGRAM_DUI -> ModBlocks.TRIGRAM_DUI.get().defaultBlockState();
            case TRIGRAM_LI -> ModBlocks.TRIGRAM_LI.get().defaultBlockState();
            case TRIGRAM_ZHEN -> ModBlocks.TRIGRAM_ZHEN.get().defaultBlockState();
            case TRIGRAM_KUN -> ModBlocks.TRIGRAM_KUN.get().defaultBlockState();
            case TRIGRAM_GEN -> ModBlocks.TRIGRAM_GEN.get().defaultBlockState();
            case TRIGRAM_KAN -> ModBlocks.TRIGRAM_KAN.get().defaultBlockState();
            case TRIGRAM_XUN -> ModBlocks.TRIGRAM_XUN.get().defaultBlockState();
            case FRAME -> ModBlocks.OMNIVERSAL_FORGE_FRAME.get().defaultBlockState();
            case TRIM -> ModBlocks.OMNIVERSAL_FORGE_TRIM.get().defaultBlockState();
            case VENT -> ModBlocks.OMNIVERSAL_FORGE_VENT.get().defaultBlockState();
            case RESONATOR -> ModBlocks.OMNIVERSAL_FORGE_RESONATOR.get().defaultBlockState();
            case INTERFACE -> ModBlocks.OMNIVERSAL_FORGE_FRAME.get().defaultBlockState();
            case AIR -> Blocks.AIR.defaultBlockState();
            case CORE -> ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get().defaultBlockState();
        };
    }

    public static com.sorrowmist.uselesscore.api.ae.CoreLinkedGrid.Access materialAccess(ServerPlayer player, ItemStack tool, BlockPos pos) {
        var linked = com.sorrowmist.uselesscore.api.ae.CoreLinkedGrid.resolve(tool, player);
        if (linked != null) return linked;
        if (player.level().getBlockEntity(pos) instanceof MultiblockAlloyFurnaceCoreBlockEntity core
                && core.getMainNode().isOnline() && core.getMainNode().getGrid() != null) {
            return new com.sorrowmist.uselesscore.api.ae.CoreLinkedGrid.Access(core.getMainNode().getGrid(),
                    appeng.api.networking.security.IActionSource.ofPlayer(player));
        }
        return null;
    }
    private static long extract(com.sorrowmist.uselesscore.api.ae.CoreLinkedGrid.Access access, ItemStack stack, Actionable mode) {
        return access == null ? 0 : access.grid().getStorageService().getInventory().extract(
                appeng.api.stacks.AEItemKey.of(stack), stack.getCount(), mode, access.source());
    }
    private static long insert(com.sorrowmist.uselesscore.api.ae.CoreLinkedGrid.Access access, ItemStack stack, Actionable mode) {
        return access == null ? 0 : access.grid().getStorageService().getInventory().insert(
                appeng.api.stacks.AEItemKey.of(stack), stack.getCount(), mode, access.source());
    }

    private static int countInventory(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static Result failure(String key, Object... arguments) {
        return new Result(false, Component.translatable(key, arguments));
    }

    private record Placement(BlockPos pos, BlockState original, BlockState expected) {
    }

    private record FunctionalLayout(
            Map<BlockPos, OmniversalAlloyFurnaceStructure.FunctionalPart> parts) {
    }

    private static final class MaterialTransaction {
        private final ServerPlayer player;
        private final com.sorrowmist.uselesscore.api.ae.CoreLinkedGrid.Access access;
        private final Map<Item, Integer> fromNetwork = new LinkedHashMap<>();
        private final List<InventoryRemoval> fromInventory = new ArrayList<>();

        private MaterialTransaction(ServerPlayer player, com.sorrowmist.uselesscore.api.ae.CoreLinkedGrid.Access access) {
            this.player = player;
            this.access = access;
        }

        private boolean commit(Map<Item, Integer> requirements) {
            for (Map.Entry<Item, Integer> requirement : requirements.entrySet()) {
                int needed = requirement.getValue();
                ItemStack requested = new ItemStack(requirement.getKey(), needed);
                int network = (int) Math.min(needed,
                        extract(access, requested, Actionable.MODULATE));
                if (network > 0) fromNetwork.merge(requirement.getKey(), network, Integer::sum);
                needed -= network;
                for (int slot = 0; slot < player.getInventory().getContainerSize() && needed > 0; slot++) {
                    ItemStack stack = player.getInventory().getItem(slot);
                    if (!stack.is(requirement.getKey())) continue;
                    int removed = Math.min(needed, stack.getCount());
                    stack.shrink(removed);
                    fromInventory.add(new InventoryRemoval(slot, requirement.getKey(), removed));
                    needed -= removed;
                }
                if (needed > 0) return false;
            }
            player.getInventory().setChanged();
            return true;
        }

        private void rollback() {
            for (Map.Entry<Item, Integer> entry : fromNetwork.entrySet()) {
                ItemStack stack = new ItemStack(entry.getKey(), entry.getValue());
                long inserted = insert(access, stack, Actionable.MODULATE);
                int remaining = entry.getValue() - (int) inserted;
                if (remaining > 0) player.getInventory().placeItemBackInInventory(
                        new ItemStack(entry.getKey(), remaining));
            }
            for (InventoryRemoval removal : fromInventory) {
                ItemStack current = player.getInventory().getItem(removal.slot);
                if (current.isEmpty()) {
                    player.getInventory().setItem(removal.slot, new ItemStack(removal.item, removal.count));
                } else if (current.is(removal.item)) {
                    current.grow(removal.count);
                } else {
                    player.getInventory().placeItemBackInInventory(new ItemStack(removal.item, removal.count));
                }
            }
            fromNetwork.clear();
            fromInventory.clear();
            player.getInventory().setChanged();
        }
    }

    private record InventoryRemoval(int slot, Item item, int count) {
    }
}
