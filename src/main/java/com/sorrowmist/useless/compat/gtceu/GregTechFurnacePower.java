package com.sorrowmist.useless.compat.gtceu;

import com.sorrowmist.useless.content.blockentities.multiblock.MultiblockAlloyFurnaceCoreBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.lang.reflect.Field;
import java.util.*;

/** Reads the native hatch containers, including GTMThings subclasses. */
public final class GregTechFurnacePower {
    private GregTechFurnacePower(){}
    public static boolean energyPart(Object machine){
        return GregTechParts.isA(machine,"com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine")
            || GregTechParts.isA(machine,"com.gregtechceu.gtceu.common.machine.multiblock.part.LaserHatchPartMachine")
            || GregTechParts.isA(machine,"com.hepdd.gtmthings.common.block.machine.multiblock.part.WirelessEnergyHatchPartMachine")
            || GregTechParts.isA(machine,"com.hepdd.gtmthings.common.block.machine.multiblock.part.WirelessLaserHatchPartMachine")
            || GregTechParts.isA(machine,"com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeEnergyHatchPartMachine")
            || GregTechParts.isA(machine,"com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeLaserHatchPartMachine");
    }
    private static Object field(Object target,String name) throws ReflectiveOperationException{
        for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(NoSuchFieldException next){}
        throw new NoSuchFieldException(name);
    }
    public static boolean inputPart(Object machine){
        if(!energyPart(machine))return false;
        try{return field(machine,"io").toString().equals("IN");}catch(ReflectiveOperationException failure){return false;}
    }
    public static Object container(MultiblockAlloyFurnaceCoreBlockEntity core)throws ReflectiveOperationException{
        List<Object> containers=new ArrayList<>();
        var api=Class.forName("com.gregtechceu.gtceu.api.capability.IEnergyContainer");
        for(var pos:core.getGregTechParts()){
            var machine=GregTechParts.machine(core.getLevel(),pos);
            if(!inputPart(machine)||!GregTechParts.isFurnacePart(machine))continue;
            if(!Boolean.TRUE.equals(GregTechParts.call(machine,"isWorkingEnabled")))continue;
            for(Class<?> c=machine.getClass();c!=null;c=c.getSuperclass())for(Field f:c.getDeclaredFields()){
                if(java.lang.reflect.Modifier.isStatic(f.getModifiers())||!api.isAssignableFrom(f.getType()))continue;
                f.setAccessible(true);Object value=f.get(machine);
                if(value!=null&&!containers.contains(value))containers.add(value);
            }
        }
        return Class.forName("com.gregtechceu.gtceu.api.misc.EnergyContainerList").getConstructor(List.class).newInstance(containers);
    }
    public static long maxVoltage(Object container)throws ReflectiveOperationException{
        long highest=((Number)GregTechParts.call(container,"getHighestInputVoltage")).longValue();
        int count=((Number)GregTechParts.call(container,"getNumHighestInputContainers")).intValue();
        Class<?> values;
        try{values=Class.forName("com.gregtechceu.gtceu.api.GTValues");}catch(ClassNotFoundException old){values=Class.forName("com.gregtechceu.gtceu.GTValues");}
        long[] voltages=(long[])values.getField("V").get(null);
        return promotedVoltage(highest,count,voltages);
    }
    public static long promotedVoltage(long highest,int count,long[] voltages){
        if(highest<=0||count<2)return highest;
        int tier=0;while(tier<voltages.length-1&&highest>voltages[tier])tier++;
        return voltages[Math.min(tier+1,voltages.length-1)];
    }
    public static void tick(MultiblockAlloyFurnaceCoreBlockEntity core){
        if(core.getGregTechParts().isEmpty())return;
        try{
            Object container=container(core);int ratio=fePerEU();
            long voltage=((Number)GregTechParts.call(container,"getInputVoltage")).longValue();
            long amps=((Number)GregTechParts.call(container,"getInputAmperage")).longValue();
            long rate=amps>0&&voltage>Long.MAX_VALUE/amps?Long.MAX_VALUE:Math.max(0,voltage*amps);
            long available=((Number)GregTechParts.call(container,"getEnergyStored")).longValue();
            long request=Math.min(Math.min(rate,available),core.getEnergyManager().receiveEnergy(Long.MAX_VALUE,true)/ratio);
            if(request<=0)return;
            long removed=-((Number)container.getClass().getMethod("changeEnergy",long.class).invoke(container,-request)).longValue();
            if(removed>0){long accepted=core.getEnergyManager().receiveEnergy(removed*ratio,false);
                if(accepted<removed*ratio){
                    core.getEnergyManager().modifyEnergyStored(-(accepted%ratio));
                    container.getClass().getMethod("changeEnergy",long.class).invoke(container,removed-accepted/ratio);
                }
                core.setChanged();}
        }catch(ReflectiveOperationException | RuntimeException | LinkageError failure){com.sorrowmist.useless.UselessMod.LOGGER.debug("GT energy hatch interface unavailable",failure);}
    }
    public static int fePerEU()throws ReflectiveOperationException{
        Object holder=Class.forName("com.gregtechceu.gtceu.config.ConfigHolder").getField("INSTANCE").get(null);
        return com.sorrowmist.useless.content.recipe.adapters.gtceu.GregTechEnergyCompat.fePerEU(field(field(holder,"compat"),"energy"));
    }
    public static Map<?,?> coils()throws ReflectiveOperationException{
        return (Map<?,?>)Class.forName("com.gregtechceu.gtceu.api.GTCEuAPI").getField("HEATING_COILS").get(null);
    }
    public static Ingredient coilMold()throws ReflectiveOperationException{
        List<ItemStack> choices=new ArrayList<>();
        for(Object supplier:coils().values()){
            Object block=((java.util.function.Supplier<?>)supplier).get();
            if(block instanceof net.minecraft.world.level.block.Block b)choices.add(new ItemStack(b));
        }
        return Ingredient.of(choices.stream());
    }
    public static int coilTemperature(ItemStack stack)throws ReflectiveOperationException{
        if(!(stack.getItem() instanceof net.minecraft.world.item.BlockItem item))return 0;
        for(var entry:coils().entrySet())if(((java.util.function.Supplier<?>)entry.getValue()).get()==item.getBlock())
            return ((Number)GregTechParts.call(entry.getKey(),"getCoilTemperature")).intValue();
        return 0;
    }
}
