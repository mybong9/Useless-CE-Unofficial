package com.sorrowmist.useless.content.recipe.adapters.mysticalagriculture;

import com.blakebr0.mysticalagriculture.api.soul.MobSoulType;
import com.blakebr0.mysticalagriculture.api.util.MobSoulUtils;
import com.blakebr0.mysticalagriculture.init.ModItems;
import com.blakebr0.mysticalagriculture.registry.MobSoulTypeRegistry;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.CountedIngredient;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import com.sorrowmist.uselesscore.api.recipe.CoreDataComponentIngredient;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Generates alloy-furnace recipes for Mystical Agriculture soul jars. */
public class SoulJarRecipeAdapter implements IRecipeAdapter<SoulJarRecipeAdapter.SoulJarDummyRecipe> {

    private final Ingredient emptySoulJar;
    private final List<CoreRecipeHolder<SoulJarDummyRecipe>> generatedRecipes = new ArrayList<>();
    private final Map<Item, List<CoreRecipeHolder<SoulJarDummyRecipe>>> recipesByMold = new HashMap<>();

    public SoulJarRecipeAdapter() {
        Item soulJar = ModItems.SOUL_JAR.get();
        this.emptySoulJar = CoreDataComponentIngredient.of(true, new ItemStack(soulJar));
        buildRecipes(soulJar);
    }

    private void buildRecipes(Item soulJar) {
        for (MobSoulType soulType : MobSoulTypeRegistry.getInstance().getMobSoulTypes()) {
            if (!soulType.isEnabled() || soulType.getSoulRequirement() <= 0D) continue;

            for (ResourceLocation entityId : soulType.getEntityIds()) {
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
                if (entityType == null) continue;

                SpawnEggItem spawnEgg = SpawnEggItem.byId(entityType);
                if (spawnEgg == null) continue;

                ItemStack mold = new ItemStack(spawnEgg);
                ItemStack output = MobSoulUtils.getFilledSoulJar(soulType, soulJar);
                ResourceLocation recipeId = recipeId(soulType.getId(), entityId);
                AdvancedAlloyFurnaceRecipe converted = new AdvancedAlloyFurnaceRecipe(
                        recipeId,
                        List.of(new CountedIngredient(emptySoulJar, 1L)),
                        List.of(),
                        List.of(output),
                        List.of(),
                        AdapterUtils.DEFAULT_ENERGY,
                        AdapterUtils.DEFAULT_PROCESS_TIME,
                        Ingredient.EMPTY,
                        0,
                        AdapterUtils.toMoldIngredient(mold),
                        AlloyFurnaceMode.NORMAL);

                CoreRecipeHolder<SoulJarDummyRecipe> holder = new CoreRecipeHolder<>(
                        recipeId, new SoulJarDummyRecipe(converted));
                generatedRecipes.add(holder);
                recipesByMold.computeIfAbsent(spawnEgg, ignored -> new ArrayList<>()).add(holder);
            }
        }
    }

    private static ResourceLocation recipeId(ResourceLocation soulTypeId, ResourceLocation entityId) {
        return new ResourceLocation(
                "mysticalagriculture",
                "soul_jar_" + idPart(soulTypeId) + "_" + idPart(entityId));
    }

    private static String idPart(ResourceLocation id) {
        return (id.getNamespace() + "_" + id.getPath()).replace('/', '_');
    }

    @Override
    public Class<SoulJarDummyRecipe> getRecipeClass() {
        return SoulJarDummyRecipe.class;
    }

    @Override
    @Nullable
    public ItemStack getMoldItem() {
        return null;
    }

    @Override
    public boolean matchesMold(@Nullable ItemStack mold) {
        return mold != null
                && !mold.isEmpty()
                && mold.getItem() instanceof SpawnEggItem
                && recipesByMold.containsKey(mold.getItem());
    }

    @Override
    public List<CoreRecipeHolder<SoulJarDummyRecipe>> getGeneratedRecipes(Level level) {
        return List.copyOf(generatedRecipes);
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(
            CoreRecipeHolder<SoulJarDummyRecipe> holder, Level level) {
        return holder == null ? List.of() : List.of(holder.value().convertedRecipe);
    }

    @Override
    @Nullable
    public AdvancedAlloyFurnaceRecipe convert(
            CoreRecipeHolder<SoulJarDummyRecipe> holder, Level level) {
        return holder == null ? null : holder.value().convertedRecipe;
    }

    @Override
    @Nullable
    public List<CoreRecipeHolder<SoulJarDummyRecipe>> findMatchingRecipes(
            Level level,
            Map<Ingredient, Long> mergedInputs,
            Map<FluidStack, Long> mergedFluids,
            @Nullable ItemStack mold) {
        return findMatchingRecipes(level, mergedInputs, mergedFluids, Map.of(), mold, List.of());
    }

    @Override
    @Nullable
    public List<CoreRecipeHolder<SoulJarDummyRecipe>> findMatchingRecipes(
            Level level,
            Map<Ingredient, Long> mergedInputs,
            Map<FluidStack, Long> mergedFluids,
            Map<appeng.api.stacks.AEKey, Long> mergedKeys,
            @Nullable ItemStack mold,
            List<ItemStack> actualInputs) {
        if (!matchesMold(mold) || !matchesEmptyJar(mergedInputs, actualInputs)) {
            return List.of();
        }
        return recipesByMold.getOrDefault(mold.getItem(), List.of());
    }

    private boolean matchesEmptyJar(Map<Ingredient, Long> mergedInputs, List<ItemStack> actualInputs) {
        if (actualInputs != null && !actualInputs.isEmpty()) {
            return actualInputs.stream()
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .anyMatch(stack -> emptySoulJar.test(stack));
        }

        return AdapterUtils.matchesRequired(
                mergedInputs, Map.of(emptySoulJar, 1L));
    }

    public static class SoulJarDummyRecipe implements com.sorrowmist.uselesscore.api.recipe.CoreRecipe<Container> {
        private final AdvancedAlloyFurnaceRecipe convertedRecipe;

        SoulJarDummyRecipe(AdvancedAlloyFurnaceRecipe convertedRecipe) {
            this.convertedRecipe = convertedRecipe;
        }

        @Override
        public boolean matches(Container input, Level level) {
            return false;
        }

        @Override
        public ItemStack assemble(Container input, HolderLookup.Provider registries) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return false;
        }

        @Override
        public ItemStack getResultItem(HolderLookup.Provider registries) {
            return ItemStack.EMPTY;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return null;
        }

        @Override
        public RecipeType<?> getType() {
            return null;
        }
    }
}
