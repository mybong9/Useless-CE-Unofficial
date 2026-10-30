package com.sorrowmist.useless.compat;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.recipe.adapters.woot.WootRecipeAdapter;
import com.sorrowmist.useless.core.config.ConfigManager;
import com.sorrowmist.useless.network.WootRecipesPacket;
import com.sorrowmist.useless.init.ModNetwork;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
@Mod.EventBusSubscriber(modid=UselessMod.MODID)
public final class WootRecipeSync {
    @SubscribeEvent public static void sync(OnDatapackSyncEvent e){
        if(e.getPlayer()==null)WootRecipeAdapter.invalidate(e.getPlayerList().getServer().overworld());
        var players=e.getPlayer()!=null?List.of(e.getPlayer()):e.getPlayerList().getPlayers();
        if(players.isEmpty())return;
        var level=players.get(0).serverLevel();
        var recipes=ModList.get().isLoaded("woot_revived")&&ConfigManager.isRecipeConversionEnabled("woot_revived")?WootRecipeAdapter.recipes(level):List.<com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe>of();
        for(var player:players){
            if(recipes.isEmpty())ModNetwork.CHANNEL.sendToPlayer(player,new WootRecipesPacket(true,true,List.of()));
            for(int i=0;i<recipes.size();i+=16)ModNetwork.CHANNEL.sendToPlayer(player,new WootRecipesPacket(i==0,i+16>=recipes.size(),recipes.subList(i,Math.min(recipes.size(),i+16))));
        }
    }
}
