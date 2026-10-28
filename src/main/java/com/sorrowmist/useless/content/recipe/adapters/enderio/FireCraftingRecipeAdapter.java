package com.sorrowmist.useless.content.recipe.adapters.enderio;

import com.enderio.base.common.recipe.FireCraftingRecipe;
import com.enderio.base.common.init.EIORecipes;
import com.mojang.logging.LogUtils;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.useless.content.recipe.AdapterUtils;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.content.recipe.ExpectedOutputScaler;
import com.sorrowmist.useless.content.recipe.FluidIngredientAllocator;
import com.sorrowmist.useless.content.recipe.IRecipeAdapter;
import com.sorrowmist.useless.content.recipe.LongSizedFluidIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import com.sorrowmist.uselesscore.api.fluid.FluidIngredient;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;

/**
 * 把 Ender IO 的火焰合成（fire_crafting）配方转换到合金炉。
 *
 * <p>火焰合成原本是「在世界上对基方块点火」，没有物品输入，因此这里改用一桶水
 * （{@value #WATER_AMOUNT} mB）作为输入，模具固定为打火石。基方块与产物一律从
 * {@link RecipeManager} 动态读取，不在代码里硬编码任何具体配方。</p>
 *
 * <p>原配方的每条产物各拆成一条独立配方，避免「无限粉」与「可疑的种子」混在同一条
 * 配方里：无限粉按需求固定折算为单个产出，其余产物按概率期望折算成最小确定性批次。</p>
 *
 * <p>转换过程只依赖配方自身的静态数据（基方块列表、产物概率），不会查询世界、世界种子或
 * 玩家位置，保证服务端与客户端生成完全一致的配方集合。</p>
 */
public final class FireCraftingRecipeAdapter implements IRecipeAdapter<FireCraftingRecipe> {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 所有火焰合成配方统一消耗一桶水。 */
    private static final int WATER_AMOUNT = 1000;

    /** 需要固定折算为单个产出的物品：Ender IO 的无限粉。 */
    private static final ResourceLocation GRAINS_OF_INFINITY =
            new ResourceLocation("enderio", "grains_of_infinity");

    @Override
    public Class<FireCraftingRecipe> getRecipeClass() {
        return FireCraftingRecipe.class;
    }

    /** 火焰合成的模具固定为打火石。 */
    @Override
    public ItemStack getMoldItem() {
        return new ItemStack(Items.FLINT_AND_STEEL);
    }

    @Override
    public List<AdvancedAlloyFurnaceRecipe> convertAll(
            CoreRecipeHolder<FireCraftingRecipe> holder, Level level) {
        if (holder == null || holder.value() == null) {
            return List.of();
        }
        FireCraftingRecipe source = holder.value();
        List<Block> bases = sortedBases(source);
        if (bases.isEmpty()) {
            return List.of();
        }

        Ingredient mold = AdapterUtils.toMoldIngredient(getMoldItem());
        List<AdvancedAlloyFurnaceRecipe> recipes = new ArrayList<>();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || source.getLootTable() == null) {
            return List.of();
        }
        for (Block base : bases) {
            AdvancedAlloyFurnaceRecipe recipe = convertLootTable(
                    holder.id(), source, base, mold, serverLevel);
            if (recipe != null) recipes.add(recipe);
        }
        return recipes;
    }

    /**
     * 把单条产物转换成一个合金炉配方。
     *
     * <p>每条产物独立成配方，因此同一个基方块下的无限粉与可疑的种子互不混合。</p>
     */
    @Nullable
    private static AdvancedAlloyFurnaceRecipe convertLootTable(
            ResourceLocation sourceId, FireCraftingRecipe source, Block base,
            Ingredient mold, net.minecraft.server.level.ServerLevel level) {
        List<ExpectedOutputScaler.WeightedItemOutput> weighted = sampleLootTable(source, base, level);
        if (weighted == null || weighted.isEmpty()) {
            return null;
        }

        // 概率产物折算成最小的确定性批次：operations 次操作恰好产出期望数量的物品。
        Optional<ExpectedOutputScaler.ScaledOutputs> scaled = ExpectedOutputScaler.scale(weighted);
        if (scaled.isEmpty() || scaled.get().outputs().isEmpty()) {
            LOGGER.warn("Skipping unsupported Ender IO fire crafting result in recipe: {}", sourceId);
            return null;
        }

        int operations = scaled.get().operations();
        OptionalInt energy = ExpectedOutputScaler.multiplyToInt(AdapterUtils.DEFAULT_ENERGY, operations);
        OptionalInt processTime = ExpectedOutputScaler.multiplyToInt(AdapterUtils.DEFAULT_PROCESS_TIME, operations);
        if (energy.isEmpty() || processTime.isEmpty()) {
            LOGGER.warn("Skipping overflowing Ender IO fire crafting recipe: {}", sourceId);
            return null;
        }

        return new AdvancedAlloyFurnaceRecipe(
                variantId(sourceId, base, scaled.get().outputs()),
                List.of(),
                waterRequirement(),
                List.of(),
                copyOutputs(scaled.get().outputs()),
                List.of(),
                List.of(),
                energy.getAsInt(),
                processTime.getAsInt(),
                Ingredient.EMPTY,
                0,
                List.of(mold),
                AlloyFurnaceMode.NORMAL);
    }

    @Override
    public List<CoreRecipeHolder<FireCraftingRecipe>> findMatchingRecipes(
            Level level, Map<Ingredient, Long> mergedInputs,
            Map<FluidStack, Long> mergedFluids, @Nullable ItemStack mold) {
        if (level == null || !matchesMold(mold)) {
            return List.of();
        }

        // 输入只有水，不再依赖任何物品输入。
        Map<FluidStack, Long> safeFluids = mergedFluids == null ? Map.of() : mergedFluids;
        if (!FluidIngredientAllocator.matchesLong(waterRequirement(), safeFluids, 1L)) {
            return List.of();
        }

        List<CoreRecipeHolder<FireCraftingRecipe>> matches = new ArrayList<>();
        RecipeManager manager = level.getRecipeManager();
        for (CoreRecipeHolder<FireCraftingRecipe> holder : com.sorrowmist.uselesscore.api.recipe.CoreRecipes.ofType(manager, EIORecipes.FIRE_CRAFTING.type().get())) {
            FireCraftingRecipe source = holder.value();
            if (source == null || sortedBases(source).isEmpty() || !hasConvertibleResult(source)) {
                continue;
            }
            matches.add(holder);
        }
        return matches;
    }

    /** 统一的水输入需求。 */
    private static List<LongSizedFluidIngredient> waterRequirement() {
        return List.of(new LongSizedFluidIngredient(FluidIngredient.single(Fluids.WATER), WATER_AMOUNT));
    }

    /**
     * 把单条产物整理成期望值折算器的输入。
     *
     * <p>无限粉固定折算为单个产出（1 次操作恰好 1 个），其余产物保留原有的
     * 概率与数量区间，由折算器换算成等期望的确定性批次。</p>
     */
    @Nullable
    private static boolean isGrainsOfInfinity(ItemStack stack) {
        return GRAINS_OF_INFINITY.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** 判断配方是否至少有一条可转换的产物。 */
    private static boolean hasConvertibleResult(FireCraftingRecipe source) {
        return source.getLootTable() != null;
    }

    /**
     * 按方块注册名排序去重后返回基方块。
     *
     * <p>基方块列表可能包含标签，标签展开顺序不稳定；排序后既保证产物输出顺序确定，
     * 也让下面基于方块 id 的配方 id 与实际遍历顺序无关。</p>
     */
    private static List<Block> sortedBases(FireCraftingRecipe source) {
        Map<ResourceLocation, Block> unique = new TreeMap<>();
        for (Block block : source.getBases()) {
            if (block == null) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null) {
                continue;
            }
            unique.putIfAbsent(id, block);
        }
        return List.copyOf(unique.values());
    }

    /**
     * 生成与内容绑定的稳定配方 id：原配方 id + 基方块注册名 + 产物注册名。
     *
     * <p>id 只能由配方内容决定，不能掺入集合迭代顺序或身份哈希，否则重启后同一配方会换 id，
     * 已编码的样板会失配。产物后缀用于区分同一个基方块下拆分出来的多条配方。</p>
     */
    private static ResourceLocation variantId(
            ResourceLocation sourceId, Block base, List<ItemStack> outputs) {
        StringBuilder path = new StringBuilder(sourceId.getPath())
                .append("_on_")
                .append(registrySuffix(BuiltInRegistries.BLOCK.getKey(base)));
        for (ItemStack output : outputs) {
            path.append("_to_").append(registrySuffix(BuiltInRegistries.ITEM.getKey(output.getItem())));
        }
        return new ResourceLocation(sourceId.getNamespace(), path.toString());
    }

    private static String registrySuffix(@Nullable ResourceLocation id) {
        return id == null
                ? "unknown"
                : id.getNamespace() + "_" + id.getPath().replace('/', '_');
    }

    /** 每条配方持有独立的产物副本，避免多条配方共享同一个 ItemStack 实例。 */
    private static List<ItemStack> copyOutputs(List<ItemStack> outputs) {
        List<ItemStack> copies = new ArrayList<>(outputs.size());
        for (ItemStack output : outputs) {
            copies.add(output.copy());
        }
        return copies;
    }

    private static double clampChance(double chance) {
        return Math.max(0.0, Math.min(1.0, chance));
    }

    /** Samples target-version loot tables into a deterministic expected-output recipe. */
    private static List<ExpectedOutputScaler.WeightedItemOutput> sampleLootTable(
            FireCraftingRecipe source, Block base,
            net.minecraft.server.level.ServerLevel level) {
        final int samples = 256;
        net.minecraft.world.level.storage.loot.LootTable table =
                level.getServer().getLootData().getLootTable(source.getLootTable());
        Map<String, SampledStack> observed = new java.util.TreeMap<>();
        for (int seed = 0; seed < samples; seed++) {
            net.minecraft.world.level.storage.loot.LootParams params =
                    new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE,
                                    base.defaultBlockState())
                            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                                    net.minecraft.world.phys.Vec3.ZERO)
                            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,
                                    new ItemStack(Items.FLINT_AND_STEEL))
                            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
            for (ItemStack drop : table.getRandomItems(params, seed)) {
                if (drop == null || drop.isEmpty()) continue;
                String key = BuiltInRegistries.ITEM.getKey(drop.getItem()) + "|" + drop.getTag();
                observed.compute(key, (ignored, current) -> current == null
                        ? new SampledStack(drop.copy(), drop.getCount())
                        : new SampledStack(current.stack(), current.total() + drop.getCount()));
            }
        }
        List<ExpectedOutputScaler.WeightedItemOutput> outputs = new ArrayList<>();
        for (SampledStack sample : observed.values()) {
            ItemStack stack = sample.stack().copyWithCount(1);
            if (isGrainsOfInfinity(stack)) {
                outputs.add(new ExpectedOutputScaler.WeightedItemOutput(stack, 1, 1, 1.0D));
                continue;
            }
            double expected = (double) sample.total() / samples;
            int whole = (int) Math.floor(expected);
            outputs.add(new ExpectedOutputScaler.WeightedItemOutput(
                    stack, whole, whole, clampChance(expected - whole)));
        }
        return List.copyOf(outputs);
    }

    private record SampledStack(ItemStack stack, long total) {}
}
