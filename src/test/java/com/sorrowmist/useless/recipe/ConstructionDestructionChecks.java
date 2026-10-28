package com.sorrowmist.useless.recipe;

import com.sorrowmist.useless.compat.constructionwand.ConstructionDestructionSelection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Key gating must protect single-block destruction and retain the full chain scan, including depth. */
public final class ConstructionDestructionChecks {
    public static void main(String[] args) {
        AtomicInteger scans = new AtomicInteger();
        var volume = List.of("0,0,0", "0,0,1", "0,1,1", "1,1,1");
        var single = ConstructionDestructionSelection.targets("0,0,0", false, () -> {
            scans.incrementAndGet(); return volume;
        });
        check(single.equals(List.of("0,0,0")) && scans.get() == 0, "Unheld key must neither scan nor destroy a plane");
        var chained = ConstructionDestructionSelection.targets("0,0,0", true, () -> {
            scans.incrementAndGet(); return volume;
        });
        check(chained.equals(volume) && scans.get() == 1, "Held key must preserve the complete 3D scan and its order");
        check(ConstructionDestructionSelection.targets("0,0,0", false, () -> volume).size() == 1,
                "Releasing the key must immediately return to one block");
        check(ConstructionDestructionSelection.targets("0,0,0", true, List::<String>of).isEmpty(),
                "Rejected chain scan must not fall back to destroying unapproved blocks");
        System.out.println("Construction destruction checks passed: single, held-chain depth, release, rejected scan.");
    }
    private static void check(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }
}
