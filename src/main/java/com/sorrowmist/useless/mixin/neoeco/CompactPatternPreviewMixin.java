package com.sorrowmist.useless.mixin.neoeco;

import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity;
import cn.dancingsnow.neoecoae.multiblock.cluster.NECraftingCluster;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sorrowmist.useless.compat.neoecoae.compact.cluster.CompactCraftingCluster;
import com.sorrowmist.useless.compat.neoecoae.compact.exposed.ExposedPatternBusBlockEntity;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.blocks.entity.ECOMachineInterfaceBlockEntity", remap = false)
public abstract class CompactPatternPreviewMixin {
    @WrapOperation(method = "ensurePatternInterfaceMapping", at = @At(value = "INVOKE",
            target = "Lcn/dancingsnow/neoecoae/multiblock/cluster/NECraftingCluster;getPatternDomainBuses()Ljava/util/List;"))
    private List<ECOCraftingPatternBusBlockEntity> useless$useUniqueBusPosition(
            NECraftingCluster cluster, Operation<List<ECOCraftingPatternBusBlockEntity>> original) {
        var buses = original.call(cluster);
        if (!(cluster instanceof CompactCraftingCluster)) return buses;
        // ECO addresses each displayed bus by block position; all F9 shadow buses share it.
        return buses.stream().filter(ExposedPatternBusBlockEntity.class::isInstance).toList();
    }
}
