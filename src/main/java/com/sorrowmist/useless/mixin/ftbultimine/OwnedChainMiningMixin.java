package com.sorrowmist.useless.mixin.ftbultimine;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="dev.ftb.mods.ftbultimine.FTBUltimine",remap=false)
public abstract class OwnedChainMiningMixin {
    @Inject(method="canUltimine",at=@At("HEAD"),cancellable=true,require=0)
    private void ownStaffMining(Player player,CallbackInfoReturnable<Boolean> cir) {
        var stack=player.getMainHandItem();
        if (stack.getItem() instanceof com.sorrowmist.useless.content.items.EndlessBeafItem
                || com.sorrowmist.useless.compat.tconstruct.TinkersChainMining.eligible(stack)) cir.setReturnValue(false);
    }
}
