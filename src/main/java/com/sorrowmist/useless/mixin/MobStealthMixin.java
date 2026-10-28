package com.sorrowmist.useless.mixin;

import com.sorrowmist.useless.event.EventHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Target events alone do not protect against retained targets or cancelled-event overrides. */
@Mixin(Mob.class)
public abstract class MobStealthMixin {
    @Shadow private LivingEntity target;

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void useless$rejectHiddenTarget(LivingEntity candidate, CallbackInfo ci) {
        if (candidate instanceof Player player && EventHandler.hasBeefAdvancedStealthItem(player)) {
            target = null;
            ci.cancel();
        }
    }

    @Inject(method = "getTarget", at = @At("RETURN"), cancellable = true)
    private void useless$hideRetainedTarget(CallbackInfoReturnable<LivingEntity> cir) {
        if (cir.getReturnValue() instanceof Player player && EventHandler.hasBeefAdvancedStealthItem(player)) {
            target = null;
            cir.setReturnValue(null);
        }
    }
}
