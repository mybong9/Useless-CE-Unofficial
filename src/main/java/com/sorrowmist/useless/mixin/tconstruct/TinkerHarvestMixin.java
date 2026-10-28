package com.sorrowmist.useless.mixin.tconstruct;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic",remap=false)
public abstract class TinkerHarvestMixin {
    @Inject(method="handleBlockBreak",at=@At("HEAD"),cancellable=true,require=0)
    private static void chainNativeTool(ItemStack stack,BlockPos pos,Player player,CallbackInfoReturnable<Boolean> cir) {
        if (com.sorrowmist.useless.compat.tconstruct.TinkersChainMining.tryChain(stack,pos,player)) cir.setReturnValue(true);
    }
}
