package com.sorrowmist.useless.content.recipe.adapters.tconstruct;

import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import com.sorrowmist.uselesscore.api.fluid.DataComponentFluidIngredient;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.common.crafting.CompoundIngredient;
import net.minecraftforge.fluids.FluidStack;
import java.lang.reflect.*;
import java.util.*;

/** Optional Tinkers 1.20.1 recipes, including native expanded material-part displays. */
public final class TinkersRecipeAdapter implements IRecipeAdapter<TinkersSyntheticRecipe> {
    private record Cached(long generation,List<CoreRecipeHolder<TinkersSyntheticRecipe>> recipes) {}
    private final Map<Object,Cached> cache=new WeakHashMap<>();
    @Override public String sourceId() { return "tconstruct"; }
    @Override public Class<TinkersSyntheticRecipe> getRecipeClass() { return TinkersSyntheticRecipe.class; }
    @Override public ItemStack getMoldItem() { return null; }
    private static Object call(Object value,String name) throws ReflectiveOperationException {
        Method method=value.getClass().getMethod(name);method.trySetAccessible();return method.invoke(value);
    }
    private static Object call(Object value,String name,Class<?> type,Object argument) throws ReflectiveOperationException {
        Method method=value.getClass().getMethod(name,type);method.trySetAccessible();return method.invoke(value,argument);
    }
    private static boolean flag(Object value,String name) {
        try { return Boolean.TRUE.equals(call(value,name)); } catch (ReflectiveOperationException ignored) { return false; }
    }
    private static boolean implementsType(Class<?> type,String name) {
        if (type==null) return false;
        if (type.getName().equals(name)) return true;
        for (Class<?> api:type.getInterfaces()) if (implementsType(api,name)) return true;
        return implementsType(type.getSuperclass(),name);
    }
    private static Ingredient exactItems(List<ItemStack> stacks) {
        List<Ingredient> choices=new ArrayList<>();
        for (ItemStack stack:stacks) if (!stack.isEmpty()) choices.add((stack.hasTag() || stack.isDamageableItem())?StrictNBTIngredient.of(stack.copyWithCount(1)):Ingredient.of(stack.copyWithCount(1)));
        return choices.isEmpty()?Ingredient.EMPTY:CompoundIngredient.of(choices.toArray(Ingredient[]::new));
    }
    private static ItemStack machine(String name) {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("tconstruct",name)));
    }
    private static LongSizedFluidIngredient fluid(FluidStack stack) {
        return new LongSizedFluidIngredient(DataComponentFluidIngredient.of(true,stack),stack.getAmount());
    }
    @Override public synchronized List<CoreRecipeHolder<TinkersSyntheticRecipe>> getGeneratedRecipes(Level level) {
        if (level==null) return List.of();
        long generation=AlloyFurnaceRecipeCatalog.generation();
        var previous=cache.get(level.getRecipeManager());
        if (previous!=null && previous.generation()==generation) return previous.recipes();
        List<CoreRecipeHolder<TinkersSyntheticRecipe>> output=new ArrayList<>();
        for (Recipe<?> source:level.getRecipeManager().getRecipes()) {
            if (!source.getClass().getName().startsWith("slimeknights.tconstruct.")) continue;
            try {
                if (source.getClass().getName().equals("slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe")) {
                    alloy(source,output);continue;
                }
                List<?> displays;
                try { displays=(List<?>)call(source,"getRecipes",RegistryAccess.class,level.registryAccess()); }
                catch (NoSuchMethodException single) { displays=List.of(source); }
                int index=0;
                for (Object display:displays) {
                    if (implementsType(display.getClass(),"slimeknights.tconstruct.library.recipe.melting.IDisplayableMeltingRecipe"))
                        index=melting(source,display,index,output);
                    else if (implementsType(display.getClass(),"slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe"))
                        index=casting(source,display,index,output);
                }
            } catch (ReflectiveOperationException | RuntimeException failure) {
                com.sorrowmist.useless.UselessMod.LOGGER.warn("Cannot convert Tinkers recipe {}",source.getId(),failure);
            }
        }
        var recipes=List.copyOf(output);cache.put(level.getRecipeManager(),new Cached(generation,recipes));return recipes;
    }
    @SuppressWarnings("unchecked")
    private static int melting(Recipe<?> source,Object display,int index,List<CoreRecipeHolder<TinkersSyntheticRecipe>> output) throws ReflectiveOperationException {
        List<ItemStack> inputs=(List<ItemStack>)call(display,"getInputs");
        List<FluidStack> results;
        try { results=(List<FluidStack>)call(display,"getOutputs"); }
        catch (NoSuchMethodException older) { results=List.of((FluidStack)call(display,"getOutput")); }
        List<List<FluidStack>> byproducts;
        try { byproducts=(List<List<FluidStack>>)call(display,"getOutputWithByproducts"); }
        catch (NoSuchMethodException older) { byproducts=List.of(results); }
        for (int i=0;i<inputs.size();i++) {
            if (inputs.get(i).isEmpty() || results.isEmpty()) continue;
            FluidStack primary=results.get(results.size()==1?0:i).copy();
            List<FluidStack> fluids=new ArrayList<>();fluids.add(primary);
            // Foundry display may apply an ore multiplier; don't silently attach its
            // boosted main output to a base melter recipe. Base melting stays base.
            boolean ore=false;
            try { ore=call(display,"getOreType")!=null; } catch (NoSuchMethodException old) {}
            if (!ore) for (int j=1;j<byproducts.size();j++) {
                var variants=byproducts.get(j);
                if (!variants.isEmpty()) fluids.add(variants.get(variants.size()==1?0:i).copy());
            }
            int time=flag(display,"isTimeDynamic")?((Number)call(display,"getTime",FluidStack.class,primary)).intValue():((Number)call(display,"getTime")).intValue();
            add(source,index++,List.of(new CountedIngredient(exactItems(List.of(inputs.get(i))),1L)),List.of(),List.of(),fluids,
                    List.of(Ingredient.of(machine("seared_melter"),machine("smeltery_controller"),machine("foundry_controller"))),Math.max(1,Math.multiplyExact(time,4)),output);
        }
        return index;
    }
    @SuppressWarnings("unchecked")
    private static int casting(Recipe<?> source,Object display,int index,List<CoreRecipeHolder<TinkersSyntheticRecipe>> output) throws ReflectiveOperationException {
        List<FluidStack> fluids=(List<FluidStack>)call(display,"getFluids");
        List<ItemStack> casts=(List<ItemStack>)call(display,"getCastItems");
        List<ItemStack> results;
        try { results=(List<ItemStack>)call(display,"getOutputs"); }
        catch (NoSuchMethodException older) { results=List.of((ItemStack)call(display,"getOutput")); }
        boolean linkFluid=flag(display,"linkFluidsToOutput"),linkCast=flag(display,"linkCastToOutput");
        String machine=source.getType().toString().contains("basin")?"casting_basin":"casting_table";
        for (int f=0;f<fluids.size();f++) {
            int castVariants=linkCast && results.size()>1 && !linkFluid?casts.size():1;
            for (int c=0;c<castVariants;c++) {
                if (results.isEmpty() || fluids.get(f).isEmpty()) continue;
                ItemStack result=results.get(results.size()==1?0:linkFluid?f:c).copy();
                if (result.isEmpty()) continue;
                Ingredient cast=exactItems(castVariants>1?List.of(casts.get(c)):casts);
                List<CountedIngredient> inputs=new ArrayList<>();
                List<Ingredient> molds=new ArrayList<>();molds.add(Ingredient.of(machine(machine)));
                if (!cast.isEmpty()) {
                    if (flag(display,"isConsumed")) inputs.add(new CountedIngredient(cast,1L));
                    else molds.add(cast);
                }
                int ticks=flag(display,"isCoolingTimeDynamic")?((Number)call(display,"getCoolingTime",FluidStack.class,fluids.get(f))).intValue():((Number)call(display,"getCoolingTime")).intValue();
                add(source,index++,inputs,List.of(fluid(fluids.get(f))),List.of(result),List.of(),molds,Math.max(1,ticks),output);
            }
        }
        return index;
    }
    private static void alloy(Recipe<?> source,List<CoreRecipeHolder<TinkersSyntheticRecipe>> output) throws ReflectiveOperationException {
        List<List<LongSizedFluidIngredient>> variants=new ArrayList<>();variants.add(List.of());
        List<List<FluidStack>> returned=new ArrayList<>();returned.add(List.of());
        for (Object value:(List<?>)call(source,"getInputs")) {
            Object ingredient=value;boolean catalyst=false;
            try { ingredient=call(value,"fluid");catalyst=flag(value,"catalyst"); } catch (NoSuchMethodException old) {}
            List<?> choices=(List<?>)call(ingredient,"getFluids");
            if ((long)variants.size()*choices.size()>256) throw new IllegalArgumentException("Too many alloy fluid alternatives");
            List<List<LongSizedFluidIngredient>> next=new ArrayList<>();List<List<FluidStack>> nextReturns=new ArrayList<>();
            for (int i=0;i<variants.size();i++) for (Object choice:choices) {
                FluidStack stack=(FluidStack)choice;if (stack.isEmpty()) continue;
                var inputs=new ArrayList<>(variants.get(i));inputs.add(fluid(stack));next.add(inputs);
                var outputs=new ArrayList<>(returned.get(i));if (catalyst) outputs.add(stack.copy());nextReturns.add(outputs);
            }
            variants=next;returned=nextReturns;
        }
        for (int i=0;i<variants.size();i++) {
            List<FluidStack> results=new ArrayList<>();results.add(((FluidStack)call(source,"getOutput")).copy());results.addAll(returned.get(i));
            add(source,i,List.of(),variants.get(i),List.of(),results,
                    List.of(Ingredient.of(machine("smeltery_controller"),machine("scorched_alloyer"),machine("foundry_controller"))),AdapterUtils.DEFAULT_PROCESS_TIME,output);
        }
    }
    private static void add(Recipe<?> source,int variant,List<CountedIngredient> inputs,List<LongSizedFluidIngredient> fluids,
                            List<ItemStack> items,List<FluidStack> outputs,List<Ingredient> molds,int ticks,List<CoreRecipeHolder<TinkersSyntheticRecipe>> target) {
        if (inputs.isEmpty() && fluids.isEmpty() || items.isEmpty() && outputs.isEmpty()) return;
        if (molds.stream().anyMatch(Ingredient::isEmpty)) return;
        var base=AdapterUtils.convertedId(source.getId());var id=new ResourceLocation(base.getNamespace(),base.getPath()+"/tconstruct_"+variant);
        var recipe=new AdvancedAlloyFurnaceRecipe(id,inputs,fluids,List.of(),items,outputs,List.of(),
                Math.multiplyExact((long)ticks,Math.max(1,AdapterUtils.DEFAULT_ENERGY/AdapterUtils.DEFAULT_PROCESS_TIME)),ticks,Ingredient.EMPTY,0,molds,AlloyFurnaceMode.NORMAL);
        target.add(new CoreRecipeHolder<>(id,new TinkersSyntheticRecipe(recipe)));
    }
    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<TinkersSyntheticRecipe> holder,Level level) {
        return holder==null?List.of():List.of(holder.value().convertedRecipe());
    }
}
