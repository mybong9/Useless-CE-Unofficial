package com.sorrowmist.useless.content.recipe;


import net.minecraft.world.Container;
import appeng.api.stacks.GenericStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sorrowmist.uselesscore.api.enums.AlloyFurnaceMode;
import com.sorrowmist.useless.init.ModRecipeSerializers;
import com.sorrowmist.useless.init.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreByteBufCodecs;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import com.sorrowmist.uselesscore.api.fluid.SizedFluidIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record AdvancedAlloyFurnaceRecipe(
        ResourceLocation id,
        List<CountedIngredient> inputs,
        List<LongSizedFluidIngredient> inputFluids,
        List<GenericStack> keyInputs,
        List<ItemStack> outputs,
        List<FluidStack> outputFluids,
        List<GenericStack> keyOutputs,
        long energy,
        int processTime,
        Ingredient catalyst,
        int catalystUses,
        List<Ingredient> molds,
        AlloyFurnaceMode mode,
        int tier
) implements com.sorrowmist.uselesscore.api.recipe.CoreRecipe<Container> {

    @Override public ResourceLocation getId() { return id; }

    /** A negative tier means that the recipe uses the ID-based configuration rule. */
    public static final int NO_EXPLICIT_TIER = -1;
    public static final int MIN_TIER = 0;
    public static final int MAX_TIER = 10;

    private static final int SIZED_FLUID_NETWORK_VERSION = -1;
    private static final int LONG_SIZED_FLUID_NETWORK_VERSION = -2;
    private static final Codec<List<LongSizedFluidIngredient>> INPUT_FLUIDS_CODEC = Codec.either(
            LongSizedFluidIngredient.CODEC.codec().listOf(),
            com.sorrowmist.uselesscore.api.fluid.CoreFluidStackCodecs.STACK.listOf().flatXmap(
                    stacks -> stacks.stream().anyMatch(FluidStack::isEmpty)
                            ? DataResult.error(() -> "Declared fluid input decoded to an empty fluid stack")
                            : DataResult.success(stacks), DataResult::success))
            .xmap(either -> either.map(
                            list -> list,
                            list -> list.stream().map(LongSizedFluidIngredient::from).toList()),
                    list -> com.mojang.datafixers.util.Either.left(list));

    public AdvancedAlloyFurnaceRecipe {
        if (tier < NO_EXPLICIT_TIER || tier > MAX_TIER) {
            throw new IllegalArgumentException("Alloy-furnace recipe tier must be between -1 and " + MAX_TIER);
        }
        List<Ingredient> normalizedMolds = new ArrayList<>();
        if (molds != null) {
            for (Ingredient mold : molds) {
                if (mold != null && !mold.isEmpty()) {
                    normalizedMolds.add(mold);
                }
            }
        }
        molds = List.copyOf(normalizedMolds);
        inputFluids = normalizeFluidIngredients(inputFluids);
    }

    // 网络同步 CoreStreamCodec
    public static final CoreStreamCodec<FriendlyByteBuf, AdvancedAlloyFurnaceRecipe> STREAM_CODEC = CoreStreamCodec.of(
            (buf, r) -> {
                CoreByteBufCodecs.RESOURCE_LOCATION.encode(buf, r.id());
                CountedIngredient.STREAM_CODEC.apply(CoreByteBufCodecs.list()).encode(buf, r.inputs());
                boolean fitsLegacyFormat = r.inputFluids().stream().allMatch(LongSizedFluidIngredient::fitsInt);
                buf.writeVarInt(fitsLegacyFormat
                        ? SIZED_FLUID_NETWORK_VERSION : LONG_SIZED_FLUID_NETWORK_VERSION);
                CoreByteBufCodecs.VAR_INT.encode(buf, r.inputFluids().size());
                for (LongSizedFluidIngredient inputFluid : r.inputFluids()) {
                    if (fitsLegacyFormat) {
                        SizedFluidIngredient.STREAM_CODEC.encode(buf, inputFluid.toSizedFluidIngredient());
                    } else {
                        LongSizedFluidIngredient.STREAM_CODEC.encode(buf, inputFluid);
                    }
                }
                com.sorrowmist.uselesscore.api.network.codec.CoreAeCodecs.GENERIC_STACK_STREAM.apply(CoreByteBufCodecs.list()).encode(buf, r.keyInputs());
                CoreByteBufCodecs.ITEM_STACK.apply(CoreByteBufCodecs.list()).encode(buf, r.outputs());
                CoreByteBufCodecs.FLUID_STACK.apply(CoreByteBufCodecs.list()).encode(buf, r.outputFluids());
                com.sorrowmist.uselesscore.api.network.codec.CoreAeCodecs.GENERIC_STACK_STREAM.apply(CoreByteBufCodecs.list()).encode(buf, r.keyOutputs());
                CoreByteBufCodecs.VAR_LONG.encode(buf, r.energy());
                CoreByteBufCodecs.VAR_INT.encode(buf, r.processTime());
                CountedIngredient.INGREDIENT_STREAM_CODEC.encode(buf, r.catalyst());
                CoreByteBufCodecs.VAR_INT.encode(buf, r.catalystUses());
                CountedIngredient.INGREDIENT_STREAM_CODEC.apply(CoreByteBufCodecs.list()).encode(buf, r.molds());
                CoreByteBufCodecs.STRING_UTF8.encode(buf, r.mode().getSerializedName());
                CoreByteBufCodecs.VAR_INT.encode(buf, r.tier());
            },
            buf -> {
                ResourceLocation id = CoreByteBufCodecs.RESOURCE_LOCATION.decode(buf);
                List<CountedIngredient> inputs = CountedIngredient.STREAM_CODEC.apply(CoreByteBufCodecs.list()).decode(buf);
                int fluidFormat = buf.readVarInt();
                List<LongSizedFluidIngredient> inputFluids = new ArrayList<>();
                if (fluidFormat == SIZED_FLUID_NETWORK_VERSION) {
                    int size = CoreByteBufCodecs.VAR_INT.decode(buf);
                    if (size < 0) {
                        throw new IllegalArgumentException("Negative sized fluid ingredient list length: " + size);
                    }
                    for (int i = 0; i < size; i++) {
                        inputFluids.add(LongSizedFluidIngredient.from(
                                SizedFluidIngredient.STREAM_CODEC.decode(buf)));
                    }
                } else if (fluidFormat == LONG_SIZED_FLUID_NETWORK_VERSION) {
                    int size = CoreByteBufCodecs.VAR_INT.decode(buf);
                    if (size < 0) {
                        throw new IllegalArgumentException("Negative long sized fluid ingredient list length: " + size);
                    }
                    for (int i = 0; i < size; i++) {
                        inputFluids.add(LongSizedFluidIngredient.STREAM_CODEC.decode(buf));
                    }
                } else {
                    if (fluidFormat < 0) {
                        throw new IllegalArgumentException("Unknown alloy-furnace fluid input format: " + fluidFormat);
                    }
                    for (int i = 0; i < fluidFormat; i++) {
                        inputFluids.add(LongSizedFluidIngredient.from(CoreByteBufCodecs.FLUID_STACK.decode(buf)));
                    }
                }
                List<GenericStack> keyInputs = com.sorrowmist.uselesscore.api.network.codec.CoreAeCodecs.GENERIC_STACK_STREAM.apply(CoreByteBufCodecs.list()).decode(buf);
                List<ItemStack> outputs = CoreByteBufCodecs.ITEM_STACK.apply(CoreByteBufCodecs.list()).decode(buf);
                List<FluidStack> outputFluids = CoreByteBufCodecs.FLUID_STACK.apply(CoreByteBufCodecs.list()).decode(buf);
                List<GenericStack> keyOutputs = com.sorrowmist.uselesscore.api.network.codec.CoreAeCodecs.GENERIC_STACK_STREAM.apply(CoreByteBufCodecs.list()).decode(buf);
                long energy = CoreByteBufCodecs.VAR_LONG.decode(buf);
                int processTime = CoreByteBufCodecs.VAR_INT.decode(buf);
                Ingredient catalyst = CountedIngredient.INGREDIENT_STREAM_CODEC.decode(buf);
                int catalystUses = CoreByteBufCodecs.VAR_INT.decode(buf);
                List<Ingredient> molds = CountedIngredient.INGREDIENT_STREAM_CODEC.apply(CoreByteBufCodecs.list()).decode(buf);
                String modeStr = CoreByteBufCodecs.STRING_UTF8.decode(buf);
                AlloyFurnaceMode mode = AlloyFurnaceMode.fromString(modeStr);
                int tier = CoreByteBufCodecs.VAR_INT.decode(buf);

                return new AdvancedAlloyFurnaceRecipe(
                        id, inputs, inputFluids, keyInputs, outputs, outputFluids, keyOutputs,
                        energy, processTime, catalyst, catalystUses, molds, mode, tier
                );
            }
    );
    // Forge 1.20.1 stores item data in the legacy tag field instead of component patches.
    private static final Codec<ItemStack> ITEM_STACK_CODEC = com.sorrowmist.uselesscore.api.recipe.CoreItemStackCodecs.STACK;

    // 主 Codec（JSON / datapack）
    public static final MapCodec<AdvancedAlloyFurnaceRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("id").forGetter(AdvancedAlloyFurnaceRecipe::id),
                    CountedIngredient.CODEC.codec().listOf().fieldOf("ingredients")
                                           .forGetter(AdvancedAlloyFurnaceRecipe::inputs),
                    StrictOptionalRecipeField.of("input_fluids", INPUT_FLUIDS_CODEC, List.of())
                                    .forGetter(AdvancedAlloyFurnaceRecipe::inputFluids),
                    com.sorrowmist.uselesscore.api.network.codec.CoreAeCodecs.GENERIC_STACK.listOf().optionalFieldOf("key_inputs", List.of())
                                    .forGetter(AdvancedAlloyFurnaceRecipe::keyInputs),
                    ITEM_STACK_CODEC.listOf().optionalFieldOf("outputs", List.of())
                            .forGetter(AdvancedAlloyFurnaceRecipe::outputs),
                    com.sorrowmist.uselesscore.api.fluid.CoreFluidStackCodecs.STACK.listOf().optionalFieldOf("output_fluids", List.of())
                                    .forGetter(AdvancedAlloyFurnaceRecipe::outputFluids),
                    com.sorrowmist.uselesscore.api.network.codec.CoreAeCodecs.GENERIC_STACK.listOf().optionalFieldOf("key_outputs", List.of())
                                    .forGetter(AdvancedAlloyFurnaceRecipe::keyOutputs),
                    Codec.LONG.optionalFieldOf("energy", 2000L).forGetter(AdvancedAlloyFurnaceRecipe::energy),
                    Codec.INT.optionalFieldOf("process_time", 200).forGetter(AdvancedAlloyFurnaceRecipe::processTime),
                    com.sorrowmist.uselesscore.api.recipe.CoreIngredientCodecs.INGREDIENT.optionalFieldOf("catalyst", Ingredient.EMPTY)
                                    .forGetter(AdvancedAlloyFurnaceRecipe::catalyst),
                    Codec.INT.optionalFieldOf("catalyst_uses", 0).forGetter(AdvancedAlloyFurnaceRecipe::catalystUses),
                    com.sorrowmist.uselesscore.api.recipe.CoreIngredientCodecs.INGREDIENT.optionalFieldOf("mold")
                                    .forGetter(AdvancedAlloyFurnaceRecipe::legacyMold),
                    com.sorrowmist.uselesscore.api.recipe.CoreIngredientCodecs.INGREDIENT.listOf().optionalFieldOf("molds")
                                    .forGetter(AdvancedAlloyFurnaceRecipe::multipleMolds),
                    AlloyFurnaceMode.CODEC.optionalFieldOf("mode", AlloyFurnaceMode.NORMAL)
                                          .forGetter(AdvancedAlloyFurnaceRecipe::mode),
                    Codec.intRange(NO_EXPLICIT_TIER, MAX_TIER)
                            .optionalFieldOf("tier", NO_EXPLICIT_TIER)
                            .forGetter(AdvancedAlloyFurnaceRecipe::tier)
            ).apply(instance, AdvancedAlloyFurnaceRecipe::fromCodec));

    private static Optional<Ingredient> legacyMold(AdvancedAlloyFurnaceRecipe recipe) {
        return recipe.molds.size() == 1 ? Optional.of(recipe.molds.get(0)) : Optional.empty();
    }

    private static Optional<List<Ingredient>> multipleMolds(AdvancedAlloyFurnaceRecipe recipe) {
        return recipe.molds.size() > 1 ? Optional.of(recipe.molds) : Optional.empty();
    }

    private static AdvancedAlloyFurnaceRecipe fromCodec(
            ResourceLocation id,
            List<CountedIngredient> inputs,
            List<LongSizedFluidIngredient> inputFluids,
            List<GenericStack> keyInputs,
            List<ItemStack> outputs,
            List<FluidStack> outputFluids,
            List<GenericStack> keyOutputs,
            long energy,
            int processTime,
            Ingredient catalyst,
            int catalystUses,
            Optional<Ingredient> legacyMold,
            Optional<List<Ingredient>> multipleMolds,
            AlloyFurnaceMode mode,
            int tier) {
        if (legacyMold.isPresent() && multipleMolds.isPresent()) {
            throw new IllegalArgumentException("Advanced alloy-furnace recipe cannot define both mold and molds");
        }
        List<Ingredient> molds = multipleMolds.orElseGet(() ->
                legacyMold.map(List::of).orElseGet(List::of));
        return new AdvancedAlloyFurnaceRecipe(
                id, inputs, inputFluids, keyInputs, outputs, outputFluids, keyOutputs,
                energy, processTime, catalyst, catalystUses, molds, mode, tier);
    }

    public AdvancedAlloyFurnaceRecipe(ResourceLocation id,
                                      List<CountedIngredient> inputs,
                                      Iterable<?> inputFluids,
                                      List<GenericStack> keyInputs,
                                      List<ItemStack> outputs,
                                      List<FluidStack> outputFluids,
                                      List<GenericStack> keyOutputs,
                                      long energy,
                                      int processTime,
                                      Ingredient catalyst,
                                      int catalystUses,
                                      List<Ingredient> molds,
                                      AlloyFurnaceMode mode) {
        this(id, inputs, convertLegacyFluids(inputFluids), keyInputs, outputs, outputFluids, keyOutputs,
                energy, processTime, catalyst, catalystUses, molds, mode, NO_EXPLICIT_TIER);
    }

    public AdvancedAlloyFurnaceRecipe(ResourceLocation id,
                                      List<CountedIngredient> inputs,
                                      Iterable<?> inputFluids,
                                      List<ItemStack> outputs,
                                      List<FluidStack> outputFluids,
                                      long energy,
                                      int processTime,
                                      Ingredient catalyst,
                                      int catalystUses,
                                      Ingredient mold,
                                      AlloyFurnaceMode mode) {
        this(id, inputs, convertLegacyFluids(inputFluids), List.of(), outputs, outputFluids, List.of(), energy, processTime, catalyst, catalystUses,
                mold == null ? List.of() : List.of(mold), mode, NO_EXPLICIT_TIER);
    }

    public AdvancedAlloyFurnaceRecipe(ResourceLocation id,
                                      List<CountedIngredient> inputs,
                                      Iterable<?> inputFluids,
                                      List<GenericStack> keyInputs,
                                      List<ItemStack> outputs,
                                      List<FluidStack> outputFluids,
                                      List<GenericStack> keyOutputs,
                                      long energy,
                                      int processTime,
                                      Ingredient catalyst,
                                      int catalystUses,
                                      Ingredient mold,
                                      AlloyFurnaceMode mode) {
        this(id, inputs, convertLegacyFluids(inputFluids), keyInputs, outputs, outputFluids, keyOutputs,
                energy, processTime, catalyst, catalystUses,
                mold == null ? List.of() : List.of(mold), mode, NO_EXPLICIT_TIER);
    }

    private static List<LongSizedFluidIngredient> convertLegacyFluids(Iterable<?> fluids) {
        if (fluids == null) return List.of();
        List<LongSizedFluidIngredient> result = new ArrayList<>();
        for (Object value : fluids) {
            if (value instanceof LongSizedFluidIngredient ingredient
                    && ingredient.ingredient() != null
                    && !ingredient.ingredient().isEmpty()
                    && ingredient.amount() > 0) {
                result.add(ingredient);
            } else if (value instanceof SizedFluidIngredient ingredient
                    && ingredient.ingredient() != null
                    && !ingredient.ingredient().isEmpty()
                    && ingredient.amount() > 0) {
                result.add(LongSizedFluidIngredient.from(ingredient));
            } else if (value instanceof FluidStack fluid
                    && !fluid.isEmpty() && fluid.getAmount() > 0) {
                result.add(LongSizedFluidIngredient.from(fluid));
            }
        }
        return result;
    }

    private static List<LongSizedFluidIngredient> normalizeFluidIngredients(List<LongSizedFluidIngredient> fluids) {
        if (fluids == null || fluids.isEmpty()) return List.of();
        List<LongSizedFluidIngredient> normalized = new ArrayList<>();
        for (LongSizedFluidIngredient fluid : fluids) {
            if (fluid == null || fluid.ingredient() == null || fluid.amount() <= 0)
                throw new IllegalArgumentException("Invalid declared fluid input");
            // Tags may resolve after recipe decoding. Keep every requirement so an unresolved
            // ingredient blocks matching/encoding rather than disappearing from the recipe.
            int existing = -1;
            for (int i = 0; i < normalized.size(); i++) {
                if (normalized.get(i).ingredient().equals(fluid.ingredient())) {
                    existing = i;
                    break;
                }
            }
            if (existing < 0) {
                normalized.add(fluid);
            } else {
                long amount;
                try {
                    amount = Math.addExact(normalized.get(existing).amount(), fluid.amount());
                } catch (ArithmeticException exception) {
                    throw new IllegalArgumentException("Fluid ingredient amount exceeds long range", exception);
                }
                normalized.set(existing, new LongSizedFluidIngredient(fluid.ingredient(), amount));
            }
        }
        return List.copyOf(normalized);
    }

    /** Compatibility accessor for the historical single-mold API. */
    public Ingredient mold() {
        return this.molds.isEmpty() ? Ingredient.EMPTY : this.molds.get(0);
    }

    // ────────────── 以下是 Recipe 接口的占位实现 ──────────────

    @Override public boolean matches(Container recipeInput, Level level) {
        return false;   // 机器自己匹配，不走原版系统
    }

    @Override public ItemStack assemble(Container recipeInput, HolderLookup.Provider provider) {
        return this.outputs.isEmpty() ? ItemStack.EMPTY : this.outputs.get(0).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.assemble(null, registries);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        this.inputs.forEach(ci -> list.add(ci.ingredient()));
        return list;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.ADVANCED_ALLOY_FURNACE_SERIALIZER.get(); // 替换成你实际的注册对象
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.ADVANCED_ALLOY_FURNACE_TYPE.get(); // 替换成你实际的注册对象
    }
}
