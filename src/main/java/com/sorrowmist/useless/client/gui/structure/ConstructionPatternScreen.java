package com.sorrowmist.useless.client.gui.structure;

import com.sorrowmist.useless.content.multiblock.ConstructionMaterialBill;
import com.sorrowmist.useless.content.multiblock.OmniversalFurnaceMaterials;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Read a saved full bill without requiring either recipe viewer. */
public final class ConstructionPatternScreen extends Screen {
    private final StructurePreview preview;
    private float scale;
    private int left,top;
    private ConstructionPatternScreen(ConstructionMaterialBill bill) {
        super(Component.translatable("item.useless_mod.omniversal_construction_pattern"));
        preview=new StructurePreview(bill.tier(),(stack,usage)->{});
        preview.showSavedMaterials(bill.entries().stream().map(e->new OmniversalFurnaceMaterials.Requirement(
                new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(e.item()))),e.count())).toList());
    }
    public static void open(ConstructionMaterialBill bill){Minecraft.getInstance().setScreen(new ConstructionPatternScreen(bill));}
    @Override protected void init() {
        scale=Math.min(1F,Math.min((width-16F)/StructurePreview.WIDTH,(height-32F)/StructurePreview.HEIGHT));
        left=(int)((width-StructurePreview.WIDTH*scale)/2);top=(int)((height-StructurePreview.HEIGHT*scale)/2)+6;
    }
    @Override public void render(GuiGraphics g,int x,int y,float tick) {
        renderBackground(g);g.drawCenteredString(font,title,width/2,Math.max(3,top-14),0xEAD9A0);
        g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scale,1);
        preview.draw(g,localX(x),localY(y));g.pose().popPose();
        var tooltip=preview.tooltip(localX(x),localY(y));
        if(!tooltip.isEmpty())g.renderComponentTooltip(font,tooltip,x,y);
    }
    private double localX(double x){return (x-left)/scale;}
    private double localY(double y){return (y-top)/scale;}
    @Override public boolean mouseClicked(double x,double y,int b){return preview.mouseClicked(localX(x),localY(y),b)||super.mouseClicked(x,y,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return preview.mouseReleased(localX(x),localY(y),b)||super.mouseReleased(x,y,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return preview.mouseDragged(localX(x),localY(y),b,dx/scale,dy/scale);}
    @Override public boolean mouseScrolled(double x,double y,double d){return preview.mouseScrolled(localX(x),localY(y),d);}
    @Override public boolean isPauseScreen(){return false;}
}
