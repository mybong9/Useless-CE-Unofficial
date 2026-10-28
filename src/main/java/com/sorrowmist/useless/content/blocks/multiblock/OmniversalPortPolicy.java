package com.sorrowmist.useless.content.blocks.multiblock;

/** Twelve bottom-level ports, including the two mandatory module types. */
public final class OmniversalPortPolicy {
    public static final int CAPACITY = 12;
    private OmniversalPortPolicy() {}
    public static boolean isPort(int x,int y,int z) {
        return y==0 && ((z==0 && (Math.abs(x)==1||Math.abs(x)==2))
                || (Math.abs(x)==3 && z>=1 && z<=4));
    }
    public static boolean validCounts(int assemblies,int moldHubs,int passiveHatches) {
        return assemblies>0 && moldHubs>0 && passiveHatches>=0
                && (long) assemblies + moldHubs + passiveHatches <= CAPACITY;
    }
}
