package com.sorrowmist.useless.content.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreByteBufCodecs;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import net.minecraftforge.fluids.FluidStack;
import com.sorrowmist.uselesscore.api.fluid.FluidIngredient;
import com.sorrowmist.uselesscore.api.fluid.SizedFluidIngredient;

import java.util.Arrays;
import java.util.Objects;

/** A fluid ingredient whose required amount is not limited to FluidStack's int count. */
public record LongSizedFluidIngredient(FluidIngredient ingredient, long amount) {
    public static final MapCodec<LongSizedFluidIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FluidIngredient.CODEC.fieldOf("ingredient").forGetter(LongSizedFluidIngredient::ingredient),
            Codec.LONG.fieldOf("amount").forGetter(LongSizedFluidIngredient::amount)
    ).apply(instance, LongSizedFluidIngredient::new));

    public static final CoreStreamCodec<FriendlyByteBuf, LongSizedFluidIngredient> STREAM_CODEC =
            CoreStreamCodec.composite(
                    FluidIngredient.STREAM_CODEC, LongSizedFluidIngredient::ingredient,
                    CoreByteBufCodecs.VAR_LONG, LongSizedFluidIngredient::amount,
                    LongSizedFluidIngredient::new
            );

    public LongSizedFluidIngredient {
        Objects.requireNonNull(ingredient, "ingredient");
        if (amount <= 0L) {
            throw new IllegalArgumentException("Fluid ingredient amount must be positive");
        }
    }

    public static LongSizedFluidIngredient from(SizedFluidIngredient ingredient) {
        Objects.requireNonNull(ingredient, "ingredient");
        return new LongSizedFluidIngredient(ingredient.ingredient(), ingredient.amount());
    }

    public static LongSizedFluidIngredient from(FluidStack stack) {
        return new LongSizedFluidIngredient(
                Objects.requireNonNull(AdapterUtils.toSizedFluidIngredient(stack), "stack").ingredient(),
                stack.getAmount());
    }

    /** Returns fluid candidates using a representable display amount. */
    public FluidStack[] getFluids() {
        int displayAmount = (int) Math.min(amount, Integer.MAX_VALUE);
        return Arrays.stream(ingredient.getStacks())
                .filter(stack -> stack != null && !stack.isEmpty())
                .map(stack -> com.sorrowmist.uselesscore.api.fluid.CoreFluidStacks.copyWithAmount(stack, displayAmount))
                .toArray(FluidStack[]::new);
    }

    /** Returns unit representatives for predicates, tags, components, and AE key creation. */
    public FluidStack[] getRepresentatives() {
        return Arrays.stream(ingredient.getStacks())
                .filter(stack -> stack != null && !stack.isEmpty())
                .map(stack -> com.sorrowmist.uselesscore.api.fluid.CoreFluidStacks.copyWithAmount(stack, 1))
                .toArray(FluidStack[]::new);
    }

    public boolean fitsInt() {
        return amount <= Integer.MAX_VALUE;
    }

    public SizedFluidIngredient toSizedFluidIngredient() {
        if (!fitsInt()) {
            throw new IllegalStateException("Fluid ingredient amount exceeds FluidStack's int range");
        }
        return new SizedFluidIngredient(ingredient, (int) amount);
    }
}
