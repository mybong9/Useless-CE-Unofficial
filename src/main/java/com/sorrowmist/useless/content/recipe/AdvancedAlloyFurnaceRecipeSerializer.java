package com.sorrowmist.useless.content.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.recipe.CoreRecipeSerializer;

public class AdvancedAlloyFurnaceRecipeSerializer implements CoreRecipeSerializer<AdvancedAlloyFurnaceRecipe> {
    public static final AdvancedAlloyFurnaceRecipeSerializer INSTANCE = new AdvancedAlloyFurnaceRecipeSerializer();

    @Override public MapCodec<AdvancedAlloyFurnaceRecipe> codec() {return AdvancedAlloyFurnaceRecipe.CODEC;}

    @Override
    public CoreStreamCodec<FriendlyByteBuf, AdvancedAlloyFurnaceRecipe> streamCodec() {return AdvancedAlloyFurnaceRecipe.STREAM_CODEC;}
}
