package com.sorrowmist.useless.content.blocks.multiblock;

import com.sorrowmist.useless.content.blockentities.multiblock.MultiblockAlloyFurnaceCoreBlockEntity;
import com.sorrowmist.useless.content.blockentities.multiblock.MePatternAssemblyBlockEntity;
import com.sorrowmist.useless.content.blockentities.multiblock.PassiveCraftingHatchBlockEntity;
import com.sorrowmist.useless.init.ModBlocks;
import com.sorrowmist.useless.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Canonical 80x32x80 template shared by validation, preview and automatic construction. */
public final class OmniversalAlloyFurnaceStructure {
    public static final int WIDTH = 80;
    public static final int MIN_X = -39;
    public static final int MIN_Z = -38;
    public static final int HEIGHT = 32;
    public static final BlockPos PREFERRED_PATTERN_ASSEMBLY_POS = new BlockPos(-1, 0, 0);
    public static final BlockPos PREFERRED_MOLD_HUB_POS = new BlockPos(1, 0, 0);

    private static final List<Entry> ENTRIES = createEntries();
    public static final int COIL_COUNT = count(Part.COIL);
    public static final int FUNCTIONAL_CASING_COUNT = count(Part.INTERFACE);
    public static final int CASING_COUNT = count(Part.CASING);

    private static int count(Part part) {
        return (int) ENTRIES.stream().filter(e -> e.part() == part).count();
    }

    private OmniversalAlloyFurnaceStructure() {
    }

    public enum Part {
        CORE,
        CASING,
        INTERFACE,
        COIL,
        COPPER,
        TRIGRAM_QIAN,
        TRIGRAM_DUI,
        TRIGRAM_LI,
        TRIGRAM_ZHEN,
        TRIGRAM_KUN,
        TRIGRAM_GEN,
        TRIGRAM_KAN,
        TRIGRAM_XUN,
        FRAME,
        TRIM,
        VENT,
        RESONATOR,
        AIR
    }

    public enum FunctionalPart {
        PATTERN_ASSEMBLY,
        MOLD_HUB,
        PASSIVE_HATCH
    }

    public record Entry(BlockPos localPos, Part part) {
        public BlockPos worldPos(BlockPos corePos, Direction facing) {
            return toWorld(corePos, facing, localPos);
        }
    }

    public record Mismatch(BlockPos worldPos, Part expected, BlockState actual) {
    }

    public record ValidationResult(boolean valid, int coilTier,
                                   @Nullable BlockPos patternAssemblyPos,
                                   @Nullable BlockPos moldHubPos,
                                   @Nullable BlockPos passiveHatchPos,
                                   List<Mismatch> mismatches,
                                   List<BlockPos> assemblies,List<BlockPos> moldHubs,List<BlockPos> passiveHatches) {
        public ValidationResult(boolean valid,int coilTier,BlockPos assembly,BlockPos hub,BlockPos hatch,List<Mismatch> mismatches) {
            this(valid,coilTier,assembly,hub,hatch,mismatches,List.of(),List.of(),List.of());
        }
        public ValidationResult {
            patternAssemblyPos = patternAssemblyPos == null ? null : patternAssemblyPos.immutable();
            moldHubPos = moldHubPos == null ? null : moldHubPos.immutable();
            passiveHatchPos = passiveHatchPos == null ? null : passiveHatchPos.immutable();
            mismatches = List.copyOf(mismatches);
            assemblies=List.copyOf(assemblies);moldHubs=List.copyOf(moldHubs);passiveHatches=List.copyOf(passiveHatches);
        }
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static BlockPos toWorld(BlockPos corePos, Direction facing, BlockPos localPos) {
        Direction horizontalFacing = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        Direction right = horizontalFacing.getClockWise();
        Direction back = horizontalFacing.getOpposite();
        return corePos.relative(right, localPos.getX())
                .above(localPos.getY())
                .relative(back, localPos.getZ());
    }

    public static ValidationResult validate(LevelReader level, BlockPos corePos, Direction facing) {
        List<Mismatch> mismatches = new ArrayList<>();
        int coilTier = 0;
        int assemblyCount = 0;
        int moldHubCount = 0;
        int passiveHatchCount = 0;
        BlockPos assemblyPos = null;
        BlockPos moldHubPos = null;
        BlockPos passiveHatchPos = null;
        List<BlockPos> assemblies=new ArrayList<>(),hubs=new ArrayList<>(),hatches=new ArrayList<>();

        for (Entry entry : ENTRIES) {
            BlockPos worldPos = entry.worldPos(corePos, facing);
            if (!isChunkLoaded(level, worldPos)) {
                return new ValidationResult(false, 0, null, null, null, List.of());
            }
            BlockState state = level.getBlockState(worldPos);
            if (entry.part == Part.COIL) {
                if (state.getBlock() instanceof UselessCoilBlock coil) {
                    if (coilTier == 0) {
                        coilTier = coil.tier();
                    } else if (coilTier != coil.tier()) {
                        mismatches.add(new Mismatch(worldPos, Part.COIL, state));
                    }
                } else {
                    mismatches.add(new Mismatch(worldPos, Part.COIL, state));
                }
                continue;
            }
            if (entry.part == Part.CASING) {
                if (!matches(Part.CASING, state)) {
                    mismatches.add(new Mismatch(worldPos,Part.CASING,state));
                }
                continue;
            }
            if (entry.part == Part.INTERFACE) {
                FunctionalPart functionalPart = functionalPart(state);
                if (functionalPart == null) {
                    if (!matches(Part.INTERFACE, state)) {
                        mismatches.add(new Mismatch(worldPos, Part.INTERFACE, state));
                    }
                    continue;
                }
                switch (functionalPart) {
                    case PATTERN_ASSEMBLY -> {
                        assemblyCount++;
                        assemblies.add(worldPos.immutable());
                        if (assemblyCount == 1) assemblyPos = worldPos.immutable();
                    }
                    case MOLD_HUB -> {
                        moldHubCount++;
                        hubs.add(worldPos.immutable());
                        if (moldHubCount == 1) moldHubPos = worldPos.immutable();
                    }
                    case PASSIVE_HATCH -> {
                        passiveHatchCount++;
                        hatches.add(worldPos.immutable());
                        if (passiveHatchCount == 1) passiveHatchPos = worldPos.immutable();
                    }
                }
                if (isClaimedByAnotherCore(level,corePos,worldPos,functionalPart)) {
                    mismatches.add(new Mismatch(worldPos,Part.INTERFACE,state));
                }
                continue;
            }
            if (!matches(entry.part, state)) {
                mismatches.add(new Mismatch(worldPos, entry.part, state));
            }
        }
        boolean functionalCountsValid = isFunctionalPartCountsValid(
                assemblyCount, moldHubCount, passiveHatchCount);
        // 缺功能方块时外壳本身全都合法，循环里不会产生 Mismatch，结构却判定失效。
        // 补一条指向首选位的记录，否则「未成型但没有可高亮的方块」。
        if (assemblyCount == 0) {
            BlockPos pos = toWorld(corePos, facing, PREFERRED_PATTERN_ASSEMBLY_POS);
            mismatches.add(new Mismatch(pos, Part.CASING, level.getBlockState(pos)));
        }
        if (moldHubCount == 0) {
            BlockPos pos = toWorld(corePos, facing, PREFERRED_MOLD_HUB_POS);
            mismatches.add(new Mismatch(pos, Part.CASING, level.getBlockState(pos)));
        }
        if (assemblyPos != null && isClaimedByAnotherCore(level, corePos, assemblyPos,
                FunctionalPart.PATTERN_ASSEMBLY)) {
            mismatches.add(new Mismatch(assemblyPos, Part.CASING, level.getBlockState(assemblyPos)));
        }
        if (passiveHatchPos != null && isClaimedByAnotherCore(level, corePos, passiveHatchPos,
                FunctionalPart.PASSIVE_HATCH)) {
            mismatches.add(new Mismatch(passiveHatchPos, Part.CASING, level.getBlockState(passiveHatchPos)));
        }
        return new ValidationResult(mismatches.isEmpty() && functionalCountsValid && coilTier > 0,
                coilTier, assemblyPos, moldHubPos, passiveHatchPos, mismatches,assemblies,hubs,hatches);
    }

    public static boolean isFunctionalPartCountsValid(
            int patternAssemblyCount, int moldHubCount, int passiveHatchCount) {
        return OmniversalPortPolicy.validCounts(patternAssemblyCount,moldHubCount,passiveHatchCount);
    }

    static boolean isPassiveHatchCountValid(int count) {
        return count >= 0;
    }

    @Nullable
    public static FunctionalPart functionalPart(BlockState state) {
        if (state.is(ModBlocks.ME_PATTERN_ASSEMBLY.get())) return FunctionalPart.PATTERN_ASSEMBLY;
        if (state.is(ModBlocks.OMNIVERSAL_MOLD_HUB.get())) return FunctionalPart.MOLD_HUB;
        if (state.is(ModBlocks.PASSIVE_CRAFTING_HATCH.get())) return FunctionalPart.PASSIVE_HATCH;
        return null;
    }

    private static boolean isClaimedByAnotherCore(
            LevelReader level, BlockPos corePos, BlockPos partPos, FunctionalPart part) {
        if (!(level instanceof Level actualLevel)) {
            return false;
        }
        return switch (part) {
            case PATTERN_ASSEMBLY -> actualLevel.getBlockEntity(partPos)
                    instanceof MePatternAssemblyBlockEntity assembly
                    && assembly.isClaimedByOtherController(corePos);
            case PASSIVE_HATCH -> actualLevel.getBlockEntity(partPos)
                    instanceof PassiveCraftingHatchBlockEntity hatch
                    && hatch.isClaimedByOtherController(corePos);
            case MOLD_HUB -> actualLevel.getBlockEntity(partPos)
                    instanceof com.sorrowmist.useless.content.blockentities.multiblock.OmniversalMoldHubBlockEntity hub
                    && hub.isClaimedByOtherController(corePos);
        };
    }

    public static void notifyNearbyCores(Level level, BlockPos changedPos) {
        if (level.isClientSide) return;
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return;
        // Inspect existing block entities in loaded chunks, never scan a 63x19x63 volume
        // or force-load chunks while a structure is being placed/unloaded.
        int cx = SectionPos.blockToSectionCoord(changedPos.getX());
        int cz = SectionPos.blockToSectionCoord(changedPos.getZ());
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                var chunk = server.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                for (var entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof MultiblockAlloyFurnaceCoreBlockEntity core) {
                        BlockPos p = core.getBlockPos();
                        if (Math.abs(p.getX() - changedPos.getX()) <= 41
                                && Math.abs(p.getZ() - changedPos.getZ()) <= 41
                                && changedPos.getY() >= p.getY() && changedPos.getY() < p.getY() + HEIGHT) {
                            core.requestStructureValidation();
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean isChunkLoaded(LevelReader level, BlockPos pos) {
        return level.hasChunk(SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
    }

    /** Shared by runtime validation and recipe-viewer replacement lists. */
    public static boolean matches(Part part, BlockState state) {
        return switch (part) {
            case CORE -> state.is(ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get());
            case CASING -> state.is(ModTags.OMNIVERSAL_FURNACE_CASINGS) && functionalPart(state)==null;
            case INTERFACE -> functionalPart(state)!=null || state.is(ModBlocks.OMNIVERSAL_FORGE_FRAME.get()) || state.is(ModTags.OMNIVERSAL_FURNACE_CASINGS);
            case AIR -> state.isAir();
            case COIL -> state.getBlock() instanceof UselessCoilBlock;
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
        };
    }

    private static List<Entry> createEntries() {
        List<Entry> entries = new ArrayList<>();
        String resource = "/data/useless_mod/structures/omniversal_furnace.csv";
        try (var input = OmniversalAlloyFurnaceStructure.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing furnace blueprint");
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                    input, java.nio.charset.StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null;) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] fields = line.split(",");
                    entries.add(new Entry(new BlockPos(Integer.parseInt(fields[0]),
                            Integer.parseInt(fields[1]), Integer.parseInt(fields[2])), Part.valueOf(fields[3])));
                }
            }
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Cannot read furnace blueprint", error);
        }
        long ports = entries.stream().filter(e -> e.part()==Part.INTERFACE).count();
        if (ports != OmniversalPortPolicy.CAPACITY || entries.stream().anyMatch(e ->
                e.part()==Part.INTERFACE && !OmniversalPortPolicy.isPort(e.localPos().getX(),e.localPos().getY(),e.localPos().getZ())))
            throw new IllegalStateException("Furnace must have twelve bottom ports beside the terminal");
        return List.copyOf(entries);
    }
}
