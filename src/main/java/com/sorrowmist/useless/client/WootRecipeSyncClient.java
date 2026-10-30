package com.sorrowmist.useless.client;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.network.WootRecipesPacket;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog;
import com.sorrowmist.useless.content.recipe.adapters.woot.WootRecipeAdapter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import java.util.*;
@Mod.EventBusSubscriber(modid=UselessMod.MODID,value=Dist.CLIENT)
public final class WootRecipeSyncClient {
    private static final List<AdvancedAlloyFurnaceRecipe> pending=new ArrayList<>();
    private static boolean receiving,dirty;
    public static void accept(WootRecipesPacket packet){
        if(packet.first()){pending.clear();receiving=true;}
        if(!receiving)return;
        if(pending.size()+packet.recipes().size()>16384)throw new IllegalArgumentException("Woot catalog too large");
        pending.addAll(packet.recipes());
        if(packet.last()){WootRecipeAdapter.acceptClient(pending);pending.clear();receiving=false;dirty=true;}
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){pending.clear();WootRecipeAdapter.acceptClient(List.of());receiving=false;dirty=false;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        var mc=Minecraft.getInstance();if(e.phase!=TickEvent.Phase.END||!dirty||mc.level==null)return;
        dirty=false;AlloyFurnaceRecipeCatalog.invalidate(mc.level);
        if(ModList.get().isLoaded("jei"))com.sorrowmist.useless.compat.jei.JEIPlugin.refreshAlloyFurnaceRecipes();
    }
}
