package com.sorrowmist.useless.recipe;

import com.sorrowmist.useless.content.recipe.adapters.extendedcrafting.SlotwiseRemainderCheck;
import com.sorrowmist.useless.content.recipe.adapters.nuclearcraft.ProcessorRecipeCosts;
import com.sorrowmist.useless.content.recipe.adapters.pneumaticcraft.StackedAssemblyInputJson;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** Runs the production remainder traversal and cost arithmetic without starting Minecraft. */
public final class OmniversalCompatChecks {
    private record Stack(String item, String tag, int count) {
        Stack copy() { return new Stack(item, tag, count); }
    }

    public static void main(String[] args) {
        StructurePreviewChecks.run();
        largeTable();
        variableRemainders();
        mutationIsolation();
        processorCosts();
        assemblyIngredients();
        System.out.println("Omniversal compatibility checks passed: 81-slot alternatives, exact remainders, NuclearCraft costs, PneumaticCraft stacked item/tag normalization and mutation isolation.");
    }

    private static void largeTable() {
        // 3^81 combinations used to exceed the 512-variant fallback almost immediately.
        var candidates = new ArrayList<List<Stack>>();
        for (int slot = 0; slot < 81; slot++) {
            candidates.add(List.of(new Stack("ore_a", "slot=" + slot, 1),
                    new Stack("ore_b", "slot=" + slot, 1), new Stack("ore_c", "slot=" + slot, 1)));
        }
        var calls = new AtomicInteger();
        List<Stack> expected = List.of(new Stack("bucket", "", 3), new Stack("catalyst", "id=solar", 1));
        var actual = SlotwiseRemainderCheck.resolve(candidates, Stack::copy, stacks -> {
            check(stacks.size() == 81, "no table slot may be dropped");
            for (int slot = 0; slot < stacks.size(); slot++) {
                check(stacks.get(slot).tag().equals("slot=" + slot), "slot and NBT identity must stay paired");
            }
            calls.incrementAndGet();
            return Optional.of(expected);
        }, List::equals);
        check(actual.orElseThrow().equals(expected), "return all buckets and catalysts");
        check(calls.get() == 163, "evaluate baseline and every one-slot substitution without a Cartesian product");
    }

    private static void variableRemainders() {
        var candidates = List.of(List.of(new Stack("filled", "a", 1), new Stack("filled", "b", 1)));
        check(SlotwiseRemainderCheck.resolve(candidates, Stack::copy,
                stacks -> Optional.of(List.of(new Stack("vessel", stacks.get(0).tag(), 1))), List::equals).isEmpty(),
                "NBT-dependent returns cannot be flattened into one output");
        check(SlotwiseRemainderCheck.resolve(candidates, Stack::copy,
                stacks -> Optional.of(stacks.get(0).tag().equals("a") ? 1 : 2), Integer::equals).isEmpty(),
                "different return counts must be rejected");
        check(SlotwiseRemainderCheck.resolve(List.<List<Stack>>of(List.of()), Stack::copy,
                stacks -> Optional.of(1), Integer::equals).isEmpty(), "unresolved inputs must be rejected");
        check(SlotwiseRemainderCheck.resolve(candidates, Stack::copy,
                stacks -> Optional.<Integer>empty(), Integer::equals).isEmpty(), "resolver rejection must propagate");
        check(SlotwiseRemainderCheck.resolve(candidates, Stack::copy,
                stacks -> { throw new IllegalStateException("unsupported transformer"); }, Integer::equals).isEmpty(),
                "one invalid recipe must not abort catalog conversion");
    }

    private static void mutationIsolation() {
        var first = new int[]{1};
        var second = new int[]{2};
        var result = SlotwiseRemainderCheck.resolve(List.of(List.of(first, second)), int[]::clone,
                stacks -> { stacks.get(0)[0] = 99; return Optional.of(1); }, Integer::equals);
        check(result.orElseThrow() == 1 && first[0] == 1 && second[0] == 2,
                "native transformers must receive private input copies");
    }

    private static void processorCosts() {
        int ticks = ProcessorRecipeCosts.ticks(200, 0.333);
        check(ticks == 66, "NuclearCraft floors fractional processing time");
        check(ProcessorRecipeCosts.energy(80, 0.333, ticks) == 1716,
                "floor FE/tick before multiplying by ticks");
        check(ProcessorRecipeCosts.ticks(200, 0) == 1, "zero native duration uses one furnace tick");
        check(ProcessorRecipeCosts.energy(0, 1, 200) == 0, "zero-power recipes remain free");
        check(ProcessorRecipeCosts.energy(Integer.MAX_VALUE, 1, Integer.MAX_VALUE) == 4611686014132420609L,
                "valid large totals must use long arithmetic");
        rejected(() -> ProcessorRecipeCosts.ticks(200, Double.NaN));
        rejected(() -> ProcessorRecipeCosts.ticks(200, -1));
        rejected(() -> ProcessorRecipeCosts.ticks(Integer.MAX_VALUE, 2));
        rejected(() -> ProcessorRecipeCosts.energy(80, Double.POSITIVE_INFINITY, 200));
        rejected(() -> ProcessorRecipeCosts.energy(Integer.MAX_VALUE, 2, 200));
        rejected(() -> ProcessorRecipeCosts.energy(80, 1, 0));
    }

    private static void assemblyIngredients() {
        for (int count : new int[]{1, 2, 64, 128, 4096}) {
            var original = JsonParser.parseString("{\"type\":\"pneumaticcraft:stacked_item\",\"item\":\"avaritia:infinity_ingot\",\"count\":" + count + "}");
            var normalized = StackedAssemblyInputJson.withoutCount(original);
            check(normalized.equals(JsonParser.parseString("{\"item\":\"avaritia:infinity_ingot\"}")),
                    "large assembly counts must not survive as an ingredient predicate");
            check(original.getAsJsonObject().get("count").getAsInt() == count,
                    "do not mutate the source recipe JSON");
        }
        var tag = JsonParser.parseString("{\"type\":\"pneumaticcraft:stacked_item\",\"tag\":\"forge:ingots/iron\",\"count\":16}");
        check(StackedAssemblyInputJson.withoutCount(tag).equals(JsonParser.parseString("{\"tag\":\"forge:ingots/iron\"}")),
                "retain a tag for substitutions and recipe reloads, not just its current items");
        var alternatives = JsonParser.parseString("[{\"type\":\"pneumaticcraft:stacked_item\",\"tag\":\"forge:ingots/iron\",\"count\":4},{\"item\":\"minecraft:gold_ingot\"}]");
        check(StackedAssemblyInputJson.withoutCount(alternatives).equals(JsonParser.parseString("[{\"tag\":\"forge:ingots/iron\"},{\"item\":\"minecraft:gold_ingot\"}]")),
                "normalize native tag values and ItemValue serialization without a count field");
        rejected(() -> StackedAssemblyInputJson.withoutCount(JsonParser.parseString("{\"item\":\"a:b\",\"tag\":\"a:b\"}")));
    }

    private static void rejected(Runnable operation) {
        try { operation.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("invalid native cost must be rejected");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
