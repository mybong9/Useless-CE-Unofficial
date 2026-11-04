package com.sorrowmist.useless.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import com.sorrowmist.uselesscore.api.enums.tool.ToolTypeMode;
import com.sorrowmist.useless.init.ModNetwork;
import com.sorrowmist.useless.network.ToolTypeModeSwitchPacket;

public final class GtToolWheelScreen extends Screen {
    private static final ToolTypeMode[] TOOLS={ToolTypeMode.WRENCH_MODE,ToolTypeMode.SCREWDRIVER_MODE,
        ToolTypeMode.MALLET_MODE,ToolTypeMode.CROWBAR_MODE,ToolTypeMode.HAMMER_MODE,ToolTypeMode.NONE_MODE};
    private final int releaseKey;
    private int selected=-1;
    public GtToolWheelScreen(int releaseKey){super(Component.translatable("key.useless_mod.gt_tool_wheel"));this.releaseKey=releaseKey;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        double dx=mx-width/2.,dy=my-height/2.;
        selected=Math.hypot(dx,dy)<22?-1:Math.floorMod((int)Math.floor((Math.atan2(dy,dx)+Math.PI/2+Math.PI/6)/(Math.PI/3)),6);
        for(int i=0;i<TOOLS.length;i++){
            double a=i*Math.PI/3-Math.PI/2;int x=width/2+(int)(Math.cos(a)*90),y=height/2+(int)(Math.sin(a)*70);
            g.fill(x-47,y-12,x+47,y+12,i==selected?0xdd367fab:0xdd263747);
            g.drawCenteredString(font,TOOLS[i].getTooltip(),x,y-4,0xffffff);
        }
        g.drawCenteredString(font,title,width/2,height/2-4,0xffffff);
    }
    private void select(){if(selected>=0)ModNetwork.CHANNEL.sendToServer(new ToolTypeModeSwitchPacket(TOOLS[selected]));onClose();}
    @Override public boolean keyReleased(int key,int scan,int mods){if(key==releaseKey){select();return true;}return super.keyReleased(key,scan,mods);}
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0){select();return true;}return super.mouseClicked(x,y,button);}
    @Override public boolean isPauseScreen(){return false;}
}
