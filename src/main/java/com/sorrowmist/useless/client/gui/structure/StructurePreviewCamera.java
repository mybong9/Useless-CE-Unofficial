package com.sorrowmist.useless.client.gui.structure;

/** Orthographic camera math, independent of the game so projection/picking can be regression-tested. */
public final class StructurePreviewCamera {
    public double yaw=-35, pitch=27, zoom=1, panX, panY;
    public double centerX=1, centerY=14, centerZ=2;
    public record Point(double x,double y,double z) {}
    public record Ray(Point origin,Point direction) {}
    public void reset() { yaw=-35; pitch=27; zoom=1; panX=panY=0; centerX=1; centerY=14; centerZ=2; }
    public void rotate(double dx,double dy) { yaw=(yaw+dx*.65)%360; pitch=Math.max(-80,Math.min(85,pitch+dy*.5)); }
    public void scroll(double amount) { zoom=Math.max(.45,Math.min(12,zoom*Math.pow(1.15,amount))); }
    public Point project(double x,double y,double z) {
        double a=Math.toRadians(yaw), b=Math.toRadians(-pitch);
        double rx=Math.cos(a)*(x-centerX)+Math.sin(a)*(z-centerZ);
        double rz=-Math.sin(a)*(x-centerX)+Math.cos(a)*(z-centerZ);
        return new Point(rx,Math.cos(b)*(y-centerY)-Math.sin(b)*rz,Math.sin(b)*(y-centerY)+Math.cos(b)*rz);
    }
    private Point inverse(double x,double y,double z) {
        double a=Math.toRadians(yaw), b=Math.toRadians(-pitch);
        double ry=Math.cos(b)*y+Math.sin(b)*z, rz=-Math.sin(b)*y+Math.cos(b)*z;
        return new Point(Math.cos(a)*x-Math.sin(a)*rz,ry,Math.sin(a)*x+Math.cos(a)*rz);
    }
    /** Input coordinates are relative to the viewport center, in scaled world units. */
    public Ray ray(double x,double y) {
        Point p=inverse(x,y,-1024), d=inverse(0,0,1);
        return new Ray(new Point(p.x+centerX,p.y+centerY,p.z+centerZ),d);
    }
    public static double intersect(Ray ray,double x,double y,double z,double height) {
        double near=0, far=Double.POSITIVE_INFINITY;
        double[] origin={ray.origin.x,ray.origin.y,ray.origin.z};
        double[] direction={ray.direction.x,ray.direction.y,ray.direction.z};
        double[] min={x,y,z},max={x+1,y+height,z+1};
        for(int i=0;i<3;i++) {
            if(Math.abs(direction[i])<1e-10) { if(origin[i]<min[i]||origin[i]>max[i])return Double.POSITIVE_INFINITY; }
            else {
                double a=(min[i]-origin[i])/direction[i], b=(max[i]-origin[i])/direction[i];
                near=Math.max(near,Math.min(a,b)); far=Math.min(far,Math.max(a,b));
                if(near>far)return Double.POSITIVE_INFINITY;
            }
        }
        return near;
    }
}
