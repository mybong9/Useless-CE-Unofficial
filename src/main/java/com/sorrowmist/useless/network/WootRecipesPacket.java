package com.sorrowmist.useless.network;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.uselesscore.api.network.*;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import java.util.*;
/** Server-authoritative Woot catalog, bounded recipe chunks. */
public record WootRecipesPacket(boolean first,boolean last,List<AdvancedAlloyFurnaceRecipe> recipes) implements CoreCustomPacketPayload {
    public static final Type<WootRecipesPacket> TYPE=new Type<>(UselessMod.id("woot_recipes"));
    public WootRecipesPacket{recipes=List.copyOf(recipes);if(recipes.size()>16)throw new IllegalArgumentException("Woot recipe chunk too large");}
    public static final CoreStreamCodec<FriendlyByteBuf,WootRecipesPacket> CODEC=CoreStreamCodec.of((buf,p)->{
        buf.writeBoolean(p.first);buf.writeBoolean(p.last);buf.writeVarInt(p.recipes.size());
        for(var r:p.recipes)AdvancedAlloyFurnaceRecipe.STREAM_CODEC.encode(buf,r);
    },buf->{boolean first=buf.readBoolean(),last=buf.readBoolean();int size=buf.readVarInt();
        if(size<0||size>16)throw new IllegalArgumentException("Invalid Woot recipe chunk");
        List<AdvancedAlloyFurnaceRecipe> recipes=new ArrayList<>();for(int i=0;i<size;i++)recipes.add(AdvancedAlloyFurnaceRecipe.STREAM_CODEC.decode(buf));
        return new WootRecipesPacket(first,last,recipes);
    });
    @Override public Type<? extends CoreCustomPacketPayload> type(){return TYPE;}
    public static void handle(WootRecipesPacket packet,CorePacketContext ctx){ctx.enqueueWork(()->com.sorrowmist.useless.client.WootRecipeSyncClient.accept(packet));}
}
