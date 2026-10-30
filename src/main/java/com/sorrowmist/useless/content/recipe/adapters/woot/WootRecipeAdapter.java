package com.sorrowmist.useless.content.recipe.adapters.woot;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.fluid.DataComponentFluidIngredient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import java.lang.reflect.*;
import java.util.*;

/** Optional reflective bridge; server recipes are authoritative for configuration and datapack loot. */
public final class WootRecipeAdapter implements IRecipeAdapter<WootSyntheticRecipe> {
    private record Cached(long generation,List<AdvancedAlloyFurnaceRecipe> recipes){}
    private static final Map<Object,Cached> CACHE=new WeakHashMap<>();
    private static volatile List<AdvancedAlloyFurnaceRecipe> clientRecipes=List.of();
    @Override public String sourceId(){return "woot_revived";}
    @Override public Class<WootSyntheticRecipe> getRecipeClass(){return WootSyntheticRecipe.class;}
    @Override public ItemStack getMoldItem(){return null;}
    public static synchronized void invalidate(Level level){CACHE.remove(level.getRecipeManager());}
    public static void acceptClient(List<AdvancedAlloyFurnaceRecipe> recipes){clientRecipes=List.copyOf(recipes);}
    @Override public List<CoreRecipeHolder<WootSyntheticRecipe>> getGeneratedRecipes(Level level){
        if(level==null)return List.of();
        return recipes(level).stream().map(r->new CoreRecipeHolder<>(r.id(),new WootSyntheticRecipe(r))).toList();
    }
    public static synchronized List<AdvancedAlloyFurnaceRecipe> recipes(Level level){
        if(level.isClientSide)return clientRecipes;
        long generation=0;Object manager=level.getRecipeManager();
        Cached old=CACHE.get(manager);if(old!=null)return old.recipes;
        List<AdvancedAlloyFurnaceRecipe> result=new ArrayList<>();
        List<Recipe<?>> dyes=new ArrayList<>();int skipped=0;
        for(Recipe<?> source:level.getRecipeManager().getRecipes()){
            String name=source.getClass().getName();if(!name.startsWith("wootrevived.woot.recipes."))continue;
            try{
                switch(source.getClass().getSimpleName()){
                    case "StygianAnvilRecipe" -> {
                        Ingredient baseInput=(Ingredient)call(source,"getBase");
                        if(Arrays.stream(baseInput.getItems()).anyMatch(stack->stack.getItem()==item("mob_shard").getItem())){
                            addProgrammedAnvilRecipes(source,result);continue;
                        }
                        List<CountedIngredient> in=new ArrayList<>();add(in,baseInput,1);
                        for(String method:List.of("getFirstComplementary","getSecondComplementary","getThirdComplementary","getFourthComplementary")){
                            Optional<?> extra=(Optional<?>)call(source,method);if(extra.isPresent())add(in,(Ingredient)extra.get(),1);
                        }
                        result.add(recipe(AdapterUtils.convertedId(source.getId()),in,List.of(),withRemainders(in,((ItemStack)call(source,"getOutput")).copy()),List.of(),0,20,anvilMolds()));
                    }
                    case "ItemInfuserRecipe" -> {
                        List<CountedIngredient> in=new ArrayList<>();add(in,(Ingredient)call(source,"getIngredient"),1);
                        Optional<?> extra=(Optional<?>)call(source,"getAugment");if(extra.isPresent())add(in,(Ingredient)extra.get(),1);
                        long energy=num(call(source,"getEnergy"));
                        result.add(recipe(AdapterUtils.convertedId(source.getId()),in,fluids((FluidStack)call(source,"getFluid"),1),withRemainders(in,((ItemStack)call(source,"getOutput")).copy()),List.of(),energy,ticks(energy,config("ItemInfuserConfig","ENERGY_PROCESS_TRANSFER")),molds("item_infuser")));
                    }
                    case "FluidInfuserRecipe" -> {
                        List<CountedIngredient> in=new ArrayList<>();add(in,(Ingredient)call(source,"getIngredient"),1);
                        long energy=num(call(source,"getEnergy"));
                        result.add(recipe(AdapterUtils.convertedId(source.getId()),in,fluids((FluidStack)call(source,"getInputFluid"),1),withRemainders(in),List.of(((FluidStack)call(source,"getOutputFluid")).copy()),energy,ticks(energy,config("FluidInfuserConfig","ENERGY_PROCESS_TRANSFER")),molds("fluid_infuser")));
                    }
                    case "DyeLiquifierRecipe" -> dyes.add(source);
                    // Intentionally no EnchantedLiquifierRecipe/enchanted-book extraction.
                }
            }catch(ReflectiveOperationException|RuntimeException bad){skipped++;UselessMod.LOGGER.debug("Cannot convert Woot recipe {}",source.getId(),bad);}
        }
        try{
            // User-requested replacement. Only ordinary books, exactly twenty buckets.
            long energy=config("EnchantedLiquifierConfig","PER_ENCHANT_ENERGY");
            result.add(recipe(id("book_enchanted_fluid"),List.of(new CountedIngredient(Ingredient.of(Items.BOOK),1)),List.of(),List.of(),List.of(fluid("enchanted_fluid",20_000)),energy,ticks(energy,config("EnchantedLiquifierConfig","ENERGY_PROCESS_TRANSFER")),molds("enchanted_liquifier")));
            addDyeRecipes(dyes,result);
        }catch(ReflectiveOperationException|RuntimeException bad){UselessMod.LOGGER.warn("Cannot read Woot fluid conversion configuration",bad);}
        try{addFactoryRecipes(level,result);}catch(ReflectiveOperationException|RuntimeException bad){UselessMod.LOGGER.warn("Cannot read Woot factory API",bad);}
        List<AdvancedAlloyFurnaceRecipe> recipes=List.copyOf(result);CACHE.put(manager,new Cached(generation,recipes));
        UselessMod.LOGGER.info("Woot furnace recipes: {} generated; {} machine recipes skipped",recipes.size(),skipped);return recipes;
    }
    private static List<Ingredient> anvilMolds(){List<Ingredient> molds=molds("stygian_anvil");molds.add(Ingredient.of(item("stygian_hammer")));return molds;}
    private static ItemStack programmedShard(CompoundTag mobTag)throws ReflectiveOperationException{
        ItemStack shard=item("mob_shard");CompoundTag tag=new CompoundTag();tag.put("Mob",mobTag.copy());tag.putInt("Kills",config("MobShardConfig","NUM_OF_KILLS"));shard.setTag(tag);return shard;
    }
    private static void addProgrammedAnvilRecipes(Recipe<?> source,List<AdvancedAlloyFurnaceRecipe> result)throws ReflectiveOperationException{
        Class<?> base=Class.forName("wootrevived.api.WootFactoryMob");
        Collection<?> mobs=(Collection<?>)Class.forName("wootrevived.woot.registries.WootFactoryMobsRegistry").getMethod("getFactoryMobValues").invoke(null);
        var outputMethod=Class.forName("wootrevived.woot.blocks.fake_spawner.FakeSpawnerBlockEntity").getMethod("getItemStack",CompoundTag.class);
        for(Object mob:mobs){
            if(Boolean.TRUE.equals(call(mob,"isBlacklisted")))continue;
            if(Arrays.stream(mob.getClass().getMethods()).anyMatch(m->m.getName().equals("saveTag")&&m.getDeclaringClass()!=base))continue;
            ResourceLocation entityId=ForgeRegistries.ENTITY_TYPES.getKey((net.minecraft.world.entity.EntityType<?>)call(mob,"getEntityType"));
            if(entityId==null)continue;
            CompoundTag tag=new CompoundTag();tag.putString("id",entityId.toString());
            List<CountedIngredient> in=new ArrayList<>();in.add(new CountedIngredient(StrictNBTIngredient.of(programmedShard(tag)),1));
            for(String method:List.of("getFirstComplementary","getSecondComplementary","getThirdComplementary","getFourthComplementary")){
                Optional<?> extra=(Optional<?>)call(source,method);if(extra.isPresent())add(in,(Ingredient)extra.get(),1);
            }
            ItemStack output=(ItemStack)outputMethod.invoke(null,tag.copy());
            ResourceLocation id=id("anvil/"+source.getId().getNamespace()+"/"+source.getId().getPath()+"/"+entityId.getNamespace()+"/"+entityId.getPath());
            result.add(recipe(id,in,List.of(),withRemainders(in,output),List.of(),0,20,anvilMolds()));
        }
    }
    private static void addDyeRecipes(List<Recipe<?>> sources,List<AdvancedAlloyFurnaceRecipe> result)throws ReflectiveOperationException{
        sources.sort(java.util.Comparator.comparing(r->r.getId().toString()));
        String[] getters={"getRed","getYellow","getBlue","getWhite"};Recipe<?>[] pure=new Recipe<?>[4];
        for(Recipe<?> source:sources)for(int c=0;c<4;c++){
            boolean single=num(call(source,getters[c]))>0;
            for(int j=0;j<4;j++)if(j!=c&&num(call(source,getters[j]))!=0)single=false;
            if(single&&pure[c]==null)pure[c]=source;
        }
        long unit=config("DyeLiquifierConfig","COLOR_PRODUCE_AMOUNT"),amount=config("DyeLiquifierConfig","PURE_DYE_PRODUCE_AMOUNT");
        if(unit<=0||amount<=0)throw new IllegalArgumentException("Invalid dye configuration");
        for(Recipe<?> source:sources)try{
            long count=1;long[] colors=new long[4];
            for(int c=0;c<4;c++){
                colors[c]=num(call(source,getters[c]));
                if(colors[c]<0)throw new IllegalArgumentException("Negative dye");
                if(colors[c]>0){long d=unit/AdapterUtils.gcd(unit,colors[c]);count=Math.multiplyExact(count/AdapterUtils.gcd(count,d),d);}
            }
            long maximum=0;for(long color:colors)maximum=Math.max(maximum,Math.multiplyExact(color,count));
            if(maximum==0)continue;
            List<CountedIngredient> in=new ArrayList<>();add(in,(Ingredient)call(source,"getIngredient"),count);
            long energy=Math.multiplyExact(num(call(source,"getEnergy")),count);
            for(int c=0;c<4;c++){
                long deficit=maximum-Math.multiplyExact(colors[c],count);if(deficit==0)continue;
                if(pure[c]==null)throw new IllegalArgumentException("Missing pure color source");
                long produced=num(call(pure[c],getters[c]));
                if(deficit%produced!=0)throw new IllegalArgumentException("Dye balance not representable");
                long needed=deficit/produced;add(in,(Ingredient)call(pure[c],"getIngredient"),needed);
                energy=Math.addExact(energy,Math.multiplyExact(num(call(pure[c],"getEnergy")),needed));
            }
            int output=Math.toIntExact(Math.multiplyExact(maximum/unit,amount));
            result.add(recipe(AdapterUtils.convertedId(source.getId()),in,List.of(),withRemainders(in),List.of(fluid("pure_dye_fluid",output)),energy,ticks(energy,config("DyeLiquifierConfig","ENERGY_PROCESS_TRANSFER")),molds("dye_liquifier")));
        }catch(RuntimeException bad){UselessMod.LOGGER.debug("Unsupported Woot dye combination {}",source.getId(),bad);}
    }
    private static void addFactoryRecipes(Level level,List<AdvancedAlloyFurnaceRecipe> result)throws ReflectiveOperationException{
        Class<?> registry=Class.forName("wootrevived.woot.registries.WootFactoryMobsRegistry");
        Class<?> base=Class.forName("wootrevived.api.WootFactoryMob");
        Collection<?> mobs=(Collection<?>)registry.getMethod("getFactoryMobValues").invoke(null);
        WootLootTable tables=new WootLootTable(level.getServer().getResourceManager());int skipped=0;
        for(Object mob:mobs)try{
            if(Boolean.TRUE.equals(call(mob,"isBlacklisted"))||Boolean.TRUE.equals(call(mob,"isSimulationDisabled")))continue;
            // Dynamic mob callbacks/variant data cannot be replaced by unconditional vanilla loot.
            String mobClass=mob.getClass().getName();
            boolean knownVanilla=Set.of("wootrevived.woot.drops.mobs.CreeperMob","wootrevived.woot.drops.mobs.SheepMob", "wootrevived.woot.drops.mobs.WitherMob", "wootrevived.woot.drops.mobs.EnderDragonMob").contains(mobClass);
            if(!knownVanilla&&Arrays.stream(mob.getClass().getMethods()).anyMatch(m->Set.of("modifyDrops","saveTag","loadEntity").contains(m.getName())&&m.getDeclaringClass()!=base)){skipped++;continue;}
            var type=(net.minecraft.world.entity.EntityType<?>)call(mob,"getEntityType");
            ResourceLocation entityId=ForgeRegistries.ENTITY_TYPES.getKey(type);if(entityId==null)continue;
            Map<ResourceLocation,WootFraction> expected=tables.read(type.getDefaultLootTable());
            expected=new LinkedHashMap<>(expected);
            switch(mobClass){
                case "wootrevived.woot.drops.mobs.SheepMob" -> {
                    String[] colors={"white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"};
                    for(String color:colors)expected.put(new ResourceLocation("minecraft",color+"_wool"),WootFraction.ONE.divide(WootFraction.of(16)));
                }
                case "wootrevived.woot.drops.mobs.WitherMob" -> {
                    // Native custom-death loot supplies one star; Woot's callback adds a rose.
                    expected.put(new ResourceLocation("minecraft:nether_star"),WootFraction.ONE);
                    expected.merge(new ResourceLocation("minecraft:wither_rose"),WootFraction.ONE,WootFraction::add);
                }
                case "wootrevived.woot.drops.mobs.EnderDragonMob" -> {
                    expected.merge(new ResourceLocation("minecraft:dragon_egg"),WootFraction.ONE,WootFraction::add);
                    expected.merge(new ResourceLocation("minecraft:dragon_breath"),WootFraction.of(16),WootFraction::add);
                }
            }
            expected.entrySet().removeIf(e->e.getValue().n().signum()<=0||!ForgeRegistries.ITEMS.containsKey(e.getKey()));
            if(expected.isEmpty())continue;
            long operations=WootFraction.batch(expected.values());
            List<ItemStack> outputs=new ArrayList<>();for(var e:expected.entrySet())outputs.add(new ItemStack(ForgeRegistries.ITEMS.getValue(e.getKey()),e.getValue().count(operations)));
            CompoundTag mobTag=new CompoundTag();mobTag.putString("id",entityId.toString());
            ItemStack shard=programmedShard(mobTag);
            List<Ingredient> molds=molds("heart");molds.add(StrictNBTIngredient.of(shard));
            List<CountedIngredient> in=new ArrayList<>();
            @SuppressWarnings("unchecked") List<ItemStack> imports=(List<ItemStack>)base.getMethod("getImportItems",CompoundTag.class,HolderLookup.Provider.class).invoke(mob,mobTag,level.registryAccess());
            for(ItemStack input:imports)in.add(new CountedIngredient(Ingredient.of(input.copyWithCount(1)),Math.multiplyExact(input.getCount(),operations)));
            List<LongSizedFluidIngredient> fluids=new ArrayList<>();
            @SuppressWarnings("unchecked") List<FluidStack> importedFluids=(List<FluidStack>)base.getMethod("getImportFluids",CompoundTag.class,HolderLookup.Provider.class).invoke(mob,mobTag,level.registryAccess());
            for(FluidStack f:importedFluids)fluids.addAll(fluids(f,operations));
            long fuel=num(call(mob,"getVitalityFuelCost"));if(fuel>0)fluids.addAll(fluids(fluid("vitality_fuel_fluid",Math.toIntExact(fuel)),operations));
            result.add(recipe(id("factory/"+entityId.getNamespace()+"/"+entityId.getPath()),in,fluids,outputs,List.of(),0,Math.toIntExact(Math.multiplyExact(Math.max(1,num(call(mob,"getSpawnTickRate"))),operations)),molds));
        }catch(ReflectiveOperationException|RuntimeException bad){skipped++;UselessMod.LOGGER.debug("Unsupported Woot factory mob {}",mob,bad);}
        UselessMod.LOGGER.info("Woot factory: {} custom/unsupported mobs skipped; {} unsupported loot branches excluded",skipped,tables.unsupported);
    }
    private static AdvancedAlloyFurnaceRecipe recipe(ResourceLocation id,List<CountedIngredient> in,List<LongSizedFluidIngredient> fluids,List<ItemStack> out,List<FluidStack> fout,long energy,int time,List<Ingredient> molds){
        if(out.isEmpty()&&fout.isEmpty())throw new IllegalArgumentException("No Woot output");
        return new AdvancedAlloyFurnaceRecipe(id,in,fluids,List.of(),out,fout,List.of(),energy,Math.max(1,time),Ingredient.EMPTY,0,molds,AlloyFurnaceMode.NORMAL,0);
    }
    private static List<ItemStack> withRemainders(List<CountedIngredient> inputs,ItemStack... products){
        List<ItemStack> result=new ArrayList<>(List.of(products));
        for(var input:inputs){
            ItemStack[] choices=input.ingredient().getItems();if(choices.length==0)continue;
            ItemStack first=choices[0];ItemStack remaining=first.getCraftingRemainingItem();
            if(remaining.isEmpty())continue;
            for(ItemStack choice:choices)if(!ItemStack.isSameItemSameTags(remaining,choice.getCraftingRemainingItem()))
                throw new IllegalArgumentException("Variable Woot container remainders");
            remaining=remaining.copyWithCount(Math.toIntExact(Math.multiplyExact(remaining.getCount(),input.count()/first.getCount())));result.add(remaining);
        }
        return result;
    }
    private static void add(List<CountedIngredient> in,Ingredient ingredient,long operations){
        ItemStack[] choices=ingredient.getItems();if(choices.length==0)throw new IllegalArgumentException("Empty Woot ingredient");
        int count=choices[0].getCount();for(ItemStack choice:choices)if(choice.getCount()!=count)throw new IllegalArgumentException("Variable ingredient counts");
        in.add(new CountedIngredient(ingredient,Math.multiplyExact(count,operations)));
    }
    private static List<LongSizedFluidIngredient> fluids(FluidStack stack,long operations){
        if(stack.isEmpty())return List.of();
        return List.of(new LongSizedFluidIngredient(DataComponentFluidIngredient.of(true,stack),Math.multiplyExact(stack.getAmount(),operations)));
    }
    private static List<Ingredient> molds(String name){return new ArrayList<>(List.of(Ingredient.of(item(name))));}
    private static ItemStack item(String name){ResourceLocation id=new ResourceLocation("woot_revived",name);if(!ForgeRegistries.ITEMS.containsKey(id))throw new IllegalArgumentException("Missing Woot item "+id);return new ItemStack(ForgeRegistries.ITEMS.getValue(id));}
    private static FluidStack fluid(String name,int amount){Fluid fluid=ForgeRegistries.FLUIDS.getValue(new ResourceLocation("woot_revived",name));if(fluid==null)throw new IllegalArgumentException("Missing Woot fluid "+name);return new FluidStack(fluid,amount);}
    private static ResourceLocation id(String path){return new ResourceLocation("useless_mod","woot/"+path);}
    private static int ticks(long energy,long rate){if(rate<=0)throw new IllegalArgumentException("Invalid Woot process rate");return Math.max(1,Math.toIntExact(energy/rate+(energy%rate==0?0:1)));}
    private static int config(String cls,String key)throws ReflectiveOperationException{Object value=Class.forName("wootrevived.woot.config."+cls).getField(key).get(null);return ((Number)call(value,"get")).intValue();}
    private static Object call(Object object,String name)throws ReflectiveOperationException{return object.getClass().getMethod(name).invoke(object);}
    private static long num(Object number){return ((Number)number).longValue();}
    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<WootSyntheticRecipe> holder,Level level){return holder==null?List.of():List.of(holder.value().convertedRecipe());}
    @Override public List<CoreRecipeHolder<WootSyntheticRecipe>> findMatchingRecipes(Level level,Map<Ingredient,Long> items,Map<FluidStack,Long> fluids,ItemStack mold){
        return getGeneratedRecipes(level).stream().filter(h->{var r=h.value().convertedRecipe();Map<Ingredient,Long> required=new LinkedHashMap<>();r.inputs().forEach(i->AdapterUtils.mergeIngredient(required,i.ingredient(),i.count()));return AdapterUtils.matchesRequired(items,required)&&FluidIngredientAllocator.matchesLong(r.inputFluids(),fluids,1L);}).toList();
    }
}
