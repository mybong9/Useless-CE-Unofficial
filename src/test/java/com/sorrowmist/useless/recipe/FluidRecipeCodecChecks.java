package com.sorrowmist.useless.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.sorrowmist.useless.content.recipe.StrictOptionalRecipeField;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Focused checks for fail-closed optional input decoding and the shipped recipe quantities. */
public final class FluidRecipeCodecChecks {
    public static void main(String[] args) throws Exception {
        var field = StrictOptionalRecipeField.of("input_fluids", Codec.LONG.listOf(), List.<Long>of()).codec();
        check(field.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).result().orElseThrow().isEmpty(), "Absent field can default");
        check(field.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"input_fluids\":[10000]}")).result().orElseThrow().equals(List.of(10000L)), "Keep full quantity");
        check(field.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"input_fluids\":{}}")).error().isPresent(), "Declared wrong shape must fail");
        check(field.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"input_fluids\":[\"invalid\"]}")).error().isPresent(), "Invalid entry must fail");
        check(field.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"input_fluids\":null}")).error().isPresent(), "Declared null must fail");

        Path root = Path.of(args[0]);
        JsonObject blood = recipe(root.resolve("bloodmagic/life_essence_to_demon_will.json"));
        checkFluid(blood, "bloodmagic:life_essence_fluid", 10000L);
        check(blood.getAsJsonArray("outputs").get(0).getAsJsonObject().getAsJsonObject("tag").get("souls").getAsDouble() == 100.0, "Exactly 100 will");
        check(blood.getAsJsonArray("conditions").get(0).getAsJsonObject().get("modid").getAsString().equals("bloodmagic"), "Blood Magic stays optional");
        checkFluid(recipe(root.resolve("ingot/useful_ingot.json")), "minecraft:lava", 10000000L);
        System.out.println("Fluid recipe checks passed: strict input field, canonical fluid leaves, life essence 10000 mB, will 100.");
    }

    private static JsonObject recipe(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static void checkFluid(JsonObject recipe, String id, long amount) {
        var inputs = recipe.getAsJsonArray("input_fluids");
        check(inputs.size() == 1, "One mandatory fluid input");
        var input = inputs.get(0).getAsJsonObject();
        check(input.get("amount").getAsLong() == amount, "Full fluid amount");
        var leaf = input.getAsJsonArray("ingredient").get(0).getAsJsonObject();
        check(leaf.get("type").getAsString().equals("single"), "Native single-fluid leaf");
        var stack = leaf.getAsJsonObject("fluid");
        check(stack.get("FluidName").getAsString().equals(id) && stack.get("Amount").getAsInt() == 1,
                "Forge FluidStack field names and positive representative amount");
    }

    private static void check(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }
}
