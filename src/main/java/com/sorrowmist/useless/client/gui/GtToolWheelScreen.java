package com.sorrowmist.useless.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import com.sorrowmist.uselesscore.api.enums.tool.ToolTypeMode;
import com.sorrowmist.useless.init.ModNetwork;
import com.sorrowmist.useless.network.ToolTypeModeSwitchPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;

public final class GtToolWheelScreen extends Screen {
    private static final ToolTypeMode[] TOOLS={ToolTypeMode.WRENCH_MODE,ToolTypeMode.SCREWDRIVER_MODE,
        ToolTypeMode.MALLET_MODE,ToolTypeMode.CROWBAR_MODE,ToolTypeMode.HAMMER_MODE,ToolTypeMode.NONE_MODE};
    private final int releaseKey;
    private int selected=-1;
    private int applied=-1;
    public GtToolWheelScreen(int releaseKey){super(Component.translatable("key.useless_mod.gt_tool_wheel"));this.releaseKey=releaseKey;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        int radius=Math.max(36,Math.min(112,Math.min(width,height)/2-12));
        selected=sectorAt(mx-width/2.,my-height/2.,radius);
        if(selected>=0&&selected!=applied){
            ModNetwork.CHANNEL.sendToServer(new ToolTypeModeSwitchPacket(TOOLS[selected]));
            applied=selected;
        }
        g.flush();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        for(int i=0;i<TOOLS.length;i++){
            double a=i*Math.PI/3-Math.PI/2;
            sector(g,a,26,radius,i==selected?0xe13b88b8:0xd4263747);
        }
        RenderSystem.disableBlend();
        for(int i=0;i<TOOLS.length;i++){
            double a=i*Math.PI/3-Math.PI/2;
            int x=width/2+(int)(Math.cos(a)*(radius+26)*.5),y=height/2+(int)(Math.sin(a)*(radius+26)*.5);
            g.drawCenteredString(font,TOOLS[i].getTooltip(),x,y-4,i==selected?0xffffff:0xc6d5df);
        }
        g.drawCenteredString(font,Component.translatable("gui.useless_mod.gt_wheel.title"),width/2,height/2-4,0xffffff);
    }
    public static int sectorAt(double dx,double dy,double radius){
        double distance=Math.hypot(dx,dy);
        return distance<26||distance>radius?-1:Math.floorMod((int)Math.floor((Math.atan2(dy,dx)+Math.PI/2+Math.PI/6)/(Math.PI/3)),TOOLS.length);
    }
    private void sector(GuiGraphics g,double center,double inner,double outer,int color){
        var buffer=Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP,DefaultVertexFormat.POSITION_COLOR);
        var matrix=g.pose().last().pose();
        for(int step=0;step<=40;step++){
            double a=center-Math.PI/6+.016+(Math.PI/3-.032)*step/40;
            for(double r:new double[]{inner,outer})buffer.vertex(matrix,(float)(width/2.+Math.cos(a)*r),(float)(height/2.+Math.sin(a)*r),0)
                .color(color>>16&255,color>>8&255,color&255,color>>>24).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());
    }
    @Override public boolean keyReleased(int key,int scan,int mods){if(key==releaseKey){onClose();return true;}return super.keyReleased(key,scan,mods);}
    @Override public boolean mouseClicked(double x,double y,int button){return true;}
    @Override public boolean isPauseScreen(){return false;}
}
