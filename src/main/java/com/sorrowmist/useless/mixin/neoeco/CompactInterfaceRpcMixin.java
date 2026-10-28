package com.sorrowmist.useless.mixin.neoeco;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.lowdragmc.lowdraglib.networking.s2c.SPacketRPCMethodPayload;
import com.lowdragmc.lowdraglib.syncdata.rpc.RPCSender;
import com.sorrowmist.useless.compat.neoecoae.compact.ui.CompactInterfaceRouting;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
@Pseudo
@Mixin(targets="cn.dancingsnow.neoecoae.platform.sync.ManagedRpcC2SPacket",remap=false)
public abstract class CompactInterfaceRpcMixin {
    @WrapOperation(method="execute",at=@At(value="INVOKE",target="Lcom/lowdragmc/lowdraglib/networking/s2c/SPacketRPCMethodPayload;processPacket(Lnet/minecraft/world/level/block/entity/BlockEntity;Lcom/lowdragmc/lowdraglib/syncdata/rpc/RPCSender;)V"))
    private void useless$route(SPacketRPCMethodPayload packet, BlockEntity target, RPCSender sender, Operation<Void> original) {
        original.call(packet,CompactInterfaceRouting.resolve(target,
                cn.dancingsnow.neoecoae.platform.sync.NERpcSenders.asPlayer(sender)),sender);
    }
}
