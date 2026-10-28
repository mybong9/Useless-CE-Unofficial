package com.sorrowmist.useless.content.recipe.adapters.enderio;

import com.enderio.base.common.item.tool.SoulVialItem;
import com.enderio.api.capability.StoredEntityData;
import com.enderio.base.common.init.EIOCapabilities;
import com.enderio.machines.common.blockentity.task.SpawnerMachineTask.SpawnType;
import com.enderio.machines.common.souldata.SpawnerSoul;
import com.enderio.base.common.util.EntityCaptureUtils;
import com.enderio.base.common.tag.EIOTags;
import com.enderio.machines.common.config.MachinesConfig;
import com.enderio.base.common.init.EIOItems;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.CountedIngredient;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import com.sorrowmist.uselesscore.api.recipe.CoreDataComponentIngredient;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Generates Ender IO soul-vial capture recipes for the alloy furnace. */
public final class SoulVialRecipeAdapter implements IRecipeAdapter<SoulVialRecipeAdapter.SoulVialDummyRecipe> {
    private static final int INPUT_COUNT = 1;

    private final Ingredient emptySoulVial = CoreDataComponentIngredient.of(
            true, EIOItems.EMPTY_SOUL_VIAL.get().getDefaultInstance());

    @Override
    public Class<SoulVialDummyRecipe> getRecipeClass() {
        return SoulVialDummyRecipe.class;
    }

    @Override
    @Nullable
    public ItemStack getMoldItem() {
        return null;
    }

    @Override
    public boolean matchesMold(@Nullable ItemStack mold) {
        if (mold == null || mold.isEmpty() || !(mold.getItem() instanceof SpawnEggItem spawnEgg)) {
            return false;
        }
        return isSupportedEntity(spawnEgg.getType(mold.getTag()));
    }

    @Override
    public List<CoreRecipeHolder<SoulVialDummyRecipe>> getGeneratedRecipes(Level level) {
        if (level == null) {
            return List.of();
        }

        List<CoreRecipeHolder<SoulVialDummyRecipe>> result = new ArrayList<>();
        for (EntityType<?> entityType : capturableTypes()) {
            if (!isSupportedEntity(entityType)) {
                continue;
            }

            SpawnEggItem spawnEgg = SpawnEggItem.byId(entityType);
            int energy = energyCost(entityType);
            if (spawnEgg == null || energy <= 0) {
                continue;
            }

            ItemStack mold = new ItemStack(spawnEgg);
            result.add(createHolder(entityType, mold, energy));
        }
        return List.copyOf(result);
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(
            CoreRecipeHolder<SoulVialDummyRecipe> holder, Level level) {
        if (holder == null || holder.value() == null || holder.value().convertedRecipe() == null) {
            return List.of();
        }
        return List.of(holder.value().convertedRecipe());
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(
            CoreRecipeHolder<SoulVialDummyRecipe> holder, Level level, List<ItemStack> actualInputs) {
        return convertAll(holder, level);
    }

    @Override
    public List<CoreRecipeHolder<SoulVialDummyRecipe>> findMatchingRecipes(
            Level level, Map<Ingredient, Long> mergedInputs,
            Map<FluidStack, Long> mergedFluids, @Nullable ItemStack mold) {
        return findMatchingRecipes(level, mergedInputs, mergedFluids, Map.of(), mold, List.of());
    }

    @Override
    public List<CoreRecipeHolder<SoulVialDummyRecipe>> findMatchingRecipes(
            Level level, Map<Ingredient, Long> mergedInputs,
            Map<FluidStack, Long> mergedFluids,
            Map<appeng.api.stacks.AEKey, Long> mergedKeys,
            @Nullable ItemStack mold, List<ItemStack> actualInputs) {
        if (level == null || !matchesMold(mold) || !matchesEmptySoulVial(mergedInputs, actualInputs)) {
            return List.of();
        }

        SpawnEggItem spawnEgg = (SpawnEggItem) mold.getItem();
        EntityType<?> entityType = spawnEgg.getType(mold.getTag());
        int energy = energyCost(entityType);
        if (energy <= 0) {
            return List.of();
        }

        return List.of(createHolder(entityType, mold.copyWithCount(1), energy));
    }

    private CoreRecipeHolder<SoulVialDummyRecipe> createHolder(
            EntityType<?> entityType, ItemStack mold, int energy) {
        ResourceLocation recipeId = recipeId(entityType);
        ItemStack output = filledVial(captureSoul(entityType, mold));
        AdvancedAlloyFurnaceRecipe converted = new AdvancedAlloyFurnaceRecipe(
                recipeId,
                List.of(new CountedIngredient(emptySoulVial, INPUT_COUNT)),
                List.of(),
                List.of(output),
                List.of(),
                energy,
                AdapterUtils.DEFAULT_PROCESS_TIME,
                Ingredient.EMPTY,
                0,
                AdapterUtils.toMoldIngredient(mold),
                AlloyFurnaceMode.NORMAL);
        return new CoreRecipeHolder<>(recipeId, new SoulVialDummyRecipe(converted));
    }

    private boolean matchesEmptySoulVial(
            Map<Ingredient, Long> mergedInputs, List<ItemStack> actualInputs) {
        if (actualInputs != null && !actualInputs.isEmpty()) {
            return actualInputs.stream()
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .anyMatch(emptySoulVial::test);
        }
        return AdapterUtils.matchesRequired(mergedInputs, Map.of(emptySoulVial, (long) INPUT_COUNT));
    }

    private static List<EntityType<?>> capturableTypes() {
        List<EntityType<?>> result = new ArrayList<>();
        for (ResourceLocation id : EntityCaptureUtils.getCapturableEntities()) {
            BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(result::add);
        }
        return result;
    }

    private static boolean isSupportedEntity(@Nullable EntityType<?> entityType) {
        return entityType != null && SpawnEggItem.byId(entityType) != null
                && EntityCaptureUtils.getCapturableEntities().contains(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
    }

    private static int energyCost(EntityType<?> entityType) {
        return SpawnerSoul.SPAWNER.matches(BuiltInRegistries.ENTITY_TYPE.getKey(entityType))
                .map(SpawnerSoul.SoulData::power)
                .orElseGet(() -> MachinesConfig.COMMON.ENERGY.POWERED_SPAWNER_USAGE.get());
    }

    private static SpawnType spawnMode(EntityType<?> entityType) {
        return SpawnerSoul.SPAWNER.matches(BuiltInRegistries.ENTITY_TYPE.getKey(entityType))
                .map(SpawnerSoul.SoulData::spawnType)
                .orElseGet(() -> MachinesConfig.COMMON.SPAWN_TYPE.get());
    }

    private static StoredEntityData captureSoul(EntityType<?> entityType, ItemStack mold) {
        if (spawnMode(entityType) == SpawnType.COPY) {
            var entityTag = mold.getTagElement("EntityTag");
            if (entityTag != null) {
                var copy = entityTag.copy();
                copy.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
                return new StoredEntityData(copy, 0);
            }
        }
        return StoredEntityData.of(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
    }

    private static ItemStack filledVial(StoredEntityData soul) {
        ItemStack result = new ItemStack(EIOItems.FILLED_SOUL_VIAL.get());
        var storage = result.getCapability(EIOCapabilities.ENTITY_STORAGE).orElseThrow(
                () -> new IllegalStateException("Ender IO filled soul vial has no entity storage"));
        storage.setStoredEntityData(soul);
        return result;
    }

    private static ResourceLocation recipeId(EntityType<?> entityType) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        String path = entityId == null ? "unknown" : entityId.toString().replace(':', '_').replace('/', '_');
        return new ResourceLocation("enderio", "soul_vial_" + path);
    }

    public static final class SoulVialDummyRecipe implements com.sorrowmist.uselesscore.api.recipe.CoreRecipe<Container> {
        private final AdvancedAlloyFurnaceRecipe convertedRecipe;

        SoulVialDummyRecipe(AdvancedAlloyFurnaceRecipe convertedRecipe) {
            this.convertedRecipe = convertedRecipe;
        }

        AdvancedAlloyFurnaceRecipe convertedRecipe() {
            return convertedRecipe;
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
