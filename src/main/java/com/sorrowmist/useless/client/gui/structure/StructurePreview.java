package com.sorrowmist.useless.client.gui.structure;

import com.sorrowmist.useless.content.blocks.multiblock.OmniversalAlloyFurnaceStructure;
import com.sorrowmist.useless.content.multiblock.OmniversalFurnaceMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import static com.sorrowmist.useless.content.blocks.multiblock.OmniversalAlloyFurnaceStructure.Part;

/** One interactive structure, with a live coil selector and a lossless construction bill. */
public final class StructurePreview {
    public static final int WIDTH=350,HEIGHT=246,VX=4,VY=24,VW=274,VH=104,INPUT_HEIGHT=HEIGHT;
    public static final double SCALE=1.7;
    public static float viewerScale() {
        var window=Minecraft.getInstance().getWindow();
        return Math.min(1F,Math.max(.35F,Math.min((window.getGuiScaledWidth()-90F)/WIDTH,(window.getGuiScaledHeight()-80F)/HEIGHT)));
    }
    public static int viewerWidth(){return (int)Math.ceil(WIDTH*viewerScale());}
    public static int viewerHeight(){return (int)Math.ceil(HEIGHT*viewerScale());}
    private static final int CANDIDATES_PER_PAGE=14;
    private int tier;
    private final StructurePreviewCamera camera=new StructurePreviewCamera();
    private StructurePreviewModel model;
    private List<OmniversalFurnaceMaterials.Requirement> materials;
    private final Map<Part,List<ItemStack>> candidates=new EnumMap<>(Part.class);
    private final BiConsumer<ItemStack,Boolean> openRecipe;
    private OmniversalAlloyFurnaceStructure.Entry selected,hovered;
    private int layer=-1,candidatePage,dragButton=-1;
    private double pressX,pressY,dragDistance;
    private boolean highlightPorts;

    public StructurePreview(int tier,BiConsumer<ItemStack,Boolean> openRecipe) {
        this.openRecipe=openRecipe;setTier(tier);
        // The initial candidate row is useful even before the first model click.
        selectDefaultPort();
    }
    public int tier(){return tier;}
    public void setTier(int tier) {
        if(tier<1||tier>10)throw new IllegalArgumentException("Invalid coil tier");
        this.tier=tier;model=new StructurePreviewModel(tier);
        materials=OmniversalFurnaceMaterials.requirements(tier);
    }
    public void showSavedMaterials(List<OmniversalFurnaceMaterials.Requirement> saved){materials=List.copyOf(saved);}
    public void encode() {
        var player=Minecraft.getInstance().player;
        if(player==null)return;
        if(player.containerMenu instanceof appeng.menu.me.items.PatternEncodingTermMenu menu)
            com.sorrowmist.useless.init.ModNetwork.CHANNEL.sendToServer(new com.sorrowmist.useless.network.EncodeConstructionPatternPacket(menu.containerId,tier));
        else player.displayClientMessage(text("encode_help"),true);
    }
    private void selectDefaultPort() {
        selected=OmniversalAlloyFurnaceStructure.entries().stream().filter(e->e.localPos().equals(
                OmniversalAlloyFurnaceStructure.PREFERRED_PATTERN_ASSEMBLY_POS)).findFirst().orElseThrow();
        candidatePage=0;
    }
    private void selectLayer(int target) {
        if(highlightPorts)camera.reset();
        highlightPorts=false;
        camera.centerY=target<0?14:target+.5;
        layer=target;dragButton=-1;
        if(selected!=null&&target>=0&&selected.localPos().getY()!=target) {selected=null;candidatePage=0;}
    }
    public static MutableComponent text(String key,Object...args) {return Component.translatable("jei.useless_mod.structure."+key,args);}
    public static int materialX(int index){return 6+index%9*38;}
    public static int materialY(int index){return 211+index/9*18;}
    public List<OmniversalFurnaceMaterials.Requirement> materials(){return materials;}
    private List<ItemStack> candidates(){return selected==null?List.of():candidates.computeIfAbsent(selected.part(),OmniversalFurnaceMaterials::candidates);}

    public void draw(GuiGraphics g,double mouseX,double mouseY) {
        var font=Minecraft.getInstance().font;
        g.fill(0,0,WIDTH,HEIGHT,0xFF15252A);
        button(g,4,4,18,"<",false);button(g,126,4,18,">",false);
        fitText(g,text("tier_short",tier),28,7,94,0xEAD9A0);
        g.drawString(font,text("dimensions"),158,7,0xA1B6B8,false);
        g.fill(VX,VY,VX+VW,VY+VH,0xFF0D171E);
        hovered=inViewport(mouseX,mouseY)?pick(mouseX,mouseY):null;
        model.draw(g,layer,camera,highlightPorts,selected,hovered);
        button(g,282,24,64,text("all").getString(),layer<0);
        for(int i=0;i<32;i++)button(g,282+i%4*16,40+i/4*11,15,Integer.toString(i+1),layer==i,10);
        button(g,4,132,44,text("reset").getString(),false);
        button(g,52,132,72,text("port_focus").getString(),highlightPorts);
        fitText(g,text("gesture_short"),132,135,112,0xA1B6B8);
        button(g,250,132,96,text("encode").getString(),false);
        if(selected!=null) {
            var pos=selected.localPos();var options=candidates();
            var title=text(options.size()==1?"only_block":"available_blocks");
            g.drawString(font,title,5,151,0xEAD9A0,false);
            String coordinates="("+pos.getX()+", "+pos.getY()+", "+pos.getZ()+")";
            g.drawString(font,coordinates,WIDTH-5-font.width(coordinates),151,0xA1B6B8,false);
            int start=candidatePage*CANDIDATES_PER_PAGE;
            for(int i=start;i<Math.min(options.size(),start+CANDIDATES_PER_PAGE);i++) {
                int x=6+(i-start)*20;
                g.fill(x-1,163,x+17,182,0xFF30464A);g.renderItem(options.get(i),x,164);
            }
            if(options.size()>CANDIDATES_PER_PAGE) {
                button(g,290,164,24,"<",false);button(g,318,164,24,">",false);
            }
            var rule=selected.part()==Part.INTERFACE?text("port_rule"):
                    selected.part()==Part.COIL?text("coil_rule"):text("candidate_hint");
            fitText(g,rule,5,185,340,0xA1B6B8);
        } else g.drawString(font,text("select_block"),5,164,0xA1B6B8,false);
        g.fill(4,196,346,197,0xFF3E5355);
        g.drawString(font,text("materials"),5,200,0xEAD9A0,false);
        var total=text("total",materials.stream().mapToInt(OmniversalFurnaceMaterials.Requirement::count).sum());
        g.drawString(font,total,345-font.width(total),200,0xA1B6B8,false);
        for(int i=0;i<materials.size();i++) {
            int x=materialX(i),y=materialY(i);
            g.fill(x-1,y-1,x+17,y+17,0xFF30464A);
            g.renderItem(materials.get(i).stack(),x,y);
            fitText(g,Component.literal(Integer.toString(materials.get(i).count())),x+18,y+5,18,0xE7DEC5);
        }
    }

    public List<Component> tooltip(double x,double y) {
        int material=materialAt(x,y);
        if(material>=0) {
            var r=materials.get(material);int size=r.stack().getMaxStackSize();
            return List.of(r.stack().getHoverName(),text("amount",r.count()).withStyle(ChatFormatting.GOLD),
                    text("stacks",r.count()/size,r.count()%size).withStyle(ChatFormatting.GRAY),text("candidate_hint"));
        }
        int candidate=candidateAt(x,y);
        if(candidate>=0) {
            var stack=candidates().get(candidate);
            var lines=new java.util.ArrayList<Component>();lines.add(stack.getHoverName());
            if(selected.part()==Part.INTERFACE) {
                var block=net.minecraft.world.level.block.Block.byItem(stack.getItem());
                var type=OmniversalAlloyFurnaceStructure.functionalPart(block.defaultBlockState());
                if(type!=null)lines.add(text(switch(type){case PATTERN_ASSEMBLY->"assembly_rule";case MOLD_HUB->"mold_rule";case PASSIVE_HATCH->"passive_rule";}).withStyle(ChatFormatting.AQUA));
                else lines.add(text("filler_rule").withStyle(ChatFormatting.GRAY));
            }
            lines.add(text("candidate_hint").withStyle(ChatFormatting.GRAY));return lines;
        }
        if(inViewport(x,y)) {
            if(hovered!=null)return List.of(OmniversalFurnaceMaterials.state(hovered,tier).getBlock().getName(),text("click_block"));
            return List.of();
        }
        if(x>=4&&x<145&&y>=4&&y<18)return List.of(text("coil_rule"));
        if(x>=282&&y>=24&&y<128)return List.of(text("layers_help"));
        if(x>=250&&y>=132&&y<146)return List.of(text("encode_help"),text("bill_only"));
        if(x>=52&&x<124&&y>=132&&y<146)return List.of(text("ports_help"));
        if(y>=132&&y<146)return List.of(text("controls"));
        if(y>=183&&y<195&&selected!=null&&selected.part()==Part.INTERFACE)return List.of(text("ports_help"));
        return List.of();
    }

    public boolean mouseClicked(double x,double y,int button) {
        if(button==0&&y>=4&&y<18) {
            if(x>=4&&x<22){setTier(tier==1?10:tier-1);return true;}
            if(x>=126&&x<144){setTier(tier==10?1:tier+1);return true;}
        }
        if(button==0&&y>=24&&y<38&&x>=282&&x<346) {selectLayer(-1);return true;}
        if(button==0&&x>=282&&x<346&&y>=40&&y<128) {
            int column=(int)(x-282)/16,row=(int)(y-40)/11;
            if((x-282)%16<15&&(y-40)%11<10) {selectLayer(row*4+column);return true;}
        }
        if(button==0&&y>=132&&y<146) {
            if(x>=4&&x<48) {camera.reset();layer=-1;highlightPorts=false;return true;}
            if(x>=52&&x<124) {
                selectLayer(0);selectDefaultPort();highlightPorts=true;camera.reset();camera.centerX=.5;camera.centerY=.5;camera.centerZ=3.5;
                camera.zoom=6;camera.yaw=-25;camera.pitch=45;return true;
            }
            if(x>=250&&x<346){encode();return true;}
        }
        int material=materialAt(x,y);
        if(material>=0&&(button==0||button==1)){openRecipe.accept(materials.get(material).stack().copy(),button==1);return true;}
        int candidate=candidateAt(x,y);
        if(candidate>=0&&(button==0||button==1)) {openRecipe.accept(candidates().get(candidate).copy(),button==1);return true;}
        if(button==0&&y>=164&&y<180&&candidates().size()>CANDIDATES_PER_PAGE) {
            int pages=(candidates().size()+CANDIDATES_PER_PAGE-1)/CANDIDATES_PER_PAGE;
            if(x>=290&&x<314) {candidatePage=(candidatePage+pages-1)%pages;return true;}
            if(x>=318&&x<342) {candidatePage=(candidatePage+1)%pages;return true;}
        }
        if(inViewport(x,y)&&button>=0&&button<=2) {
            dragButton=button;pressX=x;pressY=y;dragDistance=0;return true;
        }
        return false;
    }
    public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(dragButton!=button)return false;
        dragDistance+=Math.abs(dx)+Math.abs(dy);
        if(button==0)camera.rotate(dx,dy);
        else {camera.panX=Math.max(-VW,Math.min(VW,camera.panX+dx));camera.panY=Math.max(-VH,Math.min(VH,camera.panY+dy));}
        return true;
    }
    public boolean mouseReleased(double x,double y,int button) {
        if(dragButton!=button)return false;dragButton=-1;
        if(button==0&&dragDistance<4&&Math.hypot(x-pressX,y-pressY)<4&&inViewport(x,y)) {
            selected=pick(x,y);candidatePage=0;
        }
        return true;
    }
    public boolean mouseScrolled(double x,double y,double delta) {
        if(!inViewport(x,y))return false;camera.scroll(delta);return true;
    }
    private OmniversalAlloyFurnaceStructure.Entry pick(double x,double y) {
        return model.pick(layer,camera,(x-VX-VW/2.0-camera.panX)/(SCALE*camera.zoom),
                -(y-VY-VH/2.0-camera.panY)/(SCALE*camera.zoom));
    }
    private static boolean inViewport(double x,double y){return x>=VX&&x<VX+VW&&y>=VY&&y<VY+VH;}
    private int candidateAt(double x,double y) {
        if(y<164||y>=180||x<6||x>=286||(x-6)%20>=16)return -1;
        int i=candidatePage*CANDIDATES_PER_PAGE+(int)(x-6)/20;
        return i<candidates().size()?i:-1;
    }
    private static void button(GuiGraphics g,int x,int y,int width,String label,boolean selected) {
        button(g,x,y,width,label,selected,14);
    }
    private static void button(GuiGraphics g,int x,int y,int width,String label,boolean selected,int height) {
        var font=Minecraft.getInstance().font;g.fill(x,y,x+width,y+height,selected?0xFF827245:0xFF30464A);
        fitText(g,Component.literal(label),x+Math.max(1,(width-font.width(label))/2),y+(height-8)/2,width-2,0xE7DEC5);
    }
    private int materialAt(double x,double y) {
        for(int i=0;i<materials.size();i++)if(x>=materialX(i)&&x<materialX(i)+36&&y>=materialY(i)&&y<materialY(i)+17)return i;
        return -1;
    }
    private static void fitText(GuiGraphics g,Component text,int x,int y,int width,int color) {
        var font=Minecraft.getInstance().font;float scale=Math.min(1F,(float)width/Math.max(1,font.width(text)));
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);
        g.drawString(font,text,0,0,color,false);g.pose().popPose();
    }
}
