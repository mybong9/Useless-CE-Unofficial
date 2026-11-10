package com.sorrowmist.useless.mixin;

import com.sorrowmist.useless.content.items.BeefToolVariants;
import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import com.sorrowmist.uselesscore.api.enums.tool.ToolTypeMode;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class StaffWrenchTagMixin {
    @org.spongepowered.asm.mixin.Unique
    private static boolean useless$isWrenchTag(String namespace,String path){
        return (namespace.equals("forge") && (path.equals("tools/wrench")||path.equals("tools/wrenches")||path.equals("wrenches")))
                || (namespace.equals("c") && path.equals("tools/wrench"));
    }
    @Inject(method="is(Lnet/minecraft/tags/TagKey;)Z",at=@At("HEAD"),cancellable=true)
    private void useless$wrench(TagKey<Item> tag,CallbackInfoReturnable<Boolean> result){
        var id=tag.location();
        if(!useless$isWrenchTag(id.getNamespace(),id.getPath()))return;
        ItemStack stack=(ItemStack)(Object)this;
        if(BeefToolVariants.isBeafTool(stack))result.setReturnValue(CoreItemComponents.getOrDefault(stack,
                UselessComponents.CurrentToolTypeComponent.get(),ToolTypeMode.NONE_MODE)==ToolTypeMode.WRENCH_MODE);
    }
}

