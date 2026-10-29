package com.sorrowmist.useless.content.recipe.adapters.hostilenetworks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sorrowmist.useless.init.ModIngredientTypes;
import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.sorrowmist.uselesscore.api.recipe.CoreCustomIngredient;
import com.sorrowmist.uselesscore.api.recipe.CoreIngredientType;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 按数据区间匹配数据模型的原料。
 *
 * <p>品级本身就是一个数据区间：{@code ModelTierRegistry.getByData} 从高到低取第一个满足
 * {@code data >= requiredData} 的品级。所以「某个品级的模型」对应的数据范围是
 * {@code [requiredData(当前品级), requiredData(下一品级) - 1]}，最高品级没有上界。
 *
 * <p>{@code DataComponentIngredient} 只能表达精确值，无法表达区间，因此这里自建实现。
 * 序列化时只保存模型 id 与数据上下界。
 *
 * <p>本类引用了 HNN 与 Placebo 的类型，因此<b>只能在 HNN 存在时被加载</b>：
 * 注册入口 {@code ModIngredientTypes.DATA_MODEL_RANGE} 已经做了加载检查，
 * 不要在任何无条件执行的静态初始化里触碰本类。
 */
public final class DataModelRangeIngredient implements CoreCustomIngredient {

    /** 最高品级没有上界时使用的哨兵值。 */
    public static final int UNBOUNDED = Integer.MAX_VALUE;

    public static final MapCodec<DataModelRangeIngredient> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("model")
                            .forGetter(DataModelRangeIngredient::modelId),
                    Codec.INT.fieldOf("min_data")
                            .forGetter(DataModelRangeIngredient::minData),
                    Codec.INT.fieldOf("max_data")
                            .forGetter(DataModelRangeIngredient::maxData),
                    Codec.BOOL.optionalFieldOf("extra", false)
                            .forGetter(ingredient -> ingredient.extra)
            ).apply(instance, DataModelRangeIngredient::new));

    public static final CoreIngredientType<DataModelRangeIngredient> TYPE = new CoreIngredientType<>(CODEC);

    private final ResourceLocation modelId;
    private final int minData;
    private final int maxData;
    private final boolean extra;

    DataModelRangeIngredient(ResourceLocation modelId, int minData, int maxData) {
        this(modelId, minData, maxData, false);
    }

    DataModelRangeIngredient(ResourceLocation modelId, int minData, int maxData, boolean extra) {
        if (modelId == null) {
            throw new IllegalArgumentException("数据模型区间原料必须指定模型 id");
        }
        if (maxData < minData) {
            throw new IllegalArgumentException("数据模型区间原料的上界不能小于下界");
        }
        this.modelId = modelId;
        this.minData = minData;
        this.maxData = maxData;
        this.extra = extra;
    }

    /** 用指定品级构造区间原料；模型或品级不可用时返回 null。 */
    static Ingredient of(DataModel model, ModelTier tier) {
        if (model == null || tier == null) return null;
        ResourceLocation id = DataModelRegistry.INSTANCE.getKey(model);
        if (id == null) return null;
        int min = Math.max(0, model.getTierData(tier));
        int max = tierUpperBound(model, tier);
        if (max < min) return null;
        return com.sorrowmist.uselesscore.api.recipe.CoreCustomIngredient.wrap(new DataModelRangeIngredient(id, min, max));
    }

    /** New recipes use one canonical data model at the exact tier boundary. Iteration counters are ignored. */
    public static Ingredient atBoundary(DataModel model, ModelTier tier) {
        if (model == null || tier == null) return null;
        ResourceLocation id = DataModelRegistry.INSTANCE.getKey(model);
        if (id == null) return null;
        int boundary = Math.max(0, model.getTierData(tier));
        return CoreCustomIngredient.wrap(new DataModelRangeIngredient(id, boundary, boundary));
    }

    /** EHNN templates require exactly four identical model IDs, not partial-NBT list membership. */
    public static Ingredient extraBoundary(DataModel model, int data) {
        ResourceLocation id = model == null ? null : DataModelRegistry.INSTANCE.getKey(model);
        return id == null ? Ingredient.EMPTY : CoreCustomIngredient.wrap(new DataModelRangeIngredient(id, data, data, true));
    }

    /** 下一品级的起始数据减一即为本品级上界；已是最高品级时没有上界。 */
    private static int tierUpperBound(DataModel model, ModelTier tier) {
        List<ModelTier> tiers = List.of(ModelTier.values());
        int index = tiers.indexOf(tier);
        if (index < 0 || index + 1 >= tiers.size()) return UNBOUNDED;
        int next = model.getTierData(tiers.get(index + 1));
        return next <= 0 ? UNBOUNDED : next - 1;
    }

    ResourceLocation modelId() {
        return this.modelId;
    }

    int minData() {
        return this.minData;
    }

    int maxData() {
        return this.maxData;
    }

    @Override
    public boolean test(ItemStack stack) {
        if (this.extra) {
            if (stack == null || stack.isEmpty() || !net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
                    .equals(new ResourceLocation("extrahnn", "extra_data_model"))) return false;
            var tag = stack.getTagElement("data_model");
            if (tag == null || !DataModelRegistry.INSTANCE.holder(modelId).isBound()) return false;
            var ids = tag.getList("ids", 8);
            if (ids.size() != 4) return false;
            if (!ModelBoundaryRules.sameFour(modelId.toString(), List.of(
                    ids.getString(0), ids.getString(1), ids.getString(2), ids.getString(3)))) return false;
            int data = tag.getInt("data");
            return data >= minData && data <= maxData;
        }
        if (stack == null || stack.isEmpty() || !stack.is(Hostile.Items.DATA_MODEL.get())) return false;
        try {
            DynamicHolder<DataModel> stored = DataModelItem.getStoredModel(stack);
            if (!stored.isBound()) return false;
            if (!this.modelId.equals(DataModelRegistry.INSTANCE.getKey(stored.get()))) return false;
            int data = DataModelItem.getData(stack);
            return data >= this.minData && data <= this.maxData;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public Stream<ItemStack> getItems() {
        ItemStack representative = this.representative();
        return representative.isEmpty() ? Stream.empty() : Stream.of(representative);
    }

    /** 返回一个位于区间下界的展示用模型，用于配方展示与代表性编码。 */
    private ItemStack representative() {
        try {
            DynamicHolder<DataModel> holder = DataModelRegistry.INSTANCE.holder(this.modelId);
            if (!holder.isBound()) return ItemStack.EMPTY;
            if (extra) {
                ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        new ResourceLocation("extrahnn", "extra_data_model")));
                if (stack.isEmpty()) return stack;
                var ids = new net.minecraft.nbt.ListTag();
                for (int i = 0; i < 4; i++) ids.add(net.minecraft.nbt.StringTag.valueOf(modelId.toString()));
                var tag = stack.getOrCreateTagElement("data_model");
                tag.put("ids", ids); tag.putInt("data", minData);
                return stack;
            }
            ItemStack stack = new ItemStack(Hostile.Items.DATA_MODEL.get());
            DataModelItem.setStoredModel(stack, holder.get());
            DataModelItem.setData(stack, this.minData);
            return stack;
        } catch (RuntimeException exception) {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public CoreIngredientType<?> getType() {
        java.util.function.Supplier<CoreIngredientType<DataModelRangeIngredient>> holder =
                ModIngredientTypes.DATA_MODEL_RANGE;
        if (holder == null) {
            // 该类型只在 HNN 存在时才会被注册和实例化，走到这里说明前置缺失。
            throw new IllegalStateException(
                    "data_model_range 需要 Hostile Neural Networks，但该模组未安装");
        }
        return holder.get();
    }

    @Override
    public boolean equals(Object object) {
        return this == object
                || object instanceof DataModelRangeIngredient other
                && this.minData == other.minData
                && this.maxData == other.maxData
                && this.extra == other.extra
                && this.modelId.equals(other.modelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.modelId, this.minData, this.maxData, this.extra);
    }
}
