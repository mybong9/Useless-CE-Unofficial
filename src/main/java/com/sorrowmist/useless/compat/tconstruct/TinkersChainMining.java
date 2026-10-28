package com.sorrowmist.useless.compat.tconstruct;

import com.sorrowmist.useless.core.config.ConfigManager;
import com.sorrowmist.useless.utils.mining.MiningDispatcher;
import com.sorrowmist.useless.utils.mining.MiningUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Native harvest calls preserve Tinkers durability, modifiers, experience and AOE. */
public final class TinkersChainMining {
    private static final TagKey<Item> HARVEST=TagKey.create(Registries.ITEM,new ResourceLocation("tconstruct","modifiable/harvest"));
    private static final Set<UUID> ACTIVE=new HashSet<>();
    private TinkersChainMining() {}
    public static boolean eligible(ItemStack stack) { return ConfigManager.isTinkersToolChainMiningEnabled() && stack.is(HARVEST); }
    public static boolean tryChain(ItemStack stack,BlockPos origin,Player entity) {
        if (!(entity instanceof ServerPlayer player) || !eligible(stack) || ACTIVE.contains(player.getUUID())) return false;
        var data=MiningDispatcher.getPlayerData(player);
        if (data==null || !data.isTabPressed() || broken(stack)) return false;
        var level=player.serverLevel();
        var positions=MiningUtils.scanBlocksToMine(origin,level.getBlockState(origin),level,stack,false,false,
                data.getShape(),MiningUtils.getTargetFace(player),player);
        ACTIVE.add(player.getUUID());
        try {
            for (BlockPos target:positions) {
                if (player.getMainHandItem()!=stack || stack.isEmpty() || broken(stack)) break;
                if (!level.hasChunkAt(target) || level.getBlockState(target).isAir()) continue;
                // With ACTIVE set, the nested item hook runs Tinkers' original pipeline.
                player.gameMode.destroyBlock(target);
            }
        } finally {
            ACTIVE.remove(player.getUUID()); data.clearCache(); MiningDispatcher.syncToClient(player);
        }
        return true;
    }
    private static boolean broken(ItemStack stack) {
        try {
            Class<?> type=Class.forName("slimeknights.tconstruct.library.tools.nbt.ToolStack");
            Object tool=type.getMethod("from",ItemStack.class).invoke(null,stack);
            return (boolean)type.getMethod("isBroken").invoke(tool);
        } catch (ReflectiveOperationException missing) { return false; }
    }
}
