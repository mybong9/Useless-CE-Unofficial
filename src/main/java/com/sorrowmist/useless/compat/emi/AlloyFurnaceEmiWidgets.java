package com.sorrowmist.useless.compat.emi;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.catalyst.CatalystParallelManager;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import com.sorrowmist.useless.core.config.AlloyFurnaceTierRules;
import com.sorrowmist.useless.init.ModTags;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.TankWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Uses the JEI artwork and layout, without loading any JEI classes. */
final class AlloyFurnaceEmiWidgets {
    static final int WIDTH = 212;
    static final int HEIGHT = 72;
    private static final int ITEM_PAGE_SIZE = 12;
    private static final int FLUID_PAGE_SIZE = 4;
    private static final int MOLD_PAGE_SIZE = 3;
    private static final ResourceLocation TEXTURE = UselessMod.id("textures/gui/jei_advanced_alloy_furnace_gui.png");

    private final EmiRecipe owner;
    private final AdvancedAlloyFurnaceRecipe recipe;
    private final List<? extends EmiIngredient> itemInputs, fluidInputs, itemOutputs, fluidOutputs;
    private final List<EmiIngredient> molds;
    private final int pageCount;
    private int page;

    AlloyFurnaceEmiWidgets(EmiRecipe owner, AdvancedAlloyFurnaceRecipe recipe,
                          List<? extends EmiIngredient> itemInputs, List<? extends EmiIngredient> fluidInputs,
                          List<? extends EmiIngredient> itemOutputs, List<? extends EmiIngredient> fluidOutputs) {
        this.owner = owner;
        this.recipe = recipe;
        this.itemInputs = itemInputs;
        this.fluidInputs = fluidInputs;
        this.itemOutputs = itemOutputs;
        this.fluidOutputs = fluidOutputs;
        this.molds = recipe.molds().stream().map(EmiIngredient::of).toList();
        this.pageCount = Math.max(pages(molds, MOLD_PAGE_SIZE),
                Math.max(Math.max(pages(itemInputs, ITEM_PAGE_SIZE), pages(itemOutputs, ITEM_PAGE_SIZE)),
                        Math.max(pages(fluidInputs, FLUID_PAGE_SIZE), pages(fluidOutputs, FLUID_PAGE_SIZE))));
    }

    void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(TEXTURE, 0, 0, WIDTH, HEIGHT, 0, 0, WIDTH, HEIGHT, 256, 256);
        widgets.addDrawable(0, 0, WIDTH, HEIGHT, (graphics, mouseX, mouseY, delta) -> drawInformation(graphics));
        addItems(widgets, itemInputs, 0, false);
        addItems(widgets, itemOutputs, 140, true);
        addFluids(widgets, fluidInputs, 0, false);
        addFluids(widgets, fluidOutputs, 141, true);

        EmiIngredient catalyst = EmiIngredient.of(recipe.catalyst());
        boolean optionalCatalyst = catalyst.isEmpty()
                && recipe.outputs().stream().noneMatch(output -> CatalystParallelManager.getTargetUselessIngotTier(output) > 0);
        if (optionalCatalyst) catalyst = EmiIngredient.of(ModTags.CATALYSTS);
        if (!catalyst.isEmpty()) {
            var slot = widgets.addSlot(catalyst, 71, 54).drawBack(false);
            slot.appendTooltip(Component.translatable(optionalCatalyst
                    ? "emi.useless_mod.optional_catalyst" : "emi.useless_mod.required_catalyst").withStyle(ChatFormatting.GOLD));
            if (optionalCatalyst) {
                slot.appendTooltip(Component.translatable("jei.useless_mod.tooltip.catalyst.normal.desc1").withStyle(ChatFormatting.GRAY));
                slot.appendTooltip(Component.translatable("jei.useless_mod.tooltip.catalyst.normal.desc3").withStyle(ChatFormatting.GRAY));
            }
        }
        for (int i = 0; i < Math.min(molds.size(), MOLD_PAGE_SIZE); i++) {
            int index = i;
            widgets.add(new DisplaySlot(() -> ingredient(molds, MOLD_PAGE_SIZE, index), 88 + i * 18, 54))
                    .drawBack(false)
                    .appendTooltip(Component.translatable("emi.useless_mod.required_mold").withStyle(ChatFormatting.GOLD));
        }
        if (pageCount > 1) {
            widgets.add(new PageButton(76, -1));
            widgets.add(new PageButton(126, 1));
        }
        widgets.addTooltipText(List.of(
                Component.translatable("jei.useless_mod.tooltip.energy.base", recipe.energy() + " FE"),
                Component.translatable("jei.useless_mod.tooltip.energy.actual")), 75, 2, 62, 7);
        widgets.addTooltipText(List.of(
                Component.translatable("jei.useless_mod.tooltip.process_time", recipe.processTime()),
                Component.translatable("jei.useless_mod.tooltip.process_time.note"),
                Component.translatable("jei.useless_mod.tooltip.process_time.coil_bonus").withStyle(ChatFormatting.BLUE),
                Component.translatable("jei.useless_mod.tooltip.process_time.coil_useful").withStyle(ChatFormatting.BLUE)),
                78, 41, 56, 10);
        int tier = requiredTier();
        widgets.addTooltipText(List.of(Component.translatable(tier > 0
                ? "jei.useless_mod.gui.tier_required" : "jei.useless_mod.gui.tier_unrestricted", tier)), 74, 32, 64, 8);
    }

    private void addItems(WidgetHolder widgets, List<? extends EmiIngredient> ingredients, int x, boolean output) {
        for (int i = 0; i < Math.min(ingredients.size(), ITEM_PAGE_SIZE); i++) {
            int index = i;
            var slot = widgets.add(new DisplaySlot(() -> ingredient(ingredients, ITEM_PAGE_SIZE, index),
                    x + i % 4 * 18, i / 4 * 18)).drawBack(false);
            if (output) slot.recipeContext(owner);
            slot.appendTooltip(stack -> tooltip(Component.translatable("jei.useless_mod.tooltip.amount", stack.getAmount())
                    .withStyle(ChatFormatting.GRAY)));
        }
    }

    private void addFluids(WidgetHolder widgets, List<? extends EmiIngredient> ingredients, int x, boolean output) {
        int visible = Math.min(ingredients.size(), FLUID_PAGE_SIZE);
        for (int i = 0; i < visible; i++) {
            int index = i;
            int start = i * 71 / visible;
            int end = (i + 1) * 71 / visible;
            int tankX = x + start;
            var slot = widgets.add(new TankWidget(EmiStack.EMPTY, tankX, 54, end - start, 18, 1) {
                @Override public EmiIngredient getStack() { return ingredient(ingredients, FLUID_PAGE_SIZE, index); }
                @Override public void drawStack(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                    // Full-width JEI-style fluid strips. Normalize only the painted fill to avoid
                    // overflowing EMI's amount * height calculation for long-sized recipes.
                    new TankWidget(getStack().copy().setAmount(1), tankX, 54, end - start, 18, 1)
                            .drawStack(graphics, mouseX, mouseY, delta);
                }
                @Override public boolean shouldDrawSlotHighlight(int mouseX, int mouseY) {
                    return !getStack().isEmpty() && super.shouldDrawSlotHighlight(mouseX, mouseY);
                }
            }).drawBack(false);
            if (output) slot.recipeContext(owner);
            slot.appendTooltip(stack -> tooltip(Component.translatable("jei.useless_mod.tooltip.amount.mb", stack.getAmount())
                    .withStyle(ChatFormatting.GRAY)));
        }
    }

    private void drawInformation(GuiGraphics graphics) {
        int progress = (int) (System.currentTimeMillis() % 2000 * 32 / 2000);
        if (progress > 0) graphics.blit(TEXTURE, 90, 23, 90, 73, progress, 8, 256, 256);
        drawCentered(graphics, formatEnergy(recipe.energy()), 106, 2, 62, 0.8f, 0xff00ff00);
        drawCentered(graphics, recipe.processTime() + " ticks", 106, 42, 56, 0.8f, 0xff00ff00);
        int tier = requiredTier();
        String requirement = Component.translatable(tier > 0
                ? "emi.useless_mod.tier_short" : "jei.useless_mod.gui.tier_unrestricted", tier).getString();
        drawCentered(graphics, requirement, 106, 32, 64, 0.7f, tier > 0 ? 0xffff0000 : 0xff404040);
        if (pageCount > 1) drawCentered(graphics, (page + 1) + "/" + pageCount, 106, 13, 36, 0.7f, 0xff404040);
    }

    private int requiredTier() { return AlloyFurnaceTierRules.requiredTier(recipe.id(), recipe.tier()); }

    private EmiIngredient ingredient(List<? extends EmiIngredient> ingredients, int pageSize, int index) {
        int offset = page % pages(ingredients, pageSize) * pageSize + index;
        return offset < ingredients.size() ? ingredients.get(offset) : EmiStack.EMPTY;
    }

    private static int pages(List<?> ingredients, int pageSize) { return Math.max(1, (ingredients.size() + pageSize - 1) / pageSize); }

    private static ClientTooltipComponent tooltip(Component text) { return ClientTooltipComponent.create(text.getVisualOrderText()); }

    private static void drawCentered(GuiGraphics graphics, String text, int centerX, int y, int width, float maxScale, int color) {
        var font = Minecraft.getInstance().font;
        float scale = Math.min(maxScale, (float) width / Math.max(1, font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, -font.width(text) / 2, 0, color, false);
        graphics.pose().popPose();
    }

    private static String formatEnergy(long energy) {
        String[] suffixes = {"", "k", "M", "G", "T", "P", "E"};
        double value = energy;
        int unit = 0;
        while (value >= 1000 && unit < suffixes.length - 1) { value /= 1000; unit++; }
        return unit == 0 ? energy + " FE" : String.format(Locale.ROOT, "%.2f%s FE", value, suffixes[unit]);
    }

    /** Native EMI slots retain ingredient clicks, favorites and output recipe context on every page. */
    private static final class DisplaySlot extends SlotWidget {
        private final Supplier<EmiIngredient> ingredient;
        private DisplaySlot(Supplier<EmiIngredient> ingredient, int x, int y) {
            super(EmiStack.EMPTY, x, y);
            this.ingredient = ingredient;
        }
        @Override public EmiIngredient getStack() { return ingredient.get(); }
        @Override public boolean shouldDrawSlotHighlight(int mouseX, int mouseY) {
            return !getStack().isEmpty() && super.shouldDrawSlotHighlight(mouseX, mouseY);
        }
    }

    private final class PageButton extends Widget {
        private final int x, direction;
        private PageButton(int x, int direction) { this.x = x; this.direction = direction; }
        @Override public Bounds getBounds() { return new Bounds(x, 12, 10, 9); }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            boolean hovered = getBounds().contains(mouseX, mouseY);
            graphics.fill(x, 12, x + 10, 21, hovered ? 0xffc3c5d5 : 0xffaaadbf);
            drawCentered(graphics, direction < 0 ? "<" : ">", x + 5, 13, 8, 0.8f, 0xff404040);
        }
        @Override public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
            return List.of(tooltip(Component.translatable("emi.useless_mod.material_page", page + 1, pageCount)));
        }
        @Override public boolean mouseClicked(int mouseX, int mouseY, int button) {
            if (button != 0 || !getBounds().contains(mouseX, mouseY)) return false;
            page = Math.floorMod(page + direction, pageCount);
            return true;
        }
    }
}
