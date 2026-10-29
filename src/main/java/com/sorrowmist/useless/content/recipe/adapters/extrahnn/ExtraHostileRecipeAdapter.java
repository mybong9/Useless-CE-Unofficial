package com.sorrowmist.useless.content.recipe.adapters.extrahnn;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.useless.content.recipe.adapters.hostilenetworks.DataModelRangeIngredient;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.recipe.CoreDataComponentIngredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CompoundIngredient;
import net.minecraftforge.fluids.FluidStack;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** EHNN machine operations: four equal models and one template per tier boundary. */
public final class ExtraHostileRecipeAdapter implements IRecipeAdapter<ExtraHostileSyntheticRecipe> {
    private record Tier(String name, int data, int step, float accuracy) {}
    private record Cached(long generation, List<CoreRecipeHolder<ExtraHostileSyntheticRecipe>> recipes) {}
    private final Map<Object, Cached> cache = new WeakHashMap<>();
    private static final String MOD = "extrahnn";
    private static final int[] MULTIPLIERS = {1, 4, 8, 16};
    @Override public String sourceId() { return MOD; }
    @Override public Class<ExtraHostileSyntheticRecipe> getRecipeClass() { return ExtraHostileSyntheticRecipe.class; }
    @Override public ItemStack getMoldItem() { return null; }

    @Override public synchronized List<CoreRecipeHolder<ExtraHostileSyntheticRecipe>> getGeneratedRecipes(Level level) {
        if (level == null) return List.of();
        long generation = AlloyFurnaceRecipeCatalog.generation();
        Cached previous = cache.get(level.getRecipeManager());
        if (previous != null && previous.generation == generation) return previous.recipes;
        List<CoreRecipeHolder<ExtraHostileSyntheticRecipe>> result = new ArrayList<>();
        try {
            List<Tier> tiers = tiers();
            for (DataModel model : DataModelRegistry.INSTANCE.getValues()) {
                try {
                ResourceLocation id = DataModelRegistry.INSTANCE.getKey(model);
                if (id == null) continue;
                // Exactly four equal models; one template per tier boundary, never per kill.
                for (var recipe : combination(List.of(id,id,id,id), tiers)) result.add(holder(recipe));
                for (var recipe : nativeTraining(model)) result.add(holder(recipe));
                for (var recipe : fabrication(model)) result.add(holder(recipe));
                } catch (RuntimeException invalidModel) {
                    UselessMod.LOGGER.warn("Cannot convert EHNN data model {}", DataModelRegistry.INSTANCE.getKey(model), invalidModel);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            UselessMod.LOGGER.warn("Cannot read EHNN model/configuration API", exception);
        }
        var recipes = List.copyOf(result);
        cache.put(level.getRecipeManager(), new Cached(generation, recipes));
        return recipes;
    }

    private static List<AdvancedAlloyFurnaceRecipe> combination(List<ResourceLocation> ids, List<Tier> tiers)
            throws ReflectiveOperationException {
        List<DataModel> models = models(ids);
        if (ids.size() != 4 || !ids.stream().allMatch(ids.get(0)::equals)) return List.of();
        if (models.size() != 4 || tiers.isEmpty() || item("extra_data_model").isEmpty()) return List.of();
        List<AdvancedAlloyFurnaceRecipe> result = new ArrayList<>();
        List<CountedIngredient> mergeInputs = new ArrayList<>();
        mergeInputs.add(new CountedIngredient(Ingredient.of(item("blank_extra_data_model")), 1));
        for (DataModel model : models) mergeInputs.add(new CountedIngredient(DataModelRangeIngredient.atBoundary(model, ModelTier.SELF_AWARE), 1));
        if (!item("merger_camera").isEmpty() && !item("blank_extra_data_model").isEmpty()) result.add(recipe(
                id(ids,"merge",0,0), mergeInputs, List.of(extraStack(ids,tiers.get(0).data)),
                List.of(Ingredient.of(item("merger_camera"))), cost("mergerCameraPowerCost","mergerCameraPowerDuration",1), config("mergerCameraPowerDuration")));

        Ingredient matrix = matrix(models);
        long simCost = models.stream().mapToLong(DataModel::simCost).sum();
        for (int index = 0; index < tiers.size(); index++) {
            Tier tier = tiers.get(index);
            ItemStack boundary = extraStack(ids, tier.data);
            // Partial NBT ignores simulation counters while retaining all four ordered IDs and exact data.
            Ingredient modelIngredient = DataModelRangeIngredient.extraBoundary(models.get(0), tier.data);
            if (index + 1 < tiers.size() && tier.step > 0 && !item("simulator_modeling").isEmpty()
                    && allowedModeling(boundary)) {
                Tier next = tiers.get(index+1);
                long cycles = ceiling((long) next.data - tier.data, tier.step);
                int ticks = scaledTicks(config("simulationModelingPowerDuration"), cycles);
                if (ticks > 0) result.add(recipe(id(ids,"modeling",0,index), List.of(new CountedIngredient(modelIngredient,1)),
                        List.of(extraStack(ids,next.data)), List.of(Ingredient.of(item("simulator_modeling"))),
                        Math.multiplyExact(config("simulationModelingPowerCost"), (long) ticks), ticks));
            }
            for (int version = 1; version <= 4; version++) {
                ItemStack chamber = item("ultimate_sim_chamber_v"+version);
                if (chamber.isEmpty() || matrix.isEmpty()) continue;
                int multiplier = MULTIPLIERS[version-1];
                int duration = config("ultimateSimV"+version+"PowerDuration");
                if (duration <= 0) continue;
                if (index + 1 < tiers.size() && tier.step > 0) {
                    Tier next = tiers.get(index+1);
                    long span = (long) next.data - tier.data;
                    long cycles = ceiling(span, tier.step);
                    int ticks = scaledTicks(duration, cycles);
                    if (cycles > 0 && ticks > 0) {
                        // Native accuracy interpolates from the current tier toward the next tier.
                        double predictions = cycles * tier.accuracy
                                + (next.accuracy-tier.accuracy) * tier.step * cycles * (cycles-1) / (2.0 * span);
                        List<ItemStack> outputs = new ArrayList<>();
                        outputs.add(extraStack(ids,next.data));
                        for (DataModel model : models) {
                            output(outputs,model.baseDrop(),Math.multiplyExact(cycles,multiplier),true);
                            output(outputs,model.getPredictionDrop(),Math.multiplyExact(Math.max(0,Math.round(predictions)),multiplier),true);
                        }
                        result.add(recipe(id(ids,"training",version,index),
                                List.of(new CountedIngredient(modelIngredient,1), new CountedIngredient(matrix,Math.multiplyExact(cycles,4L*multiplier))),
                                outputs,List.of(Ingredient.of(chamber)),Math.multiplyExact(simCost,ticks),ticks));
                    }
                }
                if (index + 1 == tiers.size() && tier.accuracy > 0 && Float.isFinite(tier.accuracy)) {
                    List<ExpectedOutputScaler.WeightedItemOutput> weighted = new ArrayList<>();
                    for (DataModel model : models) {
                        if (!model.baseDrop().isEmpty()) weighted.add(new ExpectedOutputScaler.WeightedItemOutput(model.baseDrop().copyWithCount(1),multiplier,multiplier,1));
                        if (!model.getPredictionDrop().isEmpty()) weighted.add(new ExpectedOutputScaler.WeightedItemOutput(model.getPredictionDrop().copyWithCount(1),multiplier,multiplier,Double.parseDouble(Float.toString(tier.accuracy))));
                    }
                    var scaled = ExpectedOutputScaler.scale(weighted);
                    if (scaled.isPresent() && !scaled.get().outputs().isEmpty()) {
                        int operations = scaled.get().operations(), ticks = scaledTicks(duration,operations);
                        if (ticks > 0) result.add(recipe(id(ids,"inference",version,index), List.of(new CountedIngredient(matrix,4L*multiplier*operations)),
                                scaled.get().outputs(),List.of(Ingredient.of(chamber),modelIngredient),Math.multiplyExact(simCost,ticks),ticks));
                    }
                }
            }
        }
        return result;
    }

    private static List<AdvancedAlloyFurnaceRecipe> nativeTraining(DataModel model) throws ReflectiveOperationException {
        if (item("simulator_modeling").isEmpty()) return List.of();
        List<AdvancedAlloyFurnaceRecipe> result = new ArrayList<>();
        for (ModelTier tier : ModelTier.values()) {
            if (tier == tier.next()) continue;
            int step = model.getDataPerKill(tier), from = model.getTierData(tier), target = model.getTierData(tier.next());
            if (step <= 0 || target <= from) continue;
            int ticks = scaledTicks(config("simulationModelingPowerDuration"),ceiling((long)target-from,step));
            if (ticks <= 0) continue;
            ItemStack output = new ItemStack(Hostile.Items.DATA_MODEL.get());
            DataModelItem.setStoredModel(output,model); DataModelItem.setData(output,target);
            ItemStack input = output.copy(); DataModelItem.setData(input, from);
            if (!allowedModeling(input)) continue;
            ResourceLocation modelId = DataModelRegistry.INSTANCE.getKey(model);
            ResourceLocation id = new ResourceLocation(MOD,"native_modeling/"+tier.name+"/"+encode(List.of(modelId)));
            result.add(recipe(id,List.of(new CountedIngredient(DataModelRangeIngredient.atBoundary(model,tier),1)),List.of(output),
                    List.of(Ingredient.of(item("simulator_modeling"))),Math.multiplyExact(config("simulationModelingPowerCost"),(long)ticks),ticks));
        }
        return result;
    }
    private static List<AdvancedAlloyFurnaceRecipe> fabrication(DataModel model) throws ReflectiveOperationException {
        List<AdvancedAlloyFurnaceRecipe> result = new ArrayList<>();
        if (model.getPredictionDrop().isEmpty()) return result;
        for (int version=1;version<=4;version++) {
            ItemStack mold=item("ultimate_loot_fabricator_v"+version);
            if(mold.isEmpty()) continue;
            int count=4*MULTIPLIERS[version-1],ticks=config("ultimateFabV"+version+"PowerDuration");
            if(ticks<=0)continue;
            int outputIndex=0;
            for(ItemStack drop:model.fabDrops()) {
                if(drop.isEmpty()) {outputIndex++;continue;}
                var id=new ResourceLocation(MOD,"fabrication/v"+version+"/"+encode(List.of(DataModelRegistry.INSTANCE.getKey(model)))+"/"+outputIndex++);
                result.add(recipe(id,List.of(new CountedIngredient(CoreDataComponentIngredient.of(true,model.getPredictionDrop()),count)),
                        List.of(drop.copyWithCount(Math.multiplyExact(drop.getCount(),count))),List.of(Ingredient.of(mold)),
                        Math.multiplyExact(config("ultimateFabV"+version+"PowerCost"),(long)ticks),ticks));
            }
        }
        return result;
    }

    private static List<Tier> tiers() throws ReflectiveOperationException {
        Class<?> type=Class.forName("net.lmor.extrahnn.data.ExtraModelTier");
        List<Tier> result=new ArrayList<>();
        for(Object value:type.getEnumConstants()) {
            Object data=type.getMethod("data").invoke(value);
            int required=((Number)data.getClass().getMethod("requiredData").invoke(data)).intValue();
            int step=((Number)data.getClass().getMethod("dataPerKill").invoke(data)).intValue();
            float accuracy=((Number)type.getMethod("accuracy").invoke(value)).floatValue();
            if(required<0 || !Float.isFinite(accuracy) || accuracy<0 || accuracy>1)throw new IllegalArgumentException("Unsupported EHNN tier");
            result.add(new Tier(((Enum<?>)value).name().toLowerCase(Locale.ROOT),required,step,accuracy));
        }
        for(int i=1;i<result.size();i++)if(result.get(i).data<=result.get(i-1).data)throw new IllegalArgumentException("Non-increasing EHNN tiers");
        return List.copyOf(result);
    }
    private static int config(String name) throws ReflectiveOperationException {
        int value=Class.forName("net.lmor.extrahnn.ExtraHostileConfig").getField(name).getInt(null);
        if(value<0)throw new IllegalArgumentException("Negative EHNN configuration");
        return value;
    }
    private static long cost(String power,String duration,long cycles)throws ReflectiveOperationException {
        return Math.multiplyExact(Math.multiplyExact((long)config(power),config(duration)),cycles);
    }
    private static int scaledTicks(int ticks,long cycles) {long total=Math.multiplyExact((long)ticks,cycles);return total>Integer.MAX_VALUE?0:Math.toIntExact(total);}
    private static long ceiling(long span,int step) {return com.sorrowmist.useless.content.recipe.adapters.hostilenetworks.ModelBoundaryRules.steps(span,step);}
    private static boolean allowedModeling(ItemStack model) throws ReflectiveOperationException {
        Class<?> helper = Class.forName("net.lmor.extrahnn.EHNNUtils");
        return Boolean.TRUE.equals(helper.getMethod("allowedTier",ItemStack.class).invoke(null,model))
                && Boolean.TRUE.equals(helper.getMethod("allowedBlackListModel",ItemStack.class).invoke(null,model));
    }
    private static ItemStack item(String name) {return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(MOD,name)));}
    private static List<DataModel> models(List<ResourceLocation> ids) {
        if(ids.size()!=4)return List.of();
        List<DataModel> result=new ArrayList<>();
        for(var id:ids){var model=DataModelRegistry.INSTANCE.holder(id);if(!model.isBound())return List.of();result.add(model.get());}
        return result;
    }
    private static ItemStack extraStack(List<ResourceLocation> ids,int data) {
        ItemStack stack=item("extra_data_model");if(stack.isEmpty())return stack;
        ListTag list=new ListTag();ids.forEach(id->list.add(StringTag.valueOf(id.toString())));
        CompoundTag tag=stack.getOrCreateTagElement("data_model");tag.put("ids",list);tag.putInt("data",data);return stack;
    }
    private static Ingredient matrix(List<DataModel> models) {
        Ingredient[] choices=models.stream().map(DataModel::input).filter(stack->!stack.isEmpty())
                .map(stack->CoreDataComponentIngredient.of(false,stack)).toArray(Ingredient[]::new);
        return choices.length==0?Ingredient.EMPTY:CompoundIngredient.of(choices);
    }
    private static String encode(List<ResourceLocation> ids) {return HexFormat.of().formatHex(String.join("|",ids.stream().map(Object::toString).toList()).getBytes(StandardCharsets.UTF_8));}
    private static ResourceLocation id(List<ResourceLocation> ids,String operation,int version,int tier) {return new ResourceLocation(MOD,"compat/"+operation+"/"+version+"/"+tier+"/"+encode(ids));}
    private static void output(List<ItemStack> outputs,ItemStack base,long count,boolean nativeUnit) {
        if(base==null||base.isEmpty()||count<=0)return;
        int amount = Math.toIntExact(Math.multiplyExact(count,nativeUnit?1:base.getCount()));
        for (ItemStack existing : outputs) if (ItemStack.isSameItemSameTags(existing,base)) {
            existing.setCount(Math.addExact(existing.getCount(),amount)); return;
        }
        outputs.add(base.copyWithCount(amount));
    }
    private static AdvancedAlloyFurnaceRecipe recipe(ResourceLocation id,List<CountedIngredient> inputs,List<ItemStack> outputs,List<Ingredient> molds,long energy,int ticks) {
        return new AdvancedAlloyFurnaceRecipe(id,inputs,List.of(),List.of(),outputs,List.of(),List.of(),energy,ticks,
                Ingredient.EMPTY,0,molds,AlloyFurnaceMode.NORMAL,AdvancedAlloyFurnaceRecipe.NO_EXPLICIT_TIER);
    }
    private static CoreRecipeHolder<ExtraHostileSyntheticRecipe> holder(AdvancedAlloyFurnaceRecipe recipe) {return new CoreRecipeHolder<>(recipe.id(),new ExtraHostileSyntheticRecipe(recipe));}
    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<ExtraHostileSyntheticRecipe> holder,Level level) {return holder==null?List.of():List.of(holder.value().convertedRecipe());}
    @Override public List<CoreRecipeHolder<ExtraHostileSyntheticRecipe>> findMatchingRecipes(Level level,Map<Ingredient,Long> inputs,Map<FluidStack,Long> fluids,Map<appeng.api.stacks.AEKey,Long> keys,ItemStack mold,List<ItemStack> actual) {
        Map<ResourceLocation,AdvancedAlloyFurnaceRecipe> candidates=new LinkedHashMap<>();
        getGeneratedRecipes(level).forEach(holder->candidates.put(holder.id(),holder.value().convertedRecipe()));
        List<CoreRecipeHolder<ExtraHostileSyntheticRecipe>> result=new ArrayList<>();
        for(var recipe:candidates.values()) {
            if(!AdapterUtils.matchesMold(recipe.mold(),mold))continue;
            Map<Ingredient,Long> required=new LinkedHashMap<>();recipe.inputs().forEach(input->AdapterUtils.mergeIngredient(required,input.ingredient(),input.count()));
            if(AdapterUtils.matchesRequired(inputs,required))result.add(holder(recipe));
        }
        return result;
    }
}
