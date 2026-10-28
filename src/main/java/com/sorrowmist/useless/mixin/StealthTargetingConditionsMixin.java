package com.sorrowmist.useless.mixin;

import com.sorrowmist.useless.event.EventHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetingConditions.class)
public class StealthTargetingConditionsMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void useless$rejectHiddenPlayer(LivingEntity attacker, LivingEntity candidate,
            CallbackInfoReturnable<Boolean> cir) {
        if (candidate instanceof Player player && EventHandler.hasBeefAdvancedStealthItem(player))
            cir.setReturnValue(false);
    }
}
