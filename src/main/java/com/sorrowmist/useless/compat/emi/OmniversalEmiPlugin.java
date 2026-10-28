package com.sorrowmist.useless.compat.emi;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.client.gui.structure.ConstructionPatternScreen;
import com.sorrowmist.useless.client.gui.structure.StructurePreview;
import com.sorrowmist.useless.content.multiblock.ConstructionMaterialBill;
import com.sorrowmist.useless.content.multiblock.OmniversalFurnaceMaterials;
import com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog;
import com.sorrowmist.useless.init.ModBlocks;
import com.sorrowmist.useless.init.ModNetwork;
import com.sorrowmist.useless.network.EncodeConstructionPatternPacket;
import com.sorrowmist.useless.network.EncodeOmniversalPatternPacket;
import com.sorrowmist.useless.network.SelectOmniversalPatternRecipePacket;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

/** Native EMI entrypoint, independent of JEI and its recipe bridge. */
@EmiEntrypoint
public final class OmniversalEmiPlugin implements EmiPlugin {
    private static final EmiRecipeCategory ALLOY = new EmiRecipeCategory(UselessMod.id("alloy_furnace"),
            EmiStack.of(ModBlocks.ADVANCED_ALLOY_FURNACE_BLOCK.get())) {
        @Override public Component getName() { return Component.translatable("block.useless_mod.advanced_alloy_furnace_block"); }
    };
    private static final EmiRecipeCategory STRUCTURE = new EmiRecipeCategory(UselessMod.id("omniversal_structure"),
            EmiStack.of(ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get())) {
        @Override public Component getName() { return StructurePreview.text("title"); }
    };

    public static void showFurnaceRecipes() { EmiApi.displayRecipeCategory(ALLOY); }

    @Override public void register(EmiRegistry registry) {
        registry.addCategory(ALLOY);
        registry.addCategory(STRUCTURE);
        // When JEI is also installed, EMI may import its copy of these pages. Keep one native copy.
        registry.removeRecipes(recipe -> !(recipe instanceof OmniversalEmiPatternRecipe)
                && (recipe.getCategory().getId().equals(UselessMod.id("advanced_alloy_furnace"))
                || recipe.getCategory().getId().equals(STRUCTURE.getId())));
        registry.addWorkstation(ALLOY, EmiStack.of(ModBlocks.ADVANCED_ALLOY_FURNACE_BLOCK.get()));
        registry.addWorkstation(ALLOY, EmiStack.of(ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get()));
        registry.addWorkstation(STRUCTURE, EmiStack.of(ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get()));
        registry.addRecipe(new StructureRecipe());
        // EMI runs deferred recipe registration during its reload, after other plugins and metadata.
        registry.addDeferredRecipes(add -> {
            var level = Minecraft.getInstance().level;
            if (level != null) for (var entry : AlloyFurnaceRecipeCatalog.entries(level)) {
                try { add.accept(new AlloyRecipe(entry)); }
                catch (RuntimeException invalid) {
                    UselessMod.LOGGER.debug("Cannot display alloy furnace recipe {} in EMI", entry.identity().recipeId(), invalid);
                }
            }
        });
    }

    private static EmiStack stack(GenericStack stack) {
        if (stack.what() instanceof AEItemKey item) return EmiStack.of(item.toStack(), stack.amount());
        if (stack.what() instanceof AEFluidKey fluid) {
            var representative = fluid.toStack(1);
            return EmiStack.of(representative.getFluid(), representative.getTag(), stack.amount());
        }
        return EmiStack.of(stack.what().wrapForDisplayOrFilter(), stack.amount());
    }

    private static final class AlloyRecipe implements OmniversalEmiPatternRecipe {
        private final AlloyFurnaceRecipeCatalog.Entry entry;
        private final List<EmiIngredient> inputs = new ArrayList<>();
        private final List<EmiStack> outputs = new ArrayList<>();
        private final List<EmiIngredient> catalysts = new ArrayList<>();
        private final List<EmiIngredient> itemInputs = new ArrayList<>();
        private final List<EmiIngredient> fluidInputs = new ArrayList<>();
        private final List<EmiStack> itemOutputs = new ArrayList<>();
        private final List<EmiStack> fluidOutputs = new ArrayList<>();

        private AlloyRecipe(AlloyFurnaceRecipeCatalog.Entry entry) {
            this.entry = entry;
            var recipe = entry.recipe();
            for (var input : recipe.inputs()) itemInputs.add(EmiIngredient.of(input.ingredient(), input.count()));
            for (var input : recipe.inputFluids()) {
                List<EmiIngredient> variants = new ArrayList<>();
                for (var fluid : input.getRepresentatives()) variants.add(EmiStack.of(fluid.getFluid(), fluid.getTag(), 1));
                fluidInputs.add(EmiIngredient.of(variants, input.amount()));
            }
            // Keep the full ingredient lists for EMI's recipe tree and pattern transfer.
            // Only their presentation is split into the JEI-style item and fluid areas.
            inputs.addAll(itemInputs);
            inputs.addAll(fluidInputs);
            for (var input : recipe.keyInputs()) {
                var ingredient = stack(input);
                inputs.add(ingredient);
                if (input.what() instanceof AEFluidKey) fluidInputs.add(ingredient);
                else itemInputs.add(ingredient);
            }
            for (var output : recipe.outputs()) itemOutputs.add(EmiStack.of(output, output.getCount()));
            for (var fluid : recipe.outputFluids()) fluidOutputs.add(EmiStack.of(fluid.getFluid(), fluid.getTag(), fluid.getAmount()));
            outputs.addAll(itemOutputs);
            outputs.addAll(fluidOutputs);
            for (var output : recipe.keyOutputs()) {
                var ingredient = stack(output);
                outputs.add(ingredient);
                if (output.what() instanceof AEFluidKey) fluidOutputs.add(ingredient);
                else itemOutputs.add(ingredient);
            }
            if (!recipe.catalyst().isEmpty()) catalysts.add(EmiIngredient.of(recipe.catalyst()));
            for (var mold : recipe.molds()) catalysts.add(EmiIngredient.of(mold));
        }

        @Override public EmiRecipeCategory getCategory() { return ALLOY; }
        @Override public ResourceLocation getId() { return AlloyFurnaceRecipeCatalog.jeiDisplayId(entry); }
        @Override public List<EmiIngredient> getInputs() { return List.copyOf(inputs); }
        @Override public List<EmiStack> getOutputs() { return List.copyOf(outputs); }
        @Override public List<EmiIngredient> getCatalysts() { return List.copyOf(catalysts); }
        @Override public int getDisplayWidth() { return AlloyFurnaceEmiWidgets.WIDTH; }
        @Override public int getDisplayHeight() { return AlloyFurnaceEmiWidgets.HEIGHT; }
        @Override public boolean canEncode() {
            return OmniversalEmiPatternRecipe.super.canEncode()
                    && inputs.stream().noneMatch(EmiIngredient::isEmpty) && outputs.stream().noneMatch(EmiStack::isEmpty);
        }
        @Override public void encode(PatternEncodingTermMenu menu) {
            ModNetwork.CHANNEL.sendToServer(new EncodeOmniversalPatternPacket(new SelectOmniversalPatternRecipePacket(
                    menu.containerId, entry.identity().recipeId(), entry.identity().fingerprint(), entry.sourceId())));
        }
        @Override public void addWidgets(WidgetHolder widgets) {
            new AlloyFurnaceEmiWidgets(this, entry.recipe(), itemInputs, fluidInputs, itemOutputs, fluidOutputs)
                    .addWidgets(widgets);
        }
    }

    private static final class StructureRecipe implements OmniversalEmiPatternRecipe {
        private final StructurePreview preview = new StructurePreview(1, (item, uses) -> {
            if (uses) EmiApi.displayUses(EmiStack.of(item)); else EmiApi.displayRecipes(EmiStack.of(item));
        });
        @Override public EmiRecipeCategory getCategory() { return STRUCTURE; }
        @Override public ResourceLocation getId() { return UselessMod.id("structure/omniversal_furnace"); }
        @Override public List<EmiIngredient> getInputs() {
            return OmniversalFurnaceMaterials.usageItems().stream().map(EmiStack::of).map(stack -> (EmiIngredient) stack).toList();
        }
        @Override public List<EmiStack> getOutputs() { return List.of(EmiStack.of(ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get())); }
        @Override public int getDisplayWidth() { return StructurePreview.viewerWidth(); }
        @Override public int getDisplayHeight() { return StructurePreview.viewerHeight() + 22; }
        @Override public boolean supportsRecipeTree() { return false; }
        @Override public boolean hideCraftable() { return true; }
        @Override public void encode(PatternEncodingTermMenu menu) {
            ModNetwork.CHANNEL.sendToServer(new EncodeConstructionPatternPacket(menu.containerId, preview.tier()));
        }
        @Override public void addWidgets(WidgetHolder widgets) {
            widgets.add(new Widget() {
                @Override public Bounds getBounds() { return new Bounds(0, 0, getDisplayWidth(), getDisplayHeight()); }
                @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                    float scale = StructurePreview.viewerScale();
                    graphics.pose().pushPose();
                    graphics.pose().scale(scale, scale, 1);
                    preview.draw(graphics, mouseX / scale, mouseY / scale);
                    graphics.pose().popPose();
                    int y = StructurePreview.viewerHeight() + 2;
                    graphics.fill(0, y, getDisplayWidth(), y + 18, 0xff28464b);
                    graphics.drawCenteredString(Minecraft.getInstance().font,
                            Component.translatable("emi.useless_mod.open_structure"), getDisplayWidth() / 2, y + 5, 0xffe9dcb6);
                }
                @Override public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
                    float scale = StructurePreview.viewerScale();
                    return preview.tooltip(mouseX / scale, mouseY / scale).stream()
                            .map(text -> ClientTooltipComponent.create(text.getVisualOrderText())).toList();
                }
                @Override public boolean mouseClicked(int mouseX, int mouseY, int button) {
                    if (mouseY >= StructurePreview.viewerHeight() + 2 && button == 0) {
                        var entries = preview.materials().stream().map(material -> new ConstructionMaterialBill.Entry(
                                BuiltInRegistries.ITEM.getKey(material.stack().getItem()).toString(), material.count())).toList();
                        ConstructionPatternScreen.open(new ConstructionMaterialBill(preview.tier(), entries));
                        return true;
                    }
                    float scale = StructurePreview.viewerScale();
                    return preview.mouseClicked(mouseX / scale, mouseY / scale, button);
                }
            });
        }
    }
}
