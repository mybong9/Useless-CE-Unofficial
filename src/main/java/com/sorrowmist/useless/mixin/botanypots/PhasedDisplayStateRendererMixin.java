package com.sorrowmist.useless.mixin.botanypots;

import net.darkhax.botanypots.data.displaystate.render.AgingDisplayStateRenderer;
import net.darkhax.botanypots.data.displaystate.render.TransitionalDisplayStateRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Displays the mature crop through both native Botany Pots 13 growth renderers. */
@Mixin(value = {AgingDisplayStateRenderer.class, TransitionalDisplayStateRenderer.class}, remap = false)
public class PhasedDisplayStateRendererMixin {
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private float useless$matureGrowth(float growth) { return 1.0F; }
}
