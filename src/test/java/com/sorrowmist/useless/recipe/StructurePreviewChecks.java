package com.sorrowmist.useless.recipe;

import com.sorrowmist.useless.client.gui.structure.StructurePreviewCamera;
import com.sorrowmist.useless.content.blocks.multiblock.OmniversalPortPolicy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** Tests real blueprint positions and the camera's inverse projection, without a game world. */
final class StructurePreviewChecks {
    static void run() {
        check(OmniversalPortPolicy.validCounts(1,1,10),"all twelve ports may be occupied");
        check(OmniversalPortPolicy.validCounts(11,1,0),"eleven modules of one required type are legal");
        check(OmniversalPortPolicy.validCounts(1,11,0),"either required type may occupy eleven ports");
        check(!OmniversalPortPolicy.validCounts(1,1,11),"thirteenth module must be rejected");
        check(!OmniversalPortPolicy.validCounts(Integer.MAX_VALUE,Integer.MAX_VALUE,2),"sum must not overflow");
        check(!OmniversalPortPolicy.validCounts(0,1,0)&&!OmniversalPortPolicy.validCounts(1,0,0),"both mandatory modules are required");
        var ports=new ArrayList<int[]>();
        try(var input=StructurePreviewChecks.class.getResourceAsStream("/data/useless_mod/structures/omniversal_furnace.csv")) {
            if(input==null)throw new AssertionError("missing production blueprint");
            try(var reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))) {
                reader.lines().filter(s->!s.startsWith("#")&&!s.isBlank()).forEach(s->{
                    String[] fields=s.split(",");
                    if(fields[3].equals("INTERFACE"))ports.add(new int[]{Integer.parseInt(fields[0]),Integer.parseInt(fields[1]),Integer.parseInt(fields[2])});
                });
            }
        }catch(java.io.IOException error){throw new AssertionError(error);}
        check(ports.size()==12,"twelve actual replaceable blocks");
        for(var p:ports)check(OmniversalPortPolicy.isPort(p[0],p[1],p[2]),"all ports at the terminal's bottom level");
        check(ports.stream().anyMatch(p->p[0]==-1&&p[2]==0)&&ports.stream().anyMatch(p->p[0]==1&&p[2]==0),"preferred modules remain beside the terminal");

        check(!OmniversalPortPolicy.isPort(0,0,0)&&!OmniversalPortPolicy.isPort(3,0,5),"terminal and rear jade are not module slots");
        var billEntries=new ArrayList<com.sorrowmist.useless.content.multiblock.ConstructionMaterialBill.Entry>();
        for(int i=0;i<18;i++)billEntries.add(new com.sorrowmist.useless.content.multiblock.ConstructionMaterialBill.Entry("useless_mod:material_"+i,128+i*137));
        var bill=new com.sorrowmist.useless.content.multiblock.ConstructionMaterialBill(10,billEntries);
        int fullCount=billEntries.stream().mapToInt(com.sorrowmist.useless.content.multiblock.ConstructionMaterialBill.Entry::count).sum();
        billEntries.clear();
        check(bill.entries().size()==18&&bill.total()==fullCount&&bill.entries().get(17).count()==2457,"immutable bill retains more than nine inputs and quantities above 127");
        boolean rejected=false;
        try {new com.sorrowmist.useless.content.multiblock.ConstructionMaterialBill(11,bill.entries());}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"invalid coil tier rejected");
        var camera=new StructurePreviewCamera();
        for(double yaw:new double[]{-180,-35,0,90,179})for(double pitch:new double[]{-80,0,27,85}) {
            camera.yaw=yaw;camera.pitch=pitch;
            for(var p:ports) {
                var projected=camera.project(p[0]+.5,.5,p[2]+.5);
                double hit=StructurePreviewCamera.intersect(camera.ray(projected.x(),projected.y()),p[0],0,p[2],1);
                check(Double.isFinite(hit),"every port center can be picked after camera rotation");
            }
        }
        camera.reset();camera.yaw=0;camera.pitch=0;
        var point=camera.project(.5,.75,.5);var ray=camera.ray(point.x(),point.y());
        check(!Double.isFinite(StructurePreviewCamera.intersect(ray,0,0,0,.5)),"clicks above a slab must pass through");
        double near=StructurePreviewCamera.intersect(ray,0,0,0,1),far=StructurePreviewCamera.intersect(ray,0,0,3,1);
        check(near<far,"picking selects the visible front block");
        camera.rotate(10000,10000);check(camera.pitch==85,"pitch upper bound");
        camera.rotate(-10000,-10000);check(camera.pitch==-80,"pitch lower bound");
        camera.scroll(10000);check(camera.zoom==12,"zoom upper bound");
        camera.scroll(-10000);check(camera.zoom==.45,"zoom lower bound");
        System.out.println("Structure preview checks passed: 12 bottom ports, module capacity, rotated picking, slab gaps and camera limits.");
    }
    private static void check(boolean success,String message){if(!success)throw new AssertionError(message);}
}
