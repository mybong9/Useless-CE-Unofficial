package com.sorrowmist.useless.content.blocks.multiblock;

/** Bounded client geometry for the celestial formation. No entities or server ticking. */
public final class XianxiaForgeFormation {
    private XianxiaForgeFormation() {}
    public interface Sink {
        void line(double ax,double ay,double az,double bx,double by,double bz,double width,int color,double alpha);
        void mote(double x,double y,double z,double size,int color,double alpha);
    }
    private static final String[] BITS={"111","011","101","001","000","100","010","110"};
    private static final int GOLD=0xEFD394,JADE=0x9EE6CF,WHITE=0xE8F9DE;
    private static void polar(Sink out,double radius,double y,double a,double b,double width,int color,double alpha) {
        out.line(.5+Math.cos(a)*radius,y,1.5+Math.sin(a)*radius,
                .5+Math.cos(b)*radius,y,1.5+Math.sin(b)*radius,width,color,alpha);
    }
    public static void draw(double time,boolean running,double completion,Sink out) {
        double power=running?1:.42;
        // Three counter-rotating concentric seals, fine ticks and eight trigram glyphs.
        for(int layer=0;layer<3;layer++) {
            double radius=layer==0?16:layer==1?21:29;
            double y=layer==0?11:layer==1?7.7:8.7;
            double phase=time*(running?.004:.0012)*(layer%2==0?1:-1);
            int color=layer==1?JADE:GOLD;
            for(int j=0;j<96;j++) {
                double a=j*Math.PI/48+phase,b=(j+1)*Math.PI/48+phase;
                polar(out,radius,y,a,b,.035,color,power*.44);
                if(j%12<9) polar(out,radius+.55,y,a,b,.018,color,power*.23);
                if(j%2==0) out.line(.5+Math.cos(a)*(radius-.22),y,1.5+Math.sin(a)*(radius-.22),
                        .5+Math.cos(a)*(radius-(j%8==0?.85:.48)),y,1.5+Math.sin(a)*(radius-(j%8==0?.85:.48)),.027,color,power*.5);
            }
            for(int i=0;i<8;i++) {
                double a=i*Math.PI/4+phase,ux=Math.cos(a),uz=Math.sin(a);
                for(int row=0;row<3;row++) {
                    double r=radius-1.4-row*.55;
                    for(int half=0;half<(BITS[i].charAt(row)=='1'?1:2);half++) {
                        double start=BITS[i].charAt(row)=='1'?-1.15:half==0?-1.15:.32;
                        double end=BITS[i].charAt(row)=='1'?1.15:half==0?-.32:1.15;
                        out.line(.5+ux*r-uz*start,y,1.5+uz*r+ux*start,
                                .5+ux*r-uz*end,y,1.5+uz*r+ux*end,.07,color,power*.72);
                    }
                }
            }
        }
        // Two tall intersecting celestial meridians outside the bronze belly.
        for(int orbit=0;orbit<2;orbit++) for(int j=0;j<128;j++) {
            if(j%32>=25)continue;
            double a=j*Math.PI/64+time*.003*(orbit==0?1:-1),b=a+Math.PI/64;
            double turn=orbit*Math.PI/2+.35;
            double ax=Math.cos(a)*18,bx=Math.cos(b)*18;
            out.line(.5+ax*Math.cos(turn),17+Math.sin(a)*13,1.5+ax*Math.sin(turn),
                    .5+bx*Math.cos(turn),17+Math.sin(b)*13,1.5+bx*Math.sin(turn),.028,orbit==0?GOLD:JADE,power*.22);
        }
        // Two winding qi ribbons climb around the cauldron like ascending dragons.
        for(int strand=0;strand<2;strand++) for(int j=0;j<96;j++) {
            double p=j/96.0,q=(j+1)/96.0,a=p*Math.PI*4-time*.012+strand*Math.PI,b=q*Math.PI*4-time*.012+strand*Math.PI;
            double r=13.8+Math.sin(p*Math.PI)*1.5,s=13.8+Math.sin(q*Math.PI)*1.5;
            double fade=Math.pow(Math.sin(p*Math.PI),.6)*power;
            out.line(.5+Math.cos(a)*r,8+p*26,1.5+Math.sin(a)*r,.5+Math.cos(b)*s,8+q*26,1.5+Math.sin(b)*s,.065,strand==0?GOLD:JADE,fade*.38);
            out.line(.5+Math.cos(a)*r,8+p*26,1.5+Math.sin(a)*r,.5+Math.cos(b)*s,8+q*26,1.5+Math.sin(b)*s,.3,strand==0?GOLD:JADE,fade*.035);
        }
        // Floating talisman sheets drawn as open borders and cinnabar strokes.
        for(int i=0;i<(running?16:8);i++) {
            double a=i*Math.PI/8+time*(running?.004:.0015),ux=-Math.sin(a),uz=Math.cos(a);
            double x=.5+Math.cos(a)*16.8,z=1.5+Math.sin(a)*16.8,y=18.2+Math.sin(a*3-time*.018)*2.1;
            for(int side=-1;side<=1;side+=2) {
                out.line(x+ux*.55*side,y-1.1,z+uz*.55*side,x+ux*.55*side,y+1.1,z+uz*.55*side,.025,GOLD,power*.48);
                out.line(x-ux*.55,y+1.1*side,z-uz*.55,x+ux*.55,y+1.1*side,z+uz*.55,.025,GOLD,power*.48);
            }
            out.line(x,y-.82,z,x,y+.82,z,.045,0xFFB577,power*.8);
            for(int row=0;row<4;row++) {
                double yy=y+.6-row*.4,w=row%2==0?.38:.25;
                out.line(x-ux*w,yy,z-uz*w,x+ux*w,yy,z+uz*w,.04,0xFFB577,power*.8);
                out.line(x+ux*w,yy,z+uz*w,x+ux*(w-.14),yy-.22,z+uz*(w-.14),.035,0xFFB577,power*.7);
            }
        }
        // Eight sword-shaped light wisps circle the outer formation.
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4+time*(running?.006:.0018),x=.5+Math.cos(a)*34,z=1.5+Math.sin(a)*34;
            double y=13.5+Math.sin(a*2)*2,ux=-Math.sin(a),uz=Math.cos(a);
            out.line(x-ux,y,z-uz,x+ux*2.1,y+.4,z+uz*2.1,.08,WHITE,power*.8);
            out.line(x+Math.cos(a)*.48,y,z+Math.sin(a)*.48,x-Math.cos(a)*.48,y,z-Math.sin(a)*.48,.06,GOLD,power*.75);
            for(int j=0;j<12;j++) {
                double t=a-j*.014;
                polar(out,34,y,t,t-.014,.07,JADE,power*.24*(1-j/12.0));
            }
        }
        // Thin cloud ribbons below the formation; no opaque smoke curtain.
        for(int arc=0;arc<5;arc++) for(int j=0;j<72;j++) {
            double a=j*Math.PI/60+arc*1.7+time*.001*(arc%2==0?1:-1),b=a+Math.PI/60;
            double radius=18+arc*3.4,y=3.7+Math.sin(a*3+arc)*.65;
            polar(out,radius,y,a,b,.28+Math.sin(j*Math.PI/72)*.42,JADE,.012*power);
        }
        for(int i=0;i<(running?192:48);i++) {
            double phase=i*.61803398875+time*(running?.0035:.0012),p=phase-Math.floor(phase);
            double a=i*2.399963+time*.0015,r=14+(i%13)*1.55;
            double alpha=Math.pow(Math.sin(p*Math.PI),2)*power*.7;
            out.mote(.5+Math.cos(a+p*.25)*r,6+p*23,1.5+Math.sin(a+p*.25)*r,.045+(i%3)*.018,i%3==0?GOLD:JADE,alpha);
        }
        // Actual completed crafting triggers a brief tribulation flash and rising rings.
        if(completion>0) {
            double age=1-completion,fade=Math.min(1,age*8)*completion;
            for(int ring=0;ring<3;ring++) for(int j=0;j<96;j++) {
                double radius=3+age*(32+ring*8),a=j*Math.PI/48,b=(j+1)*Math.PI/48;
                polar(out,radius,24+ring*4+age*12,a,b,.06,ring==1?JADE:GOLD,fade*.65);
            }
            if(completion>.6) for(int i=0;i<18;i++) {
                double x=.5+Math.sin(i*2.7+time*.9)*(i==0?0:.7),z=1.5+Math.cos(i*1.9+time*.7)*(i==0?0:.7);
                double nx=.5+Math.sin((i+1)*2.7+time*.9)*.7,nz=1.5+Math.cos((i+1)*1.9+time*.7)*.7;
                out.line(x,32+i*1.8,z,nx,33.8+i*1.8,nz,.09,WHITE,fade*.7);
            }
        }
    }
}
