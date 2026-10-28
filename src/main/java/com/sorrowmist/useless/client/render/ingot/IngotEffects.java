package com.sorrowmist.useless.client.render.ingot;

import com.sorrowmist.useless.UselessMod;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UselessMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class IngotEffects {
    static final Map<Item, IngotEffectRenderer.Material> MATERIALS = new HashMap<>();
    @SubscribeEvent public static void bake(ModelEvent.ModifyBakingResult event) {
        MATERIALS.clear(); // Also releases masks from the previous resource pack.
        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof com.sorrowmist.useless.content.items.IngotItem)) continue;
            var id = BuiltInRegistries.ITEM.getKey(item);
            var location = new ModelResourceLocation(id, "inventory");
            var original = event.getModels().get(location);
            if (original == null || original.isCustomRenderer()) continue;
            MATERIALS.put(item, new IngotEffectRenderer.Material(original));
            event.getModels().put(location, new IngotEffectModel(original));
        }
    }
    private IngotEffects() {}
}
