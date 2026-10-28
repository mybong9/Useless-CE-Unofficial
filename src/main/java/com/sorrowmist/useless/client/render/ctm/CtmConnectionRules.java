package com.sorrowmist.useless.client.render.ctm;

import com.sorrowmist.useless.content.blocks.multiblock.DirectionalMultiblockPartBlock;
import com.sorrowmist.useless.content.blocks.multiblock.MultiblockAlloyFurnaceCoreBlock;
import com.sorrowmist.useless.content.blocks.multiblock.MultiblockPartBlock;
import com.sorrowmist.useless.content.blocks.multiblock.UselessCoilBlock;
import com.sorrowmist.useless.content.blocks.GlowPlasticBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class CtmConnectionRules {
    public static final int NONE = 0;
    public static final int FURNACE = 1;
    private static final int COIL_OFFSET = 100;
    private static final int PLASTIC_OFFSET = 1000;

    private CtmConnectionRules() {
    }

    public static int family(BlockState state) {
        if ((state.getBlock() instanceof com.sorrowmist.useless.content.blocks.multiblock.OmniversalForgePartBlock
                || state.getBlock() instanceof DirectionalMultiblockPartBlock)
                && !state.getValue(com.sorrowmist.useless.content.blocks.multiblock.OmniversalForgePartBlock.FORMED)) {
            return NONE;
        }
        return family(state.getBlock());
    }

    public static int family(Block block) {
        if (block instanceof DirectionalMultiblockPartBlock) {
            return 100_000+net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(
                    com.sorrowmist.useless.init.ModBlocks.OMNIVERSAL_FORGE_FRAME.get());
        }
        if (block instanceof com.sorrowmist.useless.content.blocks.multiblock.OmniversalForgePartBlock) {
            return 100_000 + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(block);
        }
        if (block instanceof GlowPlasticBlock plastic && plastic.hasConnectedTexture()) {
            return PLASTIC_OFFSET + plastic.getColor().ordinal() * 2
                    + (plastic.isGlowing() ? 1 : 0);
        }
        if (block instanceof UselessCoilBlock coil) {
            return COIL_OFFSET + coil.tier();
        }
        if (block instanceof MultiblockAlloyFurnaceCoreBlock
                || block instanceof DirectionalMultiblockPartBlock
                || block.getClass() == MultiblockPartBlock.class) {
            return FURNACE;
        }
        return NONE;
    }

    public static boolean connects(BlockState first, BlockState second) {
        int family = family(first);
        return family != NONE && family == family(second);
    }
}
