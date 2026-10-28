package com.sorrowmist.useless.compat.neoecoae.compact.block;


import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import appeng.api.crafting.PatternDetailsHelper;
import cn.dancingsnow.neoecoae.api.ECOPatternInsertionResult;
import cn.dancingsnow.neoecoae.blocks.crafting.ECOCraftingSystem;
import com.sorrowmist.useless.compat.neoecoae.compact.entity.CompactF9BlockEntity;
import com.sorrowmist.uselesscore.api.component.ExternalInventoryReference;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import com.sorrowmist.uselesscore.api.capability.CoreCapabilities;
import net.minecraftforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 无用型紧凑 F9 的方块形态。
 *
 * <p>直接继承 ECO 的合成系统主机方块，交互与界面沿用 ECO 实现；
 * 额外补一个「手持编码样板右键」入口——紧凑方块内部没有可交互的样板总线方块，
 * 所以给玩家一条把样板送进 ECO 样板总线的路。</p>
 *
 * <p>另外接管样板数据（存在外置存档里，见 {@link CompactF9BlockEntity}）的搬运：
 * 拆掉时把 UUID 挂到掉落物上、放下时接回来，避免拆一台满样板的主机等于烧掉 176 条总线。</p>
 */
public class CompactF9Block extends ECOCraftingSystem {

    public CompactF9Block(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CompactInterfaceAccess.INTERFACE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CompactInterfaceAccess.INTERFACE);
    }

    private InteractionResult openMenu(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!CompactInterfaceAccess.isPlayerCloseEnough(this, level, pos, player)) {
            return InteractionResult.FAIL;
        }
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                CompactInterfaceAccess.open(this, state, pos, serverPlayer);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            CompactInterfaceAccess.openMain(this, state, pos, serverPlayer);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() && isBackFace(state, hit.getDirection()))
            return openMenu(state, level, pos, player, hit);
        ItemStack heldItem = player.getItemInHand(hand);
        if (isEncodedPattern(heldItem, level)
                && level.getBlockEntity(pos) instanceof CompactF9BlockEntity compact) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            ECOPatternInsertionResult result = compact.insertPatternFromPlayerWithResult(heldItem);
            if (result == ECOPatternInsertionResult.INSERTED) {
                if (!player.isCreative()) {
                    heldItem.shrink(1);
                }
                return InteractionResult.sidedSuccess(false);
            }
            player.displayClientMessage(Component.translatable(
                    "gui.useless_mod.compact.f9.pattern_result." + result.name().toLowerCase()), true);
            return InteractionResult.sidedSuccess(false);
        }
        if (level.getBlockEntity(pos) instanceof CompactF9BlockEntity compact
                && isFluidContainer(heldItem)) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            // Output extraction and normal input filling use the public capability. The
            // second pass is a player-only recovery path for fluid left in the input pool.
            if (FluidUtil.interactWithFluidHandler(player, hand, compact.getFluidHandler())
                    || FluidUtil.interactWithFluidHandler(player, hand, compact.getInputRecoveryHandler())) {
                return InteractionResult.sidedSuccess(false);
            }
        }
        return openMenu(state, level, pos, player, hit);
    }

    /** 客户端也要能判断，否则会在服务端吞掉右键后与客户端表现不一致。 */
    public static boolean isBackFace(BlockState state, net.minecraft.core.Direction face) {
        return state.hasProperty(FACING) && face == state.getValue(FACING).getOpposite();
    }

    private static boolean isEncodedPattern(ItemStack stack, Level level) {
        return !stack.isEmpty() && PatternDetailsHelper.decodePattern(stack, level) != null;
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getCapability(CoreCapabilities.FluidHandler.ITEM).isPresent()
                || FluidUtil.getFluidContained(stack).isPresent());
    }

    // ------------------------------------------------ 外置样板数据的搬运

    /**
     * 拆掉方块时先落盘再解除占用。必须在 {@code super.onRemove} 之前做：
     * 父类会把主机集群拆掉，影子总线就没了，那时候想序列化也来不及。
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof CompactF9BlockEntity compact) {
            compact.releasePatternStore();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** 掉落物带上外置存储的引用，玩家把机器挪个地方不会丢样板。 */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CompactF9BlockEntity compact) {
            compact.commitPatternStoreForRemoval();
            ExternalInventoryReference reference = compact.patternStoreReferenceForDrop();
            if (reference != null) {
                for (ItemStack drop : drops) {
                    if (drop.is(asItem())) {
                        CoreItemComponents.set(drop, UselessComponents.EXTERNAL_INVENTORY_REFERENCE.get(), reference);
                        break;
                    }
                }
            }
        }
        return drops;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Runs before playerDestroy computes drops, so an unflushed library and a
        // lazily restored reference are committed while the bus data still exists.
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CompactF9BlockEntity compact) {
            compact.commitPatternStoreForRemoval();
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CompactF9BlockEntity compact) {
            compact.adoptPatternStore(CoreItemComponents.get(stack, UselessComponents.EXTERNAL_INVENTORY_REFERENCE.get()));
        }
    }
}
