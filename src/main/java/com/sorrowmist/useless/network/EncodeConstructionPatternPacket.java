package com.sorrowmist.useless.network;

import appeng.core.definitions.AEItems;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.items.OmniversalConstructionPatternItem;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.PatternEncodingTermMenuAccess;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.PendingOmniversalPatternHolder;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import com.sorrowmist.uselesscore.api.network.CorePacketContext;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** The client selects only a tier; all items and quantities come from the server blueprint. */
public record EncodeConstructionPatternPacket(int containerId,int tier) implements CoreCustomPacketPayload {
    public static final Type<EncodeConstructionPatternPacket> TYPE=new Type<>(UselessMod.id("encode_construction_pattern"));
    public static final CoreStreamCodec<FriendlyByteBuf,EncodeConstructionPatternPacket> STREAM_CODEC=CoreStreamCodec.of(
            (buf,p)->{buf.writeVarInt(p.containerId);buf.writeVarInt(p.tier);},
            buf->new EncodeConstructionPatternPacket(buf.readVarInt(),buf.readVarInt()));
    public static void handle(EncodeConstructionPatternPacket packet,CorePacketContext context) {
        context.enqueueWork(()->{
            if(packet.tier<1||packet.tier>10||!(context.player() instanceof ServerPlayer player)
                    ||!(player.containerMenu instanceof PatternEncodingTermMenu menu)||menu.containerId!=packet.containerId
                    ||!menu.stillValid(player)||!(menu instanceof PatternEncodingTermMenuAccess access))return;
            var logic=access.uselessMod$getEncodingLogic();
            if(logic instanceof PendingOmniversalPatternHolder holder) {
                holder.uselessMod$setPendingOmniversalRecipe(null);holder.uselessMod$setPendingOmniversalSourceId(null);
            }
            var output=logic.getEncodedPatternInv();var blank=logic.getBlankPatternInv();
            if(!output.getStackInSlot(0).isEmpty()) {
                player.displayClientMessage(Component.translatable("gui.useless_mod.omniversal_pattern.take_encoded_first"),true);return;
            }
            if(!AEItems.BLANK_PATTERN.isSameAs(blank.getStackInSlot(0))) {
                player.displayClientMessage(Component.translatable("gui.useless_mod.omniversal_pattern.insert_blank"),true);return;
            }
            var result=OmniversalConstructionPatternItem.encode(packet.tier);
            blank.extractItem(0,1,false);output.setItemDirect(0,result);
            logic.saveChanges();menu.broadcastChanges();
        });
    }
    @Override public Type<EncodeConstructionPatternPacket> type(){return TYPE;}
}
