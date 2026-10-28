package com.sorrowmist.useless.mixin.accessor;

import net.darkhax.botanypots.block.BlockEntityBotanyPot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Random;

@Mixin(value = BlockEntityBotanyPot.class ,remap = false)
public interface BlockEntityBotanyPotAccessor {

    // 访问私有字段 rng
    @Accessor("rng")
    Random getRng();

    // 访问 protected 字段 doneGrowing
    @Accessor("doneGrowing")
    boolean isDoneGrowing();

    @Accessor("doneGrowing")
    void setDoneGrowing(boolean value);

}
