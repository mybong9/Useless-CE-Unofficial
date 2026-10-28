package com.sorrowmist.useless.content.recipe.adapters.botania;

import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import vazkii.botania.api.recipe.*;

import java.util.*;

/** Uses Botania's public recipe interfaces, including recipes supplied by addons and datapacks. */
public final class BotaniaRecipeAdapter<T extends Recipe<?>> implements IRecipeAdapter<T> {
    private final Class<T> type;
    private final String machine;

    public BotaniaRecipeAdapter(Class<T> type, String machine) {
        this.type = type;
        this.machine = machine;
    }

    @Override public Class<T> getRecipeClass() { return type; }
    @Override public String sourceId() { return RecipeSourceIds.BOTANIA; }
    @Override public ItemStack getMoldItem() { return null; }

    private Ingredient machineMold() {
        return Ingredient.of(TagKey.create(Registries.ITEM,
                new ResourceLocation("useless_mod", "molds/botania/" + machine)));
    }

    @Override public boolean matchesMold(ItemStack mold) {
        return mold != null && machineMold().test(mold);
    }

    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<T> holder, Level level) {
        if (holder == null || level == null) return List.of();
        T source = holder.value();
        if (source instanceof BotanicalBreweryRecipe brewery) return brewing(holder, brewery, level);
        if (source instanceof ManaInfusionRecipe infusion) {
            return manaInfusion(holder.id(), infusion, level, source.getIngredients().stream()
                    .flatMap(ingredient -> Arrays.stream(ingredient.getItems())).toList());
        }
        List<Ingredient> ingredients = new ArrayList<>(source.getIngredients());
        List<ItemStack> outputs = new ArrayList<>();
        List<Ingredient> molds = new ArrayList<>(List.of(machineMold()));
        long mana = 0;
        int ticks = 100;
        if (source instanceof ManaInfusionRecipe infusion) {
            mana = infusion.getManaToConsume();
            StateIngredient catalyst = infusion.getRecipeCatalyst();
            if (catalyst != null) {
                Ingredient catalystItems = stateItems(catalyst);
                if (catalystItems.isEmpty()) return List.of();
                molds.add(catalystItems);
            }
        } else if (source instanceof RunicAltarRecipe runic) {
            mana = runic.getManaUsage();
            ingredients.add(runic.getReagent());
        } else if (source instanceof PetalApothecaryRecipe petals) {
            ingredients.add(petals.getReagent());
        } else if (source instanceof TerrestrialAgglomerationRecipe terra) {
            mana = terra.getMana();
        } else if (source instanceof ElvenTradeRecipe trade) {
            outputs.addAll(trade.getOutputs().stream().map(ItemStack::copy).toList());
        } else if (source instanceof PureDaisyRecipe daisy) {
            // World functions have effects that an item-processing machine cannot reproduce.
            if (daisy.getSuccessFunction() != null && daisy.getSuccessFunction().getId() != null) {
                return List.of();
            }
            ingredients = new ArrayList<>(List.of(stateItems(daisy.getInput())));
            ItemStack output = new ItemStack(daisy.getOutputState().getBlock());
            if (output.isEmpty()) return List.of();
            outputs.add(output);
            ticks = daisy.getTime();
        }
        if (outputs.isEmpty()) outputs.add(source.getResultItem(level.registryAccess()).copy());
        if (outputs.stream().anyMatch(ItemStack::isEmpty) || ingredients.isEmpty()
                || ingredients.stream().anyMatch(Ingredient::isEmpty)) return List.of();
        List<Variant> variants = new ArrayList<>(List.of(new Variant(new ArrayList<>(), new ArrayList<>())));
        for (Ingredient ingredient : ingredients) {
            boolean returnsRune = source instanceof RunicAltarRecipe && Arrays.stream(ingredient.getItems())
                    .anyMatch(s -> s.getItem() instanceof vazkii.botania.common.item.material.RuneItem);
            if (returnsRune) {
                List<Variant> expanded = new ArrayList<>();
                for (Variant variant : variants) for (ItemStack candidate : ingredient.getItems()) {
                    List<CountedIngredient> input = new ArrayList<>(variant.inputs());
                    input.add(new CountedIngredient(com.sorrowmist.uselesscore.api.recipe.CoreDataComponentIngredient.of(true, candidate), 1));
                    List<ItemStack> returned = new ArrayList<>(variant.returned());
                    if (candidate.getItem() instanceof vazkii.botania.common.item.material.RuneItem) {
                        ItemStack rune = candidate.copy(); rune.setCount(1); returned.add(rune);
                    }
                    expanded.add(new Variant(input, returned));
                }
                variants = expanded;
            } else {
                for (Variant variant : variants) variant.inputs().add(new CountedIngredient(ingredient, 1));
            }
        }
        List<FluidStack> water = source instanceof PetalApothecaryRecipe
                ? List.of(new FluidStack(Fluids.WATER, 1000)) : List.of();
        // The furnace uses FE in place of mana, at one FE per mana unit.
        List<AdvancedAlloyFurnaceRecipe> result = new ArrayList<>();
        for (int index = 0; index < variants.size(); index++) {
            Variant variant = variants.get(index);
            List<ItemStack> allOutputs = new ArrayList<>(outputs);
            allOutputs.addAll(variant.returned());
            ResourceLocation id = AdapterUtils.convertedId(holder.id());
            if (variants.size() > 1) id = new ResourceLocation(id.getNamespace(), id.getPath() + "/rune_variant_" + index);
            result.add(new AdvancedAlloyFurnaceRecipe(id, variant.inputs(), water, List.of(), allOutputs,
                    List.of(), List.of(), Math.max(0, mana), Math.max(1, ticks), Ingredient.EMPTY, 0,
                    molds, AlloyFurnaceMode.NORMAL));
        }
        return result;
    }

    private record Variant(List<CountedIngredient> inputs, List<ItemStack> returned) {}

    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<T> holder, Level level,
            List<ItemStack> actualInputs) {
        if (holder != null && holder.value() instanceof ManaInfusionRecipe infusion && actualInputs != null) {
            return manaInfusion(holder.id(), infusion, level, actualInputs);
        }
        return convertAll(holder, level);
    }

    private List<AdvancedAlloyFurnaceRecipe> manaInfusion(ResourceLocation sourceId,
            ManaInfusionRecipe source, Level level, List<ItemStack> candidates) {
        if (level == null) return List.of();
        List<Ingredient> molds = new ArrayList<>(List.of(machineMold()));
        if (source.getRecipeCatalyst() != null) {
            Ingredient catalyst = stateItems(source.getRecipeCatalyst());
            if (catalyst.isEmpty()) return List.of();
            molds.add(catalyst);
        }
        Map<ResourceLocation, AdvancedAlloyFurnaceRecipe> result = new LinkedHashMap<>();
        for (ItemStack candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) continue;
            ItemStack input = candidate.copyWithCount(1);
            if (!source.matches(input)) continue;
            ItemStack output = source.getRecipeOutput(level.registryAccess(), input.copy());
            if (output == null || output.isEmpty()) continue;
            ResourceLocation base = AdapterUtils.convertedId(sourceId);
            String hash = AdapterUtils.stableHash(AlloyFurnaceRecipeFingerprint.safeItemStack(input, level.registryAccess()));
            ResourceLocation id = new ResourceLocation(base.getNamespace(), base.getPath() + "/input_" + hash);
            result.putIfAbsent(id, new AdvancedAlloyFurnaceRecipe(id,
                    List.of(new CountedIngredient(com.sorrowmist.uselesscore.api.recipe.CoreDataComponentIngredient.of(true, input), 1)),
                    List.of(), List.of(), List.of(output.copy()), List.of(), List.of(),
                    Math.max(0, source.getManaToConsume()), 100, Ingredient.EMPTY, 0, molds, AlloyFurnaceMode.NORMAL));
        }
        return List.copyOf(result.values());
    }

    private List<AdvancedAlloyFurnaceRecipe> brewing(CoreRecipeHolder<T> holder,
            BotanicalBreweryRecipe brewery, Level level) {
        List<AdvancedAlloyFurnaceRecipe> result = new ArrayList<>();
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (!(item instanceof vazkii.botania.api.brew.BrewContainer container)) continue;
            ItemStack empty = item.getDefaultInstance();
            ItemStack output = brewery.getOutput(empty.copy());
            if (output.isEmpty()) continue;
            List<CountedIngredient> inputs = new ArrayList<>();
            brewery.getIngredients().forEach(i -> inputs.add(new CountedIngredient(i, 1)));
            inputs.add(new CountedIngredient(com.sorrowmist.uselesscore.api.recipe.CoreDataComponentIngredient.of(true, empty), 1));
            var itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            var base = AdapterUtils.convertedId(holder.id());
            var id = new ResourceLocation(base.getNamespace(), base.getPath() + "/container/"
                    + itemId.getNamespace() + "/" + itemId.getPath());
            result.add(new AdvancedAlloyFurnaceRecipe(id, inputs, List.of(), List.of(output.copy()), List.of(),
                    Math.max(0, container.getManaCost(brewery.getBrew(), empty)), 100,
                    Ingredient.EMPTY, 0, machineMold(), AlloyFurnaceMode.NORMAL));
        }
        return result;
    }

    private static Ingredient stateItems(StateIngredient states) {
        return Ingredient.of(states.getDisplayedStacks().stream().filter(s -> !s.isEmpty()));
    }

    @Override public List<CoreRecipeHolder<T>> findMatchingRecipes(Level level, Map<Ingredient, Long> items,
            Map<FluidStack, Long> fluids, ItemStack mold) {
        if (!matchesMold(mold)) return List.of();
        List<CoreRecipeHolder<T>> matches = new ArrayList<>();
        for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
            if (!type.isInstance(recipe)) continue;
            CoreRecipeHolder<T> holder = new CoreRecipeHolder<>(recipe.getId(), type.cast(recipe));
            for (AdvancedAlloyFurnaceRecipe converted : convertAll(holder, level)) {
                Map<Ingredient, Long> required = new LinkedHashMap<>();
                converted.inputs().forEach(i -> required.merge(i.ingredient(), i.count(), Long::sum));
                if (AdapterUtils.matchesRequired(items, required)) { matches.add(holder); break; }
            }
        }
        return matches;
    }

    @Override public List<CoreRecipeHolder<T>> findMatchingRecipes(Level level, Map<Ingredient, Long> items,
            Map<FluidStack, Long> fluids, Map<appeng.api.stacks.AEKey, Long> keys, ItemStack mold,
            List<ItemStack> actualInputs) {
        if (!ManaInfusionRecipe.class.isAssignableFrom(type)) return findMatchingRecipes(level, items, fluids, mold);
        if (level == null || actualInputs == null || !matchesMold(mold)) return List.of();
        List<CoreRecipeHolder<T>> matches = new ArrayList<>();
        for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
            if (!type.isInstance(recipe) || !(recipe instanceof ManaInfusionRecipe infusion)) continue;
            if (actualInputs.stream().filter(Objects::nonNull).filter(stack -> !stack.isEmpty())
                    .anyMatch(stack -> infusion.matches(stack.copyWithCount(1)))) {
                matches.add(new CoreRecipeHolder<>(recipe.getId(), type.cast(recipe)));
            }
        }
        return matches;
    }
}

