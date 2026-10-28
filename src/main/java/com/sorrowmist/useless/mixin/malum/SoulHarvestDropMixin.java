package com.sorrowmist.useless.mixin.malum;


import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.useless.content.items.EndlessBeafItem;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.sammy.malum.core.handlers.SpiritHarvestHandler", remap = false)
public abstract class SoulHarvestDropMixin {
    @Inject(method = "spawnSpiritsOnDeath", at = @At("HEAD"), cancellable = true, remap = false)
    private static void uselessMod$disableBeefSoulDrops(
            LivingDeathEvent event, CallbackInfo callback) {
        if (isDisabledBeefDamage(event.getSource())) {
            callback.cancel();
        }
    }

    private static boolean isDisabledBeefDamage(DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player player)) {
            return false;
        }

        var stack = player.getMainHandItem();
        return stack.getItem() instanceof EndlessBeafItem
                && !CoreItemComponents.getOrDefault(stack, UselessComponents.BeefMalumSpiritEnabledComponent.get(), false);
    }
}
