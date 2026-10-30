package com.sorrowmist.useless.content.recipe.adapters.woot;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.*;

/** Baseline player-kill loot, no upgrades. Unknown conditions/functions are excluded, never bypassed. */
public final class WootLootTable {
    private final ResourceManager resources;
    private final Map<ResourceLocation,Map<ResourceLocation,WootFraction>> cache=new HashMap<>();
    public int unsupported;
    public WootLootTable(ResourceManager resources){this.resources=resources;}
    public Map<ResourceLocation,WootFraction> read(ResourceLocation id){return read(id,new HashSet<>());}
    private Map<ResourceLocation,WootFraction> read(ResourceLocation id,Set<ResourceLocation> visiting){
        if(cache.containsKey(id))return cache.get(id);
        if(!visiting.add(id)||visiting.size()>16)return Map.of();
        Map<ResourceLocation,WootFraction> out=new LinkedHashMap<>();
        try {
            var resource=resources.getResource(new ResourceLocation(id.getNamespace(),"loot_tables/"+id.getPath()+".json"));
            if(resource.isEmpty())return out;
            try(var reader=resource.get().openAsReader()){
                JsonObject table=JsonParser.parseReader(reader).getAsJsonObject();
                if(table.has("functions")) throw new UnsupportedOperationException("table functions");
                for(JsonElement value:array(table,"pools")) {
                    try {
                        JsonObject pool=value.getAsJsonObject();
                        if(pool.has("functions"))throw new UnsupportedOperationException("pool functions");
                        WootFraction multiplier=conditions(pool).multiply(number(pool.get("rolls"),WootFraction.ONE));
                        JsonArray entries=array(pool,"entries");
                        // Conditional entries alter the eligible-weight denominator dynamically.
                        if(entries.size()>1)for(JsonElement e:entries)if(e.getAsJsonObject().has("conditions"))
                            throw new UnsupportedOperationException("conditional weighted pool");
                        long total=0;for(JsonElement e:entries)total+=integer(e.getAsJsonObject(),"weight",1);
                        if(total<=0)continue;
                        for(JsonElement entry:array(pool,"entries")) {
                            JsonObject e=entry.getAsJsonObject();
                            try { merge(out,entry(e,visiting),multiplier.multiply(WootFraction.of(integer(e,"weight",1))).divide(WootFraction.of(total))); }
                            catch(UnsupportedOperationException bad){unsupported++;}
                        }
                    }catch(UnsupportedOperationException bad){unsupported++;}
                }
            }
        }catch(java.io.IOException|IllegalArgumentException bad){unsupported++;}
        finally{visiting.remove(id);}
        cache.put(id,Map.copyOf(out));return out;
    }
    private Map<ResourceLocation,WootFraction> entry(JsonObject e,Set<ResourceLocation> visiting){
        String type=text(e,"type");WootFraction factor=conditions(e);
        if(factor.n().signum()==0)return Map.of();
        Map<ResourceLocation,WootFraction> out=new LinkedHashMap<>();
        switch(type){
            case "minecraft:empty","empty" -> {return out;}
            case "minecraft:item","item" -> out.put(new ResourceLocation(text(e,"name")),WootFraction.ONE);
            case "minecraft:loot_table","loot_table" -> out.putAll(read(new ResourceLocation(text(e,"name")),visiting));
            case "minecraft:group","group" -> {
                for(JsonElement child:array(e,"children"))merge(out,entry(child.getAsJsonObject(),visiting),WootFraction.ONE);
            }
            default -> throw new UnsupportedOperationException("entry: "+type);
        }
        WootFraction count=WootFraction.ONE;
        for(JsonElement value:array(e,"functions")){
            if(!type.equals("minecraft:item")&&!type.equals("item"))throw new UnsupportedOperationException("non-item count functions");
            JsonObject function=value.getAsJsonObject();
            WootFraction apply=conditions(function);
            if(apply.n().signum()==0)continue;
            if(!apply.equals(WootFraction.ONE))throw new UnsupportedOperationException("random conditional loot function");
            switch(text(function,"function")){
                case "minecraft:set_count","set_count" -> {
                    WootFraction next=number(function.get("count"),WootFraction.ONE);
                    count=function.has("add")&&function.get("add").getAsBoolean()?count.add(next):next;
                }
                case "minecraft:looting_enchant","looting_enchant","minecraft:enchanted_count_increase","minecraft:explosion_decay","explosion_decay" -> { /* Looting zero; no explosion. */ }
                default -> throw new UnsupportedOperationException("loot function");
            }
        }
        factor=factor.multiply(count);Map<ResourceLocation,WootFraction> scaled=new LinkedHashMap<>();merge(scaled,out,factor);return scaled;
    }
    private WootFraction conditions(JsonObject object){
        WootFraction probability=WootFraction.ONE;
        for(JsonElement e:array(object,"conditions"))probability=probability.multiply(condition(e.getAsJsonObject()));
        return probability;
    }
    private WootFraction condition(JsonObject c){
        return switch(text(c,"condition")){
            case "minecraft:killed_by_player","killed_by_player","minecraft:survives_explosion","survives_explosion" -> WootFraction.ONE;
            case "minecraft:random_chance","random_chance","minecraft:random_chance_with_looting","random_chance_with_looting" -> number(c.get("chance"),WootFraction.ZERO);
            case "minecraft:entity_properties","entity_properties" -> entityPredicate(c);
            case "minecraft:inverted","inverted" -> WootFraction.ONE.add(condition(c.getAsJsonObject("term")).multiply(WootFraction.of(-1)));
            // Entity-dependent/fire/variant predicates are not assumed true for a generic shard.
            default -> throw new UnsupportedOperationException("loot condition");
        };
    }
    private static WootFraction entityPredicate(JsonObject condition){
        if(!text(condition,"entity").equals("this"))throw new UnsupportedOperationException("non-target entity predicate");
        JsonObject predicate=condition.getAsJsonObject("predicate");
        if(predicate==null||predicate.size()==0)return WootFraction.ONE;
        if(predicate.size()!=1||!predicate.has("flags"))throw new UnsupportedOperationException("entity variant predicate");
        JsonObject flags=predicate.getAsJsonObject("flags");
        if(flags.size()!=1||!flags.has("is_on_fire"))throw new UnsupportedOperationException("entity flags");
        // Woot baseline holds an unenchanted netherite sword, with no burning upgrade.
        return flags.get("is_on_fire").getAsBoolean()?WootFraction.ZERO:WootFraction.ONE;
    }
    private static WootFraction number(JsonElement value,WootFraction fallback){
        if(value==null)return fallback;
        if(value.isJsonPrimitive())return WootFraction.decimal(value.getAsString());
        JsonObject v=value.getAsJsonObject();String type=text(v,"type");
        return switch(type){
            case "minecraft:constant","constant" -> number(v.get("value"),fallback);
            case "minecraft:uniform","uniform" -> {
                WootFraction min=number(v.get("min"),WootFraction.ZERO),max=number(v.get("max"),WootFraction.ZERO);
                if(min.n().signum()<0){
                    // Vanilla discards nonpositive item counts/rolls. E.g. -1..1 yields 1/3,
                    // not zero: average the positive part of the integer distribution.
                    if(!min.d().equals(java.math.BigInteger.ONE)||!max.d().equals(java.math.BigInteger.ONE))throw new UnsupportedOperationException("fractional negative range");
                    if(max.n().signum()<=0)yield WootFraction.ZERO;
                    WootFraction sum=max.multiply(max.add(WootFraction.ONE)).divide(WootFraction.of(2));
                    yield sum.divide(max.add(min.multiply(WootFraction.of(-1))).add(WootFraction.ONE));
                }
                yield min.add(max).divide(WootFraction.of(2));
            }
            case "minecraft:binomial","binomial" -> number(v.get("n"),WootFraction.ZERO).multiply(number(v.get("p"),WootFraction.ZERO));
            default -> throw new UnsupportedOperationException("number provider");
        };
    }
    private static void merge(Map<ResourceLocation,WootFraction> out,Map<ResourceLocation,WootFraction> in,WootFraction factor){
        in.forEach((key,value)->out.merge(key,value.multiply(factor),WootFraction::add));
    }
    private static JsonArray array(JsonObject o,String key){return o.has(key)?o.getAsJsonArray(key):new JsonArray();}
    private static String text(JsonObject o,String key){return o.has(key)?o.get(key).getAsString():"";}
    private static int integer(JsonObject o,String key,int fallback){return o.has(key)?o.get(key).getAsInt():fallback;}
}
