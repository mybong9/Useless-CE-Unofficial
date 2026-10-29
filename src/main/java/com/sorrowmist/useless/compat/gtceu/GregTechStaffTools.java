package com.sorrowmist.useless.compat.gtceu;

import com.sorrowmist.useless.content.items.BeefToolVariants;
import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import com.sorrowmist.uselesscore.api.enums.tool.ToolTypeMode;
import net.minecraft.world.item.ItemStack;
import java.util.Set;

/** Machine interactions use the selected native GT tool, independently of broad crafting tags. */
public final class GregTechStaffTools {
    private GregTechStaffTools() {}
    public static Set<?> selectedTools(ItemStack stack) {
        if (!BeefToolVariants.isBeafTool(stack)) return null;
        ToolTypeMode mode = CoreItemComponents.getOrDefault(stack, UselessComponents.CurrentToolTypeComponent.get(), ToolTypeMode.NONE_MODE);
        String field = switch (mode) {
            case WRENCH_MODE -> "WRENCH";
            case SCREWDRIVER_MODE -> "SCREWDRIVER";
            case MALLET_MODE -> "SOFT_MALLET";
            case CROWBAR_MODE -> "CROWBAR";
            case HAMMER_MODE -> "HARD_HAMMER";
            default -> null;
        };
        if (field == null) return Set.of();
        try { return Set.of(Class.forName("com.gregtechceu.gtceu.api.item.tool.GTToolType").getField(field).get(null)); }
        catch (ReflectiveOperationException | LinkageError unavailable) { return null; }
    }
}
