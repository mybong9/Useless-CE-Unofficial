package com.sorrowmist.useless.content.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreByteBufCodecs;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

public record CountedIngredient(
        Ingredient ingredient,
        long count
) {
    static final CoreStreamCodec<FriendlyByteBuf, Ingredient> INGREDIENT_STREAM_CODEC =
            CoreByteBufCodecs.fromCodecWithRegistries(com.sorrowmist.uselesscore.api.recipe.CoreIngredientCodecs.INGREDIENT);

    public static final CoreStreamCodec<FriendlyByteBuf, CountedIngredient> STREAM_CODEC =
            CoreStreamCodec.composite(
                    INGREDIENT_STREAM_CODEC, CountedIngredient::ingredient,
                    CoreByteBufCodecs.VAR_LONG, CountedIngredient::count,
                    CountedIngredient::new
            );
    static final MapCodec<CountedIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.sorrowmist.uselesscore.api.recipe.CoreIngredientCodecs.INGREDIENT.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
            Codec.LONG.optionalFieldOf("count", 1L).forGetter(CountedIngredient::count)
    ).apply(instance, CountedIngredient::new));
}
