package com.sorrowmist.useless.client.render.ingot;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.BakedModelWrapper;

/** Preserve the resource pack's item transforms while routing only these ingots to their renderer. */
final class IngotEffectModel extends BakedModelWrapper<BakedModel> {
    IngotEffectModel(BakedModel original) { super(original); }
    @Override public boolean isCustomRenderer() { return true; }
    @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        originalModel.applyTransform(context, pose, leftHand);
        return this;
    }
}
