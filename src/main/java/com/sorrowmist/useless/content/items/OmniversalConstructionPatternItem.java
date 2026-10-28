package com.sorrowmist.useless.content.items;

import com.sorrowmist.useless.content.multiblock.ConstructionMaterialBill;
import com.sorrowmist.useless.content.multiblock.OmniversalFurnaceMaterials;
import com.sorrowmist.useless.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.EncodedPatternItem;
import com.sorrowmist.useless.content.multiblock.ConstructionPatternDetails;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Lossless build bill that AE2 can publish as a material preparation job. */
public final class OmniversalConstructionPatternItem extends EncodedPatternItem {
    public OmniversalConstructionPatternItem(){super(new Properties().stacksTo(1));}
    @Override public IPatternDetails decode(ItemStack stack,Level level,boolean recovery) {
        return ConstructionPatternDetails.decode(AEItemKey.of(stack));
    }
    @Override public IPatternDetails decode(AEItemKey key,Level level) {
        return ConstructionPatternDetails.decode(key);
    }
    public static ItemStack encode(int tier) {
        var entries=OmniversalFurnaceMaterials.requirements(tier).stream().map(r->
                new ConstructionMaterialBill.Entry(BuiltInRegistries.ITEM.getKey(r.stack().getItem()).toString(),r.count())).toList();
        var bill=new ConstructionMaterialBill(tier,entries);
        var stack=new ItemStack(ModItems.OMNIVERSAL_CONSTRUCTION_PATTERN.get());
        var tag=stack.getOrCreateTag();tag.putInt("CoilTier",bill.tier());tag.putInt("BillVersion",1);
        var materials=new ListTag();
        for(var entry:bill.entries()) {
            var row=new CompoundTag();row.putString("Item",entry.item());
            // ItemStack.Count is a byte in 1.20.1. Save the full integer separately.
            row.putInt("Required",entry.count());materials.add(row);
        }
        tag.put("Materials",materials);return stack;
    }
    public static Optional<ConstructionMaterialBill> read(ItemStack stack) {
        if(!(stack.getItem() instanceof OmniversalConstructionPatternItem)||stack.getTag()==null)return Optional.empty();
        try {
            var tag=stack.getTag();var rows=tag.getList("Materials",Tag.TAG_COMPOUND);
            if(rows.size()>81)return Optional.empty();
            var entries=new ArrayList<ConstructionMaterialBill.Entry>();
            for(int i=0;i<rows.size();i++) {
                var row=rows.getCompound(i);entries.add(new ConstructionMaterialBill.Entry(row.getString("Item"),row.getInt("Required")));
            }
            return Optional.of(new ConstructionMaterialBill(tag.getInt("CoilTier"),entries));
        }catch(IllegalArgumentException|ArithmeticException invalid){return Optional.empty();}
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
        read(stack).ifPresent(bill->{
            tooltip.add(Component.translatable("jei.useless_mod.structure.tier_short",bill.tier()).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("jei.useless_mod.structure.total",bill.total()).withStyle(ChatFormatting.GOLD));
            for(var row:bill.entries())tooltip.add(Component.literal(row.count()+" × ").append(
                    BuiltInRegistries.ITEM.get(new ResourceLocation(row.item())).getDescription()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.useless_mod.omniversal_construction_pattern.help").withStyle(ChatFormatting.DARK_AQUA));
        });
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);var bill=read(stack);
        if(bill.isEmpty())return InteractionResultHolder.fail(stack);
        if(level.isClientSide)com.sorrowmist.useless.client.gui.structure.ConstructionPatternScreen.open(bill.get());
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
