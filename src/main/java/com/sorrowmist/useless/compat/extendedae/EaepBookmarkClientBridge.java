package com.sorrowmist.useless.compat.extendedae;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.List;

/** Client-only entry: EAEP's normal lookup rejects generated recipe displays before sending any packet. */
public final class EaepBookmarkClientBridge {
    private EaepBookmarkClientBridge() {}
    public static boolean handle(Object bookmark, boolean substitutes, boolean fluidSubstitutes) {
        Object recipe;
        try {
            recipe = bookmark.getClass().getMethod("getRecipe").invoke(bookmark);
        } catch (ReflectiveOperationException | RuntimeException unsupported) { return false; }
        if (!(recipe instanceof AlloyFurnaceRecipeCatalog.Entry entry)) return false;
        try {
            Class<?> packetClass = Class.forName("com.extendedae_plus.network.pattern.CreateCtrlQPatternC2SPacket");
            Object packet = packetClass.getConstructor(ResourceLocation.class, boolean.class,
                    List.class, List.class, boolean.class, boolean.class, boolean.class).newInstance(
                    AlloyFurnaceRecipeCatalog.jeiSelectionId(entry), false, List.of(), List.of(),
                    true, substitutes, fluidSubstitutes);
            SimpleChannel channel = (SimpleChannel) Class.forName("com.extendedae_plus.init.ModNetwork")
                    .getField("CHANNEL").get(null);
            ExtendedAEPlusJeiCompat.presetAlloyFurnaceSearchKey();
            channel.sendToServer(packet);
            UselessMod.LOGGER.info("EAEP omniversal bookmark sent: recipe={}, source={}",
                    entry.identity().recipeId(), entry.sourceId());
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            UselessMod.LOGGER.error("Cannot send EAEP omniversal bookmark", failure);
            if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("gui.useless_mod.omniversal_pattern.eaep_failed"), true);
        }
        return true;
    }
}
