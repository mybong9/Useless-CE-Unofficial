package com.sorrowmist.useless.mixin;

import mekanism.common.tile.machine.TileEntityDigitalMiner;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(value = mekanism.common.tile.machine.TileEntityDigitalMiner.class, remap = false)
public class TileEntityDigitalMinerMixin {

    @Inject(
            method = "onUpdateServer",
            at = @At(
                    value = "FIELD",
                    target = "Lmekanism/common/tile/machine/TileEntityDigitalMiner;delayTicks:I",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void modifyDelayTicks(CallbackInfo ci) {
        // 获取被混入的实例并修改 delayTicks 为 1
        TileEntityDigitalMiner miner = (TileEntityDigitalMiner) (Object) this;

        try {
            Field delayTicksField = TileEntityDigitalMiner.class.getDeclaredField("delayTicks");
            delayTicksField.setAccessible(true);
            delayTicksField.set(miner, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
