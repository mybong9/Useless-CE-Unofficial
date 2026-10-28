package com.sorrowmist.useless.compat.constructionwand;

import java.util.List;
import java.util.function.Supplier;

/** Destruction never expands beyond the clicked block unless the chain key is held. */
public final class ConstructionDestructionSelection {
    private ConstructionDestructionSelection() {}

    public static <T> List<T> targets(T origin, boolean chainHeld, Supplier<List<T>> chainScan) {
        return chainHeld ? List.copyOf(chainScan.get()) : List.of(origin);
    }
}
