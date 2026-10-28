package com.sorrowmist.useless.world.inventory;

import com.sorrowmist.uselesscore.api.component.ExternalInventoryKind;
import com.sorrowmist.uselesscore.api.component.ExternalInventoryReference;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import com.sorrowmist.uselesscore.api.component.CoreEntityComponents;
import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ExternalInventoryStore {
    private ExternalInventoryStore() {
    }

    /** Also accepts old staff silk-touch drops, whose reference lives inside block entity NBT. */
    @Nullable
    public static ExternalInventoryReference getPlacedReference(ItemStack stack, ExternalInventoryKind kind) {
        ExternalInventoryReference reference = CoreItemComponents.get(
                stack, UselessComponents.EXTERNAL_INVENTORY_REFERENCE.get());
        if (reference != null && reference.kind() == kind) return reference;

        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
        if (blockEntityTag == null) return null;
        Tag encoded = blockEntityTag.getCompound("ForgeData")
                .getCompound("UselessCoreComponents")
                .get(UselessComponents.EXTERNAL_INVENTORY_REFERENCE.get().id().toString());
        if (encoded == null) return null;
        reference = ExternalInventoryReference.CODEC.parse(NbtOps.INSTANCE, encoded).result().orElse(null);
        return reference != null && reference.kind() == kind ? reference : null;
    }

    public static ExternalInventoryReference bindAt(
            Level level,
            ExternalInventoryKind kind,
            @Nullable ExternalInventoryReference requested,
            BlockPos pos,
            ItemStackHandler inventory,
            @Nullable CompoundTag legacyInventory,
            HolderLookup.Provider registries) {
        ExternalInventoryReference reference = requested != null && requested.kind() == kind
                ? requested
                : new ExternalInventoryReference(kind, UUID.randomUUID());
        if (level.isClientSide() || level.getServer() == null) return reference;

        ExternalInventorySavedData data = ExternalInventorySavedData.get(level.getServer(), reference);
        if (!data.claimAt(level, pos)) {
            ExternalInventoryReference fork = new ExternalInventoryReference(kind, UUID.randomUUID());
            ExternalInventorySavedData forked = ExternalInventorySavedData.get(level.getServer(), fork);
            data.copyInventoryTo(forked);
            forked.claimAt(level, pos);
            forked.loadInto(inventory, registries);
            return fork;
        }

        if (data.isInitialized()) {
            data.loadInto(inventory, registries);
        } else if (legacyInventory != null) {
            inventory.deserializeNBT(legacyInventory.copy());
            data.saveFrom(inventory, registries);
        } else {
            data.saveFrom(inventory, registries);
        }
        return reference;
    }

    /**
     * 认领一份「通用载荷」外置数据（不是物品栏，而是任意一段 NBT，例如紧凑 F9 的全部样板）。
     *
     * <p>占位规则与 {@link #bindAt} 完全一致：目标坐标已被别的方块占着就分叉出一份新 UUID，
     * 把老数据整份复制过去，避免复制出来的两台机器互相踩。</p>
     */
    public static ExternalInventoryReference bindRawAt(
            Level level,
            ExternalInventoryKind kind,
            @Nullable ExternalInventoryReference requested,
            BlockPos pos) {
        ExternalInventoryReference reference = requested != null && requested.kind() == kind
                ? requested
                : new ExternalInventoryReference(kind, UUID.randomUUID());
        if (level.isClientSide() || level.getServer() == null) return reference;

        ExternalInventorySavedData data = ExternalInventorySavedData.get(level.getServer(), reference);
        if (!data.claimAt(level, pos)) {
            ExternalInventoryReference fork = new ExternalInventoryReference(kind, UUID.randomUUID());
            ExternalInventorySavedData forked = ExternalInventorySavedData.get(level.getServer(), fork);
            data.copyInventoryTo(forked);
            forked.claimAt(level, pos);
            return fork;
        }
        return reference;
    }

    /** 读出外置存储里的通用载荷；从没写过时返回 {@code null}。 */
    public static @Nullable CompoundTag loadRawPayload(Level level, @Nullable ExternalInventoryReference reference) {
        if (reference == null || level.isClientSide() || level.getServer() == null) return null;
        return ExternalInventorySavedData.get(level.getServer(), reference).rawPayload();
    }

    /** 把通用载荷写进外置存储。调用方负责控制写入频率——这份载荷可能有 MB 级。 */
    public static void saveRawPayload(Level level, @Nullable ExternalInventoryReference reference,
                                      CompoundTag payload) {
        if (reference == null || level.isClientSide() || level.getServer() == null) return;
        ExternalInventorySavedData.get(level.getServer(), reference).saveRawPayload(payload);
    }

    public static @Nullable ExternalInventoryReference getReference(BlockEntity entity) {
        return CoreEntityComponents.get(entity, UselessComponents.EXTERNAL_INVENTORY_REFERENCE.get());
    }

    public static void setReference(BlockEntity entity, ExternalInventoryReference reference) {
        CoreEntityComponents.set(entity, UselessComponents.EXTERNAL_INVENTORY_REFERENCE.get(), reference);
    }

    public static void clearLegacyComponent(BlockEntity entity, ExternalInventoryKind kind) {
        if (kind == ExternalInventoryKind.FURNACE) {
            CoreEntityComponents.set(entity, UselessComponents.FURNACE_DATA.get(), null);
        } else {
            CoreEntityComponents.set(entity, UselessComponents.MULTIBLOCK_PART_DATA.get(), null);
        }
    }

    public static void save(Level level, @Nullable ExternalInventoryReference reference,
                             ItemStackHandler inventory, HolderLookup.Provider registries) {
        if (reference == null || level.isClientSide() || level.getServer() == null) return;
        ExternalInventorySavedData.get(level.getServer(), reference).saveFrom(inventory, registries);
    }

    public static void release(Level level, BlockPos pos,
                               @Nullable ExternalInventoryReference reference) {
        if (reference == null || level.isClientSide() || level.getServer() == null) return;
        ExternalInventorySavedData.get(level.getServer(), reference).releaseAt(level, pos);
    }
}
