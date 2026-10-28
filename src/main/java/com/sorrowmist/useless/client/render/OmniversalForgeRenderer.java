package com.sorrowmist.useless.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.blockentities.multiblock.MultiblockAlloyFurnaceCoreBlockEntity;
import com.sorrowmist.useless.content.blocks.multiblock.MultiblockAlloyFurnaceCoreBlock;
import com.sorrowmist.useless.content.blocks.multiblock.MultiblockFurnaceActivity;
import com.sorrowmist.useless.content.blocks.multiblock.FuxiForgeEffects;
import com.sorrowmist.useless.content.blocks.multiblock.FuxiForgeSurfaces;
import com.sorrowmist.useless.content.blocks.multiblock.XianxiaForgeFormation;
import com.sorrowmist.useless.init.ModBlockEntities;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** Low-poly emissive voxel effects. No bloom/shader pack or per-part block entities required. */
public final class OmniversalForgeRenderer implements BlockEntityRenderer<MultiblockAlloyFurnaceCoreBlockEntity> {
    private static final float[][] CORNERS = {{-1,-1,-1},{1,-1,-1},{1,1,-1},{-1,1,-1},
            {-1,-1,1},{1,-1,1},{1,1,1},{-1,1,1}};
    private static final int[][] FACES = {{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{0,1,5,4},{3,7,6,2}};
    private static final int[][] NORMALS={{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{0,1,0}};
    private static final int[][] RIGHTS={{0,0,-1},{0,0,1},{1,0,0},{-1,0,0},{1,0,0}};
    private static final int[][] DOWNS={{0,-1,0},{0,-1,0},{0,-1,0},{0,-1,0},{0,0,1}};
    public OmniversalForgeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MultiblockAlloyFurnaceCoreBlockEntity core, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        var state = core.getBlockState();
        if (core.getLevel() == null || !state.getValue(MultiblockAlloyFurnaceCoreBlock.FORMED)) return;
        boolean running = state.getValue(MultiblockAlloyFurnaceCoreBlock.ACTIVITY) == MultiblockFurnaceActivity.RUN;
        boolean waiting = state.getValue(MultiblockAlloyFurnaceCoreBlock.ACTIVITY) == MultiblockFurnaceActivity.WAIT;
        double time = core.getLevel().getGameTime() + partialTick;
        Direction facing = state.getValue(MultiblockAlloyFurnaceCoreBlock.FACING);
        Direction right = facing.getClockWise(), back = facing.getOpposite();
        // Express all vertices in canonical local space, then rotate around the controller center.
        Matrix4f transform = new Matrix4f(
                right.getStepX(), 0, right.getStepZ(), 0,
                0, 1, 0, 0,
                back.getStepX(), 0, back.getStepZ(), 0,
                .5F, .5F, .5F, 1);
        pose.pushPose();
        pose.mulPoseMatrix(transform);
        var consumer = buffers.getBuffer(ForgeEffectRenderType.EFFECT);
        Matrix4f matrix = pose.last().pose();
        // Material animation is present as soon as the multiblock forms. One
        // batched overlay follows the actual exposed faces, without extra BEs.
        for(var surface:FuxiForgeSurfaces.faces()) {
            int color=surface.style()==2?0xe8c076:FuxiForgeEffects.COLORS[surface.station()];
            float red=((color>>16)&255)/255F,green=((color>>8)&255)/255F,blue=(color&255)/255F;
            materialPanel(consumer,matrix,surface,time,running,red,green,blue);
        }
        float pulse = (float) (.75 + .15 * Math.sin(time * .035));
        float r = waiting ? .9F : .3F, g = waiting ? .58F : .9F, b = waiting ? .25F : .84F;
        for (int station = 0; station < 8; station++) {
            int color = FuxiForgeEffects.COLORS[station];
            float red=((color>>16)&255)/255F,green=((color>>8)&255)/255F,blue=(color&255)/255F;
            int samples = running ? 18 : 1;
            if (running) {
                for (int segment=0; segment<48; segment++) {
                    var a=FuxiForgeEffects.sample(station,time,segment/48.0);
                    var c=FuxiForgeEffects.sample(station,time,(segment+1)/48.0);
                    ribbon(consumer,matrix,a,c,.035F,red,green,blue,.055F);
                }
            }
            for (int i = 0; i < samples; i++) {
                int packet=i/6,tail=i%6;
                double progress = running ? FuxiForgeEffects.packetProgress(station,time,packet,tail) : .005;
                var p = FuxiForgeEffects.sample(station,time,progress);
                float taper=1-tail/6F;
                voxel(consumer, matrix, p.x(),p.y(),p.z(), running ? .12F-tail*.012F : .09F,
                        red,green,blue,running ? .82F*taper : .28F);
                if(running && progress>=.006) {
                    var next=FuxiForgeEffects.sample(station,time,progress-.006);
                    ribbon(consumer,matrix,p,next,.09F*taper,red,green,blue,.6F*taper);
                    ribbon(consumer,matrix,p,next,.24F*taper,red,green,blue,.08F*taper);
                }
            }
            for (int packet=0; packet<(running?3:1); packet++) {
                var p=FuxiForgeEffects.bridgeSample(station,(time*(running?.008:.0015)+station*.097+packet/3.0)%1);
                column(consumer,matrix,p.x(),p.y(),p.z(),.15F,.035F,.15F,red,green,blue,running?.75F:.22F);
            }
        }
        if (running) {
            // Compact crystal, with six orbiting facets, gives the converging streams a focus.
            voxel(consumer,matrix,.5,34,1.5,.3F,.88F,.96F,1F,.88F);
            for (int i=0; i<6; i++) {
                double a=i*Math.PI/3+time*.018,radius=.72+.08*Math.sin(time*.045);
                voxel(consumer,matrix,.5+Math.cos(a)*radius,34+Math.sin(a*2)*.42,
                        1.5+Math.sin(a)*radius,.14F,.56F,.86F,1F,.65F);
            }
            for (int orbit=0; orbit<2; orbit++) for(int i=0;i<72;i++) {
                if(i%24>=16)continue;
                var a=FuxiForgeEffects.orbitSample(orbit,time,i*Math.PI/36);
                var c=FuxiForgeEffects.orbitSample(orbit,time,(i+1)*Math.PI/36);
                ribbon(consumer,matrix,a,c,.045F,.65F,.88F,1F,.36F);
                ribbon(consumer,matrix,a,c,.14F,.65F,.88F,1F,.06F);
            }
            // A low scanning halo leaves the flush trigram dial unobstructed.
            for(int i=0;i<96;i++) {
                if(i%24>=14)continue;
                double a=i*Math.PI/48+time*.007,c=(i+1)*Math.PI/48+time*.007;
                ribbon(consumer,matrix,new FuxiForgeEffects.Point(.5+Math.cos(a)*6.35,22.66,1.5+Math.sin(a)*6.35),
                        new FuxiForgeEffects.Point(.5+Math.cos(c)*6.35,22.66,1.5+Math.sin(c)*6.35),.045F,.45F,.88F,1F,.38F);
            }
        }
        float completion = core.getCompletionPulse(partialTick);
        if (completion > 0) {
            float fade = Math.min(1F,(1-completion)*8)*completion;
            column(consumer,matrix,.5,69,1.5,.16F,32F,.16F,1F,.96F,.78F,fade*.9F);
            column(consumer,matrix,.5,69,1.5,.45F,32F,.45F,.35F,.85F,1F,fade*.3F);
            column(consumer,matrix,.5,69,1.5,.85F,32F,.85F,.7F,.65F,1F,fade*.12F);
            double age=1-completion;
            float shockFade=(float)Math.max(0,1-age*2.5);
            for(int i=0;i<64;i++) {
                double a=i*Math.PI/32,c=(i+1)*Math.PI/32,radius=2+age*18;
                ribbon(consumer,matrix,new FuxiForgeEffects.Point(.5+Math.cos(a)*radius,33.8,1.5+Math.sin(a)*radius),
                        new FuxiForgeEffects.Point(.5+Math.cos(c)*radius,33.8,1.5+Math.sin(c)*radius),.1F,.7F,.9F,1F,shockFade*.65F);
            }
        }
        for (int ring = 0; ring < (running ? 3 : 2); ring++) {
            double speed = running ? .022 : .003;
            double phase = time * speed * (ring % 2 == 0 ? 1 : -1);
            double radius = 5.2 + ring * .8;
            double y = 10 + ring * 4.5;
            for (int i = 0; i < 72; i++) {
                if(i%24>=18)continue;
                double a=i*Math.PI/36+phase,c=(i+1)*Math.PI/36+phase;
                var start=new FuxiForgeEffects.Point(.5+Math.cos(a)*radius,
                        y+(running?Math.sin(a*2+phase)*.35:0),1.5+Math.sin(a)*radius);
                var end=new FuxiForgeEffects.Point(.5+Math.cos(c)*radius,
                        y+(running?Math.sin(c*2+phase)*.35:0),1.5+Math.sin(c)*radius);
                ribbon(consumer,matrix,start,end,.035F,r,g,b,pulse*(running?.3F:.18F));
            }
        }
        fireball(consumer,matrix,time,running);
        XianxiaForgeFormation.draw(time,running,completion,new XianxiaForgeFormation.Sink() {
            @Override public void line(double ax,double ay,double az,double bx,double by,double bz,double width,int color,double alpha) {
                formationLine(consumer,matrix,ax,ay,az,bx,by,bz,width,color,alpha);
            }
            @Override public void mote(double x,double y,double z,double size,int color,double alpha) {
                voxel(consumer,matrix,x,y,z,(float)size,((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,(float)alpha);
            }
        });
        pose.popPose();
    }

    private static void formationLine(VertexConsumer out,Matrix4f matrix,double ax,double ay,double az,
            double bx,double by,double bz,double width,int color,double alpha) {
        double dx=bx-ax,dz=bz-az,length=Math.hypot(dx,dz);
        double ox=length>.0001?-dz/length*width:width,oz=length>.0001?dx/length*width:0;
        float r=((color>>16)&255)/255F,g=((color>>8)&255)/255F,b=(color&255)/255F;
        out.vertex(matrix,(float)(ax-ox),(float)ay,(float)(az-oz)).color(r,g,b,(float)alpha).endVertex();
        out.vertex(matrix,(float)(bx-ox),(float)by,(float)(bz-oz)).color(r,g,b,(float)alpha).endVertex();
        out.vertex(matrix,(float)(bx+ox),(float)by,(float)(bz+oz)).color(r,g,b,(float)alpha).endVertex();
        out.vertex(matrix,(float)(ax+ox),(float)ay,(float)(az+oz)).color(r,g,b,(float)alpha).endVertex();
        if(length>.0001) {
            out.vertex(matrix,(float)ax,(float)(ay-width),(float)az).color(r,g,b,(float)alpha*.5F).endVertex();
            out.vertex(matrix,(float)bx,(float)(by-width),(float)bz).color(r,g,b,(float)alpha*.5F).endVertex();
            out.vertex(matrix,(float)bx,(float)(by+width),(float)bz).color(r,g,b,(float)alpha*.5F).endVertex();
            out.vertex(matrix,(float)ax,(float)(ay+width),(float)az).color(r,g,b,(float)alpha*.5F).endVertex();
        }
    }

    private static void fireball(VertexConsumer out,Matrix4f matrix,double time,boolean running) {
        // Bounded, texture-free geometry shares the existing additive batch.
        for(int layer=0;layer<3;layer++)for(int lat=0;lat<8;lat++)for(int lon=0;lon<24;lon++) {
            for(int corner=0;corner<4;corner++) {
                double latitude=-Math.PI/2+(lat+(corner==1||corner==2?1:0))*Math.PI/8;
                double longitude=(lon+(corner>=2?1:0))*Math.PI/12;
                var p=FuxiForgeEffects.fireSurface(layer,time,latitude,longitude,running);
                float heat=(float)FuxiForgeEffects.fireHeat(time,latitude,longitude);
                float green=layer==0?.65F+heat*.25F:layer==1?.24F+heat*.48F:.18F+heat*.2F;
                float blue=layer==0?.25F+heat*.2F:layer==1?.015F+heat*.13F:.012F;
                float alpha=(layer==0?.3F+heat*.12F:layer==1?.24F+heat*.14F:.025F+heat*.025F)*(running?1:.6F);
                out.vertex(matrix,(float)p.x(),(float)p.y(),(float)p.z()).color(1,green,blue,alpha).endVertex();
            }
        }
        for(int streak=0;streak<10;streak++)for(int segment=0;segment<18;segment++) {
            double p=segment/18.0,q=(segment+1)/18.0;
            fireStrip(out,matrix,streak,time,p,q,running,1,.36F);
            fireStrip(out,matrix,streak,time,p,q,running,2.1F,.045F);
        }
        for(int i=0;i<(running?32:12);i++) {
            double p=FuxiForgeEffects.fireEmberPhase(i,time,running);
            var point=FuxiForgeEffects.fireEmber(i,time,running);
            float alpha=(float)Math.pow(Math.sin(p*Math.PI),2)*(running?.75F:.34F);
            voxel(out,matrix,point.x(),point.y(),point.z(),(float)(.075-.045*p),1,.55F+(float)p*.3F,.12F,alpha);
        }
    }

    private static void fireStrip(VertexConsumer out,Matrix4f matrix,int streak,double time,double start,double end,
                                  boolean running,float spread,float opacity) {
        for(int corner=0;corner<4;corner++) {
            double p=corner==0||corner==3?start:end;
            var point=FuxiForgeEffects.fireTrail(streak,time,p,running);
            double dx=point.x()-.5,dz=point.z()-1.5,length=Math.hypot(dx,dz);
            double taper=Math.sin(p*Math.PI)*(1-p*.65),width=.30*taper*(running?1:.76)*spread;
            double sign=corner<2?-1:1;
            float alpha=(float)(taper*opacity*(running?1:.55));
            out.vertex(matrix,(float)(point.x()-dz/length*width*sign),(float)point.y(),
                            (float)(point.z()+dx/length*width*sign))
                    .color(1,.30F+(float)(1-p)*.45F,.03F,alpha).endVertex();
        }
    }

    private static void voxel(VertexConsumer out, Matrix4f matrix, double x, double y, double z,
                              float h, float r, float g, float b, float a) {
        column(out,matrix,x,y,z,h,h,h,r,g,b,a);
    }

    private static void materialPanel(VertexConsumer out,Matrix4f matrix,FuxiForgeSurfaces.Surface s,
                                      double time,boolean running,float red,float green,float blue) {
        int[] n=NORMALS[s.face()],r=RIGHTS[s.face()],d=DOWNS[s.face()];
        double cx=s.x()+n[0]*.508,cy=s.y()+n[1]*(s.style()==2?.008:.508),cz=s.z()+n[2]*.508;
        for(int i=0;i<4;i++) {
            float u=(i==0||i==3?-.5F:.5F),v=(i<2?-.5F:.5F);
            double x=cx+r[0]*u+d[0]*v,y=cy+r[1]*u+d[1]*v,z=cz+r[2]*u+d[2]*v;
            double phase=FuxiForgeEffects.materialPhase(x,y,z,s.style(),s.station());
            float alpha=(float)FuxiForgeEffects.materialGlow(time,phase,running)*1.5F;
            out.vertex(matrix,(float)x,(float)y,(float)z)
                    .color(red,green,blue,alpha).endVertex();
        }
    }

    private static void ribbon(VertexConsumer out, Matrix4f matrix,FuxiForgeEffects.Point a,FuxiForgeEffects.Point b,
                               float width,float red,float green,float blue,float alpha) {
        double dx=b.x()-a.x(),dz=b.z()-a.z(),length=Math.max(.0001,Math.hypot(dx,dz));
        float ox=(float)(-dz/length*width),oz=(float)(dx/length*width);
        out.vertex(matrix,(float)a.x()-ox,(float)a.y(),(float)a.z()-oz).color(red,green,blue,alpha).endVertex();
        out.vertex(matrix,(float)b.x()-ox,(float)b.y(),(float)b.z()-oz).color(red,green,blue,alpha).endVertex();
        out.vertex(matrix,(float)b.x()+ox,(float)b.y(),(float)b.z()+oz).color(red,green,blue,alpha).endVertex();
        out.vertex(matrix,(float)a.x()+ox,(float)a.y(),(float)a.z()+oz).color(red,green,blue,alpha).endVertex();
    }

    private static void column(VertexConsumer out, Matrix4f matrix, double x, double y, double z,
                              float hx,float hy,float hz,float r,float g,float b,float a) {
        for (int[] face : FACES) for (int index : face) {
            float[] p = CORNERS[index];
            out.vertex(matrix, (float)x+p[0]*hx, (float)y+p[1]*hy, (float)z+p[2]*hz)
                    .color(r,g,b,a).endVertex();
        }
    }

    @Override public boolean shouldRenderOffScreen(MultiblockAlloyFurnaceCoreBlockEntity core) { return true; }
    @Override public boolean shouldRender(MultiblockAlloyFurnaceCoreBlockEntity core, net.minecraft.world.phys.Vec3 camera) {
        var box = core.getRenderBoundingBox();
        double dx = Math.max(Math.max(box.minX - camera.x, camera.x - box.maxX), 0);
        double dy = Math.max(Math.max(box.minY - camera.y, camera.y - box.maxY), 0);
        double dz = Math.max(Math.max(box.minZ - camera.z, camera.z - box.maxZ), 0);
        var frustum = net.minecraft.client.Minecraft.getInstance().levelRenderer.getFrustum();
        return dx * dx + dy * dy + dz * dz < 192 * 192
                && (box.contains(camera) || frustum == null || frustum.isVisible(box));
    }
    @Override public int getViewDistance() { return 192; }

    @Mod.EventBusSubscriber(modid = UselessMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.MULTIBLOCK_ALLOY_FURNACE_CORE.get(), OmniversalForgeRenderer::new);
        }
    }
}

