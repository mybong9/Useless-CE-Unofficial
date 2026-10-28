package com.sorrowmist.useless.client.gui.structure;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sorrowmist.useless.content.blocks.multiblock.OmniversalAlloyFurnaceStructure;
import com.sorrowmist.useless.content.blocks.multiblock.OmniversalForgePartBlock;
import com.sorrowmist.useless.content.multiblock.OmniversalFurnaceMaterials;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import org.lwjgl.opengl.GL11;
import static com.sorrowmist.useless.content.blocks.multiblock.OmniversalAlloyFurnaceStructure.Part;

/** Actual baked block models, including directional faces, slabs and connected textures. */
final class StructurePreviewModel {
    private record Quad(OmniversalAlloyFurnaceStructure.Entry entry,BakedQuad quad,float shade,int tint) {}
    private record Projected(Quad source,float[] xy,double depth) {}
    private List<Quad> quads=List.of();
    private List<OmniversalAlloyFurnaceStructure.Entry> visible=List.of();
    private int cachedLayer=-2;
    private Object modelGeneration;
    private final int tier;
    private List<Projected> projected=List.of();
    private double projectedYaw=Double.NaN,projectedPitch,projectedX,projectedY,projectedZ;
    StructurePreviewModel(int tier) { this.tier=tier; }

    private void prepare(int layer) {
        var minecraft=Minecraft.getInstance();
        Object generation=minecraft.getModelManager().getMissingModel();
        if(layer==cachedLayer&&generation==modelGeneration)return;
        var world=new PreviewWorld();
        var entries=new ArrayList<OmniversalAlloyFurnaceStructure.Entry>();
        for(var entry:OmniversalAlloyFurnaceStructure.entries()) {
            if(entry.part()==Part.AIR || layer>=0&&entry.localPos().getY()!=layer)continue;
            var state=OmniversalFurnaceMaterials.state(entry,tier);
            if(state.hasProperty(OmniversalForgePartBlock.FORMED))state=state.setValue(OmniversalForgePartBlock.FORMED,true);
            world.states.put(entry.localPos(),state); entries.add(entry);
        }
        var built=new ArrayList<Quad>();
        var random=RandomSource.create(0);
        for(var entry:entries) {
            var pos=entry.localPos(); var state=world.getBlockState(pos);
            var model=minecraft.getBlockRenderer().getBlockModel(state);
            var data=model.getModelData(world,pos,state,ModelData.EMPTY);
            for(int f=0;f<=6;f++) {
                Direction direction=f==6?null:Direction.values()[f];
                if(direction!=null&&!Block.shouldRenderFace(state,world,pos,direction,pos.relative(direction)))continue;
                random.setSeed(state.getSeed(pos));
                for(var quad:model.getQuads(state,direction,random,data,null)) {
                    int tint=quad.isTinted()?minecraft.getBlockColors().getColor(state,world,pos,quad.getTintIndex()):0xFFFFFF;
                    built.add(new Quad(entry,quad,world.getShade(quad.getDirection(),quad.isShade()),tint));
                }
            }
        }
        visible=List.copyOf(entries);quads=List.copyOf(built);cachedLayer=layer;modelGeneration=generation;
        projectedYaw=Double.NaN;
    }

    OmniversalAlloyFurnaceStructure.Entry pick(int layer,StructurePreviewCamera camera,double x,double y) {
        prepare(layer);
        var ray=camera.ray(x,y);
        double nearest=Double.POSITIVE_INFINITY;
        OmniversalAlloyFurnaceStructure.Entry hit=null;
        for(var entry:visible) {
            var p=entry.localPos();
            double distance=StructurePreviewCamera.intersect(ray,p.getX(),p.getY(),p.getZ(),entry.part()==Part.TRIM?.5:1);
            if(distance<nearest) { nearest=distance;hit=entry; }
        }
        return hit;
    }

    void draw(GuiGraphics g,int layer,StructurePreviewCamera camera,boolean ports,
              OmniversalAlloyFurnaceStructure.Entry selected,OmniversalAlloyFurnaceStructure.Entry hovered) {
        prepare(layer);g.flush();
        int x=StructurePreview.VX,y=StructurePreview.VY,w=StructurePreview.VW,h=StructurePreview.VH;
        var top=g.pose().last().pose().transformPosition(new Vector3f(x,y,0));
        var bottom=g.pose().last().pose().transformPosition(new Vector3f(x+w,y+h,0));
        g.enableScissor((int)top.x,(int)top.y,(int)bottom.x,(int)bottom.y);
        project(camera);
        boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),blend=GL11.glIsEnabled(GL11.GL_BLEND);
        var oldShader=RenderSystem.getShader();
        float[] oldColor=RenderSystem.getShaderColor().clone();
        g.pose().pushPose();
        try {
            g.pose().translate(x+w/2.0+camera.panX,y+h/2.0+camera.panY,0);
            float scale=(float)(StructurePreview.SCALE*camera.zoom);
            g.pose().scale(scale,-scale,1);
            var matrix=g.pose().last().pose();
            // Render the actual baked faces as an ordered GUI mesh. This deliberately
            // has no font lightmap, shared text batch or dependency on JEI's depth buffer.
            RenderSystem.disableDepthTest();RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1,1,1,1);
            RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
            RenderSystem.setShaderTexture(0,InventoryMenu.BLOCK_ATLAS);
            var out=Tesselator.getInstance().getBuilder();
            out.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR_TEX);
            for(var face:projected) {
                var rendered=face.source;
                var e=rendered.entry;var data=rendered.quad.getVertices();int stride=data.length/4;
                float red=1,green=1,blue=1;
                if(e.equals(selected)) {green=.8F;blue=.25F;}
                else if(e.equals(hovered)) {red=.65F;blue=.75F;}
                else if(ports&&e.part()==Part.INTERFACE) {red=.25F;blue=.8F;}
                for(int corner=0;corner<4;corner++) {
                    int i=corner*stride,c=data[i+3];float shade=rendered.shade;
                    out.vertex(matrix,face.xy[corner*2],face.xy[corner*2+1],0)
                            .color(red*shade*(c&255)/255F*((rendered.tint>>16)&255)/255F,
                                    green*shade*((c>>8)&255)/255F*((rendered.tint>>8)&255)/255F,
                                    blue*shade*((c>>16)&255)/255F*(rendered.tint&255)/255F,1)
                            .uv(Float.intBitsToFloat(data[i+4]),Float.intBitsToFloat(data[i+5])).endVertex();
                }
            }
            BufferUploader.drawWithShader(out.end());
        } finally {
            g.pose().popPose();g.disableScissor();
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            RenderSystem.setShaderColor(oldColor[0],oldColor[1],oldColor[2],oldColor[3]);
            RenderSystem.setShader(()->oldShader);
        }
    }

    private void project(StructurePreviewCamera camera) {
        if(projectedYaw==camera.yaw&&projectedPitch==camera.pitch&&projectedX==camera.centerX
                &&projectedY==camera.centerY&&projectedZ==camera.centerZ)return;
        boolean[] front=new boolean[6];
        for(var d:Direction.values())front[d.ordinal()]=camera.project(camera.centerX+d.getStepX(),
                camera.centerY+d.getStepY(),camera.centerZ+d.getStepZ()).z()<0;
        var result=new ArrayList<Projected>();
        for(var rendered:quads) {
            if(!front[rendered.quad.getDirection().ordinal()])continue;
            var p=rendered.entry.localPos();var data=rendered.quad.getVertices();int stride=data.length/4;
            float[] xy=new float[8];double depth=0;
            for(int corner=0;corner<4;corner++) {
                int i=corner*stride;
                var v=camera.project(p.getX()+Float.intBitsToFloat(data[i]),p.getY()+Float.intBitsToFloat(data[i+1]),p.getZ()+Float.intBitsToFloat(data[i+2]));
                xy[corner*2]=(float)v.x();xy[corner*2+1]=(float)v.y();depth+=v.z();
            }
            result.add(new Projected(rendered,xy,depth/4));
        }
        result.sort(Comparator.comparingDouble(Projected::depth).reversed());
        projected=List.copyOf(result);projectedYaw=camera.yaw;projectedPitch=camera.pitch;
        projectedX=camera.centerX;projectedY=camera.centerY;projectedZ=camera.centerZ;
    }

    private static final class PreviewWorld implements BlockAndTintGetter {
        final Map<BlockPos,BlockState> states=new HashMap<>();
        public BlockState getBlockState(BlockPos pos){return states.getOrDefault(pos,Blocks.AIR.defaultBlockState());}
        public BlockEntity getBlockEntity(BlockPos pos){return null;}
        public FluidState getFluidState(BlockPos pos){return getBlockState(pos).getFluidState();}
        public int getHeight(){return 32;}
        public int getMinBuildHeight(){return 0;}
        public float getShade(Direction face,boolean shade){return !shade?1:switch(face){case UP->1;case DOWN->.5F;case EAST,WEST->.75F;default->.9F;};}
        public LevelLightEngine getLightEngine(){return null;}
        public int getBrightness(net.minecraft.world.level.LightLayer layer,BlockPos pos){return 15;}
        public int getRawBrightness(BlockPos pos,int darken){return 15;}
        public int getBlockTint(BlockPos pos,ColorResolver resolver){return 0xFFFFFF;}
    }
}
