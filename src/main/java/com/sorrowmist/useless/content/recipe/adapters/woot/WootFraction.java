package com.sorrowmist.useless.content.recipe.adapters.woot;
import java.math.*;
/** Exact rational arithmetic for loot probabilities and uniform counts. */
public record WootFraction(BigInteger n, BigInteger d) {
    public static final WootFraction ZERO=of(0), ONE=of(1);
    public WootFraction {
        if(d.signum()==0) throw new ArithmeticException("zero denominator");
        if(d.signum()<0){n=n.negate();d=d.negate();}
        BigInteger g=n.gcd(d);n=n.divide(g);d=d.divide(g);
    }
    public static WootFraction of(long n){return new WootFraction(BigInteger.valueOf(n),BigInteger.ONE);}
    public static WootFraction decimal(String s){BigDecimal v=new BigDecimal(s);int scale=Math.max(0,v.scale());return new WootFraction(v.movePointRight(scale).toBigIntegerExact(),BigInteger.TEN.pow(scale));}
    public WootFraction add(WootFraction b){return new WootFraction(n.multiply(b.d).add(b.n.multiply(d)),d.multiply(b.d));}
    public WootFraction multiply(WootFraction b){return new WootFraction(n.multiply(b.n),d.multiply(b.d));}
    public WootFraction divide(WootFraction b){return new WootFraction(n.multiply(b.d),d.multiply(b.n));}
    public static long batch(java.util.Collection<WootFraction> values){
        BigInteger size=BigInteger.ONE;
        for(var v:values)size=size.divide(size.gcd(v.d)).multiply(v.d);
        return size.intValueExact();
    }
    public int count(long batch){return n.multiply(BigInteger.valueOf(batch)).divide(d).intValueExact();}
}
