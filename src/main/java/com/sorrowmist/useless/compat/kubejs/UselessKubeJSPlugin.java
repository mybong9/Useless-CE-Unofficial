package com.sorrowmist.useless.compat.kubejs;

import com.sorrowmist.useless.init.ModRecipeSerializers;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.util.ClassFilter;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.recipe.schema.RecipeComponentFactoryRegistryEvent;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;

public final class UselessKubeJSPlugin extends KubeJSPlugin {
    @Override public void registerClasses(ScriptType type, ClassFilter filter) {
        filter.allow("com.sorrowmist.useless.compat.kubejs");
        filter.allow("com.sorrowmist.uselesscore.api");
    }
    @Override public void registerRecipeComponents(RecipeComponentFactoryRegistryEvent registry) {
        registry.register("generic_stack", GenericStackRecipeComponent.GENERIC_STACK);
        registry.register("kubejs:generic_stack", GenericStackRecipeComponent.GENERIC_STACK);
    }
    @Override public void registerRecipeSchemas(RegisterRecipeSchemasEvent registry) {
        registry.register(ModRecipeSerializers.ADVANCED_ALLOY_FURNACE_SERIALIZER.getId(),
                AdvancedAlloyFurnaceRecipeSchema.SCHEMA);
    }
}