package com.sorrowmist.useless.content.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.Objects;
import java.util.stream.Stream;

/** Absent inputs may default; malformed declared inputs must fail instead of creating a free recipe. */
public final class StrictOptionalRecipeField {
    private StrictOptionalRecipeField() {}

    public static <A> MapCodec<A> of(String name, Codec<A> codec, A fallback) {
        return new MapCodec<>() {
            @Override public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
                T value = input.get(name);
                if (value != null) return codec.parse(ops, value);
                // JsonOps exposes an explicit JSON null as null through MapLike.get as well.
                boolean declared = input.entries().anyMatch(entry ->
                        ops.getStringValue(entry.getFirst()).result().filter(name::equals).isPresent());
                return declared ? DataResult.error(() -> "Recipe input field " + name + " must not be null")
                        : DataResult.success(fallback);
            }
            @Override public <T> RecordBuilder<T> encode(A value, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return Objects.equals(value, fallback) ? prefix : prefix.add(name, codec.encodeStart(ops, value));
            }
            @Override public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.of(ops.createString(name));
            }
        };
    }
}
