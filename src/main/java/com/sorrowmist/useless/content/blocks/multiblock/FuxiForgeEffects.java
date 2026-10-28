package com.sorrowmist.useless.content.blocks.multiblock;

/** Deterministic effect paths in controller-local block coordinates; no client dependencies. */
public final class FuxiForgeEffects {
    public static final int[] COLORS = {0xF4D28A,0xC8EFD6,0xFF9860,0x78DBB2,0xEAC17B,0xD9BA87,0x85BCEB,0x99E0CB};
    public static final int[] X = {0,23,32,23,1,-22,-32,-22};
    public static final int[] Z = {-30,-21,1,24,34,24,2,-21};
    public static final int[] HEAD = {18,20,22,23,23,22,20,18};
    private FuxiForgeEffects() {}
    public record Point(double x, double y, double z) {}

    public static Point sample(int station, double time, double progress) {
        double p = Math.max(0, Math.min(1, progress));
        double envelope = Math.sin(p * Math.PI);
        double x = X[station] + (.5-X[station])*p;
        double z = Z[station] + (1.5-Z[station])*p;
        double y = HEAD[station] + (34-HEAD[station])*p + envelope*3.2;
        double phase = p*Math.PI*6 - time*.018;
        double dx=.5-X[station],dz=1.5-Z[station],length=Math.hypot(dx,dz);
        double side=0, rise=0;
        switch (station) {
            case 0 -> { side=Math.sin(phase)*.22; rise=Math.cos(phase)*.16; }
            case 1 -> { side=Math.sin(phase)*.65; rise=Math.sin(phase*2)*.16; }
            case 2 -> { side=Math.sin(phase*1.5)*.38; rise=Math.sin(phase*2)*.35; }
            case 3 -> { side=Math.asin(Math.sin(phase*1.6))*.48; rise=Math.cos(phase)*.12; }
            case 4 -> { side=Math.sin(p*Math.PI*2)*.35; rise=-.5; }
            case 5 -> { side=Math.asin(Math.sin(phase))*.25; rise=Math.asin(Math.sin(phase*1.2))*.2; }
            case 6 -> { side=Math.sin(phase)*.8; rise=Math.cos(phase)*.45; }
            case 7 -> { side=Math.cos(phase)*1.2; rise=Math.sin(phase)*.7; }
            default -> throw new IllegalArgumentException("Unknown trigram station " + station);
        }
        return new Point(x-dz/length*side*envelope,y+rise*envelope,z+dx/length*side*envelope);
    }

    /** Stagger the eight stations; each luminous head carries a short tapered wake. */
    public static double packetProgress(int station,double time,int packet,int tail) {
        double p=time*.0055+station*.097+packet/3.0-tail*.006;
        return p-Math.floor(p);
    }

    public static Point orbitSample(int orbit,double time,double angle) {
        double a=angle+time*.012*(orbit==0?1:-1),radius=orbit==0?2.2:3.1;
        return new Point(.5+Math.cos(a)*radius,33.7+Math.sin(a)*.65*(orbit==0?1:-1),1.5+Math.sin(a)*radius);
    }

    public static double materialScan(double time,double phase,boolean running) {
        double value=time*(running?.014:.0035)+phase;
        return value-Math.floor(value);
    }

    public static double materialGlow(double time,double phase,boolean running) {
        double scan=materialScan(time,phase,running);
        return .035+(running?.13:.055)*(.5+.5*Math.sin(scan*Math.PI*2));
    }

    /** World-aligned phase makes adjoining material faces share edge luminance. */
    public static double materialPhase(double x,double y,double z,int style,int station) {
        if(style==1) return y*.055-station*.097;
        if(style==2) return Math.atan2(z-1.5,x-.5)/(2*Math.PI);
        return Math.hypot(x-.5,z-1.5)*.065;
    }

    public static Point bridgeSample(int station, double progress) {
        double dx=X[station]-.5,dz=Z[station]-1.5;
        double length=Math.hypot(dx,dz);
        double socket=switch(station) {case 1,5,7 -> 6.0;case 3 -> 4.6;case 4 -> 5.9;default -> 4.9;};
        double radius=(length-socket)*(1-progress)+10*progress;
        // Spirit pulses leap above the separate jade stepping stones.
        return new Point(.5+dx/length*radius,8.68,1.5+dz/length*radius);
    }

    public static double fireHeat(double time,double latitude,double longitude) {
        return Math.max(0,Math.min(1,.55+.26*Math.sin(longitude*3+latitude*4-time*.09)
                +.18*Math.cos(longitude*5-latitude*2+time*.065)));
    }

    /** Three nested emissive shells; the outer surface rolls without leaving the chamber. */
    public static Point fireSurface(int layer,double time,double latitude,double longitude,boolean running) {
        double scale=running?1:.76,speed=running?1:.45;
        double radius=switch(layer) {case 0 -> 1.15;case 1 -> 2.05;default -> 2.48;};
        double warp=layer==0?0:.075*Math.sin(longitude*5+latitude*3-time*.055*speed)
                +.045*Math.cos(longitude*3-latitude*5+time*.04*speed);
        radius=(radius+warp*Math.cos(latitude))*scale*(1+.035*Math.sin(time*.075*speed));
        double angle=longitude+time*.012*speed,ring=Math.cos(latitude)*radius;
        return new Point(.5+Math.cos(angle)*ring,18.4+Math.sin(latitude)*radius
                +Math.sin(time*.04)*.14,1.5+Math.sin(angle)*ring);
    }

    /** Ten rising, curling flame tongues, tapered by the renderer at both ends. */
    public static Point fireTrail(int streak,double time,double progress,boolean running) {
        double p=Math.max(0,Math.min(1,progress)),scale=running?1:.76,speed=running?1:.45;
        double angle=streak*Math.PI/5+time*.035*speed+p*1.8+.18*Math.sin(p*7+streak-time*.045*speed);
        double radius=(1.55+1.12*Math.sin(p*Math.PI)+.15*Math.sin(time*.06+streak+p*8))*scale;
        double y=18.4+(-1.8+p*4.9)*scale+.15*Math.sin(time*.045+streak+p*4);
        return new Point(.5+Math.cos(angle)*radius,y,1.5+Math.sin(angle)*radius);
    }

    public static double fireEmberPhase(int index,double time,boolean running) {
        double phase=index*.61803398875+time*(running?.009:.0035);
        return phase-Math.floor(phase);
    }

    public static Point fireEmber(int index,double time,boolean running) {
        double p=fireEmberPhase(index,time,running),angle=index*2.399963+time*(running?.025:.012);
        double radius=(2.15+.35*Math.sin(index*1.7))*(1-p*.6)*(running?1:.76);
        return new Point(.5+Math.cos(angle+p)*radius,18.7+p*5.7,1.5+Math.sin(angle+p)*radius);
    }
}
