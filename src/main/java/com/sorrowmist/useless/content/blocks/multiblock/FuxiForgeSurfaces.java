package com.sorrowmist.useless.content.blocks.multiblock;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Exposed material faces generated from the same blueprint as construction. */
public final class FuxiForgeSurfaces {
    public record Surface(int x,int y,int z,int face,int station,int style,double phase) {}
    private FuxiForgeSurfaces() {}
    private static final class Holder { static final List<Surface> FACES=load(); }
    public static List<Surface> faces() { return Holder.FACES; }

    private static List<Surface> load() {
        var faces=new ArrayList<Surface>();
        try(var input=FuxiForgeSurfaces.class.getResourceAsStream("/data/useless_mod/structures/omniversal_furnace_surfaces.csv")) {
            if(input==null)throw new IllegalStateException("Missing forge material surface blueprint");
            try(var reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))) {
                for(String line;(line=reader.readLine())!=null;) {
                    if(line.isBlank()||line.startsWith("#"))continue;
                    String[] f=line.split(",");
                    faces.add(new Surface(Integer.parseInt(f[0]),Integer.parseInt(f[1]),Integer.parseInt(f[2]),
                            Integer.parseInt(f[3]),Integer.parseInt(f[4]),Integer.parseInt(f[5]),Double.parseDouble(f[6])));
                }
            }
        } catch(java.io.IOException e) { throw new IllegalStateException("Cannot read forge material surfaces",e); }
        return List.copyOf(faces);
    }
}
