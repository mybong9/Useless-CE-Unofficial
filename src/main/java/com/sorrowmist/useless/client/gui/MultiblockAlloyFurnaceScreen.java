package com.sorrowmist.useless.client.gui;



import com.sorrowmist.useless.init.ModNetwork;
import com.sorrowmist.uselesscore.api.enums.RedstoneControlMode;
import com.sorrowmist.useless.content.blocks.multiblock.MultiblockAlloyFurnaceCoreBlock;
import com.sorrowmist.useless.content.blocks.multiblock.OmniversalAlloyFurnaceStructure;
import com.sorrowmist.useless.content.blocks.multiblock.UselessCoilBlock;
import com.sorrowmist.useless.content.menus.MultiblockAlloyFurnaceMenu;
import com.sorrowmist.useless.init.ModBlocks;
import com.sorrowmist.useless.network.AECancelPacket;
import com.sorrowmist.useless.network.AETaskProgressRequestPacket;
import com.sorrowmist.useless.network.MultiblockAlloyFurnaceEnergyLimitPacket;
import com.sorrowmist.useless.network.RedstoneControlPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import com.sorrowmist.uselesscore.client.ui.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class MultiblockAlloyFurnaceScreen extends FittedContainerScreen<MultiblockAlloyFurnaceMenu> {
    private static final int PREVIEW_ORIGIN_X = 200;
    private static final int PREVIEW_ORIGIN_Y = 50;
    private static final int PREVIEW_CELL_SIZE = 1;
    private static final int PREVIEW_LAYER_STRIDE = 21;
    private static final int REDSTONE_BUTTON_X = 304;
    private static final int CANCEL_BUTTON_X = 328;
    private static final int CONTROL_BUTTON_Y = 12;
    private static final int ENERGY_BAR_X = 22;
    private static final int ENERGY_BAR_Y = 165;
    private static final int ENERGY_BAR_WIDTH = 316;
    private static final int ENERGY_BAR_HEIGHT = 6;
    private static final int TASK_LIST_X = 22;
    private static final int TASK_LIST_Y = 204;
    private static final int TASK_LIST_WIDTH = 134;
    private static final int TASK_LIST_MAX_LINES = 6;
    private static final int TASK_LIST_LINE_HEIGHT = 9;
    private static final float TASK_LIST_TEXT_SCALE = 0.75F;
    private static final int TASK_PAGE_PREVIOUS_X = 116;
    private static final int TASK_PAGE_NEXT_X = 144;
    private static final int TASK_PAGE_BUTTON_Y = 254;
    private static final int TASK_PAGE_BUTTON_WIDTH = 12;
    private static final int TASK_PAGE_BUTTON_HEIGHT = 10;
    private static final int ENERGY_LIMIT_FIELD_X = 250;
    private static final int ENERGY_LIMIT_FIELD_Y = 140;
    private static final int ENERGY_LIMIT_FIELD_WIDTH = 88;

    @Nullable
    private OmniversalAlloyFurnaceStructure.ValidationResult liveValidation;
    @Nullable
    private UiButton cancelButton;
    @Nullable
    private UiButton previousTaskPageButton;
    @Nullable
    private UiButton nextTaskPageButton;
    @Nullable
    private EditBox energyLimitField;
    private long lastSyncedEnergyLimit = Long.MIN_VALUE;
    private String syncedEnergyLimitText = "";
    private boolean updatingEnergyLimitField;
    private boolean energyLimitDirty;
    private boolean energyLimitFieldWasFocused;
    private int taskPage;
    private int previewLayer;
    private Map<Long, OmniversalAlloyFurnaceStructure.Mismatch> liveMismatches = Map.of();
    private Direction liveFacing = Direction.NORTH;
    private boolean progressRequested;
    private long nextStructureRefresh;

    public MultiblockAlloyFurnaceScreen(MultiblockAlloyFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 360;
        imageHeight = 282;
        inventoryLabelY = 188;
    }

    @Override
    protected void init() {
        String draft=energyLimitField==null?null:energyLimitField.getValue();
        boolean keepDraft=energyLimitDirty,hadFocus=energyLimitField!=null&&energyLimitField.isFocused();
        super.init();
        addRenderableWidget(new UiButton(
                leftPos + REDSTONE_BUTTON_X, topPos + CONTROL_BUTTON_Y,18,20,Component.literal("R"),
                button -> ModNetwork.CHANNEL.sendToServer(
                        new RedstoneControlPacket(menu.getBlockPos()))));
        cancelButton = addRenderableWidget(new UiButton(
                leftPos + CANCEL_BUTTON_X, topPos + CONTROL_BUTTON_Y,18,20,Component.literal("×"),
                button -> ModNetwork.CHANNEL.sendToServer(
                        new AECancelPacket(menu.getBlockPos()))).destructive());
        energyLimitField = new UiEditBox(font,
                leftPos + ENERGY_LIMIT_FIELD_X, topPos + ENERGY_LIMIT_FIELD_Y,
                ENERGY_LIMIT_FIELD_WIDTH, 22,
                Component.translatable("gui.useless_mod.multiblock_alloy_furnace.energy_limit"));
        energyLimitField.setMaxLength(24);
        energyLimitField.setFilter(ScaledEnergyAmount::isValidInput);
        energyLimitField.setResponder(value -> {
            if (!updatingEnergyLimitField) {
                energyLimitDirty = !value.equals(syncedEnergyLimitText);
            }
        });
        addRenderableWidget(energyLimitField);
        previousTaskPageButton = addRenderableWidget(new UiButton(
                leftPos + TASK_PAGE_PREVIOUS_X, topPos + TASK_PAGE_BUTTON_Y,
                TASK_PAGE_BUTTON_WIDTH, TASK_PAGE_BUTTON_HEIGHT,
                Component.literal("<"), button -> changeTaskPage(-1)));
        nextTaskPageButton = addRenderableWidget(new UiButton(
                leftPos + TASK_PAGE_NEXT_X, topPos + TASK_PAGE_BUTTON_Y,
                TASK_PAGE_BUTTON_WIDTH, TASK_PAGE_BUTTON_HEIGHT,
                Component.literal(">"), button -> changeTaskPage(1)));
        lastSyncedEnergyLimit=Long.MIN_VALUE;
        syncEnergyLimitField(true);
        if(draft!=null&&(keepDraft||hadFocus)){energyLimitField.setValue(draft);energyLimitDirty=keepDraft;if(hadFocus)setFocused(energyLimitField);}
        refreshStructurePreview();
        if(!progressRequested){progressRequested=true;ModNetwork.CHANNEL.sendToServer(new AETaskProgressRequestPacket(
                menu.containerId, menu.getBlockPos()));}
    }

    @Override
    protected boolean clickContent(double mouseX, double mouseY, int button) {
        if (energyLimitField != null && energyLimitField.isFocused()
                && !energyLimitField.isMouseOver(mouseX, mouseY)) {
            commitEnergyLimit();
            setFocused(null);
            energyLimitFieldWasFocused = false;
        }
        return super.clickContent(mouseX, mouseY, button);
    }

    @Override
    protected boolean releaseContent(double mouseX, double mouseY, int button) {
        if (cancelButton != null) {
            cancelButton.releaseVisualState();
        }
        if (previousTaskPageButton != null) {
            previousTaskPageButton.releaseVisualState();
        }
        if (nextTaskPageButton != null) {
            nextTaskPageButton.releaseVisualState();
        }
        return super.releaseContent(mouseX, mouseY, button);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean focused = energyLimitField != null && energyLimitField.isFocused();
        if (energyLimitFieldWasFocused && !focused) commitEnergyLimit();
        energyLimitFieldWasFocused = focused;
        syncEnergyLimitField(false);
        refreshStructurePreview();
        updateTaskPageControls();
    }

    private void syncEnergyLimitField(boolean force) {
        if (energyLimitField == null) return;
        long limit = menu.getAutomaticEnergyLimit();
        if ((force || (!energyLimitField.isFocused() && !energyLimitDirty))
                && limit != lastSyncedEnergyLimit) {
            setEnergyLimitFieldValue(limit);
            lastSyncedEnergyLimit = limit;
        }
    }

    private void commitEnergyLimit() {
        if (energyLimitField == null || !energyLimitDirty) return;
        long limit = ScaledEnergyAmount.parse(
                energyLimitField.getValue(), menu.getCapacity())
                .orElse(menu.getAutomaticEnergyLimit());
        setEnergyLimitFieldValue(limit);
        ModNetwork.CHANNEL.sendToServer(new MultiblockAlloyFurnaceEnergyLimitPacket(
                menu.containerId, menu.getBlockPos(), limit));
    }

    private void setEnergyLimitFieldValue(long limit) {
        if (energyLimitField == null) return;
        syncedEnergyLimitText = ScaledEnergyAmount.format(limit);
        updatingEnergyLimitField = true;
        energyLimitField.setValue(syncedEnergyLimitText);
        updatingEnergyLimitField = false;
        energyLimitDirty = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (energyLimitField != null && energyLimitField.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            commitEnergyLimit();
            setFocused(null);
            energyLimitFieldWasFocused = false;
            return true;
        }
        if (keyCode != GLFW.GLFW_KEY_ESCAPE && energyLimitField != null
                && consumesTextInputKey(energyLimitField, keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean consumesTextInputKey(EditBox field, int keyCode, int scanCode, int modifiers) {
        return field.keyPressed(keyCode, scanCode, modifiers) || field.canConsumeInput();
    }

    @Override
    public void onClose() {
        commitEnergyLimit();
        super.onClose();
    }

    private void refreshStructurePreview() {
        long now=System.nanoTime();if(now<nextStructureRefresh)return;nextStructureRefresh=now+1_000_000_000L;
        if (minecraft == null || minecraft.level == null) {
            liveValidation = null;
            liveMismatches = Map.of();
            return;
        }

        BlockPos corePos = menu.getBlockPos();
        BlockState coreState = minecraft.level.getBlockState(corePos);
        liveFacing = coreState.hasProperty(MultiblockAlloyFurnaceCoreBlock.FACING)
                ? coreState.getValue(MultiblockAlloyFurnaceCoreBlock.FACING)
                : Direction.NORTH;
        for (var entry : OmniversalAlloyFurnaceStructure.entries()) {
            if (!minecraft.level.isLoaded(entry.worldPos(corePos, liveFacing))) {
                liveValidation = null;
                liveMismatches = Map.of();
                return;
            }
        }

        liveValidation = OmniversalAlloyFurnaceStructure.validate(
                minecraft.level, corePos, liveFacing);
        Map<Long, OmniversalAlloyFurnaceStructure.Mismatch> mismatches = new HashMap<>();
        for (var mismatch : liveValidation.mismatches()) {
            mismatches.put(mismatch.worldPos().asLong(), mismatch);
        }
        liveMismatches = Map.copyOf(mismatches);
    }

    @Override
    protected void renderBg(GuiGraphics graphics,float partialTick,int mouseX,int mouseY){
        updateTaskPageControls();
        UiDraw.glass(graphics,leftPos,topPos,imageWidth,imageHeight,12,UiTheme.DARK);
        UiDraw.panel(graphics,leftPos+12,topPos+42,170,84,9,UiTheme.DARK.surface(),UiTheme.DARK.border());
        UiDraw.panel(graphics,leftPos+190,topPos+42,158,84,9,UiTheme.DARK.inset(),UiTheme.DARK.border());
        UiDraw.panel(graphics,leftPos+12,topPos+134,336,44,9,UiTheme.DARK.surface(),UiTheme.DARK.border());
        UiDraw.panel(graphics,leftPos+12,topPos+186,154,90,9,UiTheme.DARK.inset(),UiTheme.DARK.border());
        UiDraw.panel(graphics,leftPos+174,topPos+180,174,96,9,UiTheme.DARK.surface(),UiTheme.DARK.border());
        for(var slot:menu.slots)UiDraw.slot(graphics,leftPos+slot.x-1,topPos+slot.y-1,18,UiTheme.DARK,false);
        long capacity=menu.getCapacity();
        int width=capacity<=0?0:(int)Math.round(Math.min(1d,(double)menu.getEnergy()/capacity)*ENERGY_BAR_WIDTH);
        UiDraw.rounded(graphics,leftPos+ENERGY_BAR_X,topPos+ENERGY_BAR_Y,ENERGY_BAR_WIDTH,ENERGY_BAR_HEIGHT,3,UiTheme.DARK.inset());
        UiDraw.gradient(graphics,leftPos+ENERGY_BAR_X,topPos+ENERGY_BAR_Y,width,ENERGY_BAR_HEIGHT,3,0xff70dcce,0xff3496ed,0,0,0x305fcfff,1.2f);
    }
    @Override
    protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY){
        UiDraw.text(graphics,font,title.getString(),14,17,278,UiTheme.DARK.text());
        boolean valid=liveValidation==null?menu.isFormed():liveValidation.valid();
        Component state=valid?Component.translatable("gui.useless_mod.multiblock_alloy_furnace.formed")
                :liveValidation!=null&&!liveValidation.mismatches().isEmpty()
                ?Component.translatable("gui.useless_mod.multiblock_alloy_furnace.invalid_count",liveValidation.mismatches().size())
                :Component.translatable("gui.useless_mod.multiblock_alloy_furnace.invalid");
        UiDraw.text(graphics,font,state.getString(),22,51,150,valid?0xff86dfb7:UiTheme.DARK.danger());
        int tier=liveValidation==null?menu.getCoilTier():liveValidation.coilTier();
        graphics.drawString(font,Component.translatable("gui.useless_mod.multiblock_alloy_furnace.coil",tier),22,68,UiTheme.DARK.muted(),false);
        graphics.drawString(font,truncate(Component.translatable("gui.useless_mod.multiblock_alloy_furnace.tasks",menu.getActiveTasks(),menu.getMaxTasks()),150),22,85,UiTheme.DARK.text(),false);
        RedstoneControlMode mode=RedstoneControlMode.byIndex(menu.getRedstoneMode());
        graphics.drawString(font,truncate(Component.translatable("gui.useless_mod.multiblock_alloy_furnace.redstone",
                Component.translatable("gui.useless_mod.advanced_alloy_furnace.redstone_control."+mode.name().toLowerCase(Locale.ROOT))),150),22,103,UiTheme.DARK.muted(),false);
        graphics.drawString(font,Component.translatable("gui.useless_mod.multiblock_alloy_furnace.energy_limit"),22,141,UiTheme.DARK.muted(),false);
        UiDraw.text(graphics,font,ScaledEnergyAmount.format(menu.getEnergy())+" / "+ScaledEnergyAmount.format(menu.getCapacity())+" FE",22,153,214,UiTheme.DARK.text());
        renderStructurePreview(graphics);renderTaskList(graphics);
        graphics.drawString(font,playerInventoryTitle,182,inventoryLabelY,UiTheme.DARK.muted(),false);
    }
    private Component truncate(Component text, int maxWidth) {
        String value = text.getString();
        return font.width(value) <= maxWidth
                ? text
                : Component.literal(font.plainSubstrByWidth(
                        value, maxWidth - font.width("...")) + "...");
    }

    @Override
    protected boolean scrollContent(double mouseX, double mouseY, double delta) {
        if (mouseX >= leftPos + 198 && mouseX < leftPos + 266
                && mouseY >= topPos + 48 && mouseY < topPos + 116) {
            previewLayer = net.minecraft.util.Mth.clamp(previewLayer + (delta > 0 ? 1 : -1),
                    0, OmniversalAlloyFurnaceStructure.HEIGHT - 1);
            return true;
        }
        return super.scrollContent(mouseX, mouseY, delta);
    }

    private void renderStructurePreview(GuiGraphics graphics) {
        UiDraw.rounded(graphics,198,48,68,68,5,UiTheme.DARK.inset());
        for (var entry : OmniversalAlloyFurnaceStructure.entries()) {
            if (entry.localPos().getY() != previewLayer) continue;
            int x = PREVIEW_ORIGIN_X + (entry.localPos().getX() + 39) * 4 / 5;
            int y = PREVIEW_ORIGIN_Y + (entry.localPos().getZ() + 38) * 4 / 5;
            BlockPos worldPos = entry.worldPos(menu.getBlockPos(), liveFacing);
            int color = liveValidation == null ? UiTheme.DARK.muted()
                    : liveMismatches.containsKey(worldPos.asLong()) ? UiTheme.DARK.danger()
                    : partColor(entry, worldPos);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
        graphics.drawString(font, (previewLayer + 1) + "/32", 278, 54, 0xFFB9D3D4, false);
        graphics.drawString(font, "80 × 80", 278, 70, 0xFFB9D3D4, false);
    }

    private int partColor(OmniversalAlloyFurnaceStructure.Entry entry, BlockPos worldPos) {
        if (entry.part() == OmniversalAlloyFurnaceStructure.Part.INTERFACE
                && minecraft != null && minecraft.level != null) {
            OmniversalAlloyFurnaceStructure.FunctionalPart functionalPart =
                    OmniversalAlloyFurnaceStructure.functionalPart(minecraft.level.getBlockState(worldPos));
            if (functionalPart != null) {
                return switch (functionalPart) {
                    case PATTERN_ASSEMBLY -> 0xFF00838F;
                    case MOLD_HUB -> 0xFFF9A825;
                    case PASSIVE_HATCH -> 0xFF517497;
                };
            }
        }
        return switch (entry.part()) {
            case CORE -> 0xFFD84315;
            case INTERFACE -> 0xFFDBECEB;
            case CASING -> 0xFF757575;
            case COIL -> 0xFF6A1B9A;
            case COPPER -> 0xFFCA925F;
            case TRIGRAM_QIAN -> 0xFFD9A34D;
            case TRIGRAM_DUI -> 0xFFC7794E;
            case TRIGRAM_LI -> 0xFFC95043;
            case TRIGRAM_ZHEN -> 0xFF609A68;
            case TRIGRAM_KUN -> 0xFF9471AB;
            case TRIGRAM_GEN -> 0xFF41998F;
            case TRIGRAM_KAN -> 0xFF526BB2;
            case TRIGRAM_XUN -> 0xFF65B9D0;
            case FRAME -> 0xFFD1D9DC;
            case TRIM -> 0xFFE3E8E5;
            case VENT -> 0xFF484054;
            case RESONATOR -> 0xFF5BE2D3;
            case AIR -> 0xFF212121;
        };
    }

    private void renderTaskList(GuiGraphics graphics) {
        List<List<com.sorrowmist.useless.network.AETaskProgressPacket.TaskProgressData>> pages = taskPages();
        taskPage = com.sorrowmist.uselesscore.api.CoreMath.clamp(taskPage, 0, pages.size() - 1);
        List<String> lines = new ArrayList<>();
        for (var task : pages.get(taskPage)) {
            lines.addAll(taskLines(task));
        }
        graphics.pose().pushPose();
        graphics.pose().translate(TASK_LIST_X, TASK_LIST_Y, 0);
        graphics.pose().scale(TASK_LIST_TEXT_SCALE, TASK_LIST_TEXT_SCALE, 1.0F);
        for (int line = 0; line < lines.size(); line++) {
            graphics.drawString(font, lines.get(line), 0, line * TASK_LIST_LINE_HEIGHT,
                    UiTheme.DARK.muted(), false);
        }
        graphics.pose().popPose();

        if (pages.size() > 1) {
            String pageText = (taskPage + 1) + "/" + pages.size();
            graphics.drawString(font, pageText,
                    TASK_PAGE_NEXT_X - 2 - font.width(pageText), TASK_PAGE_BUTTON_Y + 1,
                    UiTheme.DARK.muted(), false);
        }
    }

    private String fitTaskLine(String text) {
        int scaledWidth = (int) (TASK_LIST_WIDTH / TASK_LIST_TEXT_SCALE);
        if (font.width(text) <= scaledWidth) return text;
        return font.plainSubstrByWidth(text, scaledWidth - font.width("...")) + "...";
    }

    private List<String> taskLines(
            com.sorrowmist.useless.network.AETaskProgressPacket.TaskProgressData task) {
        String status = task.progress > 0 && task.maxProgress > 0
                ? task.progress + "/" + task.maxProgress
                : Component.translatable(task.statusKey).getString();
        String detail = task.statusDetail == null || task.statusDetail.isBlank() ? ""
                : task.statusDetail.startsWith("gui.useless_mod.")
                ? Component.translatable(task.statusDetail).getString()
                : task.statusDetail;
        String state = detail.isEmpty() ? status : status + ": " + detail;
        String product = task.productName + " x" + task.totalOutputCount;
        String combined = product + "  " + state;
        if (font.width(combined) <= (int) (TASK_LIST_WIDTH / TASK_LIST_TEXT_SCALE)) {
            return List.of(combined);
        }
        if (detail.isEmpty() || font.width(state) <= (int) (TASK_LIST_WIDTH / TASK_LIST_TEXT_SCALE)) {
            return List.of(fitTaskLine(product), fitTaskLine(state));
        }
        return List.of(fitTaskLine(product), fitTaskLine(status), fitTaskLine(detail));
    }

    private List<List<com.sorrowmist.useless.network.AETaskProgressPacket.TaskProgressData>> taskPages() {
        var tasks = new ArrayList<>(menu.getTaskProgress());
        tasks.sort(Comparator
                .comparing((com.sorrowmist.useless.network.AETaskProgressPacket.TaskProgressData task)
                        -> task.productName)
                .thenComparingInt(task -> task.progress));
        List<List<com.sorrowmist.useless.network.AETaskProgressPacket.TaskProgressData>> pages = new ArrayList<>();
        List<com.sorrowmist.useless.network.AETaskProgressPacket.TaskProgressData> page = new ArrayList<>();
        int usedLines = 0;
        for (var task : tasks) {
            int requiredLines = taskLines(task).size();
            if (!page.isEmpty() && usedLines + requiredLines > TASK_LIST_MAX_LINES) {
                pages.add(List.copyOf(page));
                page.clear();
                usedLines = 0;
            }
            page.add(task);
            usedLines += requiredLines;
        }
        if (!page.isEmpty() || pages.isEmpty()) {
            pages.add(List.copyOf(page));
        }
        return pages;
    }

    private void updateTaskPageControls() {
        int pageCount = taskPages().size();
        taskPage = com.sorrowmist.uselesscore.api.CoreMath.clamp(taskPage, 0, pageCount - 1);
        if (previousTaskPageButton != null) {
            previousTaskPageButton.visible = pageCount > 1;
            previousTaskPageButton.active = taskPage > 0;
        }
        if (nextTaskPageButton != null) {
            nextTaskPageButton.visible = pageCount > 1;
            nextTaskPageButton.active = taskPage < pageCount - 1;
        }
    }

    private void changeTaskPage(int delta) {
        int lastPage = taskPages().size() - 1;
        taskPage = com.sorrowmist.uselesscore.api.CoreMath.clamp(taskPage + delta, 0, lastPage);
        updateTaskPageControls();
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderContent(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(ENERGY_BAR_X, ENERGY_BAR_Y,
                ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(
                    "gui.useless_mod.advanced_alloy_furnace.energy",
                    ScaledEnergyAmount.format(menu.getEnergy()),
                    ScaledEnergyAmount.format(menu.getCapacity())), mouseX, mouseY);
        }
        if (energyLimitField != null && energyLimitField.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(font, List.of(
                    Component.translatable(
                            "gui.useless_mod.multiblock_alloy_furnace.energy_limit"),
                    Component.translatable(
                            "gui.useless_mod.multiblock_alloy_furnace.energy_limit.desc"),
                    Component.translatable(
                            "gui.useless_mod.multiblock_alloy_furnace.energy_limit.units")),
                    Optional.empty(), mouseX, mouseY);
        }
        renderControlTooltips(graphics, mouseX, mouseY);
        renderStructureTooltip(graphics, mouseX, mouseY);
    }

    private void renderControlTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(REDSTONE_BUTTON_X, CONTROL_BUTTON_Y,
                AlloyFurnaceControlIcons.WIDTH, AlloyFurnaceControlIcons.HEIGHT,
                mouseX, mouseY)) {
            RedstoneControlMode mode = RedstoneControlMode.byIndex(menu.getRedstoneMode());
            graphics.renderTooltip(font, List.of(
                    Component.translatable(
                            "gui.useless_mod.advanced_alloy_furnace.redstone_control"),
                    Component.translatable(
                            "gui.useless_mod.advanced_alloy_furnace.redstone_control."
                                    + mode.name().toLowerCase(Locale.ROOT))),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(CANCEL_BUTTON_X, CONTROL_BUTTON_Y,
                AlloyFurnaceControlIcons.WIDTH, AlloyFurnaceControlIcons.HEIGHT,
                mouseX, mouseY)) {
            graphics.renderTooltip(font, List.of(
                    Component.translatable(
                            "gui.useless_mod.advanced_alloy_furnace.cancel_ae_tasks"),
                    Component.translatable(
                            "gui.useless_mod.advanced_alloy_furnace.cancel_ae_tasks.desc")),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderStructureTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        OmniversalAlloyFurnaceStructure.Entry entry = findHoveredPreviewEntry(mouseX, mouseY);
        if (entry == null || minecraft == null || minecraft.level == null) return;

        BlockPos worldPos = entry.worldPos(menu.getBlockPos(), liveFacing);
        BlockState actual = minecraft.level.getBlockState(worldPos);
        boolean known = liveValidation != null;
        boolean mismatch = known && liveMismatches.containsKey(worldPos.asLong());
        List<Component> lines = new ArrayList<>(4);
        lines.add(Component.translatable(known
                        ? mismatch
                                ? "gui.useless_mod.multiblock_alloy_furnace.preview.incorrect"
                                : "gui.useless_mod.multiblock_alloy_furnace.preview.correct"
                        : "gui.useless_mod.multiblock_alloy_furnace.preview.unavailable")
                .withStyle(!known ? ChatFormatting.GRAY
                        : mismatch ? ChatFormatting.RED : ChatFormatting.GREEN));
        Component expected = expectedPartName(entry, worldPos);
        lines.add(Component.translatable(
                "gui.useless_mod.multiblock_alloy_furnace.preview.expected", expected));
        lines.add(Component.translatable(
                "gui.useless_mod.multiblock_alloy_furnace.preview.actual",
                actual.getBlock().getName()));
        lines.add(Component.translatable(
                "gui.useless_mod.multiblock_alloy_furnace.preview.position",
                worldPos.getX(), worldPos.getY(), worldPos.getZ()));
        graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
    }

    private Component expectedPartName(
            OmniversalAlloyFurnaceStructure.Entry entry, BlockPos worldPos) {
        return switch (entry.part()) {
            case CORE -> ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get().getName();
            case CASING -> ModBlocks.OMNIVERSAL_FORGE_ARMOR.get().getName();
            case INTERFACE -> expectedCasingName(worldPos);
            case COIL -> expectedCoilName();
            case COPPER -> ModBlocks.OMNIVERSAL_FORGE_COPPER.get().getName();
            case TRIGRAM_QIAN -> ModBlocks.TRIGRAM_QIAN.get().getName();
            case TRIGRAM_DUI -> ModBlocks.TRIGRAM_DUI.get().getName();
            case TRIGRAM_LI -> ModBlocks.TRIGRAM_LI.get().getName();
            case TRIGRAM_ZHEN -> ModBlocks.TRIGRAM_ZHEN.get().getName();
            case TRIGRAM_KUN -> ModBlocks.TRIGRAM_KUN.get().getName();
            case TRIGRAM_GEN -> ModBlocks.TRIGRAM_GEN.get().getName();
            case TRIGRAM_KAN -> ModBlocks.TRIGRAM_KAN.get().getName();
            case TRIGRAM_XUN -> ModBlocks.TRIGRAM_XUN.get().getName();
            case FRAME -> ModBlocks.OMNIVERSAL_FORGE_FRAME.get().getName();
            case TRIM -> ModBlocks.OMNIVERSAL_FORGE_TRIM.get().getName();
            case VENT -> ModBlocks.OMNIVERSAL_FORGE_VENT.get().getName();
            case RESONATOR -> ModBlocks.OMNIVERSAL_FORGE_RESONATOR.get().getName();
            case AIR -> Blocks.AIR.getName();
        };
    }

    private Component expectedCasingName(BlockPos worldPos) {
        Component casingName = ModBlocks.OMNIVERSAL_FORGE_FRAME.get().getName();
        return Component.translatable(
                "gui.useless_mod.multiblock_alloy_furnace.preview.casing_with_functional_parts",
                casingName, ModBlocks.ME_PATTERN_ASSEMBLY.get().getName(),
                ModBlocks.OMNIVERSAL_MOLD_HUB.get().getName(),
                ModBlocks.PASSIVE_CRAFTING_HATCH.get().getName());
    }

    private Component expectedCoilName() {
        int tier = liveValidation == null ? menu.getCoilTier() : liveValidation.coilTier();
        var coil = ModBlocks.USELESS_COILS.get(tier);
        if (coil != null) return coil.get().getName();
        return Component.translatable(
                "gui.useless_mod.multiblock_alloy_furnace.preview.coil_any",
                ModBlocks.USELESS_COILS.get(UselessCoilBlock.MIN_TIER).get().getName(),
                ModBlocks.USELESS_COILS.get(UselessCoilBlock.MAX_TIER).get().getName());
    }

    @Nullable
    private OmniversalAlloyFurnaceStructure.Entry findHoveredPreviewEntry(int mouseX, int mouseY) {
        int relativeX = mouseX - leftPos;
        int relativeY = mouseY - topPos;
        var entries = OmniversalAlloyFurnaceStructure.entries();
        for (int index = entries.size() - 1; index >= 0; index--) {
            var entry = entries.get(index);
            if (entry.localPos().getY() != previewLayer) continue;
            int x = PREVIEW_ORIGIN_X + (entry.localPos().getX() + 39) * 4 / 5;
            int y = PREVIEW_ORIGIN_Y + (entry.localPos().getZ() + 38) * 4 / 5;
            if (relativeX >= x && relativeX < x + 1 && relativeY >= y && relativeY < y + 1) {
                return entry;
            }
        }
        return null;
    }
}


