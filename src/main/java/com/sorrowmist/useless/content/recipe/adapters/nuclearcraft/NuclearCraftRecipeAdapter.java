package com.sorrowmist.useless.content.recipe.adapters.nuclearcraft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.fluid.DataComponentFluidIngredient;
import com.sorrowmist.uselesscore.api.fluid.FluidIngredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CompoundIngredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.*;

/** Reads the public NuclearCraft 1.20 processor API without a mandatory runtime dependency. */
public final class NuclearCraftRecipeAdapter implements IRecipeAdapter<Recipe<?>> {
    // These machines use ordinary item/fluid processing. Reactor, world and particle recipes
    // have additional conditions and must not be converted as an ordinary processor.
    private static final Set<String> MACHINES=Set.of("nuclear_furnace","manufactory","alloy_smelter",
            "assembler","centrifuge","chemical_reactor","crystallizer","fuel_reprocessor",
            "decay_hastener","electrolyzer","extractor","fluid_enricher","fluid_infuser",
            "ingot_former","isotope_separator","melter","pressurizer","rock_crusher","supercooler");
    private final Class<Recipe<?>> recipeClass;
    private final Map<String,Method> access=new HashMap<>();
    private final Method processors;
    private final Set<String> warned=java.util.concurrent.ConcurrentHashMap.newKeySet();

    @SuppressWarnings("unchecked")
    public NuclearCraftRecipeAdapter() {
        try {
            recipeClass=(Class<Recipe<?>>)(Class<?>)Class.forName("igentuman.nc.recipes.AbstractRecipe");
            for(String name:List.of("getCodeId","getInputItems","getInputFluids","getResultItems",
                    "getOutputFluids","getTimeModifier","getEnergy"))access.put(name,recipeClass.getMethod(name));
            processors=Class.forName("igentuman.nc.content.processors.Processors").getMethod("all");
        } catch(ReflectiveOperationException e){throw new IllegalStateException("Unsupported NuclearCraft processor API",e);}
    }
    @Override public Class<Recipe<?>> getRecipeClass(){return recipeClass;}
    @Override public String sourceId(){return RecipeSourceIds.NUCLEARCRAFT;}
    @Override public ItemStack getMoldItem(){return null;}
    @Override public boolean matchesMold(ItemStack mold){
        if(mold==null||mold.isEmpty())return false;
        var id=ForgeRegistries.ITEMS.getKey(mold.getItem());
        return id!=null&&id.getNamespace().equals("nuclearcraft")&&MACHINES.contains(id.getPath());
    }

    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<Recipe<?>> holder,Level level){
        if(holder==null||!recipeClass.isInstance(holder.value()))return List.of();
        try {
            Object recipe=holder.value();String machine=(String)read(recipe,"getCodeId");
            if(!MACHINES.contains(machine))return List.of();
            var item=ForgeRegistries.ITEMS.getValue(new ResourceLocation("nuclearcraft",machine));
            if(item==null||item==Items.AIR)return List.of();
            var inputs=new ArrayList<CountedIngredient>();
            for(Object in:(Object[])read(recipe,"getInputItems"))inputs.add(itemInput(serialized(in)));
            var fluids=new ArrayList<LongSizedFluidIngredient>();
            for(Object in:(Object[])read(recipe,"getInputFluids"))fluids.add(fluidInput(serialized(in)));
            var outputs=new ArrayList<ItemStack>();
            for(Object out:(List<?>)read(recipe,"getResultItems")){
                ItemStack stack=(ItemStack)out;if(stack.isEmpty()||stack.is(Items.BARRIER))return List.of();
                outputs.add(stack.copy());
            }
            var fluidOutputs=new ArrayList<FluidStack>();
            for(Object out:(List<?>)read(recipe,"getOutputFluids")){
                FluidStack stack=(FluidStack)out;if(stack.isEmpty())return List.of();fluidOutputs.add(stack.copy());
            }
            if(inputs.isEmpty()&&fluids.isEmpty()||outputs.isEmpty()&&fluidOutputs.isEmpty())return List.of();
            Object prefab=((Map<?,?>)processors.invoke(null)).get(machine);
            if(prefab==null)return List.of();
            prefab=prefab.getClass().getMethod("config").invoke(prefab);
            if(!Boolean.TRUE.equals(prefab.getClass().getMethod("isRegistered").invoke(prefab)))return List.of();
            double baseTime=((Number)prefab.getClass().getMethod("getTime").invoke(prefab)).doubleValue();
            double basePower=((Number)prefab.getClass().getMethod("getPower").invoke(prefab)).doubleValue();
            int ticks=ProcessorRecipeCosts.ticks(baseTime,((Number)read(recipe,"getTimeModifier")).doubleValue());
            long energy=ProcessorRecipeCosts.energy(basePower,((Number)read(recipe,"getEnergy")).doubleValue(),ticks);
            return List.of(new AdvancedAlloyFurnaceRecipe(AdapterUtils.convertedId(holder.id()),inputs,fluids,List.of(),
                    outputs,fluidOutputs,List.of(),energy,ticks,Ingredient.EMPTY,0,List.of(Ingredient.of(item)),
                    AlloyFurnaceMode.NORMAL,AdvancedAlloyFurnaceRecipe.NO_EXPLICIT_TIER));
        } catch(Exception e){
            if(warned.add(holder.id().toString()))UselessMod.LOGGER.warn("Cannot convert NuclearCraft recipe {}: {}",holder.id(),e.toString());
            return List.of();
        }
    }

    private Object read(Object recipe,String method)throws ReflectiveOperationException{return access.get(method).invoke(recipe);}
    private static JsonElement serialized(Object ingredient)throws ReflectiveOperationException {
        return (JsonElement)ingredient.getClass().getMethod("serialize").invoke(ingredient);
    }

    static CountedIngredient itemInput(JsonElement json){
        if(json.isJsonArray()){
            var alternatives=new ArrayList<Ingredient>();long amount=-1;
            for(JsonElement child:json.getAsJsonArray()){
                var input=itemInput(child);
                if(amount!=-1&&amount!=input.count())throw new IllegalArgumentException("Item alternatives require different quantities");
                amount=input.count();alternatives.add(input.ingredient());
            }
            if(alternatives.isEmpty())throw new IllegalArgumentException("Empty item alternatives");
            return new CountedIngredient(CompoundIngredient.of(alternatives.toArray(Ingredient[]::new)),amount);
        }
        JsonObject object=json.getAsJsonObject();long count=object.has("count")?object.get("count").getAsLong():1;
        Ingredient ingredient=Ingredient.fromJson(object.get("ingredient"));
        if(count<=0||ingredient.isEmpty())throw new IllegalArgumentException("Invalid NuclearCraft item ingredient");
        return new CountedIngredient(ingredient,count);
    }

    static LongSizedFluidIngredient fluidInput(JsonElement json)throws Exception{
        if(json.isJsonArray()){
            var alternatives=new ArrayList<FluidIngredient>();long amount=-1;
            for(JsonElement child:json.getAsJsonArray()){
                var input=fluidInput(child);
                if(amount!=-1&&amount!=input.amount())throw new IllegalArgumentException("Fluid alternatives require different quantities");
                amount=input.amount();alternatives.add(input.ingredient());
            }
            if(alternatives.isEmpty())throw new IllegalArgumentException("Empty fluid alternatives");
            return new LongSizedFluidIngredient(FluidIngredient.anyOf(alternatives),amount);
        }
        JsonObject object=json.getAsJsonObject();long amount=object.get("amount").getAsLong();
        if(amount<=0)throw new IllegalArgumentException("Invalid fluid amount");
        FluidIngredient ingredient;
        if(object.has("tag"))ingredient=FluidIngredient.tag(TagKey.create(Registries.FLUID,new ResourceLocation(object.get("tag").getAsString())));
        else {
            var fluid=ForgeRegistries.FLUIDS.getValue(new ResourceLocation(object.get("fluid").getAsString()));
            if(fluid==null)throw new IllegalArgumentException("Unknown fluid");
            var stack=new FluidStack(fluid,1);
            if(object.has("nbt"))stack.setTag(TagParser.parseTag(object.get("nbt").getAsString()));
            // NC's single-fluid predicate compares NBT, including the absence of NBT.
            ingredient=DataComponentFluidIngredient.of(true,stack);
        }
        if(ingredient.isEmpty())throw new IllegalArgumentException("Unresolved fluid ingredient");
        return new LongSizedFluidIngredient(ingredient,amount);
    }

    @Override public List<CoreRecipeHolder<Recipe<?>>> findMatchingRecipes(Level level,Map<Ingredient,Long> items,
            Map<FluidStack,Long> fluids,ItemStack mold){
        if(level==null||!matchesMold(mold))return List.of();
        var matches=new ArrayList<CoreRecipeHolder<Recipe<?>>>();
        for(Recipe<?> source:level.getRecipeManager().getRecipes())if(recipeClass.isInstance(source)){
            var holder=new CoreRecipeHolder<Recipe<?>>(source.getId(),source);
            for(var recipe:convertAll(holder,level))if(recipe.molds().get(0).test(mold)){
                var required=new IdentityHashMap<Ingredient,Long>();
                recipe.inputs().forEach(input->required.put(input.ingredient(),input.count()));
                if(AdapterUtils.matchesRequired(items,required)&&AdapterUtils.matchesLongFluidIngredients(fluids,recipe.inputFluids()))matches.add(holder);
            }
        }
        return matches;
    }
}
