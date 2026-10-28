package com.sorrowmist.useless.content.items;

import com.sorrowmist.useless.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

/** The orderable AE output; actual building materials return to ME storage first. */
public final class OmniversalConstructionReceiptItem extends Item {
    public OmniversalConstructionReceiptItem() { super(new Properties()); }

    public static ItemStack create(int tier) {
        if (tier < 1 || tier > 10) throw new IllegalArgumentException("Invalid coil tier");
        ItemStack stack = new ItemStack(ModItems.OMNIVERSAL_CONSTRUCTION_RECEIPT.get());
        stack.getOrCreateTag().putInt("CoilTier", tier);
        return stack;
    }

    public static int tier(ItemStack stack) {
        if (!(stack.getItem() instanceof OmniversalConstructionReceiptItem) || stack.getTag() == null) return 0;
        int tier = stack.getTag().getInt("CoilTier");
        return tier >= 1 && tier <= 10 ? tier : 0;
    }

    @Override public Component getName(ItemStack stack) {
        int tier = tier(stack);
        return tier == 0 ? super.getName(stack)
                : Component.translatable("item.useless_mod.omniversal_construction_receipt.tier", tier);
    }

    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable("item.useless_mod.omniversal_construction_receipt.help")
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
