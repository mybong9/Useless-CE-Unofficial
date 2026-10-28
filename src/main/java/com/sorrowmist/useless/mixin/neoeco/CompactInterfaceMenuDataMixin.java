package com.sorrowmist.useless.mixin.neoeco;
import cn.dancingsnow.neoecoae.network.MenuDataTransport;
import cn.dancingsnow.neoecoae.gui.crafting.PatternPreviewCodec;
import cn.dancingsnow.neoecoae.blocks.entity.ECOMachineInterfaceBlockEntity;
import com.sorrowmist.useless.compat.neoecoae.compact.ui.CompactInterfaceRouting;
import com.sorrowmist.useless.compat.neoecoae.compact.ui.CompactUiHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Use the native pattern codec while resolving compact UI data to the same shadow used to build the menu. */
@Pseudo
@Mixin(targets="cn.dancingsnow.neoecoae.network.MenuDataCodecs",remap=false)
public abstract class CompactInterfaceMenuDataMixin {
    @Inject(method="receive",at=@At("HEAD"),cancellable=true)
    private static void useless$route(Player player, MenuDataTransport.Channel channel, FriendlyByteBuf buffer, CallbackInfo ci) {
        if(channel!=MenuDataTransport.Channel.PATTERNS)return;
        int start=buffer.readerIndex();
        var pos=buffer.readBlockPos();
        var blockEntity=player.level().getBlockEntity(pos);
        if(!(blockEntity instanceof CompactUiHost)) { buffer.readerIndex(start); return; }
        var payload=PatternPreviewCodec.read(buffer);
        var target=CompactInterfaceRouting.resolve(blockEntity,player);
        if(target instanceof ECOMachineInterfaceBlockEntity<?> machineInterface)
            machineInterface.getPatternPreviewSync().receive(payload);
        ci.cancel();
    }
}
