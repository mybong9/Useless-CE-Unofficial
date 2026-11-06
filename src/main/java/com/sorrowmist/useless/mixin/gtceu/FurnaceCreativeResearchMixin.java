package com.sorrowmist.useless.mixin.gtceu;

import com.sorrowmist.useless.compat.gtceu.GregTechParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Collection;

@Pseudo
@Mixin(targets="com.gregtechceu.gtceu.common.machine.multiblock.part.DataAccessHatchMachine",remap=false,priority=900)
public abstract class FurnaceCreativeResearchMixin {
    @Shadow(remap=false) public abstract boolean isCreative();

    @Inject(method="isRecipeAvailable(Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;Ljava/util/Collection;)Z",
            at=@At("HEAD"),cancellable=true,remap=false,require=0)
    private void useless$creativeResearch(@Coerce Object recipe,Collection<?> seen,CallbackInfoReturnable<Boolean> result){
        if(!isCreative())return;
        // Native optical traversal still checks the source controller, activity and cable connection.
        if(GregTechParts.isFurnacePart(this)||seen.stream().anyMatch(GregTechParts::isFurnacePart))
            result.setReturnValue(true);
    }
}
