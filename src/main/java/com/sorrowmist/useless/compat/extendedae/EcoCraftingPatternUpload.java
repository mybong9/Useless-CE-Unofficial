package com.sorrowmist.useless.compat.extendedae;

import appeng.menu.me.items.PatternEncodingTermMenu;
import com.sorrowmist.useless.UselessMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.util.List;

/** Delegates inventory access, duplicate handling and upload bookkeeping to EAEP. */
public final class EcoCraftingPatternUpload {
    private EcoCraftingPatternUpload() {}

    public static void upload(PatternEncodingTermMenu menu, ServerPlayer player) {
        var mods = ModList.get();
        if (!mods.isLoaded("extendedae_plus") || !mods.isLoaded("neoecoae")) return;
        try {
            var util = Class.forName("com.extendedae_plus.util.uploadPattern.ProviderUploadUtil");
            var terminals = Class.forName("com.extendedae_plus.util.PatternTerminalUtil");
            var ecoProvider = Class.forName("cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity");
            var providers = (List<?>) terminals.getMethod("listAvailableProvidersFromGrid",
                    PatternEncodingTermMenu.class).invoke(null, menu);
            var upload = util.getMethod("uploadFromEncodingMenuToProviderByIndex",
                    ServerPlayer.class, PatternEncodingTermMenu.class, int.class);
            for (int index = 0; index < providers.size(); index++) {
                if (!ecoProvider.isInstance(providers.get(index))) continue;
                Object result = upload.invoke(null, player, menu, index);
                if (Boolean.TRUE.equals(result)
                        || result instanceof Enum<?> outcome && !outcome.name().equals("FAILED")) {
                    menu.broadcastChanges();
                    return;
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            UselessMod.LOGGER.debug("EAEP ECO crafting pattern upload unavailable", ex);
        }
    }
}
