package com.sorrowmist.useless.compat.gtceu;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.blockentities.multiblock.MultiblockAlloyFurnaceCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.fml.ModList;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;

/** Native GT hatch leases, scoped to a validated furnace and its loaded world. */
public final class GregTechParts {
    private static final Map<Level, Map<BlockPos, WeakReference<MultiblockAlloyFurnaceCoreBlockEntity>>> OWNERS = new WeakHashMap<>();
    private GregTechParts() {}

    public static boolean accepts(LevelReader level, BlockPos pos) {
        if (!ModList.get().isLoaded("gtceu")) return false;
        if (!level.getBlockState(pos).hasBlockEntity()) return false;
        try {
            Object machine = machine(level, pos);
            if (machine == null) return false;
            boolean hatch = isA(machine, "com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart")
                    && (isA(machine, "com.gregtechceu.gtceu.api.capability.IDataAccessHatch")
                    || isA(machine, "com.gregtechceu.gtceu.common.machine.multiblock.part.OpticalComputationHatchMachine")
                    || isMaintenancePart(machine)
                    || isA(machine, "com.gregtechceu.gtceu.api.machine.feature.ICleanroomProvider")
                    || isA(machine, "org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.IGravityPartMachine"));
            if (!hatch || !((Collection<?>) call(machine, "getControllers")).isEmpty()) return false;
            // Output optical hatches need a native workable controller; the furnace consumes inputs.
            try { if (Boolean.TRUE.equals(call(machine, "isTransmitter"))) return false; }
            catch (NoSuchMethodException notOptical) { /* Ordinary data/maintenance hatch. */ }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
    }

    public static synchronized boolean claimedByOther(LevelReader level, BlockPos pos, BlockPos core) {
        if (!(level instanceof Level world)) return false;
        MultiblockAlloyFurnaceCoreBlockEntity owner = owner(world, pos);
        return owner != null && !owner.getBlockPos().equals(core);
    }

    public static synchronized void bind(MultiblockAlloyFurnaceCoreBlockEntity core, List<BlockPos> parts) {
        Level level = core.getLevel();
        if (level == null || level.isClientSide) return;
        if (parts.isEmpty() && !OWNERS.containsKey(level)) return;
        Map<BlockPos, WeakReference<MultiblockAlloyFurnaceCoreBlockEntity>> leases = OWNERS.computeIfAbsent(level, ignored -> new HashMap<>());
        Set<BlockPos> wanted = new HashSet<>(parts);
        leases.entrySet().removeIf(entry -> entry.getValue().get() == null
                || entry.getValue().get() == core && !wanted.contains(entry.getKey()));
        for (BlockPos pos : parts) {
            if (claimedByOther(level, pos, core.getBlockPos()) || !accepts(level, pos)) continue;
            boolean attached = owner(level, pos) == core;
            leases.put(pos.immutable(), new WeakReference<>(core));
            if (!attached) refreshResearch(level, pos);
        }
    }

    private static void refreshResearch(LevelReader level, BlockPos pos) {
        try {
            Object machine = machine(level, pos);
            if (!isA(machine, "com.gregtechceu.gtceu.common.machine.multiblock.part.DataAccessHatchMachine")) return;
            // Same cache refresh used by DataAccessHatchMachine.addedToController; does not touch inventory.
            for (Class<?> type = machine.getClass(); type != null; type = type.getSuperclass()) {
                try {
                    Method rebuild = type.getDeclaredMethod("rebuildData", boolean.class);
                    if (rebuild.trySetAccessible()) rebuild.invoke(machine, false);
                    return;
                } catch (NoSuchMethodException next) { /* Check the native base class. */ }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            UselessMod.LOGGER.debug("Cannot refresh native GT research hatch at {}", pos, exception);
        }
    }

    public static synchronized void release(MultiblockAlloyFurnaceCoreBlockEntity core) {
        Map<BlockPos, WeakReference<MultiblockAlloyFurnaceCoreBlockEntity>> leases = OWNERS.get(core.getLevel());
        if (leases != null) leases.entrySet().removeIf(e -> e.getValue().get() == null || e.getValue().get() == core);
    }

    private static MultiblockAlloyFurnaceCoreBlockEntity owner(Level level, BlockPos pos) {
        Map<BlockPos, WeakReference<MultiblockAlloyFurnaceCoreBlockEntity>> leases = OWNERS.get(level);
        var reference = leases == null ? null : leases.get(pos);
        var owner = reference == null ? null : reference.get();
        return owner != null && !owner.isRemoved() && owner.isFormed()
                && level.hasChunkAt(owner.getBlockPos()) && level.getBlockEntity(owner.getBlockPos()) == owner ? owner : null;
    }

    /** Invoked only for GT's native part machines, before their native isFormed check. */
    public static synchronized boolean isFurnacePart(Object machine) {
        if (OWNERS.isEmpty()) return false;
        try {
            Level level = (Level) call(machine, "getLevel");
            BlockPos pos = (BlockPos) call(machine, "getPos");
            return level != null && !level.isClientSide && level.hasChunkAt(pos)
                    && owner(level, pos) != null && machine(level, pos) == machine && accepts(level, pos);
        } catch (ReflectiveOperationException | RuntimeException exception) { return false; }
    }

    public static Object machine(LevelReader level, BlockPos pos) throws ReflectiveOperationException {
        Object entity = level.getBlockEntity(pos);
        if (entity == null || !isA(entity, "com.gregtechceu.gtceu.api.machine.IMachineBlockEntity")) return null;
        return call(entity, "getMetaMachine");
    }
    public static boolean isA(Object object, String className) {
        if (object == null) return false;
        try { return Class.forName(className).isInstance(object); }
        catch (ClassNotFoundException | LinkageError absent) { return false; }
    }
    public static boolean isMaintenancePart(Object machine) {
        return isA(machine, "com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine")
                && isA(machine, "com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart");
    }
    public static Object call(Object target, String name) throws ReflectiveOperationException {
        return target.getClass().getMethod(name).invoke(target);
    }
}
