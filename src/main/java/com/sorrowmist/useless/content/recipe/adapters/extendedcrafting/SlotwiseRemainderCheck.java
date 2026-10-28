package com.sorrowmist.useless.content.recipe.adapters.extendedcrafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

/** Checks invariant remainders for native recipes whose transformers depend on one slot only. */
public final class SlotwiseRemainderCheck {
    private SlotwiseRemainderCheck() {}

    public static <T, R> Optional<R> resolve(
            List<? extends List<T>> candidates,
            Function<T, T> copy,
            Function<List<T>, Optional<R>> resolve,
            BiPredicate<R, R> same) {
        try {
            var baseline = new ArrayList<T>(candidates.size());
            for (List<T> options : candidates) {
                if (options.isEmpty()) return Optional.empty();
                baseline.add(options.get(0));
            }
            Optional<R> expected = resolve.apply(copy(baseline, copy));
            if (expected.isEmpty()) return Optional.empty();
            for (int slot = 0; slot < candidates.size(); slot++) {
                // The first alternative is already covered by the baseline.
                for (int option = 1; option < candidates.get(slot).size(); option++) {
                    var variant = copy(baseline, copy);
                    variant.set(slot, copy.apply(candidates.get(slot).get(option)));
                    Optional<R> actual = resolve.apply(variant);
                    if (actual.isEmpty() || !same.test(expected.get(), actual.get())) {
                        return Optional.empty();
                    }
                }
            }
            return expected;
        } catch (RuntimeException invalid) {
            return Optional.empty();
        }
    }

    private static <T> List<T> copy(List<T> source, Function<T, T> copy) {
        var result = new ArrayList<T>(source.size());
        for (T value : source) result.add(copy.apply(value));
        return result;
    }
}
