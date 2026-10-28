package com.sorrowmist.useless.content.multiblock;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.compat.AE2Compat;
import com.sorrowmist.useless.content.blockentities.multiblock.*;
import com.sorrowmist.useless.content.blocks.multiblock.*;
import com.sorrowmist.useless.utils.mining.MiningUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Template-scoped, bounded removal using the same lossless drop path as chain mining. */
@Mod.EventBusSubscriber(modid = UselessMod.MODID)
public final class OmniversalFurnaceDismantler {
    private static final Map<UUID, Job> JOBS = new LinkedHashMap<>();
    private record Target(BlockPos pos, OmniversalAlloyFurnaceStructure.Part part) {}
    private static final class Job {
        final ServerPlayer player; final ServerLevel level; final BlockPos core;
        final ItemStack tool; final IGrid grid; final ArrayDeque<Target> targets;
        int removed;
        Job(ServerPlayer player, BlockPos core, ItemStack tool, IGrid grid, List<Target> targets) {
            this.player=player; this.level=player.serverLevel(); this.core=core.immutable();
            this.tool=tool.copy(); this.grid=grid; this.targets=new ArrayDeque<>(targets);
        }
    }
    private OmniversalFurnaceDismantler() {}
    public static void start(ServerPlayer player, ItemStack staff, BlockPos pos) {
        if (JOBS.containsKey(player.getUUID())) { message(player,"busy"); return; }
        if (!(player.serverLevel().getBlockEntity(pos) instanceof MultiblockAlloyFurnaceCoreBlockEntity core)) return;
        if (JOBS.values().stream().anyMatch(j -> j.level==player.serverLevel() && j.core.equals(pos))) {
            message(player,"busy"); return;
        }
        IGrid grid;
        var linked=AE2Compat.resolveLinkedGrid(staff,player);
        try { grid=linked!=null?linked.grid():core.getAeGrid(); }
        catch (RuntimeException unavailable) { grid=null; }
        if (grid==null || !permitted(grid,player)) { message(player,"network"); return; }
        Direction facing=core.getBlockState().getValue(MultiblockAlloyFurnaceCoreBlock.FACING);
        List<Target> targets=new ArrayList<>();
        for (var entry:OmniversalAlloyFurnaceStructure.entries()) {
            if (entry.part()==OmniversalAlloyFurnaceStructure.Part.AIR || entry.part()==OmniversalAlloyFurnaceStructure.Part.CORE) continue;
            BlockPos target=entry.worldPos(pos,facing);
            if (!player.serverLevel().hasChunkAt(target)) { message(player,"unloaded"); return; }
            BlockState state=player.serverLevel().getBlockState(target);
            if (!OmniversalAlloyFurnaceStructure.matches(entry.part(),state)) continue;
            var be=player.serverLevel().getBlockEntity(target);
            if (be instanceof MePatternAssemblyBlockEntity assembly && assembly.isClaimedByOtherController(pos)
                    || be instanceof OmniversalMoldHubBlockEntity hub && hub.isClaimedByOtherController(pos)
                    || be instanceof PassiveCraftingHatchBlockEntity hatch && hatch.isClaimedByOtherController(pos)) {
                message(player,"protected"); return;
            }
            if (!allowed(player,target)) { message(player,"protected"); return; }
            targets.add(new Target(target.immutable(),entry.part()));
        }
        // Keep network interfaces until the shell has been recovered; core is always last.
        targets.sort(Comparator.comparingInt(t -> t.part()==OmniversalAlloyFurnaceStructure.Part.INTERFACE?1:0));
        targets.add(new Target(pos.immutable(),OmniversalAlloyFurnaceStructure.Part.CORE));
        core.beginDismantling();
        JOBS.put(player.getUUID(),new Job(player,pos,staff,grid,targets));
        message(player,"started");
    }
    private static boolean allowed(ServerPlayer p,BlockPos pos) {
        return p.mayBuild() && p.serverLevel().mayInteract(p,pos)
                && p.serverLevel().getWorldBorder().isWithinBounds(pos);
    }
    private static boolean permitted(IGrid grid,ServerPlayer player) {
        // AE2 15 no longer exposes a security service. Use player-sourced storage
        // operations and Forge's block-break permission event for each removal.
        return player.mayBuild() && !grid.isEmpty() && grid.getEnergyService().isNetworkPowered();
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        var iterator=JOBS.values().iterator();
        int budget=64;
        while (iterator.hasNext() && budget>0) {
            Job job=iterator.next();
            boolean stopped=false;
            try {
                if (job.player.isRemoved() || job.player.serverLevel()!=job.level || !job.player.isAlive()) stopped=true;
                while (!stopped && !job.targets.isEmpty() && budget-->0) {
                    Target target=job.targets.peek();
                    if (!job.level.hasChunkAt(target.pos())) { message(job.player,"unloaded"); stopped=true; break; }
                    if (!allowed(job.player,target.pos()) || !permitted(job.grid,job.player)) {
                        message(job.player,"protected"); stopped=true; break;
                    }
                    BlockState state=job.level.getBlockState(target.pos());
                    if (!OmniversalAlloyFurnaceStructure.matches(target.part(),state)) { job.targets.remove(); continue; }
                    var storage=job.grid.getStorageService().getInventory();
                    var source=IActionSource.ofPlayer(job.player);
                    AEItemKey preview=AEItemKey.of(new ItemStack(state.getBlock()));
                    if (preview==null || storage.insert(preview,1,Actionable.SIMULATE,source)<1) {
                        message(job.player,"full"); stopped=true; break;
                    }
                    if (ForgeHooks.onBlockBreakEvent(job.level,job.player.gameMode.getGameModeForPlayer(),job.player,target.pos())<0) {
                        message(job.player,"protected"); stopped=true; break;
                    }
                    var outcome=MiningUtils.destroyBlockAndCollectDrops(job.level,target.pos(),state,job.player,job.tool,false);
                    job.targets.remove();
                    if (outcome.blockRemoved()) job.removed++;
                    for (ItemStack drop:outcome.drops()) {
                        AEItemKey key=AEItemKey.of(drop);
                        long inserted=0;
                        try { if (key!=null) inserted=storage.insert(key,drop.getCount(),Actionable.MODULATE,source); }
                        catch (RuntimeException disconnected) { stopped=true; }
                        int remaining=drop.getCount()-(int)Math.min(drop.getCount(),Math.max(0,inserted));
                        if (remaining>0) {
                            job.player.getInventory().placeItemBackInInventory(drop.copyWithCount(remaining),false);
                            message(job.player,"remainder"); stopped=true;
                        }
                    }
                    if (!outcome.blockRemoved()) { message(job.player,"protected"); stopped=true; }
                }
            } catch (RuntimeException failure) {
                UselessMod.LOGGER.warn("Stopped multiblock dismantling at {}",job.core,failure);
                message(job.player,"stopped"); stopped=true;
            }
            if (stopped || job.targets.isEmpty()) {
                if (job.level.hasChunkAt(job.core) && job.level.getBlockEntity(job.core) instanceof MultiblockAlloyFurnaceCoreBlockEntity core) core.endDismantling();
                if (!stopped) job.player.displayClientMessage(Component.translatable("gui.useless_mod.dismantle.done",job.removed),false);
                iterator.remove();
            }
        }
    }
    @SubscribeEvent public static void stopping(ServerStoppingEvent event) { JOBS.clear(); }
    private static void message(ServerPlayer player,String suffix) {
        player.displayClientMessage(Component.translatable("gui.useless_mod.dismantle."+suffix),false);
    }
}
