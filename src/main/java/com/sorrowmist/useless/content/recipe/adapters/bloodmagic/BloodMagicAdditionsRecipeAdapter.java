package com.sorrowmist.useless.content.recipe.adapters.bloodmagic;

import appeng.api.config.PowerUnits;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import wayoftime.bloodmagic.common.fluid.BloodMagicFluids;
import wayoftime.bloodmagic.recipe.RecipeAlchemyTable;
import wayoftime.bloodmagic.recipe.RecipeBloodAltar;

import java.util.*;

/** Blood Assembler recipes consume actual Life Essence, distinct from Blood Magic's FE variant. */
public final class BloodMagicAdditionsRecipeAdapter<T extends Recipe<?>> implements IRecipeAdapter<T> {
    private final Class<T> recipeClass;
    private final Class<?> config;
    public BloodMagicAdditionsRecipeAdapter(Class<T> recipeClass){
        this.recipeClass=recipeClass;
        try{config=Class.forName("com.edgemq.bmaddon.config.BMAddonCommonConfig");}
        catch(ClassNotFoundException e){throw new IllegalStateException("Unsupported BloodMagicAdditions config API",e);}
    }
    @Override public Class<T> getRecipeClass(){return recipeClass;}
    @Override public String sourceId(){return RecipeSourceIds.BLOOD_MAGIC_ADDITIONS;}
    @Override public ItemStack getMoldItem(){return item("blood_altar_assembler");}
    private static ItemStack item(String name){
        var item=ForgeRegistries.ITEMS.getValue(new ResourceLocation("bmaddon",name));
        return item==null||item==Items.AIR?ItemStack.EMPTY:new ItemStack(item);
    }
    private Number setting(String name)throws ReflectiveOperationException{
        return (Number)((ForgeConfigSpec.ConfigValue<?>)config.getField(name).get(null)).get();
    }
    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<T> holder,Level level){
        if(holder==null||holder.value()==null)return List.of();
        List<Ingredient> ingredients;ItemStack output;int syphon,tier;
        if(holder.value() instanceof RecipeBloodAltar altar){
            ingredients=List.of(altar.getInput());output=altar.getOutput();syphon=altar.getSyphon();tier=altar.getMinimumTier();
        }else if(holder.value() instanceof RecipeAlchemyTable table){
            ingredients=table.getInput();output=table.getOutput();syphon=table.getSyphon();tier=table.getMinimumTier();
        }else return List.of();
        var machine=getMoldItem();
        if(machine.isEmpty()||output.isEmpty()||ingredients.isEmpty()||ingredients.stream().anyMatch(Ingredient::isEmpty))return List.of();
        try{
            double multiplier=setting("BLOOD_ALTAR_ASSEMBLER_LIFE_ESSENCE_MULTIPLIER").doubleValue();
            int ticks=setting("BLOOD_ALTAR_ASSEMBLER_BASE_CRAFT_TIME_TICKS").intValue();
            double ae=setting("BLOOD_ALTAR_ASSEMBLER_AE_PER_TICK_BASE").doubleValue();
            double essence=Math.ceil(Math.max(0,syphon)*multiplier);
            double energy=PowerUnits.AE.convertTo(PowerUnits.FE,ae)*ticks;
            if(ticks<=0||!Double.isFinite(essence)||essence<0||essence>Integer.MAX_VALUE
                    ||!Double.isFinite(energy)||energy<0||energy>=Long.MAX_VALUE)return List.of();
            var fluids=new ArrayList<LongSizedFluidIngredient>();
            if(essence>0)fluids.add(LongSizedFluidIngredient.from(new FluidStack(BloodMagicFluids.LIFE_ESSENCE_FLUID.get(),(int)essence)));
            var molds=new ArrayList<Ingredient>();molds.add(Ingredient.of(machine));
            if(tier>1){
                var cards=new ArrayList<ItemStack>();
                for(int t=tier;t<=5;t++){var card=item("blood_altar_tier_card_"+t);if(!card.isEmpty())cards.add(card);}
                if(cards.isEmpty())return List.of();molds.add(Ingredient.of(cards.toArray(ItemStack[]::new)));
            }
            var id=new ResourceLocation(holder.id().getNamespace(),holder.id().getPath()+"_bmaddon_converted");
            return List.of(new AdvancedAlloyFurnaceRecipe(id,ingredients.stream().map(i->new CountedIngredient(i,1)).toList(),
                    fluids,List.of(),List.of(output.copy()),List.of(),List.of(),(long)Math.ceil(energy),ticks,
                    Ingredient.EMPTY,0,molds,AlloyFurnaceMode.NORMAL,Math.min(10,Math.max(0,tier))));
        }catch(ReflectiveOperationException|RuntimeException invalid){
            UselessMod.LOGGER.debug("Cannot convert Blood Assembler recipe {}",holder.id(),invalid);return List.of();
        }
    }
    @Override public List<CoreRecipeHolder<T>> findMatchingRecipes(Level level,Map<Ingredient,Long> items,
            Map<FluidStack,Long> fluids,ItemStack mold){
        if(level==null||!matchesMold(mold))return List.of();
        var matches=new ArrayList<CoreRecipeHolder<T>>();
        for(Recipe<?> source:level.getRecipeManager().getRecipes())if(recipeClass.isInstance(source)){
            var holder=new CoreRecipeHolder<>(source.getId(),recipeClass.cast(source));
            for(var recipe:convertAll(holder,level)){
                var required=new IdentityHashMap<Ingredient,Long>();
                recipe.inputs().forEach(input->required.put(input.ingredient(),input.count()));
                if(AdapterUtils.matchesRequired(items,required)&&AdapterUtils.matchesLongFluidIngredients(fluids,recipe.inputFluids()))matches.add(holder);
            }
        }
        return matches;
    }
}
