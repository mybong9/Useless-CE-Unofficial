package com.sorrowmist.useless.content.recipe.adapters.botania;

import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import io.github.lounode.extrabotany.api.recipe.PedestalRecipe;
import mythicbotany.infuser.InfuserRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** Crafting processes supplied by MythicBotany and ExtraBotany. */
public final class BotaniaAddonRecipeAdapter<T extends Recipe<?>> implements IRecipeAdapter<T> {
    private final Class<T> type;
    private final String source;

    public BotaniaAddonRecipeAdapter(Class<T> type, String source) { this.type = type; this.source = source; }
    @Override public Class<T> getRecipeClass() { return type; }
    @Override public String sourceId() { return source; }
    @Override public ItemStack getMoldItem() { return null; }

    private Ingredient machine() {
        if (type == mythicbotany.rune.RuneRitualRecipe.class) return Ingredient.of(TagKey.create(Registries.ITEM,
                new ResourceLocation("useless_mod", "molds/botania/rune_ritual")));
        if (type == InfuserRecipe.class) return Ingredient.of(TagKey.create(Registries.ITEM,
                new ResourceLocation("useless_mod", "molds/botania/mana_infuser")));
        return Ingredient.of(TagKey.create(Registries.ITEM,
                new ResourceLocation("useless_mod", "molds/botania/pedestal")));
    }

    @Override public boolean matchesMold(ItemStack mold) { return mold != null && machine().test(mold); }

    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<T> holder, Level level) {
        if (holder == null || level == null) return List.of();
        if (holder.value() instanceof mythicbotany.rune.RuneRitualRecipe ritual) return ritual(holder.id(), ritual);
        List<Ingredient> inputs;
        List<Ingredient> molds = new ArrayList<>(List.of(machine()));
        ItemStack output;
        long energy;
        int ticks = 100;
        if (holder.value() instanceof InfuserRecipe infuser) {
            inputs = infuser.getIngredients();
            output = infuser.getResultItem(level.registryAccess());
            energy = infuser.getManaUsage();
        } else if (holder.value() instanceof PedestalRecipe pedestal) {
            inputs = List.of(pedestal.getInput()); output = pedestal.getOutput();
            molds.add(pedestal.getSmashTools());
            energy = Math.multiplyExact((long) Math.max(0, pedestal.getExp()), 100);
            ticks = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) pedestal.getStrike()) * 20);
        } else return List.of();
        if (output.isEmpty() || molds.stream().anyMatch(Ingredient::isEmpty)) return List.of();
        return List.of(new AdvancedAlloyFurnaceRecipe(AdapterUtils.convertedId(holder.id()),
                inputs.stream().map(i -> new CountedIngredient(i, 1)).toList(), List.of(), List.of(),
                List.of(output.copy()), List.of(), List.of(), Math.max(0, energy), ticks,
                Ingredient.EMPTY, 0, molds, AlloyFurnaceMode.NORMAL));
    }

    private List<AdvancedAlloyFurnaceRecipe> ritual(ResourceLocation sourceId, mythicbotany.rune.RuneRitualRecipe ritual) {
        List<Ingredient> inputs = new ArrayList<>(ritual.getInputs());
        inputs.add(ritual.getCenterRune());
        List<Ingredient> molds = new ArrayList<>(List.of(machine()));
        for (var rune : ritual.getRunes()) {
            if (rune.isConsumed()) inputs.add(rune.getRune());
            else molds.add(rune.getRune());
        }
        List<ItemStack> outputs = new ArrayList<>(ritual.getOutputs().stream().map(ItemStack::copy).toList());
        if (ritual.getSpecialInput() != null) {
            if (!ritual.getSpecialInput().id.equals(new ResourceLocation("mythicbotany", "wandering_trader"))) return List.of();
            inputs.add(Ingredient.of(net.minecraft.world.item.Items.WANDERING_TRADER_SPAWN_EGG));
        }
        if (ritual.getSpecialOutput() != null) {
            if (!ritual.getSpecialOutput().id.equals(new ResourceLocation("mythicbotany", "mjoellnir"))) return List.of();
            outputs.addAll(ritual.getSpecialOutput().getJeiOutputItems().stream().map(ItemStack::copy).toList());
        }
        if (inputs.stream().anyMatch(Ingredient::isEmpty) || molds.stream().anyMatch(Ingredient::isEmpty)
                || outputs.isEmpty() || outputs.stream().anyMatch(ItemStack::isEmpty)) return List.of();
        return List.of(new AdvancedAlloyFurnaceRecipe(AdapterUtils.convertedId(sourceId),
                inputs.stream().map(input -> new CountedIngredient(input, 1)).toList(),
                List.of(), List.of(), outputs, List.of(), List.of(), Math.max(0, ritual.getMana()),
                Math.max(1, ritual.getTicks()), Ingredient.EMPTY, 0, molds, AlloyFurnaceMode.NORMAL));
    }

    @Override public List<CoreRecipeHolder<T>> findMatchingRecipes(Level level, Map<Ingredient, Long> items,
            Map<FluidStack, Long> fluids, ItemStack mold) {
        if (!matchesMold(mold)) return List.of();
        List<CoreRecipeHolder<T>> result = new ArrayList<>();
        for (Recipe<?> source : level.getRecipeManager().getRecipes()) {
            if (!type.isInstance(source)) continue;
            var holder = new CoreRecipeHolder<>(source.getId(), type.cast(source));
            for (var recipe : convertAll(holder, level)) {
                Map<Ingredient, Long> required = new LinkedHashMap<>();
                recipe.inputs().forEach(i -> required.merge(i.ingredient(), i.count(), Long::sum));
                if (AdapterUtils.matchesRequired(items, required)) { result.add(holder); break; }
            }
        }
        return result;
    }
}

