package com.sorrowmist.useless.content.recipe.adapters.voidminers;

import com.sorrowmist.useless.content.recipe.*;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Optional source API access is isolated here; no VoidMiners classes enter the mod's linkage. */
public final class VoidMiningRecipeAdapter implements IRecipeAdapter<VoidMiningSyntheticRecipe> {
    private record Cached(long generation, List<CoreRecipeHolder<VoidMiningSyntheticRecipe>> recipes) {}
    private final Map<Object, Cached> generated = new WeakHashMap<>();
    @Override public String sourceId() { return "voidminers"; }
    @Override public Class<VoidMiningSyntheticRecipe> getRecipeClass() { return VoidMiningSyntheticRecipe.class; }
    @Override public ItemStack getMoldItem() { return null; }
    @Override public boolean matchesMold(ItemStack mold) {
        if (mold == null || mold.isEmpty()) return false;
        var id = BuiltInRegistries.ITEM.getKey(mold.getItem());
        return id.getNamespace().equals("voidminers") && id.getPath().endsWith("_miner");
    }
    private record Source(ResourceLocation id, ResourceLocation dimension, int tier, boolean higher,
                          ItemStack output, double weight) {}

    @Override public synchronized List<CoreRecipeHolder<VoidMiningSyntheticRecipe>> getGeneratedRecipes(Level level) {
        if (level == null) return List.of();
        long generation = AlloyFurnaceRecipeCatalog.generation();
        Cached cached = generated.get(level.getRecipeManager());
        if (cached != null && cached.generation() == generation) return cached.recipes();
        List<CoreRecipeHolder<VoidMiningSyntheticRecipe>> result = new ArrayList<>();
        try {
            Class<?> recipeClass = Class.forName("com.leo.voidminers.recipe.MinerRecipe");
            List<Source> sources = new ArrayList<>();
            for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
                if (!recipeClass.isInstance(recipe)) continue;
                Object weighted = recipeClass.getMethod("output").invoke(recipe);
                ItemStack output = ((ItemStack) weighted.getClass().getField("stack").get(weighted)).copy();
                double weight = ((Number) weighted.getClass().getField("weight").get(weighted)).doubleValue();
                ResourceLocation dimension = ((ResourceKey<?>) recipeClass.getMethod("dimension").invoke(recipe)).location();
                int tier = ((Number) recipeClass.getMethod("minTier").invoke(recipe)).intValue();
                boolean higher = (boolean) recipeClass.getMethod("allowHigherTiers").invoke(recipe);
                if (!output.isEmpty() && Double.isFinite(weight) && weight > 0)
                    sources.add(new Source(recipe.getId(), dimension, tier, higher, output, weight));
            }
            Map<?, ?> tiers = (Map<?, ?>) Class.forName("com.leo.voidminers.util.MiscUtil").getField("tierMap").get(null);
            Class<?> configClass = Class.forName("com.leo.voidminers.config.ConfigLoader");
            Object config = configClass.getMethod("getInstance").invoke(null);
            for (var tierEntry : tiers.entrySet()) {
                String name = tierEntry.getKey().toString();
                int tier = ((Number) tierEntry.getValue()).intValue();
                ItemStack miner = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("voidminers", name + "_miner")));
                ItemStack trigger = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("voidminers", "frame_base")));
                if (miner.isEmpty() || trigger.isEmpty()) continue;
                Object minerConfig = configClass.getMethod("getMinerConfig", String.class).invoke(config, name);
                double duration = ((Number) minerConfig.getClass().getMethod("duration").invoke(minerConfig)).doubleValue();
                double power = ((Number) minerConfig.getClass().getMethod("energyTick").invoke(minerConfig)).doubleValue();
                if (!Double.isFinite(duration) || duration <= 0 || !Double.isFinite(power) || power < 0) continue;
                for (Source source : sources) {
                    if (!(source.higher ? source.tier <= tier : source.tier == tier)) continue;
                    double total = sources.stream().filter(s -> s.dimension.equals(source.dimension)
                            && (s.higher ? s.tier <= tier : s.tier == tier)).mapToDouble(Source::weight).sum();
                    double ticks = Math.ceil(duration * total / source.weight);
                    double energy = Math.ceil(power * ticks);
                    if (!Double.isFinite(ticks) || ticks > Integer.MAX_VALUE || !Double.isFinite(energy)
                            || energy >= Long.MAX_VALUE) continue;
                    String dimensionHex = HexFormat.of().formatHex(source.dimension.toString().getBytes(StandardCharsets.UTF_8));
                    ResourceLocation id = new ResourceLocation("voidminers", "alloy/d_" + dimensionHex + "/" + name
                            + "/" + source.id.getNamespace() + "/" + source.id.getPath());
                    // AE2 requires an input. The frame is returned unchanged, so mining
                    // consumes energy and time but does not introduce a new material cost.
                    AdvancedAlloyFurnaceRecipe converted = new AdvancedAlloyFurnaceRecipe(id,
                            List.of(new CountedIngredient(Ingredient.of(trigger), 1)), List.of(),
                            List.of(source.output.copy(), trigger.copy()), List.of(),
                            (long) energy, (int) ticks, Ingredient.EMPTY, 0,
                            AdapterUtils.toMoldIngredient(miner), AlloyFurnaceMode.NORMAL);
                    result.add(new CoreRecipeHolder<>(id, new VoidMiningSyntheticRecipe(converted)));
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            com.sorrowmist.useless.UselessMod.LOGGER.warn("Cannot build VoidMiners alloy recipes", e);
        }
        var immutable = List.copyOf(result);
        generated.put(level.getRecipeManager(), new Cached(generation, immutable));
        return immutable;
    }
    @Override public List<AdvancedAlloyFurnaceRecipe> convertAll(CoreRecipeHolder<VoidMiningSyntheticRecipe> holder, Level level) {
        return holder == null ? List.of() : List.of(holder.value().convertedRecipe());
    }
    @Override public List<CoreRecipeHolder<VoidMiningSyntheticRecipe>> findMatchingRecipes(Level level,
            Map<Ingredient, Long> inputs, Map<FluidStack, Long> fluids, ItemStack mold) {
        if (level == null || !matchesMold(mold)) return List.of();
        return getGeneratedRecipes(level).stream().filter(h -> {
            var r = h.value().convertedRecipe();
            Map<Ingredient, Long> required = new LinkedHashMap<>();
            r.inputs().forEach(i -> AdapterUtils.mergeIngredient(required, i.ingredient(), i.count()));
            return allowsDimension(r, level) && AdapterUtils.matchesMold(r.mold(), mold)
                    && AdapterUtils.matchesRequired(inputs, required);
        }).toList();
    }
    public static boolean allowsDimension(AdvancedAlloyFurnaceRecipe recipe, Level level) {
        if (recipe == null || !recipe.id().getNamespace().equals("voidminers")
                || !recipe.id().getPath().startsWith("alloy/d_")) return true;
        if (level == null) return false;
        if (!com.sorrowmist.useless.core.config.ConfigManager.isVoidMinersDimensionRestrictionEnabled()) return true;
        String path = recipe.id().getPath();
        try {
            String dimension = new String(HexFormat.of().parseHex(path.substring(8, path.indexOf('/', 8))), StandardCharsets.UTF_8);
            return level.dimension().location().toString().equals(dimension);
        } catch (RuntimeException e) { return false; }
    }
}
