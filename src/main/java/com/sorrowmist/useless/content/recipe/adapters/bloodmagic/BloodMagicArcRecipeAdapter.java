package com.sorrowmist.useless.content.recipe.adapters.bloodmagic;

import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.fluid.*;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import wayoftime.bloodmagic.recipe.RecipeARC;
import wayoftime.bloodmagic.recipe.RecipeARCPotion;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Native ARC recipes, including fluids, reusable-tool lifetime and flask potion NBT. */
public final class BloodMagicArcRecipeAdapter implements IRecipeAdapter<RecipeARC> {
    @Override public Class<RecipeARC> getRecipeClass() { return RecipeARC.class; }
    @Override public String sourceId() { return RecipeSourceIds.BLOOD_MAGIC; }
    @Override public ItemStack getMoldItem() {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("bloodmagic", "alchemicalreactionchamber"));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<RecipeARC> holder, Level level) {
        return convertAll(holder, level, List.of());
    }
    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<RecipeARC> holder, Level level,
                                                              List<ItemStack> actualInputs) {
        RecipeARC source = holder.value();
        if (source.getInput().isEmpty() || source.getTool().isEmpty() || source.getRequiredInputCount() <= 0
                || getMoldItem().isEmpty()) return List.of();
        List<ItemStack> tools = new ArrayList<>();
        for (ItemStack tool : source.getTool().getItems()) {
            if (source instanceof RecipeARCPotion && PotionUtils.getMobEffects(tool).isEmpty()) {
                for (var potion : BuiltInRegistries.POTION) {
                    if (potion.getEffects().isEmpty()) continue;
                    ItemStack variant = tool.copy();
                    PotionUtils.setCustomEffects(variant, potion.getEffects());
                    tools.add(variant);
                }
            } else tools.add(tool.copy());
        }
        if (actualInputs != null) for (ItemStack stack : actualInputs)
            if (!stack.isEmpty() && source.getTool().test(stack)) tools.add(stack.copyWithCount(1));
        Map<String, AdvancedAlloyFurnaceRecipe> result = new LinkedHashMap<>();
        for (ItemStack tool : tools) {
            if (source instanceof RecipeARCPotion && PotionUtils.getMobEffects(tool).isEmpty()) continue;
            for (List<LongSizedFluidIngredient> fluids : fluidVariants(source)) {
                // Only split item alternatives when their container remainder affects output.
                boolean containers = !source.getConsumeIngredient() && Arrays.stream(source.getInput().getItems())
                        .anyMatch(ItemStack::hasCraftingRemainingItem);
                List<ItemStack> inputs = containers ? Arrays.asList(source.getInput().getItems()) : List.of(ItemStack.EMPTY);
                for (ItemStack concrete : inputs) {
                    try {
                        var converted = convert(holder.id(), source, tool, concrete, fluids);
                        if (converted != null) result.putIfAbsent(converted.id().toString(), converted);
                    } catch (ArithmeticException overflow) {
                        // Counts must never wrap into free materials or negative fluids.
                    }
                }
            }
        }
        return List.copyOf(result.values());
    }
    private static List<List<LongSizedFluidIngredient>> fluidVariants(RecipeARC source) {
        var required = source.getFluidIngredient();
        if (required == null) return List.of(List.of());
        var groups = new LinkedHashMap<Integer, List<FluidIngredient>>();
        for (FluidStack fluid : required.getRepresentations()) {
            if (fluid == null || fluid.isEmpty()) continue;
            groups.computeIfAbsent(fluid.getAmount(), key -> new ArrayList<>())
                    .add(DataComponentFluidIngredient.of(true, fluid));
        }
        if (groups.isEmpty()) return List.of();
        List<List<LongSizedFluidIngredient>> variants = new ArrayList<>();
        groups.forEach((amount, candidates) -> variants.add(List.of(
                new LongSizedFluidIngredient(FluidIngredient.anyOf(candidates), amount))));
        return variants;
    }
    private AdvancedAlloyFurnaceRecipe convert(ResourceLocation sourceId, RecipeARC source, ItemStack tool,
            ItemStack concrete, List<LongSizedFluidIngredient> fluids) {
        var listed = source.getAllListedOutputs(concrete, tool);
        var chances = source.getAllOutputChances();
        if (listed.isEmpty()) return null;
        var weighted = new ArrayList<ExpectedOutputScaler.WeightedItemOutput>();
        for (int index = 0; index < listed.size(); index++) {
            ItemStack stack = listed.get(index);
            if (stack.isEmpty()) continue;
            double chance = index == 0 ? 1 : index - 1 < chances.size() ? chances.get(index - 1).getLeft() : 0;
            weighted.add(new ExpectedOutputScaler.WeightedItemOutput(stack, stack.getCount(), stack.getCount(), chance));
        }
        var scaled = ExpectedOutputScaler.scale(weighted).orElse(null);
        if (scaled == null) return null;
        long uses = 1;
        if (tool.isDamageableItem()) {
            uses = Math.multiplyExact((long) tool.getMaxDamage() - tool.getDamageValue(),
                    1L + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, tool));
            if (uses <= 0) return null;
        }
        var batch = ArcBatchPlan.create(scaled.operations(), uses);
        long operations = batch.operations(), factor = batch.outputFactor();
        var outputs = new ArrayList<ItemStack>();
        for (ItemStack output : scaled.outputs()) outputs.add(output.copyWithCount(
                Math.toIntExact(Math.multiplyExact(output.getCount(), factor))));
        long toolCount = batch.tools();
        // Consume whole lifetimes, rather than silently turning a consumable ARC reagent into a free mold.
        if (tool.isDamageableItem()) {
            if (!source.breakTool()) {
                ItemStack spent = tool.copy(); spent.setDamageValue(tool.getMaxDamage());
                spent.setCount(Math.toIntExact(toolCount)); outputs.add(spent);
            }
        } else if (tool.hasCraftingRemainingItem()) {
            ItemStack remainder = tool.getCraftingRemainingItem();
            remainder.setCount(Math.toIntExact(Math.multiplyExact(remainder.getCount(), toolCount))); outputs.add(remainder);
        }
        if (!concrete.isEmpty() && !source.getConsumeIngredient() && concrete.hasCraftingRemainingItem()) {
            ItemStack remainder = concrete.getCraftingRemainingItem();
            remainder.setCount(Math.toIntExact(Math.multiplyExact(remainder.getCount(), operations))); outputs.add(remainder);
        }
        var inputs = List.of(new CountedIngredient(concrete.isEmpty() ? source.getInput() : StrictNBTIngredient.of(concrete),
                        Math.multiplyExact(source.getRequiredInputCount(), operations)),
                new CountedIngredient(StrictNBTIngredient.of(tool.copyWithCount(1)), toolCount));
        var fluidInputs = fluids.stream().map(fluid -> new LongSizedFluidIngredient(fluid.ingredient(),
                Math.multiplyExact(fluid.amount(), operations))).toList();
        var fluidOutputs = new ArrayList<FluidStack>();
        FluidStack fluid = source.getFluidOutput();
        if (fluid != null && !fluid.isEmpty()) {
            fluid = fluid.copy(); fluid.setAmount(Math.toIntExact(Math.multiplyExact(fluid.getAmount(), operations)));
            fluidOutputs.add(fluid);
        }
        if (outputs.isEmpty() && fluidOutputs.isEmpty()) return null;
        // Canonical NBT keeps client and server recipe IDs independent of object identity hashes.
        String signature = tool.save(new net.minecraft.nbt.CompoundTag()) + "|" + concrete.save(new net.minecraft.nbt.CompoundTag());
        for (var required : fluids) for (var candidate : required.getFluids())
            signature += "|" + candidate.writeToNBT(new net.minecraft.nbt.CompoundTag());
        var id = new ResourceLocation(sourceId.getNamespace(), sourceId.getPath() + "_arc_" + digest(signature) + "_converted");
        return new AdvancedAlloyFurnaceRecipe(id, inputs, fluidInputs, List.of(), outputs, fluidOutputs, List.of(),
                0, batch.ticks(), Ingredient.EMPTY, 0,
                List.of(Ingredient.of(getMoldItem())), AlloyFurnaceMode.NORMAL, 0);
    }
    private static String digest(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)), 0, 8); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    @Override public List<CoreRecipeHolder<RecipeARC>> findMatchingRecipes(Level level, Map<Ingredient, Long> items,
            Map<FluidStack, Long> fluids, ItemStack mold) {
        return findMatchingRecipes(level, items, fluids, Map.of(), mold, List.of());
    }
    @Override public List<CoreRecipeHolder<RecipeARC>> findMatchingRecipes(Level level, Map<Ingredient, Long> items,
            Map<FluidStack, Long> fluids, Map<appeng.api.stacks.AEKey, Long> keys, ItemStack mold, List<ItemStack> actual) {
        if (!matchesMold(mold)) return List.of();
        var matches = new ArrayList<CoreRecipeHolder<RecipeARC>>();
        for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) if (recipe instanceof RecipeARC arc) {
            var holder = new CoreRecipeHolder<>(recipe.getId(), arc);
            for (var converted : convertAll(holder, level, actual)) {
                Map<Ingredient, Long> required = new LinkedHashMap<>();
                converted.inputs().forEach(input -> required.merge(input.ingredient(), input.count(), Long::sum));
                if (AdapterUtils.matchesRequired(items, required) && AdapterUtils.matchesLongFluidIngredients(fluids, converted.inputFluids())) {
                    matches.add(holder); break;
                }
            }
        }
        return matches;
    }
}
