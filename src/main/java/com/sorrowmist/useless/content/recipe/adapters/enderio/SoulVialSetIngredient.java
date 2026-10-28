package com.sorrowmist.useless.content.recipe.adapters.enderio;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sorrowmist.useless.init.ModIngredientTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.sorrowmist.uselesscore.api.recipe.CoreCustomIngredient;
import com.sorrowmist.uselesscore.api.recipe.CoreIngredientType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** A component-exact OR ingredient used for filtered Ender IO soul-vial displays. */
public final class SoulVialSetIngredient implements CoreCustomIngredient {
    public static final MapCodec<SoulVialSetIngredient> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    com.sorrowmist.uselesscore.api.recipe.CoreItemStackCodecs.STACK.listOf().fieldOf("vials")
                            .forGetter(SoulVialSetIngredient::vials)
            ).apply(instance, SoulVialSetIngredient::new));

    public static final CoreIngredientType<SoulVialSetIngredient> TYPE = new CoreIngredientType<>(CODEC);

    private final List<ItemStack> vials;

    SoulVialSetIngredient(List<ItemStack> vials) {
        List<ItemStack> normalized = new ArrayList<>();
        if (vials != null) {
            for (ItemStack vial : vials) {
                if (vial == null || vial.isEmpty()) {
                    continue;
                }
                ItemStack single = vial.copyWithCount(1);
                if (normalized.stream().noneMatch(existing ->
                        ItemStack.isSameItemSameTags(existing, single))) {
                    normalized.add(single);
                }
            }
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("A soul-vial set ingredient needs at least one vial");
        }
        this.vials = List.copyOf(normalized);
    }

    static Ingredient of(List<ItemStack> vials) {
        return com.sorrowmist.uselesscore.api.recipe.CoreCustomIngredient.wrap(new SoulVialSetIngredient(vials));
    }

    List<ItemStack> vials() {
        return vials.stream().map(ItemStack::copy).toList();
    }

    @Override
    public boolean test(ItemStack stack) {
        return stack != null && !stack.isEmpty() && vials.stream()
                .anyMatch(vial -> ItemStack.isSameItemSameTags(vial, stack));
    }

    @Override
    public Stream<ItemStack> getItems() {
        return vials.stream().map(ItemStack::copy);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public CoreIngredientType<?> getType() {
        return ModIngredientTypes.SOUL_VIAL_SET.get();
    }

    @Override
    public boolean equals(Object object) {
        return this == object
                || object instanceof SoulVialSetIngredient other
                && vials.equals(other.vials);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vials);
    }
}
