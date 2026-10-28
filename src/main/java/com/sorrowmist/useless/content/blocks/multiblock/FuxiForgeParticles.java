package com.sorrowmist.useless.content.blocks.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class FuxiForgeParticles {
    private FuxiForgeParticles() {}
    public static void tick(Level level, BlockPos pos, Direction facing, boolean running) {
        if (!level.isClientSide) return;
        double time = level.getGameTime();
        Direction right = facing.getClockWise(), back = facing.getOpposite();
        // Cosmetic flames only; no projectile/entity, block fire or server work.
        if(level.getGameTime()%(running?3:12)==0) {
            int streak=(int)(level.getGameTime()/3%10);
            var p=FuxiForgeEffects.fireTrail(streak,time,.64,running);
            double x=pos.getX()+.5+right.getStepX()*p.x()+back.getStepX()*p.z();
            double z=pos.getZ()+.5+right.getStepZ()*p.x()+back.getStepZ()*p.z();
            level.addParticle(ParticleTypes.SMALL_FLAME,x,pos.getY()+.5+p.y(),z,0,running?.035:.012,0);
        }
        for (int station=0; station<8; station++) {
            // Spread emissions across ticks instead of flashing all eight stations at once.
            if(level.getGameTime()%(running?4:24)!=station%(running?4:24))continue;
            double progress = running ? FuxiForgeEffects.packetProgress(station,time,0,0) : .005;
            var p = FuxiForgeEffects.sample(station,time,progress);
            var next=FuxiForgeEffects.sample(station,time,Math.min(1,progress+.012));
            double vx=(next.x()-p.x())*.08,vy=(next.y()-p.y())*.08,vz=(next.z()-p.z())*.08;
            double x=pos.getX()+.5+right.getStepX()*p.x()+back.getStepX()*p.z();
            double z=pos.getZ()+.5+right.getStepZ()*p.x()+back.getStepZ()*p.z();
            ParticleOptions kind = !running ? ParticleTypes.ENCHANT : switch(station) {
                case 0 -> ParticleTypes.END_ROD;
                case 1 -> ParticleTypes.WHITE_ASH;
                case 2 -> ParticleTypes.FLAME;
                case 3 -> ParticleTypes.ELECTRIC_SPARK;
                case 4 -> new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.YELLOW_TERRACOTTA.defaultBlockState());
                case 5 -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BROWN_TERRACOTTA.defaultBlockState());
                case 6 -> ParticleTypes.DRIPPING_WATER;
                default -> ParticleTypes.CLOUD;
            };
            level.addParticle(kind,x,pos.getY()+.5+p.y(),z,
                    running?right.getStepX()*vx+back.getStepX()*vz:0,running?vy:.005,
                    running?right.getStepZ()*vx+back.getStepZ()*vz:0);
            if (running && (level.getGameTime()-station)%12==0) {
                int color=FuxiForgeEffects.COLORS[station];
                var tint=new org.joml.Vector3f(((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F);
                level.addParticle(new net.minecraft.core.particles.DustParticleOptions(tint,.55F),
                        x,pos.getY()+.5+p.y(),z,0,.015,0);
            }
        }
    }
}
