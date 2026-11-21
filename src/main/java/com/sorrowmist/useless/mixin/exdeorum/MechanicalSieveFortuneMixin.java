package com.sorrowmist.useless.mixin.exdeorum;

import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "thedarkcolour.exdeorum.blockentity.logic.SieveLogic", remap = false)
public abstract class MechanicalSieveFortuneMixin {
    @Shadow @Final private boolean mechanical;
    @Shadow protected ItemStack mesh;
    @Shadow private int fortune;

    @Inject(method = "getResultAmount", at = @At("HEAD"), require = 1)
    private void useless$refreshMeshFortune(CallbackInfoReturnable<Integer> callback) {
        if (!mechanical) return;
        int level = mesh.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE);
        var tag = mesh.getTag();
        if (tag != null) {
            var entries = tag.getList("Enchantments", Tag.TAG_COMPOUND);
            for (int index = 0; index < entries.size(); index++) {
                var entry = entries.getCompound(index);
                if ("minecraft:fortune".equals(entry.getString("id"))) {
                    level = Math.max(level, entry.getInt("lvl"));
                }
            }
        }
        fortune = Math.min(1024, Math.max(0, level));
    }
}
