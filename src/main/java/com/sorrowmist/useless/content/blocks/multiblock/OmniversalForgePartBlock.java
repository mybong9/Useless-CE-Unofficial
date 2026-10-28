package com.sorrowmist.useless.content.blocks.multiblock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Formation is synchronized once per structural transition, not once per animation frame. */
public final class OmniversalForgePartBlock extends MultiblockPartBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    private static final VoxelShape LOW_PROFILE = Block.box(0,0,0,16,8,16);
    private final boolean lowProfile;
    public OmniversalForgePartBlock(Properties properties) {
        this(properties,false);
    }
    public OmniversalForgePartBlock(Properties properties,boolean lowProfile) {
        super(properties);
        this.lowProfile=lowProfile;
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }
    @Override
    public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return lowProfile ? LOW_PROFILE : super.getShape(state,level,pos,context);
    }
    @Override
    public boolean useShapeForLightOcclusion(BlockState state) { return lowProfile; }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }
}
