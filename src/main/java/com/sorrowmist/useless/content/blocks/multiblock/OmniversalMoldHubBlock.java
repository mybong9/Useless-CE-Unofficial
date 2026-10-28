package com.sorrowmist.useless.content.blocks.multiblock;


import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.useless.content.blockentities.multiblock.OmniversalMoldHubBlockEntity;
import com.sorrowmist.uselesscore.api.component.ExternalInventoryKind;
import com.sorrowmist.uselesscore.api.component.ExternalInventoryReference;
import com.sorrowmist.uselesscore.api.component.MultiblockPartData;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import com.sorrowmist.useless.world.inventory.ExternalInventoryStore;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class OmniversalMoldHubBlock extends DirectionalMultiblockPartBlock implements EntityBlock {
    public OmniversalMoldHubBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OmniversalMoldHubBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof OmniversalMoldHubBlockEntity hub) {
            hub.releaseExternalInventory();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof OmniversalMoldHubBlockEntity hub) {
            hub.saveExternalInventory();
            ExternalInventoryReference reference = hub.getExternalInventoryReference();
            for (ItemStack drop : drops) {
                if (drop.is(asItem())) {
                    if (reference != null) {
                        CoreItemComponents.set(drop, UselessComponents.EXTERNAL_INVENTORY_REFERENCE.get(), reference);
                    }
                    break;
                }
            }
        }
        return drops;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OmniversalMoldHubBlockEntity hub) {
            ExternalInventoryReference reference = ExternalInventoryStore.getPlacedReference(
                    stack, ExternalInventoryKind.MOLD_HUB);
            MultiblockPartData itemData = CoreItemComponents.get(stack, UselessComponents.MULTIBLOCK_PART_DATA.get());
            hub.bindPlacedExternalInventory(reference,
                    itemData == null ? null : itemData.inventory(), level.registryAccess());
            ExternalInventoryStore.clearLegacyComponent(hub, ExternalInventoryKind.MOLD_HUB);
        }
    }

    @Override
    public InteractionResult use(BlockState state, net.minecraft.world.level.Level level,
                                               BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            com.sorrowmist.uselesscore.api.network.CoreMenus.open(player, provider, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
